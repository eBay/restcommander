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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


import play.Play;
import play.vfs.VirtualFile;

import models.asynchttp.response.GenericAgentResponse;
import models.data.AgentCommandMetadata;
import models.data.HttpHeaderMetadata;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeGroupSourceType;
import models.data.NodeReqResponse;
import models.data.NodeReqResponse.ResponseContent;
import models.utils.DateUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.CONFIG_FILE_TYPE;

/**
 * Singleton: all data is here :-)
 * 
 * @author ypei
 * 
 */
public class AgentDataProvider {

	private static final AgentDataProvider instance = new AgentDataProvider();

	public static AgentDataProvider getInstance() {
		return instance;
	}

	public static Date lastRefreshDataInProgress = new Date(0);
	public static Date lastRefreshDataValid = new Date(0);

	// 20130926: remove final because really want to try to reallocate
	// this is for data of the nodes from config file
	public static final Map<String, NodeGroupDataMap> allAgentData = new HashMap<String, NodeGroupDataMap>();
	public static final Map<String, NodeGroupSourceMetadata> nodeGroupSourceMetadatas = new HashMap<String, NodeGroupSourceMetadata>();
	
	// this is for adhoc request for agent upgrade or other requests
	//20130826:adhocAgentData change from heatmap to treemap; since key has timestamp inside, may help to sort and them trim the old data out as cron. 
	public static final Map<String, NodeGroupDataMap> adhocAgentData = new TreeMap<String, NodeGroupDataMap>();
	//20130826:adhocNodeGroups change from heatmap to treemap; since key has timestamp inside, may help to sort and them trim the old data out as cron.
	public static final Map<String, NodeGroupSourceMetadata> adhocNodeGroups = new TreeMap<String, NodeGroupSourceMetadata>();

	
	public static final Map<String, HttpHeaderMetadata> headerMetadataMap = new HashMap<String, HttpHeaderMetadata>();
	
	/**
	 * All the rest: shared. 
	 */
	
	public static final Map<String, String> aggregationMetadatas = new TreeMap<String, String>();
	public static final Map<String, AgentCommandMetadata> agentCommandMetadatas = new HashMap<String, AgentCommandMetadata>();


	public static Map<String, NodeGroupDataMap> getAllagentdata() {
		return allAgentData;
	}

	private AgentDataProvider() {
		super();
		// update the NodeGroupSourceMetaData from configuration.

	}

	public static Map<String, NodeGroupDataMap> getAdhocAgentdata() {
		return adhocAgentData;
	}

	public static Map<String, String> getAggregationMetadatas() {
		return aggregationMetadatas;
	}

	public static Map<String, NodeGroupSourceMetadata> getAdhocNodegroups() {
		return adhocNodeGroups;
	}

	
	public static Map<String, HttpHeaderMetadata> getHeaderMetadataMap() {
		return headerMetadataMap;
	}

	public static Map<String, String> getAggregationmetadata() {
		return aggregationMetadatas;
	}

	public void resetData() {

		allAgentData.clear();
		nodeGroupSourceMetadatas.clear();
		agentCommandMetadatas.clear();
		aggregationMetadatas.clear();
		
		
		adhocAgentData.clear();
		adhocNodeGroups.clear();

		headerMetadataMap.clear();
		
		lastRefreshDataValid = new Date(0);
		lastRefreshDataInProgress = new Date(0);

		updateConfigFromAllFiles();
	}

	// must not be in constructor; when the nodeGroupSourceMetadatas is still
	// null.
	// update the node list too.
	public void updateConfigFromAllFiles() {
		AgentConfigProvider acp = AgentConfigProvider.getInstance();
		acp.updateNodeGroupSourceMetadatasFromConf();
		acp.updateAgentCommandMetadatasFromConf();
		acp.updateAggregationMetadatasFromConf();


		//20131108 add for http header data driven.
		acp.updateCommonHttpHeaderFromConf();
		// will get the actual node list here

	}

	public String updateConfigFromFile(String configFileType) {

		String result = "success";

		if (configFileType.equalsIgnoreCase(CONFIG_FILE_TYPE.ALL.toString())) {
			updateConfigFromAllFiles();
		} else if (configFileType.equalsIgnoreCase(CONFIG_FILE_TYPE.AGGREGATION
				.toString())) {
			updateAggregationConfigFromFile();
		} else if (configFileType
				.equalsIgnoreCase(CONFIG_FILE_TYPE.AGENTCOMMAND.toString())) {
			updateCommandConfigFromFile();
		} else if (configFileType.equalsIgnoreCase(CONFIG_FILE_TYPE.NODEGROUP
				.toString())) {
			updateNodeGroupConfigFromFile();

		} else if (configFileType.equalsIgnoreCase(CONFIG_FILE_TYPE.WISBVAR
				.toString())) {
			//updateWisbVarConfigFromFile();
		} else if (configFileType.equalsIgnoreCase(CONFIG_FILE_TYPE.HTTPHEADER
				.toString())) {
			updateHttpHeaderConfigFromFile();

		} else {
			result = "Error in reloadConfig with type ?? No such type "
					+ configFileType;
			models.utils.LogUtils.printLogError(result);
		}

		return result;

	}

	public void updateCommandConfigFromFile() {
		AgentConfigProvider acp = AgentConfigProvider.getInstance();
		acp.updateAgentCommandMetadatasFromConf();

	}

	
	public void updateHttpHeaderConfigFromFile() {
		AgentConfigProvider acp = AgentConfigProvider.getInstance();
		acp.updateCommonHttpHeaderFromConf();

	}

	public void updateNodeGroupConfigFromFile() {
		AgentConfigProvider acp = AgentConfigProvider.getInstance();

		acp.updateNodeGroupSourceMetadatasFromConf();

		

	}

	public void updateAggregationConfigFromFile() {
		AgentConfigProvider acp = AgentConfigProvider.getInstance();
		acp.updateAggregationMetadatasFromConf();

	}



	public static void main(String[] args) {
		test();

	}

	public static void test() {
	}





	public synchronized void updateNodeListInNodeGroupSourceMetadatas(
			String nodeGroupType, List<String> newNodeList) {

		this.nodeGroupSourceMetadatas.get(nodeGroupType).getNodeList().clear();
		this.nodeGroupSourceMetadatas.get(nodeGroupType).addNodesToNodeList(
				newNodeList);

	}



	
	public void generateAgentCommandInNodeGroupDataMap(String nodeGroupType,
			String agentCommandType, Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore) {

		NodeGroupSourceMetadata nodeGroupSourceMetadata = nodeGroupStore
				.get(nodeGroupType);

		AgentCommandMetadata agentCommandMetadata = agentCommandMetadatas
				.get(agentCommandType);

		if(agentCommandMetadata==null){
			models.utils.LogUtils.printLogError("agentCommandMetadata iS NULL in generateAgentCommandInNodeGroupDataMap(); RETURN at time " + DateUtils.getNowDateTimeStrSdsm());
			
			return;
		}
		
		String requestContentTemplate = agentCommandMetadata
				.getRequestContentTemplate();
		String requestContent = AgentCommandMetadata
				.replaceDefaultFullRequestContent(requestContentTemplate);

		if (nodeGroupSourceMetadata == null || agentCommandMetadata == null) {
			 models.utils.LogUtils.printLogError
					 ("nodeGroupSourceMetadata or agentCommandMetadata is NULL in generateAgentCommandInNodeGroupDataMap. EXIT!!");
			return;
		}

		NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);

		// will automatically replace the old NodeRequestResponse when
		// updateAgentDataForNode
		// using not the nodeGroupSourceMetadata.getNodeList; but the actual
		// data.
		try {

			for (String fqdn : nodeGroupSourceMetadata.getNodeList()) {

				NodeReqResponse nodeReqResponse = new NodeReqResponse();
				nodeReqResponse.setDefaultReqestContent(requestContent);
				AgentDataProviderHelper.updateAgentDataForNode(ngdm, fqdn,
						nodeReqResponse, agentCommandType);
			}

			boolean copyRequestContentOnly = true;
			lastRefreshDataInProgress = new Date();
			updateDataValidFromInProgress(nodeGroupType, agentCommandType,
					copyRequestContentOnly, dataStore);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}// end func.

	/**
	 * Sync to the valid data set
	 */
	public synchronized void updateDataValidFromInProgressAll() {

		for (Entry<String, NodeGroupSourceMetadata> entry : nodeGroupSourceMetadatas
				.entrySet()) {

			NodeGroupSourceMetadata ngsm = entry.getValue();
			NodeGroupDataMap ngdm = allAgentData.get(ngsm.getNodeGroupType());
			ngdm.getNodeDataMapValid().clear();
			ngdm.getNodeDataMapValid().putAll(ngdm.getNodeDataMapInProgress());

		}

		lastRefreshDataValid = new Date();

	}

	/**
	 * fully sync all agent command and duplicate
	 * 
	 * @param nodeGroupType
	 */
	public synchronized void duplicateValidFromInProgress(String nodeGroupType) {

		NodeGroupDataMap ngdm = allAgentData.get(nodeGroupType);
		ngdm.getNodeDataMapValid().clear();
		ngdm.getNodeDataMapValid().putAll(ngdm.getNodeDataMapInProgress());
		lastRefreshDataValid = new Date();

	}

	/**
	 * Normally dont need to do this.
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 */
	public synchronized void clearNodeDataInProgress(String nodeGroupType,
			String agentCommandType) {

		NodeGroupDataMap ngdm = allAgentData.get(nodeGroupType);

		Map<String, NodeData> nodeDataMapInProgress = ngdm
				.getNodeDataMapInProgress();

		for (Entry<String, NodeData> entry : nodeDataMapInProgress.entrySet()) {

			NodeData nodeData = entry.getValue();

			if (nodeData == null) {

				models.utils.LogUtils.printLogError("nodeData ==null in clearDataInProgress");
			} else {
				nodeData.getDataMap().remove(agentCommandType);
			}

		}// end for loop

		lastRefreshDataInProgress = new Date();

	}

	/**
	 * only update for specific command nrr is inProgress will never have
	 * response; so will clear the response in Valid
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 */
	public void updateDataValidFromInProgress(String nodeGroupType,
			String agentCommandType, boolean copyRequestContentOnly,
			Map<String, NodeGroupDataMap> dataStore) {

		NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);

		Map<String, NodeData> nodeDataMapInProgress = ngdm
				.getNodeDataMapInProgress();
		Map<String, NodeData> nodeDataMapValid = ngdm.getNodeDataMapValid();

		for (Entry<String, NodeData> entry : nodeDataMapInProgress.entrySet()) {

			String fqdn = entry.getKey();

			NodeData nodeDataInProgress = entry.getValue();

			if (nodeDataInProgress == null) {

				 models.utils.LogUtils.printLogError
						 ("nodeData ==null in updateDataValidFromInProgress");
			} else {
				// deep copy
				NodeData nodeDataInProgressClone = new NodeData(
						nodeDataInProgress);

				NodeReqResponse nrr = nodeDataInProgressClone.getDataMap().get(
						agentCommandType);
				// replace with the new data
				if (nodeDataMapValid != null && fqdn != null && nrr != null) {

					// check if the valid has nodeData too?
					if (nodeDataMapValid.containsKey(fqdn)) {
						NodeData nodeDataValid = nodeDataMapValid.get(fqdn);

						if (!copyRequestContentOnly) {

							// this will replace the nodeDataValid
							nodeDataValid.getDataMap().put(agentCommandType,
									nrr);

							// only copy the request content.
						} else {
							// check if valid nodeData have this
							// agentCommandType in dataMap?

							if (nodeDataValid.getDataMap().containsKey(
									agentCommandType)) {
								// just copy the requestParameters
								nodeDataValid.getDataMap()
										.get(agentCommandType)
										.getRequestParameters().clear();
								nodeDataValid.getDataMap()
										.get(agentCommandType)
										.getRequestParameters();

								for (Entry<String, String> entryLocal : nrr
										.getRequestParameters().entrySet()) {

									// replacement from inProgress Clone to
									// valid
									nodeDataValid
											.getDataMap()
											.get(agentCommandType)
											.getRequestParameters()
											.put(entryLocal.getKey(),
													entryLocal.getValue());

								}

							} else {
								// copy the whole nrr from inProgress.
								nodeDataValid.getDataMap().put(
										agentCommandType, nrr);
							}

						}

					} else {

						// likely this is the first time transfer from
						// inProgress to Valid: just copy the whole nodeData.
						// Since first time, cannot have response.
						nodeDataMapValid.put(fqdn, nodeDataInProgressClone);

					}

				} else {
					 models.utils.LogUtils.printLogError
							 ("nodeDataMapValid != null && fqdn != null && nrr != null FAIL "
									+ fqdn);
				}
			}

		}// end for loop

		lastRefreshDataValid = new Date();

	}

	public int getTotalNodeCountInDataMapInProgress() {

		int totalNodeCount = 0;
		for (Entry<String, NodeGroupDataMap> entry : allAgentData.entrySet()) {

			// ASSUMPTION: KEY size == node list size
			totalNodeCount += entry.getValue().getNodeDataMapInProgress()
					.keySet().size();

		}

		return totalNodeCount;
	}

	/**
	 * 20130718: change to none duplicate
	 * 
	 * @return
	 */
	public int getTotalNodeCountInNodeGroupMetadatas() {

		int totalNodeCount = 0;
		Set<String> allNodes = new HashSet<String>();

		for (Entry<String, NodeGroupSourceMetadata> entry : nodeGroupSourceMetadatas
				.entrySet()) {

			allNodes.addAll(entry.getValue().getNodeList());

			// ASSUMPTION: KEY size == node list size
			// totalNodeCount += entry.getValue().getNodeList().size();

		}

		totalNodeCount = allNodes.size();
		return totalNodeCount;
	}

	public int getTotalCommandCountInAgentCommandMetadatas() {

		int totalNodeCount = agentCommandMetadatas.entrySet().size();

		return totalNodeCount;
	}

	public int getTotalNodeCountInDataMapValid() {

		int totalNodeCount = 0;
		for (Entry<String, NodeGroupDataMap> entry : allAgentData.entrySet()) {

			// ASSUMPTION: KEY size == node list size
			totalNodeCount += entry.getValue().getNodeDataMapValid().keySet()
					.size();

		}

		return totalNodeCount;
	}
	
	public int getTotalNodeGroupCountForAdhocNodegroups() {

		int totalNodeGroupCount = (adhocNodeGroups==null || adhocNodeGroups.entrySet()==null) ? 0 : adhocNodeGroups.entrySet().size();

		return totalNodeGroupCount;
	}
	

	public static Map<String, AgentCommandMetadata> getAgentcommandmetadatas() {
		return agentCommandMetadatas;
	}

	public static Date getLastRefreshDataValid() {
		return lastRefreshDataValid;
	}

	public static void setLastRefreshDataValid(Date lastRefreshDataValid) {
		AgentDataProvider.lastRefreshDataValid = lastRefreshDataValid;
	}

	public static Date getLastRefreshDataInProgress() {
		return lastRefreshDataInProgress;
	}

	public static void setLastRefreshDataInProgress(
			Date lastRefreshDataInProgress) {
		AgentDataProvider.lastRefreshDataInProgress = lastRefreshDataInProgress;
	}

	public static Map<String, NodeGroupSourceMetadata> getNodegroupsourcemetadatas() {
		return nodeGroupSourceMetadatas;
	}

}
