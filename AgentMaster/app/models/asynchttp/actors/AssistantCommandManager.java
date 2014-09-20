/*  

Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package models.asynchttp.actors;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

import scala.concurrent.duration.Duration;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.ContinueToSendToBatchSenderAsstManager;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.agent.batch.commands.message.RequestToBatchSenderAsstManager;
import models.agent.batch.commands.message.ResponseCountToBatchSenderAsstManager;
import models.asynchttp.RequestProtocol;
import models.asynchttp.request.AgentRequest;
import models.asynchttp.request.GenericAgentRequest;
import models.asynchttp.response.AgentResponse;
import models.asynchttp.response.GenericAgentResponse;
import models.data.AgentCommandMetadata;
import models.data.NodeData;
import models.data.NodeDataCmdType;
import models.data.NodeGroupDataMap;
import models.data.NodeReqResponse;
import models.data.providers.AgentDataProvider;
import models.data.providers.AgentDataProviderHelper;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.LogUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Deploy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.remote.RemoteScope;
import RemoteCluster.ClusterState;
import RemoteCluster.JobStatus;
import RemoteCluster.SupermanApp;
import RemoteCluster.CommunicationMessages.ResetMaxConc;
import RemoteCluster.CommunicationMessages.initOpMessage;
/**
 * 
 * 20131014; update major logic to enable concurrency control
 * 
 * 20130730 The assistant manager is soley for sending out requests in batch;
 * with interval based sleep 1. Enables IN PARALLEL sending batch requests and
 * geting response 2. Enables > 5K nodes per requests. The manager gives all
 * request sending task to this AgentCommandBatchSenderAsstManager. this guy
 * tells workers to directly reply back to the original manager. So that
 * requests and reply can happent the same time.
 * 
 * @author ypei
 * 
 */
public class AssistantCommandManager extends UntypedActor {

	protected int responseCount = 0;
	protected int requestTotalCount = 0;
	protected long startTime = System.currentTimeMillis();
	protected long endTime = -1L;
	protected ActorRef originalManager = null;

	protected List<ActorRef> workers = new ArrayList<ActorRef>();
	protected List<String> jobIdQ = null;
	protected List<NodeData> nodeDataQ = null;

	protected int oriMaxConcurrent = VarUtils.MAX_CONCURRENT_SEND_SIZE;
	protected int maxConcurrencyAdjusted = VarUtils.MAX_CONCURRENT_SEND_SIZE;
	protected int processedWorkerCount = 0;
	protected int trueProcessedWorkerCount = 0;
	protected String directorJobId = null;

	protected boolean localMode = true;
	
	/**
	 * @author chunyang
	 */
	private String agentCommandType;
	private AgentCommandMetadata agentCommandMetadata;
	private String resourcePathOrig = null;
	private String httpMethod = null;
	private String requestProtocol;
	private int requestPort;
	private boolean pollable;
	private String httpHeaderType;
	private long pauseIntervalWorkerMillis;
	private int sentRequestCounter = 0;
	
	private ActorRef createWorker(String target_node, NodeData nodeData, boolean localmode) {
		
			ActorRef worker = null;
			try {
	
				if (!nodeData.getDataMap().containsKey(
						agentCommandMetadata.getAgentCommandType())) {

					models.utils.LogUtils
							.printLogError("HAS NOT GENERATED AGENT COMMAND before sending Command!");
					NodeReqResponse nodeReqResponse = new NodeReqResponse();
					nodeReqResponse.setDefaultEmptyReqestContent();
					nodeData.getDataMap().put(
							agentCommandMetadata.getAgentCommandType(),
							nodeReqResponse);
				}

				NodeReqResponse nodeReqResponse = nodeData.getDataMap()
						.get(agentCommandMetadata.getAgentCommandType());

				// BECAREFUL NPE
				final String requestContentOrig = (nodeReqResponse == null || nodeReqResponse
						.getRequestParameters() == null) ? ""
						: nodeReqResponse.getRequestParameters().get(
								VarUtils.NODE_REQUEST_FULL_CONTENT_TYPE);

				final String requestContent = NodeReqResponse
						.replaceStrByMap(
								nodeReqResponse.getRequestParameters(),
								requestContentOrig);
				String resourcePathBeforeEncoding = NodeReqResponse
						.replaceStrByMap(
								nodeReqResponse.getRequestParameters(),
								resourcePathOrig);

				final String resourcePath = URLEncoder.encode(
						resourcePathBeforeEncoding, "UTF-8");

				// 1st. add the static template part
				final Map<String, String> httpHeaderMapLocal = MyHttpUtils
						.getHttpHeaderMapCopyFromHeaderMetadataMapStatic(
								httpHeaderType, requestProtocol);
				// 2nd, add the dynamic part (hard coded) from this logic;
				// based
				// on different httpHeaderType
				/*MyHttpUtils.addDynamicHeadersIntoHttpHeaderMap(
						httpHeaderMapLocal, httpHeaderType);*/
				// 3rd, add the dynamic part ; generic var based
				// replacement.
				// 20140310
				/*MyHttpUtils.replaceHttpHeaderMapNodeSpecific(
						httpHeaderMapLocal, httpHeaderType,
						nodeReqResponse.getRequestParameters());*/

				if (VarUtils.PRINT_HTTP_TRUE_HEADER_MAP) {

					for (Entry<String, String> headerEntry : httpHeaderMapLocal
							.entrySet()) {
						String headerKey = headerEntry.getKey();
						String headerValue = headerEntry.getValue();

						nodeReqResponse
								.getRequestParameters()
								.put(VarUtils.REQUEST_PARAMETER_HTTP_HEADER_PREFIX
										+ headerKey, headerValue);
					}

				}
				
				String target_node_new_replacing_old = nodeReqResponse
						.getRequestParameters()
						.get(VarUtils.VAR_NAME_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR_WHEN_CHECK);
				if (target_node_new_replacing_old != null) {

					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_TRUE_TARGET_NODE1,
							target_node_new_replacing_old);
				}

				/**
				 * Some hard coded logic for LBMS/UDNS use slow client. TODO
				 */
				final String hostUniform = (target_node_new_replacing_old == null) ? null
						: target_node_new_replacing_old;
				// 20130511 replacement:

				nodeReqResponse.getRequestParameters()
						.put(VarUtils.NODE_REQUEST_TRUE_CONTENT1,
								requestContent);

				// put the one before encoding
				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_TRUE_URL1, resourcePathBeforeEncoding);

				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_TRUE_PORT1,
						Integer.toString(requestPort));
				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_HTTP_METHOD1, httpMethod);

				if (requestProtocol.equalsIgnoreCase(RequestProtocol.SSH
						.toString())) {

					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_HTTP_HEADER_TYPE,
							VarUtils.STR_SSH_PASSWORD_MASKED);
				} else {

					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_HTTP_HEADER_TYPE,
							httpHeaderType);
				}

				long prepareRequestTime = System.currentTimeMillis();

				String prepareRequestTimeStr = DateUtils
						.getDateTimeStr(new Date(prepareRequestTime));
				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_PREPARE_TIME1,
						prepareRequestTimeStr);

				final long shouldPauseTimeForThisNodeMillis = (long) sentRequestCounter
						* pauseIntervalWorkerMillis;

				final int shouldTimeoutTimeForThisNodeSeconds = VarUtils.ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT;
				if (VarUtils.IN_DETAIL_DEBUG) {

					String secondElapsedStr = new Double(
							(prepareRequestTime - startTime) / 1000.0)
							.toString();
					models.utils.LogUtils
							.printLogNormal("REQUEST GENERATED: "
									+ (int) (sentRequestCounter + 1)
									+ " / " + jobIdQ.size() + " after "
									+ secondElapsedStr + " secs"
									+ ":  (NOT SEND YET) " + target_node
									+ " at " + prepareRequestTimeStr);

				}

				worker = getContext().system().actorOf(
						Props.create(OperationWorker.class,
								new initOpMessage(target_node,
								hostUniform, httpHeaderType,
								requestPort, 
									//RequestProtocol.valueOf(requestProtocol.toUpperCase()),
								requestProtocol.toString(),
								1, 500L, 1000L,
								shouldTimeoutTimeForThisNodeSeconds,
								20000, resourcePath, requestContent,
								httpMethod.toUpperCase(), pollable,
								shouldPauseTimeForThisNodeMillis,
								agentCommandType, originalManager)
								));
				
				workers.add(worker);				
				
			} catch (UnsupportedEncodingException e) {
				LogUtils.printLogError("encoding error for resource path: "
						+ e.getLocalizedMessage());
			} catch (Throwable t) {
				LogUtils.printLogError("error in manager: "
						+ t.getLocalizedMessage());
			}
		return worker;
	}
	
	
	/**
	 * Note that if there is sleep in this method
	 */
	public void sendMessageUntilStopCount(int stopCount) {

		// always send with valid data.
		for (int i = processedWorkerCount; i < jobIdQ.size(); ++i) {
			sentRequestCounter ++;
			ActorRef worker =  createWorker(jobIdQ.get(i), nodeDataQ.get(i), localMode);
			try {

				/**
				 * !!! This is a must; without this sleep; stuck occured at 5K.
				 * AKKA seems cannot handle too much too fast message send out.
				 */
				Thread.sleep(1L);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// origin manager
			worker.tell(
					OperationWorker.MessageType.PROCESS_REQUEST,
					originalManager);

			processedWorkerCount++;
			trueProcessedWorkerCount++;

			if (trueProcessedWorkerCount > stopCount) {
				return;
			}
			//System.out.println(maxConcurrencyAdjusted);
			if (VarUtils.IN_DEBUG) {
				models.utils.LogUtils.printLogNormal("REQUEST SENT: "
						+ (int) (processedWorkerCount) + "/"
						+ requestTotalCount
						// + "  RESPONSE#: " + responseCount
						+ "  at " + DateUtils.getNowDateTimeStr()
						+ " directorJobId: " + directorJobId);
			}

		}// end for loop
	}

	public void waitAndRetry() {
		ContinueToSendToBatchSenderAsstManager continueToSendToBatchSenderAsstManager = new ContinueToSendToBatchSenderAsstManager(
				processedWorkerCount);

		if (VarUtils.IN_DETAIL_DEBUG) {

			models.utils.LogUtils.printLogNormal("NOW WAIT Another " + VarUtils.RETRY_INTERVAL_MILLIS
					+ " MS. at " + DateUtils.getNowDateTimeStrSdsm());
		}
		/**
		 * Migrate to akka 2.3.3
		 */
		getContext()
				.system()
				.scheduler()
				.scheduleOnce(
						Duration.create(VarUtils.RETRY_INTERVAL_MILLIS,
								TimeUnit.MILLISECONDS), getSelf(),
						continueToSendToBatchSenderAsstManager,
						getContext().system().dispatcher(), getSelf());
		return;
	}

	public void onReceive(Object message) {

		// Start all workers
		if (message instanceof RequestToBatchSenderAsstManager) {
			
			RequestToBatchSenderAsstManager request = (RequestToBatchSenderAsstManager) message;
			
			// initialize	
			InitialRequestToManager iniRequest = request.request;
			
			directorJobId = iniRequest.directorJobId;
			
			agentCommandType = iniRequest.getAgentCommandType();
			
			agentCommandMetadata = AgentDataProvider.agentCommandMetadatas
					.get(agentCommandType);
			
			final String resourcePathOrig0 = agentCommandMetadata
					.getRequestUrlPostfix();

			resourcePathOrig = resourcePathOrig0.replace(
					VarUtils.NULL_URL_VAR, "");

			httpMethod = agentCommandMetadata.getHttpMethod();
			requestProtocol = agentCommandMetadata
					.getRequestProtocol();
			requestPort = Integer.parseInt(agentCommandMetadata
					.getRequestPort());
			pollable = false;

			pauseIntervalWorkerMillis = agentCommandMetadata
					.getPauseIntervalWorkerMillis();

			// 20131215
			httpHeaderType = agentCommandMetadata
					.getHttpHeaderType();
			originalManager = getSender();
			directorJobId = request.getDirectorJobId();
			jobIdQ = request.jobIdQ;
			nodeDataQ = request.nodeDataQ;
			localMode = iniRequest.localMode;
			requestTotalCount = jobIdQ.size();
			oriMaxConcurrent = iniRequest.maxConcNum;
			maxConcurrencyAdjusted = iniRequest.maxConcNum;
			
			// If there are no requests in the incoming message, send a
			// message back
			if (requestTotalCount <= 0) {
				originalManager.tell(new BatchResponseFromManager(), getSelf());
				return;
			}
			
			sendMessageUntilStopCount(maxConcurrencyAdjusted);

			// if not completed; will schedule a continue send msg
			if (trueProcessedWorkerCount < requestTotalCount) {

				waitAndRetry();
				return;
			} else {
				models.utils.LogUtils.printLogNormal
						 ("Now finished sending all needed messages. Done job of ASST Manager at "
								+ DateUtils.getNowDateTimeStrSdsm());
				return;
			}
		} else if (message instanceof ContinueToSendToBatchSenderAsstManager) {

			// now reaching the end; have processed all of them, just waiting the response to come back
			int notProcessedNodeCount = requestTotalCount - trueProcessedWorkerCount;
			if(notProcessedNodeCount <=0){
				models.utils.LogUtils.printLogNormal("!Finished sending all msg in ASST MANAGER at " + DateUtils.getNowDateTimeStrSdsm()
						
						+ " STOP doing wait and retry."
						);
				return;
			}
			
			int extraSendCount = maxConcurrencyAdjusted
					- (trueProcessedWorkerCount - responseCount);
			
			
			if (extraSendCount > 0) {
				models.utils.LogUtils.printLogNormal("HAVE ROOM to send extra of : "
						+ extraSendCount + " MSG. now Send at "
						+ DateUtils.getNowDateTimeStrSdsm());

					sendMessageUntilStopCount(trueProcessedWorkerCount + extraSendCount);
				waitAndRetry();
			} else {
				models.utils.LogUtils.printLogNormal
						 ("NO ROOM to send extra. Windowns is full. extraSendCount is negative: "
								+ extraSendCount
								+ " reschedule now at "
								+ DateUtils.getNowDateTimeStrSdsm());
				waitAndRetry();

			}
		} else if (message instanceof ResponseCountToBatchSenderAsstManager) {

			responseCount = ((ResponseCountToBatchSenderAsstManager) message)
					.getResponseCount();

			if (VarUtils.IN_DETAIL_DEBUG) {
				 models.utils.LogUtils.printLogError
						 ("RECV IN batchSenderAsstManager FROM AgentCommandManager responseCount: "
								+ responseCount);

			}
		} else if (message instanceof ResetMaxConc) {
			maxConcurrencyAdjusted = (int) (oriMaxConcurrent * ((ResetMaxConc) message).capacityAllowedPercent);
		} else {
			models.utils.LogUtils.printLogError("Unhandled in asst Manager: " + message);
			unhandled(message);
		}
	}

	public int getRequestCount() {
		return requestTotalCount;
	}

	public void setRequestCount(int requestCount) {
		this.requestTotalCount = requestCount;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public List<ActorRef> getWorkers() {
		return workers;
	}

	public void setWorkers(List<ActorRef> workers) {
		this.workers = workers;
	}

}
