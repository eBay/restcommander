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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Generic
 * 
 * @author ypei
 * 
 */
public class GenericAgentResponse extends AgentResponse implements Serializable{

	public GenericAgentResponse(String responseContent, String receiveTime, String workerPath) {
		super();
		this.responseContent = responseContent;
		this.receiveTime = receiveTime;
		this.workerPath = workerPath;
	}
	
	public GenericAgentResponse(AgentResponse agentResponse, String receiveInManagerTime) {
		super(agentResponse);
		this.responseContent = "";
		this.receiveTime = "UNKNOWN: received in manager time: " + receiveInManagerTime;
		
		if(agentResponse!=null){
			this.setErrorMessage(agentResponse.getErrorMessage()) ;
		}
		
	}

	private String workerPath;
	
	private String responseContent;

	private String receiveTime;

	public String getReceiveTime() {
		return receiveTime;
	}

	public void setReceiveTime(String receiveTime) {
		this.receiveTime = receiveTime;
	}

	public String getResponseContent() {
		return responseContent;
	}

	public void setResponseContent(String responseContent) {
		this.responseContent = responseContent;
	}
	
	public String getWorkerPath() {
		return this.workerPath;
	}

	@Override
	public String toString() {
		return "GenericAgentResponse [responseContent=" + responseContent
				+ ", receiveTime=" + receiveTime + ", isError()=" + isError()
				+ ", getErrorMessage()=" + getErrorMessage()
				+ ", getErrorCode()=" + getErrorCode() + ", getStackTrace()="
				+ getStackTrace() + ", getOperationTimeMillis()="
				+ getOperationTimeMillis() + ", getRequest()=" + getRequest()
				+ ", getHost()=" + getHost() + ", getAgentPort()="
				+ getAgentPort() + ", getProtocol()=" + getProtocol()
				+ ", toString()=" + super.toString() + ", getClass()="
				+ getClass() + ", hashCode()=" + hashCode() + "]";
	}
}
