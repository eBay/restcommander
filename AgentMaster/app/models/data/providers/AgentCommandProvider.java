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
public class AgentCommandProvider {

	private static final AgentCommandProvider instance = new AgentCommandProvider();

	public static AgentCommandProvider getInstance() {
		return instance;
	}

	private AgentCommandProvider() {

	}
	
	public static GenericResponseFromDirector generateUpdateSendAgentCommandToNodeGroupAndSleep(
			String nodeGroupType, String agentCommandType,
			long sleepTimeAfterDoneMillis) {

		GenericResponseFromDirector batchResponseFromManager = generateUpdateSendAgentCommandToNodeGroupPredefined(
				nodeGroupType, agentCommandType);
		try {
			Thread.sleep(sleepTimeAfterDoneMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return batchResponseFromManager;

	}
	
	public static GenericResponseFromDirector generateUpdateSendAgentCommandToNodeGroupAdhocWithReplaceVarAndSleep(
			String nodeGroupType, String agentCommandType, Map<String, String> replacementVarMap,
			long sleepTimeAfterDoneMillis) {

		GenericResponseFromDirector batchResponseFromManager = generateUpdateSendAgentCommandWithReplaceVarAdhocMap(
				nodeGroupType, agentCommandType, replacementVarMap);
		try {
			Thread.sleep(sleepTimeAfterDoneMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return batchResponseFromManager;

	}

	/**
	 * THIS IS THE OLD ONE. BEFORE 20130827This includes the initialization of
	 * in the hashmap, update content; then send request. !!! THIS IS FOR ALL
	 * DATA. NOT FOR ADHOC DATA
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 */
	public static GenericResponseFromDirector generateUpdateSendAgentCommandToNodeGroupPredefined(
			String nodeGroupType, String agentCommandType) {

		boolean isAdhocData = false;
		boolean useReplacementVarMap = false;
		Map<String, String> replacementVarMap = null;
		
		boolean useReplacementVarMapNodeSpecific = false;
		Map<String, StrStrMap> replacementVarMapNodeSpecific = null;
		
		
		return AgentCommandProviderHelperInternalFlow.generateUpdateSendAgentCommandToNodeGroupHelper(nodeGroupType,
				agentCommandType, isAdhocData, useReplacementVarMap,
				replacementVarMap,
				useReplacementVarMapNodeSpecific,replacementVarMapNodeSpecific
				);

	}// end function.

	/**
	 * @author chunyang
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param token
	 * @return
	 * 
	 * For async polling
	 */
	
	public static GenericResponseFromDirector generateUpdateSendAgentCommandToNodeGroupPredefined(
			String nodeGroupType, String agentCommandType, boolean localMode, boolean failOver, int maxConcNum) {

		boolean isAdhocData = false;
		boolean useReplacementVarMap = false;
		Map<String, String> replacementVarMap = null;
		
		boolean useReplacementVarMapNodeSpecific = false;
		Map<String, StrStrMap> replacementVarMapNodeSpecific = null;
		
		
		return AgentCommandProviderHelperInternalFlow.generateUpdateSendAgentCommandToNodeGroupHelper(nodeGroupType,
				agentCommandType, isAdhocData, useReplacementVarMap,
				replacementVarMap,
				useReplacementVarMapNodeSpecific,replacementVarMapNodeSpecific
				, localMode, failOver, maxConcNum);

	}// end function.
	
	/**
	 * THIS IS THE NEWLY ADDED ONE. 20130827 LIMITATION: NOW ONLY FOR ADHOC
	 * TODO? Why only ad hoc? Add comments.
	 * 
	 * useReplacementVarMap: all target nodes replace with a single var in the command.e.g sth like agetn version
	 * replacementVarMapNodeSpecific: each target node has a specific replace map.
	 * 
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param replacementVarMap
	 *            : generic replace the strings in the request.
	 * @return
	 */
	public static GenericResponseFromDirector generateUpdateSendAgentCommandWithReplaceVarAdhocMap(
			String nodeGroupType, String agentCommandType,
			Map<String, String> replacementVarMap) {

		boolean isAdhocData = true;
		boolean useReplacementVarMap = true;
		boolean useReplacementVarMapNodeSpecific = false;
		Map<String, StrStrMap> replacementVarMapNodeSpecific = null;
		
		return AgentCommandProviderHelperInternalFlow.generateUpdateSendAgentCommandToNodeGroupHelper(nodeGroupType,
				agentCommandType, isAdhocData, useReplacementVarMap,
				replacementVarMap,
				useReplacementVarMapNodeSpecific,replacementVarMapNodeSpecific
				
				);

	}// end func.
	
	
	/**
	 * 20131022 NEW Node specific LIMITATION: NOW ONLY FOR ADHOC
	 * TODO? Why only ad hoc? Add comments.
	 * 
	 * useReplacementVarMap: all target nodes replace with a single var in the command.e.g sth like agetn version
	 * replacementVarMapNodeSpecific: each target node has a specific replace map.
	 * 
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param replacementVarMap
	 *            : generic replace the strings in the request.
	 * @return
	 */
	public static GenericResponseFromDirector generateUpdateSendAgentCommandWithReplaceVarMapNodeSpecificAdhoc(
			String nodeGroupType, String agentCommandType,
			Map<String, StrStrMap> replacementVarMapNodeSpecific) {

		boolean isAdhocData = true;
		boolean useReplacementVarMap = false;
		boolean useReplacementVarMapNodeSpecific = true;
		Map<String, String> replacementVarMap = null;
		
		return AgentCommandProviderHelperInternalFlow.generateUpdateSendAgentCommandToNodeGroupHelper(nodeGroupType,
				agentCommandType, isAdhocData, useReplacementVarMap,
				replacementVarMap,
				useReplacementVarMapNodeSpecific,replacementVarMapNodeSpecific
				
				);

	}// end func.
	
	/**
	 * 20131017 No replace; for adhoc
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param replacementVarMap
	 *           
	 * @return
	 */
	public static GenericResponseFromDirector generateUpdateSendAgentCommandWithoutReplaceVarAdhocMap(
			String nodeGroupType, String agentCommandType) {

		boolean isAdhocData = true;
		boolean useReplacementVarMap = false;
		boolean useReplacementVarMapNodeSpecific = false;
		Map<String, StrStrMap> replacementVarMapNodeSpecific = null;
		Map<String, String> replacementVarMap = null;
		
		return AgentCommandProviderHelperInternalFlow.generateUpdateSendAgentCommandToNodeGroupHelper(nodeGroupType,
				agentCommandType, isAdhocData, useReplacementVarMap,
				replacementVarMap,
				useReplacementVarMapNodeSpecific,replacementVarMapNodeSpecific
				
				);

	}// end func.
	
	
	/**
	 * THIS IS THE NEWLY NEWLY ADDED ONE. 20130916
	 * For node specific VAR replacement
	 * LIMITATION: NOW ONLY FOR ADHOC
	 * TODO? Why only ad hoc? Add comments. 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param replacementVarMap
	 *            : generic replace the strings in the request.
	 * @return
	 */
	public static GenericResponseFromDirector generateUpdateSendAgentCommandWithReplaceVarAdhocMapNodeSpecific(
			String nodeGroupType, String agentCommandType,
			Map<String, StrStrMap> replacementVarMapNodeSpecific) {

		boolean isAdhocData = true;
		boolean useReplacementVarMap = false;
		Map<String, String> replacementVarMap = null;
		
		boolean useReplacementVarMapNodeSpecific = true;
		
		return AgentCommandProviderHelperInternalFlow.generateUpdateSendAgentCommandToNodeGroupHelper(nodeGroupType,
				agentCommandType, isAdhocData, useReplacementVarMap,
				replacementVarMap,
				useReplacementVarMapNodeSpecific,replacementVarMapNodeSpecific
				);

	}// end func.






}
