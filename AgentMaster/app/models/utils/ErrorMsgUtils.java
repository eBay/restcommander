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

import java.util.HashMap;
import java.util.Map;
/**
 * 
 * @author ypei
 *
 */
public class ErrorMsgUtils {

	public enum ERROR_TYPE {
		TIMEOUT_EXCEPTION, CONNECTION_EXCEPTION
	}

	public static final Map<ERROR_TYPE, String> errorMapOrig = new HashMap<ERROR_TYPE, String>();
	public static final Map<ERROR_TYPE, String> errorMapReplace = new HashMap<ERROR_TYPE, String>();

	static {
		errorMapOrig
				.put(ERROR_TYPE.TIMEOUT_EXCEPTION,
						"java.util.concurrent.TimeoutException: No response received after");
		errorMapReplace
				.put(ERROR_TYPE.TIMEOUT_EXCEPTION,
						"TimeoutException-SocketTimeoutException. No response received after: CONNECTION_TIMEOUT after "
								+ VarUtils.NING_FASTCLIENT_CONNECTION_TIMEOUT_MS
								/ 1000 + "/" + VarUtils.NING_SLOWCLIENT_CONNECTION_TIMEOUT_MS
								/ 1000  
								+ " SEC or REQUEST_TIMEOUT after "
								+ VarUtils.NING_FASTCLIENT_REQUEST_TIMEOUT_MS
								/ 1000+ "/" + VarUtils.NING_SLOWCLIENT_REQUEST_TIMEOUT_MS
								/ 1000 
								+ " SEC.");

		errorMapOrig.put(ERROR_TYPE.CONNECTION_EXCEPTION,
				"java.net.ConnectException");
		errorMapReplace.put(ERROR_TYPE.CONNECTION_EXCEPTION,
				"java.net.ConnectException");
	}


	public static String replaceErrorMsg(String origMsg) {

		String replaceMsg = origMsg;
		for (ERROR_TYPE errorType : ERROR_TYPE.values()) {

			if (origMsg.contains( errorMapOrig.get(errorType))) {
				replaceMsg = errorMapReplace.get(errorType);
				break;
			}

		}

		return replaceMsg;

	}

}
