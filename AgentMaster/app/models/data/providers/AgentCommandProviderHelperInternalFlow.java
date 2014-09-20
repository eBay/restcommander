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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import RemoteCluster.SupermanApp;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.GenericResponseFromDirector;
import models.data.AgentCommandMetadata;
import models.data.JsonResult;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeGroupSourceType;
import models.data.StrStrMap;
import models.utils.AgentUtils;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class AgentCommandProviderHelperInternalFlow {

	private static final AgentCommandProviderHelperInternalFlow instance = new AgentCommandProviderHelperInternalFlow();

	public static AgentCommandProviderHelperInternalFlow getInstance() {
		return instance;
	}

	private AgentCommandProviderHelperInternalFlow() {

	}

	/**
	 * Goal: generic handle both adhoc/none adhoc cases for data map ( node list
	 * ) handle both the adhoc pass in parameter for replacement; also the SREPO
	 * WISB based replacement ( predefined)
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param isAdhocData
	 * @param useReplacementVarMap
	 * @param replacementVarMap
	 * @return
	 */
	public static GenericResponseFromDirector generateUpdateSendAgentCommandToNodeGroupHelper(
			String nodeGroupType, String agentCommandType, boolean isAdhocData,
			boolean useReplacementVarMap,
			Map<String, String> replacementVarMap,
			boolean useReplacementVarMapNodeSpecific,
			Map<String, StrStrMap> replacementVarMapNodeSpecific) {

		Map<String, NodeGroupDataMap> dataStore = (isAdhocData) ? AgentDataProvider.adhocAgentData
				: AgentDataProvider.allAgentData;

		Map<String, NodeGroupSourceMetadata> nodeGroupStore = (isAdhocData) ? AgentDataProvider.adhocNodeGroups
				: AgentDataProvider.nodeGroupSourceMetadatas;

		GenericResponseFromDirector batchResponseFromManager = null;

		try {

			/**
			 * CATCH EXCEPTION: fixed 20130828. LESSON: for none existing node
			 * group; must have check
			 */

			NodeGroupSourceMetadata nodeGroupSourceMetadata = nodeGroupStore
					.get(nodeGroupType);
			if (nodeGroupSourceMetadata == null) {

				String errMessage = "nodeGroupSourceMetadata  is NULL in generateUpdateSendAgentCommandToNodeGroupHelper. EXIT!!"
						+ DateUtils.getNowDateTimeStrSdsm();
				models.utils.LogUtils.printLogError(errMessage);
				batchResponseFromManager = new BatchResponseFromManager();

				return batchResponseFromManager;
			}

			// generate content
			AgentDataProvider adp = AgentDataProvider.getInstance();
			adp.generateAgentCommandInNodeGroupDataMap(nodeGroupType,
					agentCommandType, dataStore, nodeGroupStore);

			// 20130828. updateRequestContent Whether to do VAR replacement?
			if (useReplacementVarMap) {

				updateRequestContentGenericWithVarReplacement(nodeGroupType,
						agentCommandType, dataStore, nodeGroupStore,
						replacementVarMap);

				// 20130916 do node specific VAR replacement
			} else if (useReplacementVarMapNodeSpecific) {
				updateRequestContentGenericWithVarReplacementNodeSpecific(
						nodeGroupType, agentCommandType, dataStore,
						nodeGroupStore, replacementVarMapNodeSpecific);

			} else {
				updateRequestContentGeneric(nodeGroupType, agentCommandType,
						dataStore, nodeGroupStore);
			}

			/**
			 * @author chunyang
			 * Distribute Supermen, use Cluster & remote deploy
			 */
			SupermanApp.initClusterSystem("2551", "");
			batchResponseFromManager = SupermanApp.sendAgentCommandToManager(nodeGroupType, agentCommandType, dataStore, false, false, VarUtils.MAX_CONCURRENT_SEND_SIZE, false);
			
		} catch (Throwable t) {

			t.printStackTrace();
		}
		return batchResponseFromManager;

	}// end function.

	
	/**
	 * @author chunyang
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param isAdhocData
	 * @param useReplacementVarMap
	 * @param replacementVarMap
	 * @param useReplacementVarMapNodeSpecific
	 * @param replacementVarMapNodeSpecific
	 * @param token
	 * @return
	 * 
	 * For async polling
	 */
	
	public static GenericResponseFromDirector generateUpdateSendAgentCommandToNodeGroupHelper(
			String nodeGroupType, String agentCommandType, boolean isAdhocData,
			boolean useReplacementVarMap,
			Map<String, String> replacementVarMap,
			boolean useReplacementVarMapNodeSpecific,
			Map<String, StrStrMap> replacementVarMapNodeSpecific,
			boolean localMode, boolean failOver, int maxConcNum) {

		Map<String, NodeGroupDataMap> dataStore = (isAdhocData) ? AgentDataProvider.adhocAgentData
				: AgentDataProvider.allAgentData;

		Map<String, NodeGroupSourceMetadata> nodeGroupStore = (isAdhocData) ? AgentDataProvider.adhocNodeGroups
				: AgentDataProvider.nodeGroupSourceMetadatas;

		GenericResponseFromDirector batchResponseFromManager = null;

		try {

			/**
			 * CATCH EXCEPTION: fixed 20130828. LESSON: for none existing node
			 * group; must have check
			 */

			NodeGroupSourceMetadata nodeGroupSourceMetadata = nodeGroupStore
					.get(nodeGroupType);
			if (nodeGroupSourceMetadata == null) {

				String errMessage = "nodeGroupSourceMetadata  is NULL in generateUpdateSendAgentCommandToNodeGroupHelper. EXIT!!"
						+ DateUtils.getNowDateTimeStrSdsm();
				models.utils.LogUtils.printLogError(errMessage);
				batchResponseFromManager = new BatchResponseFromManager();

				return batchResponseFromManager;
			}

			// generate content
			AgentDataProvider adp = AgentDataProvider.getInstance();
			adp.generateAgentCommandInNodeGroupDataMap(nodeGroupType,
					agentCommandType, dataStore, nodeGroupStore);

			// 20130828. updateRequestContent Whether to do VAR replacement?
			if (useReplacementVarMap) {

				updateRequestContentGenericWithVarReplacement(nodeGroupType,
						agentCommandType, dataStore, nodeGroupStore,
						replacementVarMap);

				// 20130916 do node specific VAR replacement
			} else if (useReplacementVarMapNodeSpecific) {
				updateRequestContentGenericWithVarReplacementNodeSpecific(
						nodeGroupType, agentCommandType, dataStore,
						nodeGroupStore, replacementVarMapNodeSpecific);

				// traditional; no replacement or hardcoded node specific
				// replacement for cassinit topology, hwPath, colo, rack....
				// etc.
			} else {
				updateRequestContentGeneric(nodeGroupType, agentCommandType,
						dataStore, nodeGroupStore);
			}

			/**
			 * @author chunyang
			 * Distribute Supermen, use Cluster & remote deploy
			 */
			SupermanApp.initClusterSystem("2551", "");
			batchResponseFromManager = SupermanApp.sendAgentCommandToManager(nodeGroupType, agentCommandType, dataStore, localMode, failOver, maxConcNum, true);
			
		} catch (Throwable t) {

			t.printStackTrace();
		}
		return batchResponseFromManager;

	}// end function.
	
	/**
	 * 20130828: this is the API based replacement.
	 * 
	 * Uniform var replacement
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param dataStore
	 * @param nodeGroupStore
	 * @return
	 */
	public static String updateRequestContentGenericWithVarReplacement(
			String nodeGroupType, String agentCommandType,
			Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore,
			Map<String, String> replacementVarMap) {

		boolean useReplacementVarMap = true;
		boolean useReplacementVarMapNodeSpecific = false;
		Map<String, StrStrMap> replacementVarMapNodeSpecific = null;

		return updateRequestContentGenericHelper(nodeGroupType,
				agentCommandType, dataStore, nodeGroupStore,
				useReplacementVarMap, replacementVarMap,
				useReplacementVarMapNodeSpecific, replacementVarMapNodeSpecific);

	}// end func.

	/**
	 * 20130918: this is replacementVarMapNodeSpecific
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param dataStore
	 * @param nodeGroupStore
	 * @return
	 */
	public static String updateRequestContentGenericWithVarReplacementNodeSpecific(
			String nodeGroupType, String agentCommandType,
			Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore,
			Map<String, StrStrMap> replacementVarMapNodeSpecific) {

		boolean useReplacementVarMap = false;
		Map<String, String> replacementVarMap = null;

		boolean useReplacementVarMapNodeSpecific = true;
		return updateRequestContentGenericHelper(nodeGroupType,
				agentCommandType, dataStore, nodeGroupStore,
				useReplacementVarMap, replacementVarMap,
				useReplacementVarMapNodeSpecific, replacementVarMapNodeSpecific);

	}// end func.

	/**
	 * This just call the updateRequestContentGenericHelper this is for
	 * traditional none replacement.
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param dataStore
	 * @param nodeGroupStore
	 * @return
	 */
	public static String updateRequestContentGeneric(String nodeGroupType,
			String agentCommandType, Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore) {

		boolean useReplacementVarMap = false;
		Map<String, String> replacementVarMap = null;
		boolean useReplacementVarMapNodeSpecific = false;
		Map<String, StrStrMap> replacementVarMapNodeSpecific = null;
		return updateRequestContentGenericHelper(nodeGroupType,
				agentCommandType, dataStore, nodeGroupStore,
				useReplacementVarMap, replacementVarMap,
				useReplacementVarMapNodeSpecific, replacementVarMapNodeSpecific);

	}// end func.

	/**
	 * TODO: this may need to be expended.
	 * 
	 * 20130916: add node specific replacement Var Map; replacementVarMap VS.
	 * replacementVarMap
	 * 
	 * replacementVarMap: is for the *uniform * var replacement identical to all
	 * nodes replacementVarMapNodeSpecific: is for node specific var
	 * replacement; e.g. each node wants a diff id, hwpath etc
	 * 
	 * EXPENSION: 20130610: add the POOL LEVEL WISB
	 * 
	 * only for Node specific request content of
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @return
	 */
	public static String updateRequestContentGenericHelper(
			String nodeGroupType, String agentCommandType,
			Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore,
			boolean useReplacementVarMap,
			Map<String, String> replacementVarMap,
			boolean useReplacementVarMapNodeSpecific,
			Map<String, StrStrMap> replacementVarMapNodeSpecific

	) {

		String operationResult = VarUtils.OPERATION_SUCCESSFUL;

		try {


			/**
			 * CATCH EXCEPTION: fixed 20130828. LESSON: for none existing node
			 * group; must have check
			 */
			NodeGroupSourceMetadata nodeGroupSourceMetadata = nodeGroupStore
					.get(nodeGroupType);
			if (nodeGroupSourceMetadata == null
					|| 	agentCommandType == null
					) {

				
				String errMessage = "nodeGroupSourceMetadata  is NULL or 	agentCommandType == null in updateRequestContentGenericHelper. EXIT!!"
						+ DateUtils.getNowDateTimeStrSdsm();
				models.utils.LogUtils.printLogError(errMessage);
				operationResult = errMessage;
				return operationResult;
			}



			/**
			 * 
			 * 2013.08.28 STARTING POINT: REPLACEMENT FOR INPUT PARAMETER BASED.
			 * 
			 * ENABLE CRETIRIA: useReplacementVarMap==true
			 * 
			 * first to check if the requestContentTemplate or requestUrlPostfix
			 */
			if (useReplacementVarMap == true && replacementVarMap != null) {

				for (Entry<String, String> entry : replacementVarMap.entrySet()) {

					VarReplacementProvider vrp = VarReplacementProvider
							.getInstance();

					String replaceVarKey = entry.getKey();
					String replaceVarValue = entry.getValue();

					vrp.genericUpdateRequestByAddingReplaceVarKeyValuePairHelper(
							nodeGroupType, agentCommandType, replaceVarKey,
							replaceVarValue, dataStore, nodeGroupStore,
							VarUtils.VAR_REPLACEMENT_TYPE.ADHOC_FROM_API);

				}// end for loop

			}// end if

			/**
			 * 
			 * 2013.08.28 STARTING POINT: REPLACEMENT FOR INPUT PARAMETER BASED.
			 * 
			 * ENABLE CRETIRIA: useReplacementVarMap==true
			 * 
			 * first to check if the requestContentTemplate or requestUrlPostfix
			 */
			if (useReplacementVarMapNodeSpecific == true
					&& replacementVarMapNodeSpecific != null) {

				VarReplacementProvider vrp = VarReplacementProvider
						.getInstance();

				vrp.genericUpdateRequestByAddingReplaceVarKeyValuePairHelperNodeSpecific(
						nodeGroupType, agentCommandType, dataStore,
						nodeGroupStore,
						VarUtils.VAR_REPLACEMENT_TYPE.ADHOC_FROM_API,
						replacementVarMapNodeSpecific);

			}// end if

		} catch (Throwable t) {

			operationResult = t.getLocalizedMessage();
			t.printStackTrace();
		}
		return operationResult;

	}// end func.

}
