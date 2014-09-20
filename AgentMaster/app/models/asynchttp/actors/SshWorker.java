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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.asynchttp.HttpMethod;
import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.HttpWorker.MessageType;
import models.asynchttp.actors.HttpWorker.MyResponse;
import models.data.providers.ssh.SshProvider;
import models.data.providers.ssh.SshProvider.SshResponse;
import models.utils.DateUtils;
import models.utils.ErrorMsgUtils;
import models.utils.MyHttpUtils;
import models.utils.StringUtils;
import models.utils.TokenUtils;
import models.utils.VarUtils;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
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
 * TODO: take out the MyResponse to be a common one
 * 
 * @author ypei
 * 
 */
public class SshWorker extends UntypedActor {

	private final String passwd; // using httpHeaderType
	private final String commandSshLine;  // to replace postData
	
	private final RequestProtocol protocol; // to use SSH
	private final String requestUrl; // user@host
	private final HttpMethod httpMethod;  // NA
	// 20131215: 
	private final long retryIntervalMillis;
	

	
	private final int maxTries;
	private ActorRef sender = null;
	private Throwable cause;
	private int tryCount = 0;
	private Cancellable timeoutMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;
	// private FiniteDuration retryDuration = null;
	private boolean sentReply = false;




	public SshWorker(String passwd, String commandSshLine,
			RequestProtocol protocol, String requestUrl, HttpMethod httpMethod,
			long retryIntervalMillis, int maxTries) {
		super();
		this.passwd = passwd;
		this.commandSshLine = commandSshLine;
		this.protocol = protocol;
		this.requestUrl = requestUrl;
		this.httpMethod = httpMethod;
		this.retryIntervalMillis = retryIntervalMillis;
		this.maxTries = maxTries;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		try {
			if (message instanceof MessageType) {
				switch ((MessageType) message) {
				case PROCESS_REQUEST:
					tryCount++;

					if (tryCount == 1) {
						sender = getSender();

						// Construct and submit NING Request
				
						SshResponse sshResponse = new SshResponse();

						timeoutDuration = Duration
								.create(VarUtils.ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT,
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

						/**
						 * This part become synchronize
						 */
						sshResponse = SshProvider.executeSshCommand(requestUrl, passwd, commandSshLine);
						onComplete(sshResponse);
						
					}

					break;

				case CANCEL:
					cancelCancellable();
					reply(null, true, "RequestCanceled", null, VarUtils.NA);
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
					reply(null, true, detailErrorMsgWithStackTrace,
							sw.toString(), VarUtils.NA);

					break;

				case PROCESS_ON_TIMEOUT:
					 models.utils.LogUtils.printLogError
							 ("!!!Inside PROCESS_ON_TIMEOUT.................commandSshLine: "
									+ commandSshLine
									+ "......... at "
									+ DateUtils.getNowDateTimeStrSdsm());
					cancelCancellable();

					// 20130801 To match this:
					// PATTERN_EXTRACT_EXCEPTION_SUMMARY_FROM_ERROR_MSG to get
					// the regular
					// expression.
					String errorMsg = String
							.format("HttpWorker Timedout after %d SEC (no response but no exception catched). Check URL: may be very slow or stuck. Details more info",
									VarUtils.ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT);

					reply(null, true, errorMsg, errorMsg, VarUtils.NA);
					break;

				case PROCESS_ON_RESPONSE_DATA:
					models.utils.LogUtils.printLogNormal
							 ("Inside PROCESS_ON_RESPONSE_DATA UnsupportedOperation.................");
					cancelCancellable();
					reply(null, true, "UnsupportedOperation", null, VarUtils.NA);
					break;
				}
			} else {
				unhandled(message);
			}
		} catch (Exception e) {
			tryCount = maxTries;
			this.cause = e;
			getSelf().tell(MessageType.PROCESS_ON_EXCEPTION, getSelf());
		}
	}

	public MyResponse onComplete(SshResponse sshResponse) {

		cancelCancellable();
		try {

			String statusCode =""+ sshResponse.getExitStatus();
			
			reply(sshResponse.getResponseContent(), sshResponse.isError(), sshResponse.getErrorMessage(), null, statusCode);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public void cancelCancellable() {

		if (timeoutMessageCancellable != null) {
			timeoutMessageCancellable.cancel();
		}
	}

	private void reply(final String response, final boolean error,
			final String errorMessage, final String stackTrace,
			final String statusCode) {

		/**
		 * this is needed if NIO has not even send out! MEMROY LEAK if not.
		 * double check
		 */

		if (!sentReply) {
			final MyResponse res = new MyResponse(protocol, requestUrl,
					httpMethod, commandSshLine, maxTries,
					retryIntervalMillis, response, error, errorMessage,
					stackTrace, statusCode);
			
			
			if (!getContext().system().deadLetters().equals(sender)) {
				sender.tell(res, getSelf());
			}

			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils.printLogError("DEBUG: real response: " + response
						+ " err: " + errorMessage);
			}

			sentReply = true;
		}

		// Self-terminate; should have no need. the Operation Worker will kill
		// it
		// getSelf().tell(PoisonPill.getInstance(), null);
		// getContext().stop(getSelf());
	}



	public void onComplete(Response response) {

		cancelCancellable();
		try {

			String statusCode = response.getStatusCode() + " " + response.getStatusText();
			
			reply(response.getResponseBody(), false, null, null, statusCode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onThrowable(Throwable cause) {
		this.cause = cause;
		getSelf().tell(MessageType.PROCESS_ON_EXCEPTION, getSelf());

	}


	
	

}
