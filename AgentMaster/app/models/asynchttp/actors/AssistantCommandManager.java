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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.ContinueToSendToBatchSenderAsstManager;
import models.agent.batch.commands.message.RequestToBatchSenderAsstManager;
import models.agent.batch.commands.message.ResponseCountToBatchSenderAsstManager;
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
import models.utils.DateUtils;
import models.utils.VarUtils;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

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

	protected int maxConcurrencyAdjusted = VarUtils.MAX_CONCURRENT_SEND_SIZE;
	protected int processedWorkerCount = 0;
	protected String directorJobId = null;

	/**
	 * Note that if there is sleep in this method
	 */


	public void sendMessageUntilStopCount(int stopCount) {

		// always send with valid data.
		for (int i = processedWorkerCount; i < workers.size(); ++i) {
			ActorRef worker = workers.get(i);
			try {

				/**
				 * !!! This is a must; without this sleep; stuck occured at 5K.
				 * AKKA seems cannot handle too much too fast message send out.
				 */
				Thread.sleep(1L);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// send as if the sender is the origin manager; so reply back to
			// origin manager
			worker.tell(
					OperationWorker.MessageType.PROCESS_REQUEST,
					originalManager);

			processedWorkerCount++;

			if (processedWorkerCount > stopCount) {
				return;
			}

			if (VarUtils.IN_DEBUG) {

				models.utils.LogUtils.printLogNormal("REQUEST SENT: "
						+ (int) (processedWorkerCount) + "/"
						+ requestTotalCount
						// + "  RESPONSE#: " + responseCount
						+ "  at " + DateUtils.getNowDateTimeStr()

						+ " directorJobId: " + directorJobId)

				;

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
		getContext()
				.system()
				.scheduler()
				.scheduleOnce(
						Duration.create(VarUtils.RETRY_INTERVAL_MILLIS,
								TimeUnit.MILLISECONDS), getSelf(),
						continueToSendToBatchSenderAsstManager,
						getContext().system().dispatcher());
		return;
	}

	public void onReceive(Object message) {

		// Start all workers
		if (message instanceof RequestToBatchSenderAsstManager) {

			// clear responseMap

			RequestToBatchSenderAsstManager request = (RequestToBatchSenderAsstManager) message;
			originalManager = getSender();
			directorJobId = request.getDirectorJobId();
			workers = request.getWorkers();
			maxConcurrencyAdjusted = request.getMaxConcurrency();
			requestTotalCount = workers.size();
			// If there are no requests in the incoming message, send a
			// message back
			if (requestTotalCount <= 0) {
				originalManager.tell(new BatchResponseFromManager(), getSelf());
				return;
			}

			sendMessageUntilStopCount(maxConcurrencyAdjusted);

			// if not completed; will schedule a continue send msg
			if (processedWorkerCount < requestTotalCount) {

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
			int notProcessedNodeCount = requestTotalCount - processedWorkerCount;
			if(notProcessedNodeCount <=0){
				models.utils.LogUtils.printLogNormal("!Finished sending all msg in ASST MANAGER at " + DateUtils.getNowDateTimeStrSdsm()
						
						+ " STOP doing wait and retry."
						);
				return;
			}
			
			int extraSendCount = maxConcurrencyAdjusted
					- (processedWorkerCount - responseCount);
			
			
			if (extraSendCount > 0) {
				models.utils.LogUtils.printLogNormal("HAVE ROOM to send extra of : "
						+ extraSendCount + " MSG. now Send at "
						+ DateUtils.getNowDateTimeStrSdsm());

				sendMessageUntilStopCount(processedWorkerCount + extraSendCount);
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
