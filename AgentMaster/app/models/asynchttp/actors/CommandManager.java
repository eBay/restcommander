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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import com.ning.http.client.AsyncHttpClient;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import models.agent.batch.commands.message.BatchResponseFromManager;
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
import models.data.NodeGroupDataMap;
import models.data.NodeReqResponse;
import models.data.providers.AgentDataProvider;
import models.data.providers.AgentDataProviderHelper;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.japi.Function;

/**
 * 20130917: add the logic to replace
 * REPLACE-VAR_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR
 * 
 * @author ypei
 * 
 */
public class CommandManager extends UntypedActor {

	protected int responseCount = 0;
	protected int requestCount = 0;
	protected long startTime = System.currentTimeMillis();
	protected long endTime = -1L;
	protected ActorRef director = null;

	protected List<ActorRef> workers = new ArrayList<ActorRef>();

	protected ActorRef batchSenderAsstManager = null;
	protected final Map<String, GenericAgentResponse> responseMap = new HashMap<String, GenericAgentResponse>();

	protected String nodeGroupType = null;
	protected String agentCommandType = null;

	protected String directorJobId = null;

	protected enum InternalMessageType {
		OPERATION_TIMEOUT
	}

	// default is -1: which will not to limit the response length
	protected int responseExtractIndexStart = -1;
	protected int responseExtractIndexEnd = -1;
	protected Cancellable timeoutMessageCancellable = null;
	protected Map<String, NodeGroupDataMap> dataStore = null;

	@Override
	public void onReceive(Object message) {

		// Start all workers
		if (message instanceof InitialRequestToManager) {
			director = getSender();

			// clear responseMap

			InitialRequestToManager request = (InitialRequestToManager) message;
			nodeGroupType = request.getNodeGroupType();

			directorJobId = request.getDirectorJobId();
			agentCommandType = request.getAgentCommandType();

			dataStore = request.getDataStore();

			AgentCommandMetadata agentCommandMetadata = AgentDataProvider.agentCommandMetadatas
					.get(agentCommandType);

			// Get request parameters to construct a REST CALL
			final String resourcePathOrig0 = agentCommandMetadata
					.getRequestUrlPostfix();

			final String resourcePathOrig = resourcePathOrig0.replace(
					VarUtils.NULL_URL_VAR, "");

			final String httpMethod = agentCommandMetadata.getHttpMethod();
			final String requestProtocol = agentCommandMetadata
					.getRequestProtocol();
			final int requestPort = Integer.parseInt(agentCommandMetadata
					.getRequestPort());
			final boolean pollable = false;

			final long pauseIntervalWorkerMillis = agentCommandMetadata
					.getPauseIntervalWorkerMillis();

			final int maxConcurrencyAdjusted = AgentUtils
					.processMaxConcurrency(agentCommandMetadata
							.getMaxConcurrency());

			// 20131215
			final String httpHeaderType = agentCommandMetadata
					.getHttpHeaderType();

			// update responseLengthLimit
			responseExtractIndexStart = agentCommandMetadata
					.getResponseExtractIndexStart();
			responseExtractIndexEnd = agentCommandMetadata
					.getResponseExtractIndexEnd();

			NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);

			// SAFETY GUARD. when some request content is not really
			// initialized,  will
			// remove them.

			models.utils.LogUtils.printLogNormal("Before Safety Check: total entry count: "
					+ ngdm.getNodeDataMapValid().size());
			Map<String, NodeData> nodeDataMapValidSafe = new HashMap<String, NodeData>();

			AgentDataProviderHelper.filterUnsafeOrUnnecessaryRequest(
					ngdm.getNodeDataMapValid(), nodeDataMapValidSafe,
					agentCommandType);

			models.utils.LogUtils.printLogNormal
					 ("After Safety Check: total entry count in nodeDataMapValidSafe: "
							+ nodeDataMapValidSafe.size());

			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils.printLogNormal("pauseIntervalWorkerMillis : "
						+ pauseIntervalWorkerMillis);

			}

			models.utils.LogUtils.printLogNormal("maxConcurrencyAdjusted : "
					+ maxConcurrencyAdjusted);

			// assumption: key value pairs number equals to
			requestCount = nodeDataMapValidSafe.size();

			models.utils.LogUtils.printLogNormal
					 ("!Obtain request! agent command request for nodeGroupType : "
							+ nodeGroupType + "  with count: " + requestCount);
			// If there are no seeders in the incoming message, send a
			// message back
			if (requestCount <= 0) {
				getSender().tell(new BatchResponseFromManager(), getSelf());
				return;
			}

			int sentRequestCounter = 0;

			// always send with valid data.
			for (Entry<String, NodeData> entry : nodeDataMapValidSafe
					.entrySet()) {
				final String target_node = entry.getKey();
				NodeData nodeData = entry.getValue();

				// use the command metadata to retrieve the content
				// if this time has not been before: create it.

				if (!nodeData.getDataMap().containsKey(
						agentCommandMetadata.getAgentCommandType())) {

					 models.utils.LogUtils.printLogError
							 ("HAS NOT GENERATED AGENT COMMAND before sending Command!");
					NodeReqResponse nodeReqResponse = new NodeReqResponse();
					nodeReqResponse.setDefaultEmptyReqestContent();
					nodeData.getDataMap().put(
							agentCommandMetadata.getAgentCommandType(),
							nodeReqResponse);
				}

				NodeReqResponse nodeReqResponse = nodeData.getDataMap().get(
						agentCommandMetadata.getAgentCommandType());

				// BECAREFUL NPE
				final String requestContentOrig = (nodeReqResponse == null || nodeReqResponse
						.getRequestParameters() == null) ? "" : nodeReqResponse
						.getRequestParameters().get(
								VarUtils.NODE_REQUEST_FULL_CONTENT_TYPE);

				final String requestContent = NodeReqResponse.replaceStrByMap(
						nodeReqResponse.getRequestParameters(),
						requestContentOrig);
				final String resourcePath = NodeReqResponse.replaceStrByMap(
						nodeReqResponse.getRequestParameters(),
						resourcePathOrig);

				/**
				 * 20130917 replace
				 * REPLACE-VAR_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR if
				 * exists Only in this manager; but not in agent upgrade
				 * manager. upgrade manager does not need to have this logic of
				 * hitting a single node
				 */
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
				 * 
				 */
				final AsyncHttpClient asyncHttpClient = (
						
						!httpHeaderType
						.equalsIgnoreCase(MyHttpUtils.STR_HTTP_HEADER_TYPE_LBMS)
						&& !httpHeaderType
						.equalsIgnoreCase(MyHttpUtils.STR_HTTP_HEADER_TYPE_LBMS_ASYNC)
						&& !httpHeaderType
								.equalsIgnoreCase(MyHttpUtils.STR_HTTP_HEADER_TYPE_UDNS) && !agentCommandType
						.contains(VarUtils.AGENT_CMD_KEY_SLOW_CLIENT_SUBSTR)) ? VarUtils.ningClientFactory
						.getFastClient() : VarUtils.ningClientFactory
						.getSlowClient();

				final String hostUniform = (target_node_new_replacing_old == null) ? null
						: target_node_new_replacing_old;
				// 20130511 replacement:

				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_TRUE_CONTENT1, requestContent);

				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_TRUE_URL1, resourcePath);

				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_TRUE_PORT1,
						Integer.toString(requestPort));
				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_HTTP_METHOD1, httpMethod);

				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_HTTP_HEADER_TYPE, httpHeaderType);
				
				long prepareRequestTime = System.currentTimeMillis();

				String prepareRequestTimeStr = DateUtils
						.getDateTimeStr(new Date(prepareRequestTime));
				nodeReqResponse.getRequestParameters().put(
						VarUtils.NODE_REQUEST_PREPARE_TIME1,
						prepareRequestTimeStr);

				final long shouldPauseTimeForThisNodeMillis = (long) sentRequestCounter
						* pauseIntervalWorkerMillis;
				// 20131007; this is constant; the scheduler cancellable is not
				// with the initial 1st pause.
				final int shouldTimeoutTimeForThisNodeSeconds = VarUtils.ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT;

				if (VarUtils.IN_DETAIL_DEBUG) {

					String secondElapsedStr = new Double(
							(prepareRequestTime - startTime) / 1000.0)
							.toString();
					models.utils.LogUtils.printLogNormal("REQUEST GENERATED: "
							+ (int) (sentRequestCounter + 1) + " / "
							+ requestCount + " after " + secondElapsedStr
							+ " secs" + ":  (NOT SEND YET) " + target_node
							+ " at " + prepareRequestTimeStr);

				}
				final ActorRef worker = getContext().system().actorOf(
						new Props(new UntypedActorFactory() {
							private static final long serialVersionUID = 1L;

							final AgentRequest request = new GenericAgentRequest(
									1, 500, 1000,
									shouldTimeoutTimeForThisNodeSeconds,
									120000, resourcePath, requestContent,
									httpMethod.toUpperCase(), pollable,
									shouldPauseTimeForThisNodeMillis);

							public Actor create() {

								return new OperationWorker(target_node,
										hostUniform,
										httpHeaderType, requestPort,
										RequestProtocol.valueOf(requestProtocol
												.toUpperCase()), request,
										asyncHttpClient);
							}
						}));

				workers.add(worker);

				++sentRequestCounter;
			}// end for loop
			/**
			 * 20130730: now send to the sender
			 */
			final RequestToBatchSenderAsstManager requestToBatchSenderAsstManager = new RequestToBatchSenderAsstManager(
					directorJobId, workers, getSelf(), maxConcurrencyAdjusted);

			batchSenderAsstManager = getContext().system().actorOf(
					new Props(new UntypedActorFactory() {
						private static final long serialVersionUID = 1L;

						public UntypedActor create() {
							return new AssistantCommandManager();
						}
					}),
					"RequestToBatchSenderAsstManager-"
							+ UUID.randomUUID().toString());

			batchSenderAsstManager.tell(requestToBatchSenderAsstManager,
					getSelf());

			// 2013 1013 add cancel.
			final FiniteDuration timeOutDuration = Duration.create(
					VarUtils.TIMEOUT_IN_MANAGER_SCONDS, TimeUnit.SECONDS);
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeOutDuration, getSelf(),
							InternalMessageType.OPERATION_TIMEOUT,
							getContext().system().dispatcher());
			if (VarUtils.IN_DETAIL_DEBUG) {
				 models.utils.LogUtils.printLogError
						 ("Scheduled TIMEOUT_IN_MANAGER_SCONDS OPERATION_TIMEOUT after SEC: "
								+ VarUtils.TIMEOUT_IN_MANAGER_SCONDS
								+ " at "
								+ DateUtils.getNowDateTimeStrSdsm());
			}

		} else if (message instanceof AgentResponse) {

			try {
				GenericAgentResponse gap = null;

				// this guareetee gap is not NULL :-)
				if ((message instanceof GenericAgentResponse)) {

					gap = (GenericAgentResponse) message;
				} else {

					gap = new GenericAgentResponse((AgentResponse) message,
							DateUtils.getNowDateTimeStrSdsm());
				}

				// 20130422 to trim the message if needed; careful, when there
				// are exception: will make the
				// bug fixed: 20130423 check gap.getResponseContent() length
				// ==0; then -1 will make it out of bound!

				if (gap != null && gap.getResponseContent() != null
						&& !gap.getResponseContent().isEmpty()

						&& responseExtractIndexStart >= 0
						&& responseExtractIndexEnd >= 0
						&& responseExtractIndexStart <= responseExtractIndexEnd) {

					int trimStartIndex = (int) Math.min(
							responseExtractIndexStart, gap.getResponseContent()
									.length() - 1);
					int trimEndIndex = (int) Math.min(responseExtractIndexEnd,
							gap.getResponseContent().length() - 1);
					trimStartIndex = (trimStartIndex < 0) ? 0 : trimStartIndex;
					trimEndIndex = (trimEndIndex < 0) ? 0 : trimEndIndex;
					gap.setResponseContent(gap.getResponseContent().substring(
							trimStartIndex, trimEndIndex));
				}

				this.responseCount += 1;

				/**
				 * 20131009: add feedback of current responseCount to asst
				 * manager ResponseCountToBatchSenderAsstManager
				 */
				final ResponseCountToBatchSenderAsstManager responseCountToBatchSenderAsstManager = new ResponseCountToBatchSenderAsstManager(
						this.responseCount);

				if (batchSenderAsstManager != null) {
					batchSenderAsstManager.tell(
							responseCountToBatchSenderAsstManager, getSelf());

					if (VarUtils.IN_DETAIL_DEBUG) {

						 models.utils.LogUtils.printLogError
								 ("Send batchSenderAsstManager to responseCountToBatchSenderAsstManager: "
										+ this.responseCount);
					}
				} else {
					 models.utils.LogUtils.printLogError
							 ("batchSenderAsstManager is empty; when trying to send responseCountToBatchSenderAsstManager In manager resonse handling. "
									+ DateUtils.getNowDateTimeStrSdsm());
				}

				String hostName = gap.getHost();
				if (responseMap.containsKey(hostName)) {
					models.utils.LogUtils.printLogError("ERROR: dupliated entry: " + hostName
							+ DateUtils.getNowDateTimeStr());
				}
				responseMap.put(hostName, gap);

				// Also update the valid entry in datamap
				AgentDataProviderHelper.getInstance()
						.updateResponseFromAgentGenericResponse(nodeGroupType,
								agentCommandType, gap, dataStore);

				String displayResponse = (gap.getResponseContent() == null || gap
						.getResponseContent().length() < 1) ? "RESPONSE_CONTENT_EMPTY_OR_NULL"
						: gap.getResponseContent()
								.substring(
										0,
										Math.min(
												VarUtils.AGNET_RESPONSE_MAX_RESPONSE_DISPLAY_BYTE1,
												gap.getResponseContent()
														.length()));

				if (this.responseCount % 1 == 0) {
					long responseReceiveTime = System.currentTimeMillis();
					// %.5g%n
					double progressPercent = (double) (responseCount)
							/ (double) (requestCount) * 100.0;
					String responseReceiveTimeStr = DateUtils
							.getDateTimeStr(new Date(responseReceiveTime));
					String secondElapsedStr = new Double(
							(responseReceiveTime - startTime) / 1000.0)
							.toString();
					// percent is escaped using percent sign
					models.utils.LogUtils.printLogNormal(String
							.format("\n[%d]__RESPONSE_RECV_IN_MGR %d (+%d) / %d (%.5g%%)  after %s SEC for %s at %s , directorJobId : %s , RESPONSE_BRIEF: %s\n",
									responseCount, responseCount, requestCount
											- responseCount, requestCount,
									progressPercent, secondElapsedStr,
									hostName, responseReceiveTimeStr,
									directorJobId, displayResponse));
				}

				if (this.responseCount == this.requestCount) {
					models.utils.LogUtils.printLogNormal
							 ("SUCCESSFUL GOT ON ALL RESPONSES: Received all the expected messages. Count matches: "
									+ this.requestCount
									+ " at time: "
									+ DateUtils.getNowDateTimeStrSdsm());

					BatchResponseFromManager batchResponseFromManager = new BatchResponseFromManager();

					batchResponseFromManager.getResponseMap().putAll(
							responseMap);
					if (director != null) {
						director.tell(batchResponseFromManager, getSelf());

					} else {
						 models.utils.LogUtils.printLogError
								 ("ERROR: Initiator is NULL for BatchSeederHealthManager ");
					}

					// Send message to the future with the result
					long currTime = System.currentTimeMillis();
					models.utils.LogUtils.printLogNormal
							 ("\nTime taken to get all responses back : "
									+ (currTime - startTime) / 1000.0 + " secs");

					// MUST SHUT DOWN: This is a double check. Acutally agent
					// operation worker has already shutdown.
					for (ActorRef worker : workers) {
						getContext().stop(worker);
					}

					// 20131010: kill asst manager

					// stop the manager:agentCommandManager
					if (batchSenderAsstManager != null) {
						ActorConfig.getActorSystem().stop(
								batchSenderAsstManager);
					}

					if (timeoutMessageCancellable != null) {
						timeoutMessageCancellable.cancel();
					}

					// Suicide
					// getSelf().tell(PoisonPill.getInstance(), null);
					getContext().stop(getSelf());

				}// end when all requests have resonponse

			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else if (message instanceof InternalMessageType) {
			switch ((InternalMessageType) message) {

			case OPERATION_TIMEOUT:
				cancelRequestAndCancelWorkers();
				break;
			}
		} else {
			models.utils.LogUtils.printLogError("Unhandled: " + message);
			unhandled(message);
		}
	}// end func

	/**
	 * Potential bug: this assumes when cancel; all initial request to each node
	 * has been sent out by asst manager to each op worker.
	 * 
	 * For those op worker who has receive this cancel first PRIOR to the asst
	 * manager's request message; the reply back could
	 * 
	 * This way prevent memory leak by sending cancel to OP worker in order to
	 * stop http worker; rather than directly stopping OP worker without
	 * stopping http worker If not; this did not really stop the ASST manager..
	 * 
	 * will rely on the global ASK manager timeout for if .
	 */
	private void cancelRequestAndCancelWorkers() {

		for (ActorRef worker : workers) {
			if (worker == null) {
				models.utils.LogUtils.printLogError("worker is gone. null ptr: ");
			} else if (!worker.isTerminated()) {
				worker.tell(OperationWorker.MessageType.CANCEL, getSelf());
			}
		}

		 models.utils.LogUtils.printLogError
				 ("--DEBUG--AgentCommandManager sending cancelPendingRequest at time: "
						+ DateUtils.getNowDateTimeStr());
	}

	public int getResponseCount() {
		return responseCount;
	}

	public void setResponseCount(int responseCount) {
		this.responseCount = responseCount;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public void setRequestCount(int requestCount) {
		this.requestCount = requestCount;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public ActorRef getDirector() {
		return director;
	}

	public void setDirector(ActorRef director) {
		this.director = director;
	}

	public List<ActorRef> getWorkers() {
		return workers;
	}

	public void setWorkers(List<ActorRef> workers) {
		this.workers = workers;
	}

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public void setNodeGroupType(String nodeGroupType) {
		this.nodeGroupType = nodeGroupType;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public void setAgentCommandType(String agentCommandType) {
		this.agentCommandType = agentCommandType;
	}

	public Map<String, GenericAgentResponse> getResponseMap() {
		return responseMap;
	}

}
