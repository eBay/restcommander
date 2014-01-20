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
import java.util.concurrent.TimeUnit;

import models.asynchttp.HttpMethod;
import models.asynchttp.RequestProtocol;
import models.utils.DateUtils;
import models.utils.ErrorMsgUtils;
import models.utils.MyHttpUtils;
import models.utils.StringUtils;
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
 * @author ypei
 * 
 */
public class HttpWorker extends UntypedActor {
	private final AsyncHttpClient client;
	private final RequestProtocol protocol;
	private final String requestUrl;
	private final HttpMethod httpMethod;
	private final String postData;
	private final String contentType;
	private final int maxTries;
	private final long retryIntervalMillis;


	// 20131215: 
	private final String httpHeaderType;
	
	private ActorRef sender = null;
	private Throwable cause;
	private int tryCount = 0;
	private Cancellable timeoutMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;
	// private FiniteDuration retryDuration = null;
	private boolean sentReply = false;

	public enum MessageType {
		PROCESS_REQUEST, CANCEL, PROCESS_ON_RESPONSE, PROCESS_ON_EXCEPTION, PROCESS_ON_TIMEOUT, PROCESS_ON_TIMEOUT_SO, PROCESS_ON_RESPONSE_DATA
	}

	public HttpWorker(final AsyncHttpClient client,
			final RequestProtocol protocol, final String requestUrl,
			final HttpMethod httpMethod, final String postData,
			final String contentType, final int maxTries,
			final long retryIntervalMillis, final String httpHeaderType

	) {
		this.client = client;
		this.protocol = protocol;
		this.requestUrl = requestUrl;
		this.httpMethod = httpMethod;
		this.postData = postData;
		this.contentType = contentType;
		this.maxTries = maxTries;
		this.retryIntervalMillis = retryIntervalMillis;
		this.httpHeaderType = httpHeaderType;

	}
	

	public BoundRequestBuilder createRequest() {
		BoundRequestBuilder builder = null;

		try {

			String completeUrl = requestUrl;

			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils.printLogError("NING completeUrl " + completeUrl);
			}

			switch (httpMethod) {
			case GET:
				builder = client.prepareGet(completeUrl);
				break;
			case POST:
				builder = client.preparePost(completeUrl);
				break;
			case PUT:
				builder = client.preparePut(completeUrl);
				break;
			case DELETE:
				builder = client.prepareDelete(completeUrl);
				break;
			default:
				break;
			}
			if (builder != null) {

				//MyHttpUtils.addAllHeaders(builder, protocol);
				
				MyHttpUtils.addAllHeadersFromHeaderMetadataMap(builder, httpHeaderType);
//
//				if (httpHeaderTypeNum == OperationWorker.HTTP_HEADER_TYPE_CMS) {
//					builder.addHeader("Authorization", VarUtils.AUTH_CMS_AGENT);
//				}
				
				
				
				if (!StringUtils.isNullOrEmpty(postData)) {
					builder.setBody(postData);
				}

			} else {
				 models.utils.LogUtils.printLogError
						 ("Failed to build request; builder is null, unknown method in createRequest()");
			}
		} catch (Throwable t) {
			models.utils.LogUtils.printLogError("Error in createRequest at "
					+ DateUtils.getNowDateTimeStrSdsm());

			t.printStackTrace();
		}

		return builder;

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
						BoundRequestBuilder request = createRequest();
						// String targetUrl = request.build().getUrl();
						request.execute(new HttpAsyncHandler(this));

						timeoutDuration = Duration
								.create(VarUtils.ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT,
										TimeUnit.SECONDS);

					}

					// To handle cases where nio response never comes back, we
					// schedule a 'timeout' message to be sent to us 2 seconds
					// after NIO's SO_TIMEOUT
					timeoutMessageCancellable = getContext()
							.system()
							.scheduler()
							.scheduleOnce(timeoutDuration, getSelf(),
									MessageType.PROCESS_ON_TIMEOUT,
									getContext().system().dispatcher());

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
							 ("!!!Inside PROCESS_ON_TIMEOUT................."
									+ requestUrl
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

	public void cancelCancellable() {

		// if (nioResponseFuture != null && !nioResponseFuture.isDone()) {
		// nioResponseFuture.cancel(true);
		// }
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
					httpMethod, postData, contentType, maxTries,
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

	public static class MyResponse {
		// Response attributes
		private final String response;
		private final boolean error;
		private final String errorMessage;
		private final String stackTrace;

		private final String statusCode;

		// Request
		private final MyRequest request;

		public MyResponse(RequestProtocol protocol, String requestUrl,
				HttpMethod httpMethod, String postData, String contentType,
				int maxTries, long retryIntervalMillis, String response,
				boolean error, String errorMessage, String stackTrace,
				String statusCode) {
			super();

			this.request = new MyRequest(protocol, requestUrl, httpMethod,
					postData, contentType, maxTries, retryIntervalMillis);
			this.response = response;
			this.error = error;
			this.errorMessage = errorMessage;
			this.stackTrace = stackTrace;
			this.statusCode = statusCode;
		}

		public String getResponse() {
			return response;
		}

		public boolean isError() {
			return error;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public String getStackTrace() {
			return stackTrace;
		}

		public MyRequest getRequest() {
			return request;
		}

		public String getStatusCode() {
			return statusCode;
		}
		
		@Override
		public String toString() {
			return "Response [response=" + response + ", error=" + error
					+ ", errorMessage=" + errorMessage + ", stackTrace="
					+ stackTrace + ", request=" + request + "]";
		}

		public static class MyRequest {
			private final RequestProtocol protocol;
			private final String requestUrl;
			private final HttpMethod httpMethod;
			private final String postData;
			private final String contentType;
			private final int maxTries;
			private final long retryIntervalMillis;

			public MyRequest(RequestProtocol protocol, String requestUrl,
					HttpMethod httpMethod, String postData, String contentType,
					int maxTries, long retryIntervalMillis) {
				super();
				this.protocol = protocol;
				this.requestUrl = requestUrl;
				this.httpMethod = httpMethod;
				this.postData = postData;
				this.contentType = contentType;
				this.maxTries = maxTries;
				this.retryIntervalMillis = retryIntervalMillis;
			}

			public RequestProtocol getProtocol() {
				return protocol;
			}

			public String getRequestUrl() {
				return requestUrl;
			}

			public HttpMethod getHttpMethod() {
				return httpMethod;
			}

			public String getPostData() {
				return postData;
			}

			public String getContentType() {
				return contentType;
			}

			public int getMaxTries() {
				return maxTries;
			}

			public long getRetryIntervalMillis() {
				return retryIntervalMillis;
			}

			@Override
			public String toString() {
				return "Request [protocol=" + protocol + ", requestUrl="
						+ requestUrl + ", httpMethod=" + httpMethod
						+ ", postData=" + postData + ", contentType="
						+ contentType + ", maxTries=" + maxTries
						+ ", retryIntervalMillis=" + retryIntervalMillis + "]";
			}
		}

	}// end subclass

	/**
	 * NING handler wrapper
	 * 
	 */
	static class HttpAsyncHandler extends AsyncCompletionHandler<MyResponse> {
		private final HttpWorker httpWorker;

		public HttpAsyncHandler(HttpWorker httpWorker) {
			this.httpWorker = httpWorker;
		}

		@Override
		public MyResponse onCompleted(Response response) throws Exception {

			MyResponse myResponse = null;
			httpWorker.onComplete(response);
			return myResponse;
		}

		@Override
		public void onThrowable(Throwable t) {
			httpWorker.onThrowable(t);
		}

	}

	public MyResponse onComplete(Response response) {

		cancelCancellable();
		try {

			String statusCode = response.getStatusCode() + " " + response.getStatusText();
			
			reply(response.getResponseBody(), false, null, null, statusCode);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void onThrowable(Throwable cause) {
		this.cause = cause;
		getSelf().tell(MessageType.PROCESS_ON_EXCEPTION, getSelf());

	}

}
