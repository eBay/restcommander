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
package models.data.providers.actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.agent.batch.commands.message.RequestToBatchSenderAsstManager;
import models.agent.batch.commands.message.ResponseCountToBatchSenderAsstManager;
import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.OperationWorker;
import models.asynchttp.request.AgentRequest;
import models.asynchttp.request.GenericAgentRequest;
import models.asynchttp.response.AgentResponse;
import models.asynchttp.response.GenericAgentResponse;

import models.data.AgentCommandMetadata;
import models.data.AggregateData;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeReqResponse;
import models.data.providers.AgentDataAggregator;
import models.data.providers.AgentDataProvider;
import models.data.providers.AgentDataProviderHelper;
import models.utils.AgentUtils;
import models.utils.DateUtils;
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
public class AggregationManager extends UntypedActor {

	protected int responseCount = 0;
	protected int requestCount = 0;
	protected int requestNotFireCount = 0;
	protected long startTime = System.currentTimeMillis();
	protected long endTime = -1L;
	protected ActorRef director = null;
	protected List<ActorRef> workers = new ArrayList<ActorRef>();
	public String patternStr = null;
	public AggregateData aggregateData = null;
	protected String directorJobId = null;

	protected enum InternalMessageType {
		OPERATION_TIMEOUT
	}

	// default is -1: which will not to limit the response length
	protected int responseExtractIndexStart = -1;
	protected int responseExtractIndexEnd = -1;
	protected Cancellable timeoutMessageCancellable = null;
	protected Map<String, NodeGroupDataMap> dataStore = null;

	protected String agentCommandType = null;
	protected Map<String, NodeData> nodeDataMapValid = null;

	@Override
	public void onReceive(Object message) {

		// Start all workers
		if (message instanceof RequestToAggregationManager) {
			director = getSender();

			// clear responseMap

			RequestToAggregationManager request = (RequestToAggregationManager) message;
			patternStr = request.getPatternStr();

			directorJobId = request.getDirectorJobId();
			aggregateData = request.getAggregateData();
			nodeDataMapValid = aggregateData.getNodeDataMapValid();

			agentCommandType = aggregateData.getAgentCommandType();

			AgentDataProvider adp = AgentDataProvider.getInstance();
			final String errorMsgPatternStr = adp.aggregationMetadatas
					.get(VarUtils.AGGREGATION_PATTERN_EXTRACT_EXCEPTION_SUMMARY_FROM_ERROR_MSG);

			if (nodeDataMapValid == null || nodeDataMapValid.entrySet() == null) {
				 models.utils.LogUtils.printLogError
						 (" ERROR: ? Data source is missing. Return. nodeDataMapValid == null || nodeDataMapValid.entrySet() == null  in genResponseToMetadataMap()"
								+ DateUtils.getNowDateTimeStrSdsm());

				return;
			}

			// assumption: key value pairs number equals to
			requestCount = nodeDataMapValid.size();

			models.utils.LogUtils.printLogNormal
					 ("!Obtain request! aggregation request   with count: "
							+ requestCount);
			if (requestCount <= 0) {
				getSender().tell(
						new ResponseFromAggregationManager(new Date() + ""),
						getSelf());
				return;
			}

			for (Entry<String, NodeData> entry : nodeDataMapValid.entrySet()) {

				// 20130730 TODO: Potential bug: verified NPE. this can be
				// empty. When no data is coming back.

				final String fqdn = entry.getKey();
				if (entry.getValue() == null
						|| entry.getValue().getDataMap() == null
						|| entry.getValue().getDataMap().get(agentCommandType) == null
						|| entry.getValue().getDataMap().get(agentCommandType)
								.getResponseContent() == null) {
					
					if(VarUtils.IN_DETAIL_DEBUG){
						
						models.utils.LogUtils.printLogNormal
						 ("ERROR~Understandable: Will happen when Response is null and request fails to send out.  NodeData in nodeDataMapValid is NULL in function genResponseToMetadataMap at "
								+ DateUtils.getNowDateTimeStrSdsm());
					}
					boolean isError = true;
					aggregateData.getFqdnResponseExtractMap().put(fqdn,
							VarUtils.NA);
					aggregateData.getFqdnIsErrorExtractMap().put(fqdn, isError);
					aggregateData.getFqdnErrorMsgExtractMap().put(fqdn,
							VarUtils.SUPERMAN_NOT_FIRE_REQUEST);
					++requestNotFireCount;
					continue;
				}

				
				final NodeData nodeData = entry.getValue();
				final ActorRef worker = getContext().system().actorOf(
						new Props(new UntypedActorFactory() {
							private static final long serialVersionUID = 1L;

							final RequestToAggregationWorker request = new RequestToAggregationWorker(
									nodeData, agentCommandType,
									errorMsgPatternStr, patternStr);

							public Actor create() {
								return new AggregationWorker(request, fqdn);
							}
						}));

				workers.add(worker);

				worker.tell(AggregationWorker.MessageType.PROCESS_REQUEST,
						getSelf());

			}// end for loop
			
			//TODO
			if(workers.isEmpty()){
				models.utils.LogUtils.printLogNormal("NO RESPONSES coming back in this case. Just return");
				
				ResponseFromAggregationManager responseFromAggregationManager = new ResponseFromAggregationManager(
						new Date() + "");

				if (director != null) {
					director.tell(responseFromAggregationManager, getSelf());

				} else {
					 models.utils.LogUtils.printLogError
							 ("ERROR: Initiator is NULL for AggregationManager ");
				}
				getContext().stop(getSelf());
			}

			// 2013 1013 add cancel.
			final FiniteDuration timeOutDuration = Duration.create(
					VarUtils.TIMEOUT_ASK_AGGREGATION_MANAGER_SCONDS,
					TimeUnit.SECONDS);
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeOutDuration, getSelf(),
							InternalMessageType.OPERATION_TIMEOUT,
							getContext().system().dispatcher());
			if (VarUtils.IN_DETAIL_DEBUG) {
				 models.utils.LogUtils.printLogError
						 ("Scheduled TIMEOUT_ASK_AGGREGATION_MANAGER_SCONDS OPERATION_TIMEOUT after SEC: "
								+ VarUtils.TIMEOUT_ASK_AGGREGATION_MANAGER_SCONDS
								+ " at " + DateUtils.getNowDateTimeStrSdsm());
			}

		} else if (message instanceof ResponseToAggregationManagerFromWorker) {

			try {
				ResponseToAggregationManagerFromWorker responseFromWorker = null;
				responseFromWorker = (ResponseToAggregationManagerFromWorker) message;

				// 20130422 to trim the message if needed; careful, when there
				// are exception: will make the
				// bug fixed: 20130423 check gap.getResponseContent() length
				// ==0; then -1 will make it out of bound!

				this.responseCount += 1;

				if (responseFromWorker.getRequest() == null) {
					models.utils.LogUtils.printLogError("responseFromWorker request is null!!"
							+ DateUtils.getNowDateTimeStrSdsm());
				}

				String fqdn = responseFromWorker.getFqdn();
				String extractErrorMsg = responseFromWorker
						.getExtractErrorMsg();
				String extractedResponse = responseFromWorker
						.getExtractedResponse();
				String agentCommandType = responseFromWorker.getRequest()
						.getAgentCommandType();

				boolean isError = responseFromWorker.isError();

				
				
				// 20131026: with this: for status code / response time only
				if (patternStr!=null&& patternStr
						.equalsIgnoreCase(VarUtils.AGGREGATION_SUPERMAN_SPECIAL_STATUS_CODE)) {
					String statusCode = responseFromWorker.getRequest()
							.getNodeData().getDataMap().get(agentCommandType)
							.getResponseContent().getStatusCode();
					extractedResponse = statusCode;
				} else if (patternStr!=null&& patternStr
						.equalsIgnoreCase(VarUtils.AGGREGATION_SUPERMAN_SPECIAL_RESPONSE_TIME)) {
					String responseTime = responseFromWorker.getRequest()
							.getNodeData().getDataMap().get(agentCommandType)
							.getResponseContent().getResponseTime();
					extractedResponse = responseTime;
				}

				if (VarUtils.IN_DETAIL_DEBUG) {
					models.utils.LogUtils.printLogNormal(" stringMatcherByPattern output: "
							+ extractedResponse);

					models.utils.LogUtils.printLogNormal(" extractErrorMsg output: "
							+ extractErrorMsg);
				}

				// put into the init hashmap
				aggregateData.getFqdnResponseExtractMap().put(fqdn,
						extractedResponse);
				aggregateData.getFqdnIsErrorExtractMap().put(fqdn, isError);
				aggregateData.getFqdnErrorMsgExtractMap().put(fqdn,
						extractErrorMsg);

				if (this.responseCount % 1 == 0) {

					if (VarUtils.IN_DETAIL_DEBUG) {
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
								.format("\n[%d]__RESPONSE_RECV_IN_MGR %d (+%d) / %d (%.5g%%)  after %s SEC for %s at %s , directorJobId : %s\n",
										responseCount, responseCount,
										requestCount - responseCount,
										requestCount, progressPercent,
										secondElapsedStr, fqdn,
										responseReceiveTimeStr, directorJobId)
										
						);
					}
				}

				if (this.responseCount + this.requestNotFireCount == this.requestCount ) {
					models.utils.LogUtils.printLogNormal
							 ("SUCCESSFUL GOT ON ALL RESPONSES: Received all the expected messages. Count matches: "
									+ this.requestCount
									+ " at time: "
									+ DateUtils.getNowDateTimeStrSdsm());

					ResponseFromAggregationManager responseFromAggregationManager = new ResponseFromAggregationManager(
							new Date() + "");

					if (director != null) {
						director.tell(responseFromAggregationManager, getSelf());

					} else {
						 models.utils.LogUtils.printLogError
								 ("ERROR: Initiator is NULL for AggregationManager ");
					}

					// Send message to the future with the result
					long currTime = System.currentTimeMillis();
					models.utils.LogUtils.printLogNormal
							 ("\nTime taken to get all responses back in Aggregation Manager: "
									+ (currTime - startTime) / 1000.0 + " secs");

					// MUST SHUT DOWN: This is a double check. Acutally agent
					// operation worker has already shutdown.
					for (ActorRef worker : workers) {
						getContext().stop(worker);
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
				// just stop should be fine. since no next layer
				getContext().stop(worker);
			}
		}

		 models.utils.LogUtils.printLogError
				 ("--DEBUG--aggregationManager sending cancelPendingRequest at time: "
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

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public void setAgentCommandType(String agentCommandType) {
		this.agentCommandType = agentCommandType;
	}

}
