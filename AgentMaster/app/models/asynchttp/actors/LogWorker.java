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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.asynchttp.HttpMethod;
import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.HttpWorker.MessageType;
import models.asynchttp.actors.HttpWorker.MyResponse;
import models.data.NodeGroupDataMap;
import models.data.providers.LogProvider;
import models.data.providers.ssh.SshProvider;
import models.data.providers.ssh.SshProvider.SshResponse;
import models.utils.DateUtils;
import models.utils.ErrorMsgUtils;
import models.utils.MyHttpUtils;
import models.utils.StringUtils;
import models.utils.VarUtils;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
//import akka.actor.PoisonPill;
import akka.actor.UntypedActor;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

/**
 * 
 * 20131013 THIS IS NING BASED
 * 
 * BE VERY CAREFUL: the MyResponse and the MessageType are all under HttpWorker.
 * 
 * this log worker has internally timeout; and will
 * suicide if timeout; so should no need to have external tiemout
 * check/kill again
 * 
 * 
 * @author ypei
 * 
 */
public class LogWorker extends UntypedActor {

	private final String nodeGroupType;
	private final String agentCommandType;
	private final String directorJobId;

	private final Map<String, NodeGroupDataMap> dataStore;

	protected long startTime = System.currentTimeMillis();
	protected long endTime = -1L;

	//private ActorRef sender = null;
	private Throwable cause;
	private Cancellable timeoutMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;

	public String getSummary() {
		return "logWorker for nodeGroupType " + nodeGroupType
				+ " agentCommandType: " + agentCommandType;
	}

	
	public LogWorker(String nodeGroupType, String agentCommandType,
			 Map<String, NodeGroupDataMap> dataStore, String directorJobId) {
		super();
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
		this.dataStore = dataStore;
		this.directorJobId = directorJobId;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		try {
			if (message instanceof MessageType) {
				switch ((MessageType) message) {
				case PROCESS_REQUEST:

					models.utils.LogUtils
					.printLogNormal("!!START LogWorker. directorJobId: "
							+ directorJobId + " " + getSummary()
							+ " time: " + DateUtils.getDateTimeStr(new Date(startTime)));
					//sender = getSender();
					timeoutDuration = Duration
							.create(VarUtils.LOG_WORKER_MAX_OPERATION_TIME_SECONDS_DEFAULT,
									TimeUnit.SECONDS);
					// To handle cases where response never comes back, we
					// schedule a 'timeout' message to be sent to us 2 seconds
					// after NIO's SO_TIMEOUT
					/**
					 * Migrate to akka 2.3.3
					 */
					timeoutMessageCancellable = getContext()
							.system()
							.scheduler()
							.scheduleOnce(timeoutDuration, getSelf(),
									MessageType.PROCESS_ON_TIMEOUT,
									getContext().system().dispatcher(), getSelf());

					LogProvider.saveAgentDataInLog(nodeGroupType,
							agentCommandType, dataStore);
					
					
					selfTerminate(VarUtils.SUCCESS_FLAG, false, VarUtils.NA, null,
							VarUtils.NA);
					break;

				case CANCEL:
					cancelCancellable();
					selfTerminate(null, true, "RequestCanceled", null,
							VarUtils.NA);
					break;

				case PROCESS_ON_EXCEPTION:
					final StringWriter sw = new StringWriter();
					final PrintWriter pw = new PrintWriter(sw);
					cause.printStackTrace(pw);

					String displayError = ErrorMsgUtils.replaceErrorMsg(cause
							.toString());

					// 20130522: get details of error message out.
					String detailErrorMsgWithStackTrace = displayError
							+ " Details: " + sw.toString();
					cancelCancellable();
					selfTerminate(null, true, detailErrorMsgWithStackTrace,
							sw.toString(), VarUtils.NA);

					break;

				case PROCESS_ON_TIMEOUT:
					models.utils.LogUtils
							.printLogError("!!!Inside PROCESS_ON_TIMEOUT.................logWorker: "
									+ getSummary()
									+ "......... at "
									+ DateUtils.getNowDateTimeStrSdsm());
					cancelCancellable();

					// 20130801 To match this:
					// PATTERN_EXTRACT_EXCEPTION_SUMMARY_FROM_ERROR_MSG to get
					// the regular
					// expression.
					String errorMsg = String
							.format("LogWorker Timedout after %d SEC (no response but no exception catched). Check URL: may be very slow or stuck. Details more info",
									VarUtils.ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT);

					selfTerminate(null, true, errorMsg, errorMsg, VarUtils.NA);
					break;

				case PROCESS_ON_RESPONSE_DATA:
					models.utils.LogUtils
							.printLogNormal("Inside PROCESS_ON_RESPONSE_DATA UnsupportedOperation.................");
					cancelCancellable();
					selfTerminate(null, true, "UnsupportedOperation", null,
							VarUtils.NA);
					break;
				}
			} else {
				unhandled(message);
				selfTerminate(null, true, "unknown message type; exit", null,
						VarUtils.NA);
			}
		} catch (Exception e) {
			this.cause = e;
			getSelf().tell(MessageType.PROCESS_ON_EXCEPTION, getSelf());
		}
	}

	public void cancelCancellable() {

		if (timeoutMessageCancellable != null) {
			timeoutMessageCancellable.cancel();
		}
	}

	private void selfTerminate(final String response, final boolean error,
			final String errorMessage, final String stackTrace,
			final String statusCode) {

		endTime = System.currentTimeMillis();
		models.utils.LogUtils
				.printLogNormal("!!END LogWorker. directorJobId: "
						+ directorJobId
						+ " time: "
						+ DateUtils.getDateTimeStr(new Date(endTime))
						+ "\n!!!Duration (LogWorker last): "
						+ DateUtils.getDurationSecFromTwoDatesDouble(new Date(
								startTime), new Date(endTime)) + "sec for " + getSummary() );

		// Self-terminate; should have no need. the Operation Worker will kill
		// it
		getSelf().tell(PoisonPill.getInstance(), null);
		getContext().stop(getSelf());
	}

}
