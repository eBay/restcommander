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

import static akka.pattern.Patterns.ask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.HttpWorker.MyResponse;
import models.asynchttp.request.AgentRequest;
import models.asynchttp.response.AgentResponse;
import models.asynchttp.response.GenericAgentResponse;
import models.utils.DateUtils;
import models.utils.VarUtils;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import models.data.NodeData;
import models.data.providers.AgentDataAggregator;

/**
 * Ning based
 * 
 * @author ypei
 * 
 */
public class AggregationWorker extends UntypedActor {

	private String fqdn;
	private RequestToAggregationWorker request = null;
	private ActorRef sender = null;
	private boolean sentReply = false;

	public enum MessageType {
		PROCESS_REQUEST, GET_PROGRESS, CANCEL
	}

	private enum InternalMessageType {
		POLL_PROGRESS, OPERATION_TIMEOUT
	}

	public AggregationWorker(final RequestToAggregationWorker request,
			final String fqdn) {
		super();

		this.fqdn = fqdn;
		this.request = request;

	}

	@Override
	public void onReceive(Object message) throws Exception {
		try {
			if (message instanceof InternalMessageType) {
				switch ((InternalMessageType) message) {

				case OPERATION_TIMEOUT:
					operationTimeoutOrCancel();
					break;
				}
			} else if (message instanceof MessageType) {
				switch ((MessageType) message) {
				case PROCESS_REQUEST:
					sender = getSender();
					processRequest();
					break;

				case CANCEL:
					// use the same function
					operationTimeoutOrCancel();
					break;
				}
			} else {
				unhandled(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			boolean isError = true;
			reply(fqdn, VarUtils.NA, VarUtils.NA, isError, request);
			getContext().stop(getSelf());
		}
	}

	public void processRequest() {

		
		String errorMsgPatternStr = request.getErrorMsgPatternStr();

		String agentCommandType = request.getAgentCommandType();

		final String response = request.getNodeData().getDataMap()
				.get(agentCommandType).getResponseContent().getResponse();

		final boolean isError = request.getNodeData().getDataMap()
				.get(agentCommandType).getResponseContent().isError();

		final String errorMsg = request.getNodeData().getDataMap()
				.get(agentCommandType).getResponseContent().getErrorMessage();

		String extractErrorMsg = (errorMsg == null) ? "ErrorMsg is empty"
				: AgentDataAggregator.stringMatcherByPattern(errorMsg,
						errorMsgPatternStr);

		// be careful when the response is NULL; handled inside of
		// stringMatcherByPattern. will be -1 .
		// Therefore the final hash map key will not be null.
		String extractedResponse = AgentDataAggregator.stringMatcherByPattern(
				response, request.getPatternStr());

		reply(fqdn, extractedResponse, extractErrorMsg, isError, request);

	}

	private final void operationTimeoutOrCancel() {

		//  should return;
		boolean isError = true;
		reply(fqdn, VarUtils.NA, VarUtils.NA, isError, request);
		getContext().stop(getSelf());
	}

	private final void reply(String fqdn, String extractedResponse,
			String extractErrorMsg, boolean isError,
			RequestToAggregationWorker request) {
		if (!sentReply) {

			if (sender != null
					&& !sender.equals(getContext().system().deadLetters())) {

				ResponseToAggregationManagerFromWorker responseToManager = new ResponseToAggregationManagerFromWorker(
						fqdn, extractedResponse, extractErrorMsg, isError,
						request);

				sender.tell(responseToManager, getSelf());
			}
			sentReply = true;
		}
	}

}
