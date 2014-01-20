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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Principle: Key problem: when sync a new list in progress: should not affect
 * the current result
 * 
 * TODO: Hard part: check if the inprogress copy is complete:
 *  Goal: when sync new data; if agent request; should still have a full copy of old data. 
 * the new data is saved in the _progress objects.   Only when the in progress data is COMPELTE; will overwrite the valid copy 
 * 
 * @author ypei
 * 
 */
public class NodeGroupDataMap {

	private String nodeGroupType;

	// node list can be obtained from nodeDataMap
	
	// Map<DataType, dataToAgent>; CAREFUL: when node list is resync, this needs
	// to be reset!
	private final Map<String, NodeData> nodeDataMapValid = new HashMap<String, NodeData>();
	private final Map<String, NodeData> nodeDataMapInProgress = new HashMap<String, NodeData>();

	public NodeGroupDataMap(String nodeGroupType) {
		super();
		this.nodeGroupType = nodeGroupType;
	}

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public void setNodeGroupType(String nodeGroupType) {
		this.nodeGroupType = nodeGroupType;
	}

	public Map<String, NodeData> getNodeDataMapValid() {
		return nodeDataMapValid;
	}

	public Map<String, NodeData> getNodeDataMapInProgress() {
		return nodeDataMapInProgress;
	}
	
	
	public Map<String, NodeData> getNodeGroupDataMapValidForSingleCommand(String agentCommandType){
		
		Map<String, NodeData> nodeDataMapValidForSingleCommand = new HashMap<String,NodeData>();
		
		try{
			for (Entry<String, NodeData> entry : nodeDataMapValid.entrySet()) {
				String fqdn = entry.getKey();
				
				NodeData nodeDataValid = entry.getValue();
				// same ptr: still ptr the one in valid
				NodeReqResponse nrrValid = nodeDataValid.getDataMap().get(agentCommandType);
						
				NodeData nodeDatanew = new NodeData(fqdn);
				// same ptr: still ptr the one in valid
				nodeDatanew.getDataMap().put(agentCommandType, nrrValid);
				
				nodeDataMapValidForSingleCommand.put(fqdn, nodeDatanew);
			}
			
		}catch(Throwable t){
			t.printStackTrace();
		}
		
		
		return nodeDataMapValidForSingleCommand;
	}// end func.

	
	public Map<String, NodeData> getNodeGroupDataMapValidForMultipleCommands(List<String> agentCommandTypes){
		
		Map<String, NodeData> nodeDataMapValidForSingleCommand = new HashMap<String,NodeData>();
		
		try{
			for (Entry<String, NodeData> entry : nodeDataMapValid.entrySet()) {
				String fqdn = entry.getKey();
				
				NodeData nodeDataValid = entry.getValue();
				// same ptr: still ptr the one in valid
				
				NodeData nodeDatanew = new NodeData(fqdn);
				for(String agentCommandType : agentCommandTypes){
					
					NodeReqResponse nrrValid = nodeDataValid.getDataMap().get(agentCommandType);
					
					// same ptr: still ptr the one in valid
					nodeDatanew.getDataMap().put(agentCommandType, nrrValid);
					
				}
				
				nodeDataMapValidForSingleCommand.put(fqdn, nodeDatanew);
			}
			
		}catch(Throwable t){
			t.printStackTrace();
		}
		
		
		return nodeDataMapValidForSingleCommand;
	}// end func.

	
}
