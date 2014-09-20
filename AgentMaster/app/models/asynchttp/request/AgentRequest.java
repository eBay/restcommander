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
package models.asynchttp.request;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import models.asynchttp.HttpMethod;
import models.asynchttp.response.AgentResponse;

import org.codehaus.jackson.annotate.JsonIgnore;


public abstract class AgentRequest implements Serializable{
	private final int maxTries;
	private final long retryIntervalMillis;
	private final long pollIntervalMillis;
	private final int maxOperationTimeSeconds;
	private final int statusChangeTimeoutSeconds;
	private final boolean pollable;
	private final Class<? extends AgentResponse> responseType;
	private final long pauseIntervalBeforeSendMillis;

	private final Map<String, String> httpHeaderMap = new HashMap<String,String>();
	public AgentRequest(int maxTries, long retryIntervalMillis,
			long pollIntervalMillis, int maxOperationTimeSeconds,
			int statusChangeTimeoutSeconds, boolean pollable,
			Class<? extends AgentResponse> responseType,
			long pauseIntervalBeforeSendMillis,
			Map<String, String> httpHeaderMap
			) {
		super();
		this.maxTries = maxTries;
		this.retryIntervalMillis = retryIntervalMillis;
		this.pollIntervalMillis = pollIntervalMillis;
		this.maxOperationTimeSeconds = maxOperationTimeSeconds;
		this.statusChangeTimeoutSeconds = statusChangeTimeoutSeconds;
		this.pollable = pollable;
		this.responseType = responseType;
		this.pauseIntervalBeforeSendMillis = pauseIntervalBeforeSendMillis;
		this.httpHeaderMap.putAll(httpHeaderMap);
	}

	@JsonIgnore
	public int getMaxTries() {
		return maxTries;
	}

	@JsonIgnore
	public long getRetryIntervalMillis() {
		return retryIntervalMillis;
	}

	@JsonIgnore
	public long getPollIntervalMillis() {
		return pollIntervalMillis;
	}

	@JsonIgnore
	public int getMaxOperationTimeSeconds() {
		return maxOperationTimeSeconds;
	}

	@JsonIgnore
	public int getStatusChangeTimeoutSeconds() {
		return statusChangeTimeoutSeconds;
	}

	@JsonIgnore
	public boolean isPollable() {
		return pollable;
	}

	@JsonIgnore
	public Class<? extends AgentResponse> getResponseType() {
		return responseType;
	}

	@JsonIgnore
	public abstract String getResourcePath();

	@JsonIgnore
	public abstract HttpMethod getHttpMethod();

	@JsonIgnore
	public abstract String getPostData();

	@JsonIgnore
	public long getPauseIntervalBeforeSendMillis() {
		return pauseIntervalBeforeSendMillis;
	}

	@JsonIgnore
	public Map<String, String> getHttpHeaderMap() {
		return httpHeaderMap;
	}

	@Override
	public String toString() {
		return "AgentRequest [maxTries=" + maxTries + ", retryIntervalMillis="
				+ retryIntervalMillis + ", pollIntervalMillis="
				+ pollIntervalMillis + ", maxOperationTimeSeconds="
				+ maxOperationTimeSeconds + ", statusChangeTimeoutSeconds="
				+ statusChangeTimeoutSeconds + ", pollable=" + pollable
				+ ", responseType=" + responseType + "]";
	}

}
