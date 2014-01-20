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
package models.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import models.asynchttp.RequestProtocol;
import models.asynchttp.actors.OperationWorker;
import models.data.HttpHeaderMetadata;
import models.data.providers.AgentDataProvider;

/**
 * Adding http header logic TODO; should finally be data driven and specific for
 * each command. Now it is defined here and user can easily change.
 * 
 * @author ypei
 * 
 */
public class MyHttpUtils {

	
	public static final String STR_HTTP_HEADER_TYPE_LBMS = "HEADER_LBMS";
	public static final String STR_HTTP_HEADER_TYPE_LBMS_ASYNC = "HEADER_LBMS_ASYNC";
	public static final String STR_HTTP_HEADER_TYPE_UDNS = "HEADER_UDNS";
	

	public static void addHeaders(BoundRequestBuilder builder,
			Map<String, String> headerMap) {
		for (Entry<String, String> entry : headerMap.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();
			builder.addHeader(name, value);
		}

	}

	/**
	 *  20131215
	 * Called in httpWorker
	 * @param builder
	 * @param protocol
	 */
	public static void addAllHeadersFromHeaderMetadataMap(BoundRequestBuilder builder,
			String httpHeaderType) {
		if (builder != null && httpHeaderType != null) {

			AgentDataProvider adp = AgentDataProvider.getInstance();
			HttpHeaderMetadata httpHeaderMetadata = adp.headerMetadataMap.get(httpHeaderType);
			addHeaders(builder, httpHeaderMetadata.getHeaderMap());

			// add dynamic headers
			addDynamicHeaders(builder, httpHeaderType);
			
		} else {
			 models.utils.LogUtils.printLogError
					 ("Failed to build request; builder is null, unknown method in createRequest()");
		}
	}
	
	public static void addDynamicHeaders(BoundRequestBuilder builder,
			String httpHeaderType) {
		if (builder != null && httpHeaderType != null) {

			/**
			 * TODO: for further extension
			 */
//			if (httpHeaderType.equalsIgnoreCase(MyHttpUtils.TBC)
//					){
//				builder.addHeader("DynamicHeaderKey", HeaderUtils.addDynamicHeader(arg));
//			}
			
		} else {
			 models.utils.LogUtils.printLogError
					 ("Failed to build request; builder is null, unknown method in createRequest()");
		}
	}
	
	public static String readHttpRequestPostData(InputStream httpBody) {

		String postData = null;

		if (httpBody == null) {
			return postData;
		}

		int STRING_BUILDER_DEFAULT_CAPACITY = 512;
		// first to read from http request of the
		InputStreamReader inputStreamReader;
		StringBuilder stringBuilder = new StringBuilder(STRING_BUILDER_DEFAULT_CAPACITY);

		try {
			inputStreamReader = new InputStreamReader(httpBody,
					VarUtils.INPUT_STREAM_READ_HTTP_TEXT_ENCODING_UTF);
			BufferedReader bufferedReader = new BufferedReader(
					inputStreamReader);

			// From now on, the right way of moving from bytes to utf-8
			// characters:

			int singleCharRead;

			while ((singleCharRead = bufferedReader.read()) != -1) {
				stringBuilder.append((char) singleCharRead);
			}

			inputStreamReader.close();
			bufferedReader.close();
		} catch (Throwable e) {

			VarUtils.printSysErrWithTimeAndOptionalReason(
					"readHttpRequestPost", e.getLocalizedMessage());
			e.printStackTrace();
		}

		postData = stringBuilder.toString();
		models.utils.LogUtils.printLogNormal("http Post data: " + postData);

		return postData;

	}
}
