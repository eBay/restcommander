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
package models.asynchttp.response;

import java.io.Serializable;

import models.asynchttp.RequestProtocol;
import models.asynchttp.request.AgentRequest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
/**
 * 
 * @author ypei
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentResponse implements Serializable{
	// Response attributes
	private boolean error = false;
	private String errorMessage;
	private int errorCode;
	private String stackTrace;
	private long operationTimeMillis;

	private String statusCode;

	// jeff for generic agent response
	protected AgentResponse(AgentResponse orig) {
		super();
		this.error = orig.error;
		this.errorMessage = orig.errorMessage;
		this.errorCode = orig.errorCode;
		this.stackTrace = orig.stackTrace;
		this.operationTimeMillis = orig.operationTimeMillis;
		this.request = orig.request;
		this.host = orig.host;
		this.agentPort = orig.agentPort;
		this.protocol = orig.protocol;

		this.statusCode = orig.statusCode;
	}

	// Request attributes
	private AgentRequest request;

	// Host details
	private String host;
	private int agentPort;
	private RequestProtocol protocol;

	public AgentResponse() {
		super();
	}

	@JsonProperty("isError")
	public boolean isError() {
		return error;
	}

	@JsonProperty("isError")
	public void setError(boolean error) {
		this.error = error;
	}

	@JsonProperty("errorMsg")
	public String getErrorMessage() {
		return errorMessage;
	}

	@JsonProperty("errorMsg")
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@JsonProperty("error")
	public int getErrorCode() {
		return errorCode;
	}

	@JsonProperty("error")
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public long getOperationTimeMillis() {
		return operationTimeMillis;
	}

	public void setOperationTimeMillis(long operationTimeMillis) {
		this.operationTimeMillis = operationTimeMillis;
	}

	public AgentRequest getRequest() {
		return request;
	}

	public void setRequest(AgentRequest request) {
		this.request = request;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getAgentPort() {
		return agentPort;
	}

	public void setAgentPort(int agentPort) {
		this.agentPort = agentPort;
	}

	public RequestProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(RequestProtocol protocol) {
		this.protocol = protocol;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public String toString() {
		return "AgentResponse [error=" + error + ", errorMessage="
				+ errorMessage + ", errorCode=" + errorCode + ", stackTrace="
				+ stackTrace + ", operationTimeMillis=" + operationTimeMillis
				+ ", statusCode=" + statusCode + ", request=" + request
				+ ", host=" + host + ", agentPort=" + agentPort + ", protocol="
				+ protocol + "]";
	}

}
