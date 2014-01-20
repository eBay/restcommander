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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


import play.Play;
import play.vfs.VirtualFile;

import models.asynchttp.response.GenericAgentResponse;
import models.data.AgentCommandMetadata;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeGroupSourceType;
import models.data.NodeReqResponse;
import models.data.NodeReqResponse.ResponseContent;
import models.utils.DateUtils;
import models.utils.VarUtils;

/**
 * Singleton: additional actions on the ADP.
 * 
 * @author ypei
 * 
 */
public class AgentDataProviderHelper {

	private static final AgentDataProviderHelper instance = new AgentDataProviderHelper();

	public static Date lastRefreshDataValid = new Date(0);
	public static Date lastRefreshDataInProgress = new Date(0);

	public static AgentDataProviderHelper getInstance() {
		return instance;
	}

	

	
	public static void filterUnsafeOrUnnecessaryRequest(
			Map<String, NodeData> nodeDataMapValidSource,
			Map<String, NodeData> nodeDataMapValidSafe, String agentCommandType) {

		for (Entry<String, NodeData> entry : nodeDataMapValidSource.entrySet()) {

			String fqdn = entry.getKey();
			NodeData nd = entry.getValue();

			if (nd == null) {
				// bad entry; skip
				models.utils.LogUtils.printLogError("NodeData is NULL!! in filterUnsafeRequest");
				continue;
			}

			NodeReqResponse nrr = nd.getDataMap().get(agentCommandType);
			if (nrr == null) {
				models.utils.LogUtils.printLogError("nrr is NULL!! in filterUnsafeRequest");
				continue;
			}
			// now get the content.
			Map<String, String> map = nrr.getRequestParameters();
			if (map == null) {
				 models.utils.LogUtils.printLogError
						 ("nrr.getRequestParameters is NULL!! in filterUnsafeRequest");
				continue;
			}

			/**
			 * 20130507: will generally apply to all requests: if have this field and this field is false
			 */
			if(map.containsKey(VarUtils.NODE_REQUEST_WILL_EXECUTE)){
				
				Boolean willExecute = Boolean.parseBoolean(map.get(VarUtils.NODE_REQUEST_WILL_EXECUTE));
				
				if(VarUtils.IN_DEBUG){
					models.utils.LogUtils.printLogNormal("!!Not executed command for command " + agentCommandType +" on node: " + fqdn + " at " + DateUtils.getNowDateTimeStrSdsm()); 
				}
				
				if(willExecute==false){
					continue;
				}
			}
			

			// now safely to add this node in.

			// note that this is shallow copy; put the pointer of the source
			// in...
			nodeDataMapValidSafe.put(fqdn, nd);
		}// end for loop

	}
	

	public void updateResponseFromAgentGenericResponse(String nodeGroupType,
			String agentCommandType, GenericAgentResponse gap, Map<String, NodeGroupDataMap> dataStore) {

		if (gap != null || nodeGroupType != null || agentCommandType != null) {
			String fqdn = gap.getHost();

			NodeGroupDataMap ngdm = dataStore
					.get(nodeGroupType);
			if (ngdm != null) {
				NodeData nodeData = ngdm.getNodeDataMapValid().get(fqdn);

				if (nodeData != null) {
					NodeReqResponse nrr = nodeData.getDataMap().get(
							agentCommandType);
					ResponseContent responseContent = new ResponseContent(gap);
					nrr.setResponseContent(responseContent);

				} else {
					 models.utils.LogUtils.printLogError
							 ("ERROR nodeData is null in updateResponseFromAgentGenericResponse");
				}

			} else {
				 models.utils.LogUtils.printLogError
						 ("ERROR ngdm is null in updateResponseFromAgentGenericResponse");
			}
		} else {
			 models.utils.LogUtils.printLogError
					 ("ERROR gap or nodeGroupType or agentCommandType is null in updateResponseFromAgentGenericResponse");
		}

	}

	public static void updateAgentDataForNode(NodeGroupDataMap ngdm,
			String fqdn, NodeReqResponse nodeReqResponse,
			String agentCommandType) {

		NodeData nd = null;

		// check if the node has already has a NodeData Object

		// ASSUMPTION: When the key is not empty, there must be a value of
		// NodeData
		if (ngdm.getNodeDataMapInProgress().containsKey(fqdn)
				&& ngdm.getNodeDataMapInProgress().get(fqdn) != null) {
			// use existing
			nd = ngdm.getNodeDataMapInProgress().get(fqdn);
		} else {
			nd = new NodeData(fqdn);
		}
		nd.getDataMap().put(agentCommandType, nodeReqResponse);
		// push this back to map ; needed for the newly constructed NodeData
		ngdm.getNodeDataMapInProgress().put(fqdn, nd);
	}

	/**
	 * Go thru target node list.
	 * 
	 * @param nodeGroupTypeSource
	 *            : is the source of the request content: e.g. c
	 * @param nodeGroupTypeTarget
	 *            : is the target nodes : e.g. AD HOC
	 * @param agentCommandType
	 */
	public static void copyRequestToNodeGroup(String nodeGroupTypeSource,
			String nodeGroupTypeTarget, String agentCommandType) {

		AgentDataProvider adp = AgentDataProvider.getInstance();

		List<String> targetNodeList = adp.nodeGroupSourceMetadatas.get(
				nodeGroupTypeTarget).getNodeList();

		// check if the node has been in the source node group? if yes, get the
		// content out.

		for (String fqdn : targetNodeList) {

			try {

				// is this node in target nodeGroup?
				if (!adp.allAgentData.get(nodeGroupTypeSource)
						.getNodeDataMapValid().containsKey(fqdn)) {
					continue;
				}

				// Hahh.. catch NPE !!
				NodeReqResponse nrrSource = adp.allAgentData
						.get(nodeGroupTypeSource).getNodeDataMapValid()
						.get(fqdn).getDataMap().get(agentCommandType);
				// deep copy
				NodeReqResponse nrrNew = new NodeReqResponse(nrrSource);

				// TODO: not tested target must already have NodeData
				
				if(! adp.allAgentData.get(nodeGroupTypeTarget).getNodeDataMapValid()
						.containsKey(fqdn)){
					 NodeData nd = new NodeData(fqdn);
					 adp.allAgentData.get(nodeGroupTypeTarget).getNodeDataMapValid()
						.put(fqdn, nd);
				}
				
				adp.allAgentData.get(nodeGroupTypeTarget).getNodeDataMapValid()
				.get(fqdn).getDataMap().put(agentCommandType, nrrNew);
				
				
				
				// now copy the inProgress;
				

				// Hahh.. catch NPE !!
				NodeReqResponse nrrSourceInProgress = adp.allAgentData
						.get(nodeGroupTypeSource).getNodeDataMapInProgress()
						.get(fqdn).getDataMap().get(agentCommandType);
				// deep copy
				NodeReqResponse nrrNewInProgress = new NodeReqResponse(nrrSourceInProgress);
				// TODO: not tested target must already have NodeData
				
				if(! adp.allAgentData.get(nodeGroupTypeTarget).getNodeDataMapInProgress()
						.containsKey(fqdn)){
					 NodeData nd = new NodeData(fqdn);
					 adp.allAgentData.get(nodeGroupTypeTarget).getNodeDataMapInProgress()
						.put(fqdn, nd);
				}
				
				adp.allAgentData.get(nodeGroupTypeTarget).getNodeDataMapInProgress()
				.get(fqdn).getDataMap().put(agentCommandType, nrrNewInProgress);


			} catch (Throwable t) {

				t.printStackTrace();
			}
		}

	}

}
