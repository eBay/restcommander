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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.asynchttp.response.GenericAgentResponse;
import models.utils.DateUtils;
import models.utils.VarUtils;

/**
 * To save the content request and response By default the responseContent is
 * NULL because dont need it in InProgress entries. But must init!! when in
 * valid entries.
 * 
 * @author ypei
 * 
 */
public class NodeReqResponse implements Serializable{

	private final Map<String, String> requestParameters = new HashMap<String, String>();

	private ResponseContent responseContent = null;

	public NodeReqResponse() {
		super();
	}

	public NodeReqResponse(NodeReqResponse nrr) {
		super();

		responseContent = (nrr.getResponseContent() == null) ? null
				: new ResponseContent(nrr.getResponseContent());

		for (Entry<String, String> entry : nrr.requestParameters.entrySet()) {
			requestParameters.put(entry.getKey(), entry.getValue());
		}

	}

	public static void main(String[] args) {


	}



	/**
	 * 20130507: auto replace part
	 */
	public static String replaceStrByMap(Map<String, String> requestParameters,
			String sourceContent) {

		String sourceContentHelper = sourceContent;

		for (Entry<String, String> entry : requestParameters.entrySet()) {

			String sourceContentHelperNew = sourceContentHelper;
			String varName = entry.getKey();
			String replacement = entry.getValue();

			if (varName.contains(VarUtils.NODE_REQUEST_PREFIX_REPLACE_VAR)) {

				String varTrueName = "$"
						+ varName.replace(
								VarUtils.NODE_REQUEST_PREFIX_REPLACE_VAR, "");
				sourceContentHelperNew = sourceContentHelper.replace(
						varTrueName, replacement);
				sourceContentHelper = sourceContentHelperNew;
			}

		}

		return sourceContentHelper;
	}

	public void setResponseContent(ResponseContent responseContent) {
		this.responseContent = responseContent;
	}

	public void setDefaultReqestContent(String requestFullContent) {

		requestParameters.put(VarUtils.NODE_REQUEST_FULL_CONTENT_TYPE,
				requestFullContent);
	}

	public void setDefaultEmptyReqestContent() {

		requestParameters.put(VarUtils.NODE_REQUEST_FULL_CONTENT_TYPE, "");
	}

	public void setCustomReqestContent(String requestVarName,
			String requestVarContent) {

		requestParameters.put(requestVarName, requestVarContent);
	}

	public Map<String, String> getRequestParameters() {
		return requestParameters;
	}

	public ResponseContent getResponseContent() {
		return responseContent;
	}

	public static class ResponseContent implements Serializable{
		private String response;
		private String responseTime;

		private String errorMessage;

		private String statusCode;

		private boolean isError;

		public ResponseContent(GenericAgentResponse gap) {
			super();

			// actually are pointers for string.... but static string should be
			// fine.
			isError = gap.isError();
			responseTime = gap.getReceiveTime();
			response = gap.getResponseContent();
			errorMessage = gap.getErrorMessage();
			statusCode = gap.getStatusCode();
		}

		public ResponseContent() {
			super();
			isError = false;
			responseTime = DateUtils.getDateTimeStrSdsm(new Date(0L));
			response = VarUtils.NODE_RESPONSE_INIT;
			errorMessage = VarUtils.NODE_RESPONSE_INIT;
			statusCode = VarUtils.NA;
		}

		public ResponseContent(ResponseContent rc) {
			super();

			if (rc != null) {
				this.response = rc.response;
				this.responseTime = rc.responseTime;
				this.errorMessage = rc.errorMessage;
				this.isError = rc.isError;
				this.statusCode = rc.statusCode;

			}

		}

		public String getResponse() {
			return response;
		}

		public void setResponse(String response) {
			this.response = response;
		}

		public String getResponseTime() {
			return responseTime;
		}

		public void setResponseTime(String responseTime) {
			this.responseTime = responseTime;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public boolean isError() {
			return isError;
		}

		public void setError(boolean isError) {
			this.isError = isError;
		}

		public String getStatusCode() {
			return statusCode;
		}

		public void setStatusCode(String statusCode) {
			this.statusCode = statusCode;
		}

		@Override
		public String toString() {
			return "ResponseContent [response=" + response + ", responseTime="
					+ responseTime + ", errorMessage=" + errorMessage
					+ ", statusCode=" + statusCode + ", isError=" + isError
					+ "]";
		}

	}
}
