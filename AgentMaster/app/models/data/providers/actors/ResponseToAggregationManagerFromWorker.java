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

import java.util.Map;

import models.data.AgentCommandMetadata;
import models.data.AggregateData;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class ResponseToAggregationManagerFromWorker {
	public final String fqdn;
	public final String extractedResponse;
	public final String extractErrorMsg;

	public final boolean isError;

	public final RequestToAggregationWorker request;

	public String getFqdn() {
		return fqdn;
	}

	public String getExtractedResponse() {
		return extractedResponse;
	}

	public String getExtractErrorMsg() {
		return extractErrorMsg;
	}

	public ResponseToAggregationManagerFromWorker(String fqdn,
			String extractedResponse, String extractErrorMsg, boolean isError,
			RequestToAggregationWorker request) {
		super();
		this.fqdn = fqdn;
		this.extractedResponse = extractedResponse;
		this.extractErrorMsg = extractErrorMsg;
		this.isError = isError;
		this.request = request;
	}

	public RequestToAggregationWorker getRequest() {
		return request;
	}

	public boolean isError() {
		return isError;
	}

}
