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

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.asynchttp.HttpMethod;
import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.HttpWorker.MyResponse;
import models.asynchttp.request.AgentRequest;
import models.asynchttp.response.AgentResponse;
import models.asynchttp.response.GenericAgentResponse;
import models.asynchttp.request.GenericAgentRequest;
import models.utils.DateUtils;
import models.utils.LogUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.ning.http.client.AsyncHttpClient;

import RemoteCluster.SupermanApp;
import RemoteCluster.CommunicationMessages.initOpMessage;

/**
 * Ning based
 * 
 * @author ypei
 * 
 */
public class OperationWorker extends UntypedActor {
	private final AsyncHttpClient client;
	private final RequestProtocol protocol;
	private final String host;

	// 20130917 ASSUMPTION: WHEN IT IS NOT NULL: THIS ONE WILL REPLACE THE host
	// to send the HTTP REQUEST!!!
	private final String hostUniform;
	
	//20131215
	private final String httpHeaderType;
	
	private final int agentPort;
	private final AgentRequest request;
	private AgentResponse agentResponse = null;

	private ActorRef asyncWorker = null;
	private ActorRef sender = null;
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	private Cancellable pollMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;
	private long startTimeMillis = 0;
	private boolean sentReply = false;

	// jeff
	private boolean hasBeenDelayed = false;
	
	// 20140429 add for knowing if it is SSH

	private static final int HTTP_MAX_RETRIES = 1;
	private static final long HTTP_RETRY_INTERVAL_MILLIS = 500;

	public enum MessageType implements Serializable {
		PROCESS_REQUEST, GET_PROGRESS, CANCEL
	}

	private enum InternalMessageType {
		POLL_PROGRESS, OPERATION_TIMEOUT
	}

	public OperationWorker(initOpMessage message) {
		super();

		this.protocol = RequestProtocol.fromString(message.protocol);
		this.host = message.host;
		this.hostUniform = message.hostUniform;
		this.httpHeaderType = message.httpHeaderType;
		this.agentPort = message.agentPort;
		final Map<String, String> httpHeaderMap = MyHttpUtils
				.getHttpHeaderMapCopyFromHeaderMetadataMapStatic(
						message.httpHeaderType, message.protocol);
		this.request = new GenericAgentRequest(message.maxTries, message.retryIntervalMillis,
				message.pollIntervalMillis, message.maxOperationTimeSeconds,
				message.statusChangeTimeoutSeconds, message.resourcePath,
				message.requestContent, message.httpMethod, message.pollable, message.pauseIntervalBeforeSendMillis,
				httpHeaderMap);
		this.client =  (
				!httpHeaderType
				.equalsIgnoreCase(VarUtils.STR_HTTP_HEADER_TYPE_LBMS)
				&& !httpHeaderType
						.equalsIgnoreCase(VarUtils.STR_HTTP_HEADER_TYPE_LBMS_ASYNC)
				&& !httpHeaderType
						.equalsIgnoreCase(VarUtils.STR_HTTP_HEADER_TYPE_UDNS) && !message.agentCommandType
				.contains(VarUtils.AGENT_CMD_KEY_SLOW_CLIENT_SUBSTR)) ? VarUtils.ningClientFactory
				.getFastClient() : VarUtils.ningClientFactory
				.getSlowClient();
		this.sender = message.manager;
		processRequest();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		try {
			//System.out.println("Op received at : " + Calendar.getInstance().getTimeInMillis());
			if (message instanceof InternalMessageType) {
				switch ((InternalMessageType) message) {

				case OPERATION_TIMEOUT:
					operationTimeoutOrCancel();
					break;
				}
			} else if (message instanceof MessageType) {
				switch ((MessageType) message) {
				/*case PROCESS_REQUEST:
					processRequest();
					break;*/

				case GET_PROGRESS:
					sendProgress();
					break;

				case CANCEL:
					// use the same function
					operationTimeoutOrCancel();
					break;
				}
			} else if (message instanceof MyResponse) {
				final MyResponse myResponse = (MyResponse) message;
				handleHttpWorkerResponse(myResponse);
			} else /*if (message instanceof InitOperationMessage) {
				
			} else */{
				unhandled(message);
			}
		} catch (Exception e) {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			reply(true, e.toString(), sw.toString(), VarUtils.NA);
		}
	}

	private final void handleHttpWorkerResponse(
			MyResponse myResponse) throws Exception {
		// Successful response from GenericAsyncHttpWorker

		// Jeff 20310411: use generic response
		
		agentResponse = new GenericAgentResponse(myResponse.getResponse(),
				DateUtils.getNowDateTimeStrSdsm(), getSelf().path().toString());
		if (myResponse.isError()) {
			reply(true, myResponse.getErrorMessage(), myResponse.getStackTrace(),  myResponse.getStatusCode());
		}else{
		
			// agentResponse = mapper.readValue(r.getResponse(),
			// request.getResponseType());
			if (agentResponse.getErrorCode() > 0)
				agentResponse.setError(true);
			
			reply(agentResponse.isError(), agentResponse.getErrorMessage(), null,  myResponse.getStatusCode());
			
		}
		
		
	}// end func

	private final void processRequest() {

		// the first time dont count. the first time will delay
		if (!hasBeenDelayed) {

			// jeff only the first time will pause

			//sender = getSender();
			startTimeMillis = System.currentTimeMillis();

			validateRequest(request);
			timeoutDuration = Duration.create(
					request.getMaxOperationTimeSeconds(), TimeUnit.SECONDS);

			hasBeenDelayed = true;
			/**
			 * 20131013 if it is 0; no need to schedule another message.
			 */
			if (this.request.getPauseIntervalBeforeSendMillis() != 0L) {
				long MAX_PAUSE_INTERVAL_MILLIS = 600000L; // 600 sec
				long pauseIntervalWorkerMillis = Math.min(
						MAX_PAUSE_INTERVAL_MILLIS,
						this.request.getPauseIntervalBeforeSendMillis());
				/**
				 * Migrate to akka 2.3.3
				 */
				getContext()
						.system()
						.scheduler()
						.scheduleOnce(
								Duration.create(pauseIntervalWorkerMillis,
										TimeUnit.MILLISECONDS),
								getSelf(),
								OperationWorker.MessageType.PROCESS_REQUEST,
								getContext().system().dispatcher(), getSelf());
				return;

			}
		}

		/**
		 * 20130917: change to add uniform target node capability
		 */

		final String trueTargetNode = (hostUniform == null) ? host
				: hostUniform;

		
		/**
		 * 20140429: change to based on agent command type SSH? HTTP to decide to call whether SshWorker or HttpWorker 
		 * COMMAND_PREFIX_SSH
		 */
		
		//if(!agentCommandType.contains(VarUtils.COMMAND_PREFIX_SSH)){
		if(protocol!=RequestProtocol.SSH){	
			/**
			 * Migrate to akka 2.3.3
			 */
			asyncWorker = getContext().actorOf(
						Props.create(HttpWorker.class,
								client, protocol, 
								String.format("%s://%s:%d%s",
										protocol.toString(), trueTargetNode, agentPort,
										request.getResourcePath()),
								request.getHttpMethod(), request.getPostData(),
								HTTP_MAX_RETRIES,
								HTTP_RETRY_INTERVAL_MILLIS, httpHeaderType,
								request.getHttpHeaderMap())
					);
		}else{
			
			// 20140428
			/**
			 * Migrate to akka 2.3.3
			 */
			asyncWorker = getContext().actorOf(
						Props.create(SshWorker.class,
								httpHeaderType, 
								request.getPostData(), 
								protocol, 
								String.format("%s@%s",
										request.getResourcePath(), trueTargetNode),
								request.getHttpMethod(),
								HTTP_RETRY_INTERVAL_MILLIS,
								HTTP_MAX_RETRIES
								)
					);

		}
		
		
		asyncWorker.tell(HttpWorker.MessageType.PROCESS_REQUEST,
				getSelf());
		
		
		

		// To handle cases where this operation takes extremely long, schedule a
		// 'timeout' message to be sent to us
		/**
		 * Migrate to akka 2.3.3
		 */
		timeoutMessageCancellable = getContext()
				.system()
				.scheduler()
				.scheduleOnce(timeoutDuration, getSelf(),
						InternalMessageType.OPERATION_TIMEOUT,
						getContext().system().dispatcher(), getSelf());

	}


	private final void operationTimeoutOrCancel() {

		/**
		 * FIXED 20131011; BIGGEST BUG MEMORY LEAK!!! CAUSE MEMROY LEAK IF
		 * DIRECTLY REPLY BACK TO MANAGER after send msg to http worker.
		 * 
		 * Operation worker will be killed first, then the async http worker is
		 * in memory leak!!!
		 * 
		 * LESSON: MUST KILL AND WAIT FOR CHILDREN to reply back before kill
		 * itself!!!!
		 */
		cancelCancellable();
		if (asyncWorker != null) {
			asyncWorker.tell(
					HttpWorker.MessageType.PROCESS_ON_TIMEOUT,
					getSelf());

		} else {
			models.utils.LogUtils.printLogError("asyncWorker is null ptr.");
		}

	}


	private final void cancelCancellable() {
		if (retryMessageCancellable != null
				&& !retryMessageCancellable.isCancelled()) {
			retryMessageCancellable.cancel();
		}
		if (timeoutMessageCancellable != null
				&& !timeoutMessageCancellable.isCancelled()) {
			timeoutMessageCancellable.cancel();
		}
		if (pollMessageCancellable != null
				&& !pollMessageCancellable.isCancelled()) {
			pollMessageCancellable.cancel();
		}
	}

	private final void reply(final boolean error, final String errorMessage,
			final String stackTrace, final String statusCode) {
		if (!sentReply) {
			final long operationTimeMillis = System.currentTimeMillis()
					- startTimeMillis;

			if (sender != null
					&& !sender.equals(getContext().system().deadLetters())) {
				if (agentResponse == null) {
					try {
						agentResponse = request.getResponseType().newInstance();
					} catch (Exception e) {
						models.utils.LogUtils.printLogError(e.getLocalizedMessage()
								+ " in reply() in GAOpWorker");
						agentResponse = new AgentResponse();
					}
				}

				agentResponse.setError(error);
				agentResponse.setErrorMessage(errorMessage);
				agentResponse.setStackTrace(stackTrace);
				agentResponse.setOperationTimeMillis(operationTimeMillis);
				agentResponse.setRequest(request);
				agentResponse.setHost(host);
				agentResponse.setAgentPort(agentPort);
				agentResponse.setProtocol(protocol);
				agentResponse.setStatusCode(statusCode);
				sender.tell(agentResponse, getSelf());
			}

			sentReply = true;

			// Self-terminate
			// getSelf().tell(PoisonPill.getInstance(), null);
			// TODO 20131011

			if (asyncWorker != null) {
				getContext().stop(asyncWorker);
			} else {
				models.utils.LogUtils.printLogError("asyncWorker worker is null ptr.");
			}
		}
	}

	private final void sendProgress() {
		final long operationTimeMillis = System.currentTimeMillis()
				- startTimeMillis;
		if (agentResponse == null) {
			try {
				agentResponse = request.getResponseType().newInstance();
			} catch (Exception e) {
				models.utils.LogUtils.printLogError(e.getLocalizedMessage()
						+ " in sendProgress() in GAOpWorker");
				agentResponse = new AgentResponse();
			}
		}
		agentResponse.setOperationTimeMillis(operationTimeMillis);
		agentResponse.setRequest(request);
		agentResponse.setHost(host);
		agentResponse.setAgentPort(agentPort);
		agentResponse.setProtocol(protocol);

		getSender().tell(agentResponse, getSelf());
	}

	private final static void validateRequest(final AgentRequest request)
			throws IllegalArgumentException {
		if (request.getMaxOperationTimeSeconds() < 1) {
			throw new IllegalArgumentException(
					"maxOperationTimeSeconds should be positive");
		}

		if (request.isPollable()
				&& request.getPollIntervalMillis() > 1000L * request
						.getMaxOperationTimeSeconds()) {
			throw new IllegalArgumentException(
					"pollIntervalMillis should be less than maxOperationTimeSeconds for pollable operation");
		}
	}

}
