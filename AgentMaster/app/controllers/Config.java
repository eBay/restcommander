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

import models.asynchttp.actors.ActorConfig;
import models.data.providers.AgentConfigProvider;
import models.data.providers.AgentDataProvider;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.CONFIG_FILE_TYPE;

import play.mvc.Controller;

/**
 * 
 * @author ypei
 *
 */
public class Config extends Controller {

	public static void getAgentCommand() {

		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();

			renderJSON(adp.getAgentcommandmetadatas());
		} catch (Throwable t) {
			renderJSON("Error occured in getAgentCommand");
		}

	}

	public static void getAggregation() {

		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();

			renderJSON(adp.getAggregationmetadata());
		} catch (Throwable t) {
			renderJSON("Error occured in getAggregation");
		}

	}

	public static void getNodeGroup() {

		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();

			renderJSON(adp.getNodegroupsourcemetadatas());
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in getNodeGroup");
		}

	}
	
	public static void getHttpHeaders(String httpHeaderType) {

		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();

			if(httpHeaderType ==null 
				|| 
				!adp.getHeaderMetadataMap().containsKey(httpHeaderType)
					){
				
				renderJSON(adp.getHeaderMetadataMap());
			}else{
				renderJSON(adp.getHeaderMetadataMap().get(httpHeaderType));
			}

		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in getHttpHeaders");
		}

	}
	

	/**
	 * 
	 */
	public static void shutDownActorSystem() {

		try {
			ActorConfig.shutDownActorSystemWhenNoJobRunning();
			renderJSON( "Success in shutDownActorSystem at " + DateUtils.getNowDateTimeStrSdsm());
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in shutDownActorSystem");
		}

	}
	
	public static void runGC() {

		try {
			ActorConfig.runGCWhenNoJobRunning();
			renderJSON( "Success in RunGC at " + DateUtils.getNowDateTimeStrSdsm());
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in RunGC");
		}

	}

	public static void reloadConfig(String type) {

		if (type == null) {
			type = CONFIG_FILE_TYPE.ALL.toString();
		}
		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();

			String result = adp.updateConfigFromFile(type);

			renderJSON(result + " in reloadConfig with type " + type);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in reloadConfig with type" + type);
		}

	}

	public static void editConfig(String configFile) {

		String page = "editConfig";
		String topnav = "config";

		try {
			AgentConfigProvider acp = AgentConfigProvider.getInstance();

			if (configFile == null) {
				renderJSON("configFile is NULL. Error occured in editConfig");
			}

			CONFIG_FILE_TYPE configFileType = CONFIG_FILE_TYPE
					.valueOf(configFile.toUpperCase(Locale.ENGLISH));

			String configFileContent = acp.readConfigFile(configFileType);

			String configFileUpper = configFile.toUpperCase(Locale.ENGLISH);

			page = new String(page + configFile.toLowerCase(Locale.ENGLISH));

			String alert = null;

			render(page, topnav, configFileUpper, configFileContent, alert);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in editConfig");
		}

	}// end func

	public static void editConfigUpdate(String configFile,
			String configFileContent) {

		String page = "editConfig";
		String topnav = "config";

		try {
			AgentConfigProvider acp = AgentConfigProvider.getInstance();

			if (configFile == null) {
				renderJSON("configFile is NULL. Error occured in editConfig");
			}

			CONFIG_FILE_TYPE configFileType = CONFIG_FILE_TYPE
					.valueOf(configFile.toUpperCase(Locale.ENGLISH));

			acp.saveConfigFile(configFileType, configFileContent);

			String configFileUpper = configFile.toUpperCase(Locale.ENGLISH);

			page = new String(page + configFile.toLowerCase(Locale.ENGLISH));

			String alert = "Config was successfully updated at "
					+ DateUtils.getNowDateTimeStrSdsm();

			// reload after
			AgentDataProvider adp = AgentDataProvider.getInstance();
			adp.updateConfigFromFile(configFileUpper);

			renderTemplate("Config/editConfig.html", page, topnav,
					configFileContent, configFileUpper, alert);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in editConfigUpdate");
		}

	}// end func

	public static void index() {

		String page = "index";
		String topnav = "config";

		try {
			AgentConfigProvider acp = AgentConfigProvider.getInstance();

			String configFileContentAgentCommand = acp
					.readConfigFile(CONFIG_FILE_TYPE.AGENTCOMMAND);
			String configFileContentNodeGroup = acp
					.readConfigFile(CONFIG_FILE_TYPE.NODEGROUP);
			
			String configFileContentAggregation= acp
					.readConfigFile(CONFIG_FILE_TYPE.AGGREGATION);
			
			String configFileContentWisbvar= acp
					.readConfigFile(CONFIG_FILE_TYPE.WISBVAR);
			
			String configFileContentHttpheader= acp
					.readConfigFile(CONFIG_FILE_TYPE.HTTPHEADER);

			String configFileAgentCommandTitle = CONFIG_FILE_TYPE.AGENTCOMMAND
					.toString();
			String configFileNodeGroupTitle = CONFIG_FILE_TYPE.NODEGROUP
					.toString();
			
			String configFileAggregationTitle = CONFIG_FILE_TYPE.AGGREGATION
					.toString();
			
			String configFileWisbvarTitle = CONFIG_FILE_TYPE.WISBVAR
					.toString();
			
			String configFileHttpheaderTitle = CONFIG_FILE_TYPE.HTTPHEADER
					.toString();

			render(page, topnav, configFileAgentCommandTitle,
					configFileNodeGroupTitle, configFileAggregationTitle, configFileContentAgentCommand,
					configFileContentNodeGroup, configFileContentAggregation,
					configFileWisbvarTitle,configFileHttpheaderTitle,
					configFileContentWisbvar, configFileContentHttpheader
					
					
					);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in editConfig");
		}

	}


	/**
	 * 20130718 add
	 * 
	 * @param runCronJob
	 */
	public static void setRunCronJob(boolean runCronJob) {

		ConfUtils.setRunCronJob(runCronJob);
		renderText("Set runCronJob as " + runCronJob + " at time: "
				+ DateUtils.getNowDateTimeStrSdsm());

	}

	
	
}
