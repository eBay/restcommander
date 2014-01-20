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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.data.AgentCommandMetadata;
import models.data.JsonResult;
import models.data.LogFile;
import models.data.NodeGroupSourceMetadata;
import models.data.providers.AgentConfigProvider;
import models.data.providers.AgentDataProvider;
import models.data.providers.LogProvider;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.FileIoUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.CONFIG_FILE_TYPE;

import play.mvc.Controller;

/**
 * 
 * @author ypei
 *
 */
public class Wizards extends Controller {

	public static void index() {

		String page = "index";
		String topnav = "wizards";

		try {
			String lastRefreshed = DateUtils.getNowDateTimeStrSdsm();


			render(page, topnav, lastRefreshed);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in index of logs");
		}

	}
	
	public static void wizardToAdhocNodeGroup() {

		String page = "wizardToAdhocNodeGroup";
		String topnav = "wizards";

		try {

			AgentDataProvider adp = AgentDataProvider.getInstance();

			List<AgentCommandMetadata> agentCommandMetadataList = AgentCommandMetadata
					.convertMapToList(adp.getAgentcommandmetadatas());

			String agentCommandMetadataListJsonArray = AgentUtils
					.toJson(agentCommandMetadataList);

			render(page, topnav, agentCommandMetadataListJsonArray
					 );
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON(new JsonResult("Error occured in agentUpdateWizardAdhocs "));
		}

	}
	
	


	public static void wizard() {

		String page = "wizard";
		String topnav = "commands";

		try {

			AgentDataProvider adp = AgentDataProvider.getInstance();

			List<NodeGroupSourceMetadata> nodeGroupSourceMetadataList = NodeGroupSourceMetadata
					.convertMapToList(adp.getNodegroupsourcemetadatas());

			String nodeGroupSourceMetadataListJsonArray = AgentUtils
					.toJson(nodeGroupSourceMetadataList);

			List<AgentCommandMetadata> agentCommandMetadataList = AgentCommandMetadata
					.convertMapToList(adp.getAgentcommandmetadatas());

			String agentCommandMetadataListJsonArray = AgentUtils
					.toJson(agentCommandMetadataList);

			render(page, topnav, nodeGroupSourceMetadataListJsonArray,
					agentCommandMetadataListJsonArray);
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON(new JsonResult("Error occured in wizard"));
		}

	}
	

	/**
	 * 201401 singleServerWizard
	 */
	
	public static void singleServerWizard() {

		String page = "singleServerWizard";
		String topnav = "wizards";

		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();
			List<AgentCommandMetadata> agentCommandMetadataList = AgentCommandMetadata
					.convertMapToList(adp.getAgentcommandmetadatas());

			String agentCommandMetadataListJsonArray = AgentUtils
					.toJson(agentCommandMetadataList);

			
			render(page, topnav, agentCommandMetadataListJsonArray);
		} catch (Throwable t) {

			t.printStackTrace();
			renderJSON(new JsonResult("Error occured in singleServerWizard"));
		}
	}// end 

	
}
