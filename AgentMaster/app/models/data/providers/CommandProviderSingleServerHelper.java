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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.data.AggregationNodeMetadata;
import models.data.RawDataSourceType;
import models.data.StrStrMap;
import models.data.providers.AgentCommadProviderHelperAggregation;
import models.data.providers.AgentCommandProvider;
import models.data.providers.NodeGroupProvider;
import models.rest.beans.responses.AggregationResponse;
import models.utils.DateUtils;
import models.utils.VarUtils;

/**
 * 20140111: for the single server multiple requests; only support a single VAR
 * replacement
 * 
 * @author ypei
 * 
 */
public class CommandProviderSingleServerHelper {

	/**
	 * 
	 * @param targetNodes
	 * @param attributeName
	 * @param attributeValue
	 */
	public static String commandToSingleTargetServer(List<String> targetNodes,
			String agentCommandType, String varName,
			 String targetServerNew) {

		String nodeGroupType = null;
		try {
			if (targetNodes == null || targetNodes.isEmpty()
					|| agentCommandType == null
					|| varName == null
					|| targetServerNew == null) {
				models.utils.LogUtils
						.printLogError("targetNodes or agentCommandType or varName or willReplaceTargetServer or targetServerNew is NULL or empty; now exit in func assetDiscoveryWorkFlow() !!"
								+ DateUtils.getNowDateTimeStrSdsm());
				return nodeGroupType;
			}


			/**
			 * STEP 0: Generate the adhoc node group
			 */

			// this nodeGroupType has the timestamp.
			nodeGroupType = NodeGroupProvider
					.generateAdhocNodeGroupHelper(targetNodes);

			Map<String, AggregationNodeMetadata> nodeValueMap =  AggregationNodeMetadata.generateAggregationNodeMetadataMapFromNodeList(targetNodes);

			/**
			 * STEP1 to have this replaced
			 */
			Boolean willReplaceTargetServer = true;
			Map<String, StrStrMap> replacementVarMapNodeSpecific = generateReplacementVarMapNodeSpecificGenericSingleVar(
					nodeValueMap, varName,
					willReplaceTargetServer, targetServerNew);

			AgentCommandProvider
					.generateUpdateSendAgentCommandWithReplaceVarMapNodeSpecificAdhoc(
							nodeGroupType, agentCommandType,
							replacementVarMapNodeSpecific);

		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils
					.printLogError("Error in function assetDiscoveryWorkFlow()"
							+ t.getLocalizedMessage()
							+ DateUtils.getNowDateTimeStrSdsm());
		}

		return nodeGroupType;

	}// end func.

	/**
	 * Replace a single variable. This is a pretty generic one
	 * 
	 * @param nodeValueMap
	 * @param varName
	 * @param willReplaceTargetServer
	 * @param targetServerNew
	 * @return
	 */
	public static Map<String, StrStrMap> generateReplacementVarMapNodeSpecificGenericSingleVar(
			Map<String, AggregationNodeMetadata> nodeValueMap, String varName,
			Boolean willReplaceTargetServer, String targetServerNew) {
		Map<String, StrStrMap> replacementVarMapNodeSpecific = new HashMap<String, StrStrMap>();

		try {
			// Validation
			if (nodeValueMap == null || nodeValueMap.isEmpty()) {
				models.utils.LogUtils
						.printLogError("ERROR Validation: nodeValueMap==null || nodeValueMap.isEmpty()in generateReplacementVarMapNodeSpecificForUuid() ");
				return replacementVarMapNodeSpecific;
			}

			for (Entry<String, AggregationNodeMetadata> entry : nodeValueMap
					.entrySet()) {

				String fqdn = entry.getKey();
				AggregationNodeMetadata nodeMetadata = entry.getValue();

				StrStrMap strStrMap = new StrStrMap();
				if (nodeMetadata.isError()) {
					// not to fire requests for this node; sinc even the last
					// step does not have the value extracted. (e.g. fail to get
					// response back)
					// therefore; this step will not really to fire requests.
					strStrMap.getMap().put(VarUtils.NA, VarUtils.NA);
				} else {

					// for requests to a single server
					if (willReplaceTargetServer) {
						strStrMap
								.getMap()
								.put(VarUtils.VAR_NAME_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR_WHEN_INSERT,
										targetServerNew);
					}

					// get from the aggregation from last step and push it into
					// the nodemap
					String valueFromLastAggregationForThisFqdn = nodeMetadata
							.getValue();
					strStrMap.getMap().put(varName,
							valueFromLastAggregationForThisFqdn);
				}
				replacementVarMapNodeSpecific.put(fqdn, strStrMap);

			}// end for
			;
		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils
					.printLogError("Error in function generateReplacementVarMapNodeSpecificForUuid()"
							+ t.getLocalizedMessage()
							+ DateUtils.getNowDateTimeStrSdsm());
		}

		return replacementVarMapNodeSpecific;
	}

}
