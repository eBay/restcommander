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
package RemoteCluster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

import com.ning.http.client.AsyncHttpClient;

import RemoteCluster.CommunicationMessages.*;
import RemoteCluster.JobStatus;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.cluster.Member;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;
import akka.util.Timeout;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.agent.batch.commands.message.RequestToBatchSenderAsstManager;
import models.agent.batch.commands.message.ResponseCountToBatchSenderAsstManager;
import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.AssistantCommandManager;
import models.asynchttp.actors.OperationWorker;
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
import models.utils.LogUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
/**
 * 
 * @author chunyang
 *
 */
public class LocalManager extends UntypedActor {

	private List<ActorRef> workerQ = new ArrayList<ActorRef>(); 
	/**
	 * Copy from CommandManager
	 */
	protected int responseCount = 0;
	protected int requestCount = 0;
	protected long startTime = System.currentTimeMillis();
	protected long endTime = -1L;
	protected ActorRef director = null;

	protected ActorRef batchSenderAsstManager = null;
	protected List<String> responseListKey = new ArrayList<String>();
	protected List<GenericAgentResponse> responseListValue = new ArrayList<GenericAgentResponse>();

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
	
	AgentCommandMetadata agentCommandMetadata;
	InitialRequestToManager request;
	boolean congestionMark = true;
	double capacityPercent = 1.0;
	
	protected boolean requestComplete = false;
	
	@Override
	public void onReceive(Object message) {
		
		if (message instanceof InitialRequestToManager) {
			/**
			 * Copy from Command Manager
			 */
			
			System.out.println("InitialRequestToManager");
			
			director = getSender();
			
			request = (InitialRequestToManager) message;
			nodeGroupType = request.getNodeGroupType();

			directorJobId = request.getDirectorJobId();
			
			agentCommandType = request.getAgentCommandType();
			
			agentCommandMetadata = AgentDataProvider.agentCommandMetadatas
					.get(agentCommandType);
			
			
			
			dataStore = request.getDataStore();
			

			// update responseLengthLimit
			responseExtractIndexStart = agentCommandMetadata
					.getResponseExtractIndexStart();
			responseExtractIndexEnd = agentCommandMetadata
					.getResponseExtractIndexEnd();
			
			NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);

			// SAFETY GUARD. when some request content is not really
			// initialized, e.g. dont have the right Cassini Tag content. will
			// remove them.

			models.utils.LogUtils
					.printLogNormal("Before Safety Check: total entry count: "
							+ ngdm.getNodeDataMapValid().size());
			Map<String, NodeData> nodeDataMapValidSafe = new HashMap<String, NodeData>();

			AgentDataProviderHelper.filterUnsafeOrUnnecessaryRequest(
					ngdm.getNodeDataMapValid(), nodeDataMapValidSafe,
					agentCommandType);

			models.utils.LogUtils
					.printLogNormal("After Safety Check: total entry count in nodeDataMapValidSafe: "
							+ nodeDataMapValidSafe.size());

			requestCount = nodeDataMapValidSafe.size();

			models.utils.LogUtils
					.printLogNormal("!Obtain request! agent command request for nodeGroupType : "
							+ nodeGroupType + "  with count: " + requestCount);
			// If there are no seeders in the incoming message, send a
			// message back
			if (requestCount <= 0) {
				getSender().tell(new BatchResponseFromManager(), getSelf());
				return;
			}
			
			director.tell("ACK", getSelf());
		
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
		
						models.utils.LogUtils
								.printLogError("Send batchSenderAsstManager to responseCountToBatchSenderAsstManager: "
										+ this.responseCount);
					}
				} else {
					models.utils.LogUtils
							.printLogError("batchSenderAsstManager is empty; when trying to send responseCountToBatchSenderAsstManager In manager resonse handling. "
									+ DateUtils.getNowDateTimeStrSdsm());
				}
		
				String hostName = gap.getHost();
				responseListKey.add(hostName);
				responseListValue.add(gap);
		
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
					models.utils.LogUtils
							.printLogNormal(String
									.format("\n[%d]__RESPONSE_RECV_IN_MGR %d (+%d) / %d (%.5g%%)  after %s SEC for %s at %s , directorJobId : %s , RESPONSE_BRIEF: %s\n",
											responseCount, responseCount,
											requestCount - responseCount,
											requestCount, progressPercent,
											secondElapsedStr, hostName,
											responseReceiveTimeStr,
											directorJobId, displayResponse));
				}
		
				getContext().stop(getSender());
				
				if (this.responseCount == this.requestCount && requestComplete) {
					
					System.out.println("All result back.");
					
					models.utils.LogUtils
							.printLogNormal("SUCCESSFUL GOT ON ALL RESPONSES: Received all the expected messages. Count matches: "
									+ this.requestCount
									+ " at time: "
									+ DateUtils.getNowDateTimeStrSdsm());
		
					// Send message to the future with the result
					long currTime = System.currentTimeMillis();
					models.utils.LogUtils
							.printLogNormal("\nTime taken to get all responses back : "
									+ (currTime - startTime) / 1000.0 + " secs");
		
					// MUST SHUT DOWN: This is a double check. Acutally agent
					// operation worker has already shutdown.
					for (ActorRef worker : workerQ) {
						if (worker!=null)
							getContext().stop(worker);
					}
		
					// 20131010: kill asst manager
		
					if (timeoutMessageCancellable != null) {
						timeoutMessageCancellable.cancel();
					}
				
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
		} else if (message instanceof Map<?, ?>) {
			
			System.out.println("Part request!");
			
			requestComplete = false;
			List<NodeData> retryNodeDataList = new ArrayList<NodeData>();
			List<String> retryJobIdList = new ArrayList<String>();
			requestCount += ((Map<?, ?>) message).size();
			for(Entry<String, NodeData> e: ((Map<String, NodeData>) message).entrySet()) {
				retryJobIdList.add(e.getKey());
				retryNodeDataList.add(e.getValue());
				dataStore.get(nodeGroupType).getNodeDataMapValid().put(e.getKey(), e.getValue());
			}
			getSender().tell("ACK", getSelf());
			
		}else if (message instanceof streamRequestToManager) {
			streamRequestToManager sRTM = (streamRequestToManager) message;
			
			System.out.println("StreamRequestToManager : " + sRTM.index);
			
			BatchResponseFromManager partStreamResponse = new BatchResponseFromManager();
			if (sRTM.index >= responseListKey.size()) {
				getSender().tell(partStreamResponse, getSelf());
			} else {
				int chunckSize = Math.min(1024, responseListKey.size()-sRTM.index);
				try {
					for (int i=sRTM.index; i< sRTM.index + chunckSize; i++) {
						partStreamResponse.responseMap.put(responseListKey.get(i), responseListValue.get(i));
					}
					while (sizeof(partStreamResponse) > 15728640 && chunckSize >= 1) {
						chunckSize /= 2;
						partStreamResponse = new BatchResponseFromManager();
						for (int i=sRTM.index; i< sRTM.index + chunckSize; i++) {
							partStreamResponse.responseMap.put(responseListKey.get(i), responseListValue.get(i));
						}
					}
					if (chunckSize == 0) {
						partStreamResponse.responseMap.put(responseListKey.get(sRTM.index), new GenericAgentResponse("Content Too Large, Discarded.", "", ""));
					}				
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				getSender().tell(partStreamResponse, getSelf());
			}
			
		} else if (message instanceof querySlaveProgressMessage) {
			getSender().tell(new slaveProgressMessage(requestCount, responseCount, (this.responseCount >= this.requestCount && requestComplete), capacityPercent), getSelf());
			
			
		} else if (message instanceof  notCongestionSignal) {
			congestionMark = false;
			System.out.println("not Congestion Signal Received");
			
		}else if (message instanceof congestionMessage) {
			if (congestionMark) {
				capacityPercent /=2;
			} else {
				if (capacityPercent < 0.95)
					capacityPercent += 0.04;
			}
			
			System.out.println(congestionMark);
			
			if (batchSenderAsstManager != null)
				batchSenderAsstManager.tell(new ResetMaxConc(capacityPercent), ActorRef.noSender());
			
			congestionMark = true;
		
			if (this.responseCount < this.requestCount && requestComplete) {
				getContext().system().scheduler().scheduleOnce((FiniteDuration) Duration.create(1.5, TimeUnit.SECONDS),
						new Runnable() {
						    @Override
						    public void run() {
						      getSelf().tell(new congestionMessage(), ActorRef.noSender());
						 }
				}, getContext().system().dispatcher());
			}
			
		}else if (message instanceof endOfRequest) {
			
			System.out.println("RequestComplete");	
			
			requestComplete = true;
			
			final RequestToBatchSenderAsstManager requestToBatchSenderAsstManager = new RequestToBatchSenderAsstManager(
					directorJobId, workerQ, getSelf(), 1500, request);

			batchSenderAsstManager = getContext().system().actorOf(
				Props.create(AssistantCommandManager.class),
				"RequestToBatchSenderAsstManager-"
						+ UUID.randomUUID().toString()
			);

			batchSenderAsstManager.tell(requestToBatchSenderAsstManager,
					getSelf());

			// 2013 1013 add cancel.
			final FiniteDuration timeOutDuration = Duration.create(
					VarUtils.TIMEOUT_IN_MANAGER_SCONDS, TimeUnit.SECONDS);
			/**
			 * Migrate to akka 2.3.3
			 */
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeOutDuration, getSelf(),
							InternalMessageType.OPERATION_TIMEOUT,
							getContext().system().dispatcher(), getSelf());
			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils
						.printLogError("Scheduled TIMEOUT_IN_MANAGER_SCONDS OPERATION_TIMEOUT after SEC: "
								+ VarUtils.TIMEOUT_IN_MANAGER_SCONDS
								+ " at "
								+ DateUtils.getNowDateTimeStrSdsm());
			}
			
			if (this.responseCount < this.requestCount && requestComplete) {
				getContext().system().scheduler().scheduleOnce((FiniteDuration) Duration.create(5, TimeUnit.SECONDS),
						new Runnable() {
						    @Override
						    public void run() {
						      getSelf().tell(new congestionMessage(), ActorRef.noSender());
						 }
				}, getContext().system().dispatcher());
			}
		}
	}
	
	@Override
	public void postStop() {
		if (batchSenderAsstManager != null)
			getContext().stop(batchSenderAsstManager);
		responseListKey = null;
		responseListValue = null;
	}
	
	
	public int sizeof(Object obj) throws IOException {

	    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
	    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);

	    objectOutputStream.writeObject(obj);
	    objectOutputStream.flush();
	    objectOutputStream.close();

	    return byteOutputStream.toByteArray().length;
	}
	
	/**
	 * Copy from CommandManager
	 */
	private void cancelRequestAndCancelWorkers() {

		for (ActorRef worker : workerQ) {
			if (worker == null) {
				models.utils.LogUtils
						.printLogError("worker is gone. null ptr: ");
			} else if (!worker.isTerminated()) {
				worker.tell(OperationWorker.MessageType.CANCEL, getSelf());
			}
		}

		models.utils.LogUtils
				.printLogError("--DEBUG--AgentCommandManager sending cancelPendingRequest at time: "
						+ DateUtils.getNowDateTimeStr());
	}
}
//#frontend
