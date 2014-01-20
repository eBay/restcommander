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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.data.LogFile;
import models.data.NodeGroupSourceMetadata;
import models.data.providers.AgentDataProvider;
import models.data.providers.LogProvider;
import models.data.providers.NodeGroupProvider;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.FileIoUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.ADHOC_COMPONENT_TYPE;

import play.mvc.Controller;

/**
 * 
 * @author ypei
 *
 */
public class NodeGroups extends Controller {

	public static void index() {

		String page = "index";
		String topnav = "nodeGroups";

		boolean isAdhoc = false;
		try {
			NodeGroupProvider ngp = NodeGroupProvider.getInstance();
			List<NodeGroupSourceMetadata> nodeGroups = ngp
					.getNodeGroupSourceMetadatas(isAdhoc);
			String lastRefreshed = DateUtils.getNowDateTimeStrSdsm();

			render(page, topnav, nodeGroups, lastRefreshed);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in index of logs");
		}

	}

	public static void getNodeGroupSourceMetadata(String nodeGroupType,
			Boolean isAdhoc, Boolean isTxtFqdnOnly) {
		if (isAdhoc == null) {
			isAdhoc = false;
		}

		if (isTxtFqdnOnly == null) {
			isTxtFqdnOnly = false;
		}

		try {
			NodeGroupProvider ngp = NodeGroupProvider.getInstance();
			NodeGroupSourceMetadata nodeGroupSourceMetadata = ngp
					.getNodeGroupSourceMetadata(isAdhoc, nodeGroupType);

			if (isTxtFqdnOnly) {
				/**
				 * 20130925: add for adhoc ones; which better read from file if
				 * not in memory
				 */
				if (nodeGroupSourceMetadata == null) {
					// try read from file

					// TODO if fqdn only; need to map this back to
					renderText(LogProvider
							.genFilePathAndReadLogAdhocComponents(
									ADHOC_COMPONENT_TYPE.NODE_GROUP,
									nodeGroupType));
				} else {

					renderText(AgentUtils
							.cleanDisplayStringListLineByLineFromJavaStringList(nodeGroupSourceMetadata
									.getNodeList()));
				}

			} else {
				/**
				 * 20130925: add for adhoc ones; which better read from file
				 */
				if (nodeGroupSourceMetadata == null) {
					// try read from file

					renderText(LogProvider
							.genFilePathAndReadLogAdhocComponents(
									ADHOC_COMPONENT_TYPE.NODE_GROUP,
									nodeGroupType));
				} else {

					renderJSON(nodeGroupSourceMetadata);
				}

			}

		} catch (Throwable t) {
			t.printStackTrace();
			renderText("Error occured in getNodeGroupSourceMetadata");
		}

	}// end func



}
