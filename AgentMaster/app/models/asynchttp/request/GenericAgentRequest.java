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

import models.asynchttp.HttpMethod;
import models.asynchttp.response.GenericAgentResponse;
import models.utils.DateUtils;


/**
 * Generic request; Structure is different from other request. In other request,
 * the request itself is mapped to a json to be postData to agent. A
 * 
 * Here, the specific filed of "requestContent" is used as the post data; and
 * resourcePath is used as the URI path; httpMethod is defined by the user too.
 * 
 * @author ypei 20130410
 * 
 */
public class GenericAgentRequest extends AgentRequest {
	private final String resourcePath;
	private final String requestContent;
	private final String httpMethod;


	public GenericAgentRequest(int maxTries, long retryIntervalMillis,
			long pollIntervalMillis, int maxOperationTimeSeconds,
			int statusChangeTimeoutSeconds, String resourcePath,
			String requestContent, String httpMethod, boolean pollable, long pauseIntervalBeforeSendMillis) {
		super(maxTries, retryIntervalMillis, pollIntervalMillis,
				maxOperationTimeSeconds, statusChangeTimeoutSeconds, pollable,
				GenericAgentResponse.class, pauseIntervalBeforeSendMillis);

		this.resourcePath = resourcePath;
		this.requestContent = requestContent;
		this.httpMethod = httpMethod;
	}

	@Override
	public String getResourcePath() {
		return resourcePath;
	}

	@Override
	public HttpMethod getHttpMethod() {

		try {

			return HttpMethod.valueOf(httpMethod);
		} catch (Throwable t) {
			 models.utils.LogUtils.printLogError
					 ("FATAL ERROR; Does not match any existing HTTP method!!! Using POST as default"
							+ DateUtils.getNowDateTimeStr());
			t.printStackTrace();
			return HttpMethod.POST;
		}
	}

	@Override
	public String getPostData() {
		return requestContent;
	}

	public String getRequestContent() {
		return requestContent;
	}

	@Override
	public String toString() {
		return "GenericAgentRequest [resourcePath=" + resourcePath
				+ ", requestContent=" + requestContent + ", httpMethod="
				+ httpMethod + "]";
	}

}
