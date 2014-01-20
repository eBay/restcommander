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
package models.agent.batch.commands.message;

import java.util.Map;

import models.data.AgentCommandMetadata;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class InitialRequestToManager {

	public final String nodeGroupType;

	public final String directorJobId;
	public final String agentCommandType;

	public final Map<String, NodeGroupDataMap> dataStore;

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public String getDirectorJobId() {
		return directorJobId;
	}


	public InitialRequestToManager(String nodeGroupType,
			String agentCommandType, String directorJobId,
			Map<String, NodeGroupDataMap> dataStore) {
		super();
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
		this.directorJobId = directorJobId;
		this.dataStore = dataStore;
	}

	public Map<String, NodeGroupDataMap> getDataStore() {
		return dataStore;
	}

}
