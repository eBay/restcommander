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
package models.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class AgentCommandMetadata implements Comparable<AgentCommandMetadata> {

	private String agentCommandType; // this is the PK
	private String httpMethod;
	private String requestUrlPostfix;
	private String requestContentTemplate;

	private String requestProtocol;
	private String requestPort;

	private long pauseIntervalWorkerMillis;
	private int responseExtractIndexStart;
	private int responseExtractIndexEnd;

	// 20131013
	private int maxConcurrency;

	// 20131213
	private String httpHeaderType;

	public AgentCommandMetadata(String agentCommandType, String httpMethod,
			String requestUrlPostfix, String requestContentTemplate,
			String requestProtocol, String requestPort, int maxConcurrency,
			long pauseIntervalWorkerMillis, int responseExtractIndexStart,
			int responseExtractIndexEnd, String httpHeaderType

	) {
		super();
		this.agentCommandType = agentCommandType;
		this.httpMethod = httpMethod;
		this.requestUrlPostfix = requestUrlPostfix;
		this.requestContentTemplate = requestContentTemplate;
		this.requestProtocol = requestProtocol;
		this.requestPort = requestPort;
		this.maxConcurrency = maxConcurrency;
		this.pauseIntervalWorkerMillis = pauseIntervalWorkerMillis;
		this.responseExtractIndexStart = responseExtractIndexStart;
		this.responseExtractIndexEnd = responseExtractIndexEnd;
		this.httpHeaderType = httpHeaderType;
	}

	public String getHttpHeaderType() {
		return httpHeaderType;
	}

	public void setHttpHeaderType(String httpHeaderType) {
		this.httpHeaderType = httpHeaderType;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	/**
	 * THIS DOES NOT COMPLETELY COPY!! MISSING requestContentTemplate, LIST
	 * ELEMENT for easy JSON passing to html template!!
	 * 
	 * @param other
	 */
	public AgentCommandMetadata(AgentCommandMetadata other

	) {
		super();
		this.agentCommandType = other.agentCommandType;
		this.httpMethod = other.httpMethod;
		// this.requestUrlPostfix = other.requestUrlPostfix;
		// this.requestContentTemplate = other.requestContentTemplate;
		this.requestProtocol = other.requestProtocol;
		this.requestPort = other.requestPort;
		this.pauseIntervalWorkerMillis = other.pauseIntervalWorkerMillis;
		this.responseExtractIndexStart = other.responseExtractIndexStart;
		this.responseExtractIndexEnd = other.responseExtractIndexEnd;
		this.maxConcurrency = other.maxConcurrency;
	}

	public String getRequestProtocol() {
		return requestProtocol;
	}

	public void setRequestProtocol(String requestProtocol) {
		this.requestProtocol = requestProtocol;
	}

	public String getRequestPort() {
		return requestPort;
	}

	public void setRequestPort(String requestPort) {
		this.requestPort = requestPort;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public void setAgentCommandType(String agentCommandType) {
		this.agentCommandType = agentCommandType;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getRequestUrlPostfix() {
		return requestUrlPostfix;
	}

	public void setRequestUrlPostfix(String requestUrlPostfix) {
		this.requestUrlPostfix = requestUrlPostfix;
	}

	public String getRequestContentTemplate() {
		return requestContentTemplate;
	}

	public void setRequestContentTemplate(String requestContentTemplate) {
		this.requestContentTemplate = requestContentTemplate;
	}

	public long getPauseIntervalWorkerMillis() {
		return pauseIntervalWorkerMillis;
	}

	public void setPauseIntervalWorkerMillis(long pauseIntervalWorkerMillis) {
		this.pauseIntervalWorkerMillis = pauseIntervalWorkerMillis;
	}

	public int getResponseExtractIndexStart() {
		return responseExtractIndexStart;
	}

	public void setResponseExtractIndexStart(int responseExtractIndexStart) {
		this.responseExtractIndexStart = responseExtractIndexStart;
	}

	public int getResponseExtractIndexEnd() {
		return responseExtractIndexEnd;
	}

	public void setResponseExtractIndexEnd(int responseExtractIndexEnd) {
		this.responseExtractIndexEnd = responseExtractIndexEnd;
	}

	@Override
	public String toString() {
		return "AgentCommandMetadata [agentCommandType=" + agentCommandType
				+ ", httpMethod=" + httpMethod + ", requestUrlPostfix="
				+ requestUrlPostfix + ", requestContentTemplate="
				+ requestContentTemplate + ", requestProtocol="
				+ requestProtocol + ", requestPort=" + requestPort + "]";
	}

	public static String replaceFullRequestContent(
			String requestContentTemplate, String replacementString) {
		return (requestContentTemplate.replace(
				VarUtils.AGENT_COMMAND_VAR_DEFAULT_REQUEST_CONTENT,
				replacementString));
	}

	// only replace "$AM_FULL_CONTENT" by ""
	public static String replaceDefaultFullRequestContent(
			String requestContentTemplate) {
		return replaceFullRequestContent(requestContentTemplate, "");
	}

	/**
	 * THIS DOES NOT COMPLETELY COPY!! MISSING requestContentTemplate, LIST
	 * ELEMENT for easy JSON passing to html template!!
	 * 
	 * @param other
	 */
	public static List<AgentCommandMetadata> convertMapToList(
			Map<String, AgentCommandMetadata> map) {

		List<AgentCommandMetadata> list = new ArrayList<AgentCommandMetadata>();
		for (Entry<String, AgentCommandMetadata> entry : map.entrySet()) {

			AgentCommandMetadata copyWithoutList = new AgentCommandMetadata(
					entry.getValue());
			list.add(copyWithoutList);
		}

		return list;

	}

	@Override
	public int compareTo(AgentCommandMetadata o) {

		String agentCommandTypeOther = ((AgentCommandMetadata) o).agentCommandType;

		// ascending order
		return this.agentCommandType.compareTo(agentCommandTypeOther);

	}

}
