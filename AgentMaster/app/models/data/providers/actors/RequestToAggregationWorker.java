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
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class RequestToAggregationWorker {

	public final NodeData nodeData;
	public final String agentCommandType;

	public final String errorMsgPatternStr;
	public final String patternStr;

	public NodeData getNodeData() {
		return nodeData;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public RequestToAggregationWorker(NodeData nodeData,
			String agentCommandType, String errorMsgPatternStr,
			String patternStr) {
		super();
		this.nodeData = nodeData;
		this.agentCommandType = agentCommandType;
		this.errorMsgPatternStr = errorMsgPatternStr;
		this.patternStr = patternStr;
	}

	public String getErrorMsgPatternStr() {
		return errorMsgPatternStr;
	}

	public String getPatternStr() {
		return patternStr;
	}

}
