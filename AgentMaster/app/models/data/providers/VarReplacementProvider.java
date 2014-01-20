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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import models.data.AgentCommandMetadata;
import models.data.LogFile;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeReqResponse;
import models.data.StrStrMap;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.FileIoUtils;
import models.utils.LogUtils;
import models.utils.NaturalDeserializer;
import models.utils.VarUtils;

/**
 * Deal with WISB Var generator and get values
 * 
 * 20130828
 * 
 * extending from replacing only WISB based. now to also API based.
 * 
 * @author ypei
 * 
 */
public class VarReplacementProvider {

	private static final VarReplacementProvider instance = new VarReplacementProvider();

	public static VarReplacementProvider getInstance() {
		return instance;
	}

	private VarReplacementProvider() {

	}

	public static boolean isWisbVarFromCommand(String agentCommandType) {

		AgentDataProvider adp = AgentDataProvider.getInstance();
		AgentCommandMetadata agentCommandMetadata = adp.agentCommandMetadatas
				.get(agentCommandType);

		String requestContentTemplate = agentCommandMetadata
				.getRequestContentTemplate();

		String requestUrlPostfix = agentCommandMetadata.getRequestUrlPostfix();

		boolean isWisbVarInString = false;

		if (requestContentTemplate.contains(VarUtils.WISB_VAR_NAME_PREFIX)
				|| requestUrlPostfix.contains(VarUtils.WISB_VAR_NAME_PREFIX)) {
			isWisbVarInString = true;
		}

		return isWisbVarInString;
	}






	/**
	 * GENERIC!!! HELPER FUNCION FOR REPLACEMENT
	 * 
	 * update the var: DYNAMIC REPLACEMENT of VAR.
	 * 
	 * @param agentCommand
	 * @param nodeGroupType
	 * @param replaceVarKey
	 * @param dataStore
	 * @param nodeGrupStore
	 */
	public void genericUpdateRequestByAddingReplaceVarKeyValuePairHelper(
			String nodeGroupType, String agentCommand, String replaceVarKey,
			String replaceVarValue, Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGrupStore,
			VarUtils.VAR_REPLACEMENT_TYPE varReplacementType) {

		NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);

		/**
		 * From Valid NodeDataMap; and only update the valid data
		 */
		for (Entry<String, NodeData> entry : ngdm.getNodeDataMapValid()
				.entrySet()) {

			NodeData nodeData = entry.getValue();
			if (nodeData.getDataMap().containsKey(agentCommand)) {

				NodeReqResponse nodeReqResponse = nodeData.getDataMap().get(
						agentCommand);

				if (nodeReqResponse == null) {
					nodeReqResponse = new NodeReqResponse();
					nodeReqResponse.setDefaultEmptyReqestContent();

					nodeData.getDataMap().put(agentCommand, nodeReqResponse);
				}

				// Safeguard!! When the wisbVarValue is "NA" (e.g. fail to get
				// the wisb) should alert that
				// Safeguard: if NA, then dont run it!
				if (replaceVarKey.equalsIgnoreCase(VarUtils.NA)) {

					if (varReplacementType == VarUtils.VAR_REPLACEMENT_TYPE.ADHOC_FROM_API) {
						models.utils.LogUtils
								.printLogNormal("Get an invalid var value as NA."
										+ DateUtils.getNowDateTimeStrSdsm());

						// 20130731: add error msg
						nodeReqResponse
								.getRequestParameters()
								.put(VarUtils.NODE_REQUEST_EXECUTE_MSG,
										VarUtils.NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_VALUE_NA);

					}

					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_WILL_EXECUTE,
							new Boolean(false).toString());

					/**
					 * 20130828: make it generic to check NULL KEY/VALUE
					 */
				} else if (replaceVarKey == null || replaceVarValue == null) {
					models.utils.LogUtils
							.printLogNormal("Get NULL repalceVarKey or value.."
									+ DateUtils.getNowDateTimeStrSdsm());

					// 20130731: add error msg
					nodeReqResponse
							.getRequestParameters()
							.put(VarUtils.NODE_REQUEST_EXECUTE_MSG,
									VarUtils.NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_KEY_OR_VALUE_NULL);

					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_WILL_EXECUTE,
							new Boolean(false).toString());

				} else {
					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_PREFIX_REPLACE_VAR
									+ replaceVarKey, replaceVarValue);

					// CAREFUL! This is added to prevent a last time run
					// "NOT EXECUTE" to continue to be effective this time.
					// Since the whole nodeReqResponse will not replaced
					// everytime
					nodeReqResponse.getRequestParameters().put(
							VarUtils.NODE_REQUEST_WILL_EXECUTE,
							new Boolean(true).toString());

				}

			} else {
				models.utils.LogUtils
						.printLogError("agentCommand  is null in genericUpdateRequestByAddingReplaceVarKeyValuePairHelper()"
								+ DateUtils.getNowDateTimeStrSdsm());
			}
		}// end for loop

	}// end func

	/**
	 * 20130916: structure is a little diff from the
	 * genericUpdateRequestByAddingReplaceVarKeyValuePairHelper
	 * 
	 * Will change replacementVarMapNodeSpecific according to each node
	 * specifically
	 * 
	 * 20131205: with KEY set as NA; will not run the command ONLY if the NA is
	 * the last replacement; note in this logic; when it is not NA; will set AS
	 * True.
	 * 
	 * @param agentCommand
	 * @param nodeGroupType
	 * @param replaceVarKey
	 * @param dataStore
	 * @param nodeGrupStore
	 */
	public void genericUpdateRequestByAddingReplaceVarKeyValuePairHelperNodeSpecific(
			String nodeGroupType, String agentCommand,
			Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGrupStore,
			VarUtils.VAR_REPLACEMENT_TYPE varReplacementType,
			Map<String, StrStrMap> replacementVarMapNodeSpecific

	) {

		NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);

		/**
		 * From Valid NodeDataMap; and only update the valid data
		 */
		for (Entry<String, NodeData> entry : ngdm.getNodeDataMapValid()
				.entrySet()) {

			String fqdn = entry.getKey();
			StrStrMap replacementVarMapForThisNode = replacementVarMapNodeSpecific
					.get(fqdn);

			//20140105: fix NPE.
			if (replacementVarMapForThisNode == null
					|| replacementVarMapForThisNode.getMap() == null) {
				LogUtils.printLogError("replacementVarMapForThisNode is null in "
						+ "genericUpdateRequestByAddingReplaceVarKeyValuePairHelperNodeSpecific for fqnd "
						+ fqdn + " at " + DateUtils.getNowDateTimeStrSdsm());
				
				continue;
			}

			for (Entry<String, String> entryReplaceMap : replacementVarMapForThisNode
					.getMap().entrySet()) {

				String replaceVarKey = entryReplaceMap.getKey();
				String replaceVarValue = entryReplaceMap.getValue();

				NodeData nodeData = entry.getValue();
				if (nodeData.getDataMap().containsKey(agentCommand)) {

					NodeReqResponse nodeReqResponse = nodeData.getDataMap()
							.get(agentCommand);

					if (nodeReqResponse == null) {
						nodeReqResponse = new NodeReqResponse();
						nodeReqResponse.setDefaultEmptyReqestContent();

						nodeData.getDataMap()
								.put(agentCommand, nodeReqResponse);
					}

					// Safeguard!! When the wisbVarValue is "NA" (e.g. fail to
					// get
					// the wisb) should alert that
					// Safeguard: if NA, then dont run it!
					if (replaceVarKey.equalsIgnoreCase(VarUtils.NA)) {

						if (varReplacementType == VarUtils.VAR_REPLACEMENT_TYPE.WISB_SREPO) {
							models.utils.LogUtils
									.printLogNormal("Wisb fail to get wisbVarValue"
											+ DateUtils.getNowDateTimeStrSdsm());
							// 20130731: add error msg
							nodeReqResponse
									.getRequestParameters()
									.put(VarUtils.NODE_REQUEST_EXECUTE_MSG,
											VarUtils.NODE_REQUEST_EXECUTE_MSG_DETAIL_TYPE_WISB_NOT_EXIST);
						} else if (varReplacementType == VarUtils.VAR_REPLACEMENT_TYPE.ADHOC_FROM_API) {
							models.utils.LogUtils
									.printLogNormal("Get an invalid var value as NA."
											+ DateUtils.getNowDateTimeStrSdsm());

							// 20130731: add error msg
							nodeReqResponse
									.getRequestParameters()
									.put(VarUtils.NODE_REQUEST_EXECUTE_MSG,
											VarUtils.NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_VALUE_NA);

						}

						nodeReqResponse.getRequestParameters().put(
								VarUtils.NODE_REQUEST_WILL_EXECUTE,
								new Boolean(false).toString());

						/**
						 * 20130828: make it generic to check NULL KEY/VALUE
						 */
					} else if (replaceVarKey == null || replaceVarValue == null) {
						models.utils.LogUtils
								.printLogNormal("Get NULL repalceVarKey or value.."
										+ DateUtils.getNowDateTimeStrSdsm());

						// 20130731: add error msg
						nodeReqResponse
								.getRequestParameters()
								.put(VarUtils.NODE_REQUEST_EXECUTE_MSG,
										VarUtils.NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_KEY_OR_VALUE_NULL);

						nodeReqResponse.getRequestParameters().put(
								VarUtils.NODE_REQUEST_WILL_EXECUTE,
								new Boolean(false).toString());

					} else {
						nodeReqResponse.getRequestParameters().put(
								VarUtils.NODE_REQUEST_PREFIX_REPLACE_VAR
										+ replaceVarKey, replaceVarValue);

						// CAREFUL! This is added to prevent a last time run
						// "NOT EXECUTE" to continue to be effective this time.
						// Since the whole nodeReqResponse will not replaced
						// everytime

						/*
						 * 20131205: to prevent this overwrite when there is a
						 * NA field passed in. This will check
						 */
						if (replacementVarMapForThisNode.getMap().keySet()
								.contains(VarUtils.NA)) {
							nodeReqResponse.getRequestParameters().put(
									VarUtils.NODE_REQUEST_WILL_EXECUTE,
									new Boolean(false).toString());
						} else {
							nodeReqResponse.getRequestParameters().put(
									VarUtils.NODE_REQUEST_WILL_EXECUTE,
									new Boolean(true).toString());
						}

					}

				} else {
					models.utils.LogUtils
							.printLogError("agentCommand  is null in genericUpdateRequestByAddingReplaceVarKeyValuePairHelper()"
									+ DateUtils.getNowDateTimeStrSdsm());
				}

			}// end loop thru entryReplaceMap

		}// end for loop of nodeData

	}// end func
}
