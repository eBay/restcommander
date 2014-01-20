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
package models.data.providers;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;

import models.data.LogFile;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.FileIoUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.ADHOC_COMPONENT_TYPE;
/**
 * 
 * @author ypei
 *
 */
public class NodeGroupProvider {

	private static final NodeGroupProvider instance = new NodeGroupProvider();

	public static NodeGroupProvider getInstance() {
		return instance;
	}

	private NodeGroupProvider() {

	}

	
	public static List<NodeGroupSourceMetadata> getNodeGroupSourceMetadatas(boolean isAdhoc){
		
		List<NodeGroupSourceMetadata> resultList = new ArrayList<NodeGroupSourceMetadata>();
		
		if(isAdhoc){
			resultList.addAll(AgentDataProvider.adhocNodeGroups.values());
			
		}else{
			resultList.addAll(AgentDataProvider.nodeGroupSourceMetadatas.values());
		}
		
		Collections.sort(resultList,Collections.reverseOrder());
		
		return resultList;
	} 
	
	public static NodeGroupSourceMetadata getNodeGroupSourceMetadata(boolean isAdhoc, String nodeGroupType){
		
		if(isAdhoc){
			NodeGroupSourceMetadata nodeGroupSourceMetadata = AgentDataProvider.adhocNodeGroups.get(nodeGroupType);
			
		
			return nodeGroupSourceMetadata;
					
			
			
		}else{
			return AgentDataProvider.nodeGroupSourceMetadatas.get(nodeGroupType);
		}
	} 
	
	
	public static List<String> getNodeList(boolean isAdhoc, String nodeGroupType){
		
		List<String> nodeList = new ArrayList<String>();
		NodeGroupSourceMetadata nodeGroupSourceMetadata = null;
		if(isAdhoc){
			nodeGroupSourceMetadata =  AgentDataProvider.adhocNodeGroups.get(nodeGroupType);
		}else{
			nodeGroupSourceMetadata = AgentDataProvider.nodeGroupSourceMetadatas.get(nodeGroupType);
		}
		if(nodeGroupSourceMetadata!=null){
			
			nodeList = nodeGroupSourceMetadata.getNodeList();
		}else{
			models.utils.LogUtils.printLogError("ERROR FINDING NODE GROUP TYPE " + nodeGroupType + " in NodeGroupProvider.getNodeList() at time " + DateUtils.getNowDateTimeStrSdsm() );
			
		}
		return nodeList;
		
	} 
	

	/**
	 * ! Attn: this only generate inside of AgentDataProvider.adhocNodeGroups
	 * 
	 * ASSUMPTION: only 1 node group per timestamp:
	 * 
	 * @param nodeList
	 * @return
	 */
	public static String generateAdhocNodeGroupHelper(List<String> nodeList) {

		String nodeGroupType = VarUtils.ADHOC_NODEGROUP_PREFIX
				+ DateUtils.getNowDateTimeStrConcise();

		NodeGroupSourceMetadata metadata = new NodeGroupSourceMetadata(
				VarUtils.ADHOC, nodeGroupType, nodeList);

		AgentDataProvider.adhocNodeGroups.put(nodeGroupType, metadata);

		//20130924: put into the folder
		LogProvider.genFilePathAndSaveLogAdhocComponents(ADHOC_COMPONENT_TYPE.NODE_GROUP, nodeGroupType,  metadata);
		
		// update the dataStore;
		AgentConfigProvider acp = AgentConfigProvider.getInstance();
		acp.initAllAgentDataFromNodeGroupSourceMetadatas(
				AgentDataProvider.adhocAgentData,
				AgentDataProvider.adhocNodeGroups);

		return nodeGroupType;
	}
}
