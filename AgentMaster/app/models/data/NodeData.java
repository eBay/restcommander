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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
/**
 * 
 * @author ypei
 *
 */
public class NodeData {

	private String fqdn;

	// data is a string (likely an json)
	private final Map<String, NodeReqResponse> dataMap = new HashMap<String, NodeReqResponse>();

	public String getFqdn() {
		return fqdn;
	}

	public void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}

	// key is AgentCommandMetadata's agentCommandType String
	public Map<String, NodeReqResponse> getDataMap() {
		return dataMap;
	}

	public NodeData(String fqdn) {
		super();
		this.fqdn = fqdn;
	}

	public NodeData() {
		super();
	}

	public NodeData(NodeData nd) {
		this.fqdn = nd.fqdn;

		for (Entry<String, NodeReqResponse> entry : nd.dataMap.entrySet()) {

			String agentCommandType = entry.getKey();
			// deep copy..
			NodeReqResponse nrr = new NodeReqResponse(entry.getValue());
			dataMap.put(agentCommandType, nrr);
		}
	}

	@Override
	public String toString() {
		return "NodeData [fqdn=" + fqdn + ", dataMap=" + dataMap + "]";
	}

}
