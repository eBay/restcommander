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

package controllers;

import java.util.List;
import java.util.Map;

import RemoteCluster.SupermanApp;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.data.AggregateData;
import models.data.AggregationValueMetadata;
import models.data.JsonResult;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.providers.AgentCommandProvider;
import models.data.providers.AgentCommandProviderHelperForWholeJob;
import models.data.providers.AgentDataAggregator;
import models.data.providers.AgentDataProvider;
import models.data.providers.AgentDataProviderHelper;
import models.utils.AgentUtils;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
import play.mvc.Controller;

/**
 * 
 * @author ypei
 *
 */

public class Agents extends Controller {

	public static void index(String nodeGroup) {

		String page = "agents";
		String topnav = "agents";

		render(page, topnav);
	}

	public static void resetData() {

		try {

			AgentDataProvider adp = AgentDataProvider.getInstance();

			adp.resetData();

			renderJSON("Successful resetData " + DateUtils.getNowDateTimeStr());
		} catch (Throwable t) {

			renderJSON("Error occured in resetData");
		}

	}


	public static void getAgentData(String nodeGroupType, String fqdn,
			String agentCommandType, Boolean isAdhoc) {

		try {

			Map<String, NodeGroupDataMap> dataStore = null;
			if (isAdhoc == null || isAdhoc == false) {
				dataStore = AgentDataProvider.allAgentData;
			} else {
				dataStore = AgentDataProvider.adhocAgentData;
			}

			if (nodeGroupType == null && fqdn == null) {

				renderJSON(dataStore);
			} else if (nodeGroupType != null && fqdn == null) {

				
				if(dataStore.get(nodeGroupType)==null){
					renderJSON("nodeGroupType" + nodeGroupType + 
							" does not exist in the dataStore.");
				}
				
				
				if (agentCommandType == null) {

					renderJSON(dataStore.get(nodeGroupType));
				} else {

					renderJSON(dataStore.get(nodeGroupType)
							.getNodeGroupDataMapValidForSingleCommand(
									agentCommandType));
				}

			}

		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON("Error occured in getAgentData");
		}

	}

	/**
	 * This includes the initialization of in the hashmap.
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 */
	public static void sendAgentCommandToNodeGroup(String nodeGroupType,
			String agentCommandType, Boolean isAdhoc) {

		try {

			// AgentCommandDirector director =
			// AgentCommandDirector.getInstance();
			//CommandDirector director = new CommandDirector();

			Map<String, NodeGroupDataMap> dataStore = null;
			if (isAdhoc == null || isAdhoc == false) {
				dataStore = AgentDataProvider.allAgentData;
			} else {
				dataStore = AgentDataProvider.adhocAgentData;
			}
			/**
			 * @author chunyang
			 * Migrate to distributed superman
			 */
			/*BatchResponseFromManager batchResponseFromManager = director
					.sendAgentCommandToManager(nodeGroupType, agentCommandType,
							dataStore);*/
			SupermanApp.sendAgentCommandToManager(nodeGroupType, agentCommandType, 
					dataStore, false, false, VarUtils.MAX_CONCURRENT_SEND_SIZE, false);
			
			renderJSON(dataStore.get(nodeGroupType).getNodeGroupDataMapValidForSingleCommand(agentCommandType));
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON(new JsonResult(
					"Error occured in sendAgentCommandToNodeGroup"));
		}

	}

	/**
	 * In all agent data only. not to expose the adhoc part. Becasue the node
	 * list is also adhoc.
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 */
	public static void generateAgentCommandInNodeGroup(String nodeGroupType,
			String agentCommandType) {

		try {

			AgentDataProvider adp = AgentDataProvider.getInstance();

			Map<String, NodeGroupDataMap> dataStore = null;
			Map<String, NodeGroupSourceMetadata> nodeGroupSource = null;
			dataStore = AgentDataProvider.allAgentData;
			nodeGroupSource = AgentDataProvider.nodeGroupSourceMetadatas;

			adp.generateAgentCommandInNodeGroupDataMap(nodeGroupType,
					agentCommandType, dataStore, nodeGroupSource);

			renderJSON(new JsonResult(
					"Success in generateAgentCommandInNodeGroupDataMap"));
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON(new JsonResult(
					"Error in generateAgentCommandInNodeGroupDataMap"));
		}

	}

	public static void generateAgentCommandInAdhocAgentData(
			String nodeGroupType, String agentCommandType) {

		try {

			AgentDataProvider adp = AgentDataProvider.getInstance();

			adp.generateAgentCommandInNodeGroupDataMap(nodeGroupType,
					agentCommandType, AgentDataProvider.adhocAgentData,
					AgentDataProvider.adhocNodeGroups);

			renderJSON(new JsonResult(
					"Success in generateAgentCommandInNodeGroupDataMap"));
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON(new JsonResult(
					"Error in generateAgentCommandInNodeGroupDataMap"));
		}

	}

	public static void copyRequestToNodeGroup(String nodeGroupTypeSource,
			String nodeGroupTypeTarget, String agentCommandType) {

		try {

			AgentDataProviderHelper.copyRequestToNodeGroup(nodeGroupTypeSource,
					nodeGroupTypeTarget, agentCommandType);

			renderJSON("Success in copyRequestToNodeGroup");
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON("Error occured in copyRequestToNodeGroup");
		}

	}

	
	
	



	/*
	 * 
	 */
	public static void aggregatePieChart(String nodeGroupType,
			String agentCommandType, String timeStamp,String rawDataSourceType, String aggrRule, Boolean textOnly) {
		String page = "aggregatePieChart";
		String topnav = "agent";
		String lastRefreshed = DateUtils.getNowDateTimeStrSdsm();
		
		String patternStr = "N/A";
		
		//20130805: add option to see text only
		if(textOnly==null){
			textOnly = false;
		}
		
		try {

			AgentDataAggregator ada = AgentDataAggregator.getInstance();
			AgentDataProvider adp = AgentDataProvider.getInstance();
			Map<String, String> rulesMap = adp.aggregationMetadatas;
			//20130610: fix null
			
			if(aggrRule==null){
				aggrRule=VarUtils.AGGREGATION_DEFAULT_METRIC;
			}
			patternStr = rulesMap.get(   (aggrRule==null || aggrRule.length()==0 ) ?VarUtils.AGGREGATION_DEFAULT_METRIC:aggrRule);
			AggregateData aggregateData = ada.aggregateMetricsWithGroupingNew(
					nodeGroupType, agentCommandType, timeStamp, rawDataSourceType, patternStr);

			String pieDataStr = null; 

			boolean humanReadFriendly = false;
			
			List<AggregationValueMetadata> avmList = aggregateData
					.getResponseToMetadataList();

			if(!textOnly){
				
				pieDataStr = aggregateData.getValueCountJSNumber2D(humanReadFriendly);
				
				render(page, topnav, pieDataStr, avmList, agentCommandType,
						nodeGroupType, lastRefreshed, patternStr, timeStamp, rawDataSourceType, rulesMap, aggrRule);
			}else{
				
				String avmListString = AgentUtils.renderJson(avmList);
				humanReadFriendly = true;
				pieDataStr = aggregateData.getValueCountJSNumber2D(humanReadFriendly);
				
				
				StringBuilder finalResponse = new StringBuilder( "//SUPERMAN*****OVERVIEW -- AGGREGATION SUMMARY - START *************\n\n");
				finalResponse.append(pieDataStr);
				finalResponse.append( "\n\n//SUPERMAN*****OVERVIEW -- AGGREGATION SUMMARY - END *************\n\n");
				
				finalResponse.append( "//SUPERMAN*****DETAILS (SORTED BY KEY) - START *************\n\n");
				finalResponse.append(avmListString);
				finalResponse.append( "\n\n//SUPERMAN*****DETAILS (SORTED BY KEY) - END *************\n\n");
				renderText(finalResponse.toString());
			}
		} catch (Throwable t) {

			renderJSON(new JsonResult(
					"Error in aggregatePieChart for nodeGroupType"
							+ nodeGroupType + " error: "
							+ t.getLocalizedMessage() + "  at time: "
							+ DateUtils.getNowDateTimeStrSdsm()));
		}
	}

	public static void aggregateMetaDataNodeList(String nodeGroupType,
			String agentCommandType, String value, String timeStamp,String rawDataSourceType, String aggrRule) {
		try {

			AgentDataProvider adp = AgentDataProvider.getInstance();
			Map<String, String> rulesMap = adp.aggregationMetadatas;
			String patternStr = rulesMap.get(   (aggrRule==null || aggrRule.length()==0 ) ?VarUtils.AGGREGATION_DEFAULT_METRIC:aggrRule);
			
			AgentDataAggregator ada = AgentDataAggregator.getInstance();

			
			
			AggregateData aggregateData = ada.aggregateMetricsWithGroupingNew(
					nodeGroupType, agentCommandType, timeStamp, rawDataSourceType, patternStr);

			String nodeListDetails = aggregateData.getResponseToMetadataMap()
					.get(value).nodeListDetailsStr();

			renderText(nodeListDetails);
		} catch (Throwable t) {

			renderJSON(new JsonResult(
					"Error in aggregateMetaDataNodeList for nodeGroupType"
							+ nodeGroupType + " error: "
							+ t.getLocalizedMessage() + "  at time: "
							+ DateUtils.getNowDateTimeStrSdsm()));
		}
	}

}
