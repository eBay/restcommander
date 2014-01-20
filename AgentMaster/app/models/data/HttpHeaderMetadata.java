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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 20131213
 * @author ypei
 *
 */
public class HttpHeaderMetadata {

	private String httpHeaderType;

	private final Map<String, String> headerMap = new HashMap<String, String>();

	public String getHttpHeaderType() {
		return httpHeaderType;
	}

	public void setHttpHeaderType(String httpHeaderType) {
		this.httpHeaderType = httpHeaderType;
	}

	public Map<String, String> getHeaderMap() {
		return headerMap;
	}

	public HttpHeaderMetadata(String httpHeaderType, Map<String, String> headerMap) {
		super();
		this.httpHeaderType = httpHeaderType;
		this.headerMap.putAll(headerMap);
	}
	
	

}
