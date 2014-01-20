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
package models.utils;

import java.net.UnknownHostException;
import java.util.Date;

import play.Play;
/**
 * 
 * @author ypei
 *
 */
public class ConfUtils {

	public static boolean runCronJob = runCronJob();
	
	public static String nodeGroupConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH + "nodegroup.conf";
	public static String agentCommandConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH +"agentcommand.conf";
	public static String aggregationConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH +"aggregation.conf";
	public static String httpHeaderConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH +"httpheader.conf";
	
	
	public static final String localHostName = getLocalHostName();

	
	/**
	 * 20140111: for dynamically get parameter from conf
	 * @param varName
	 * @return
	 */
	public static String getStrFromApplicationConfVarValue(String varName) {

		String varValueStr = Play.configuration
				.getProperty("agentmaster.var."+varName);
		
		if(VarUtils.IN_DEBUG){
			models.utils.LogUtils.printLogNormal("GetFromConf varName: " + varName + "\tValue:" + varValueStr);
		}
		
		return varValueStr;

	}
	
	public static int getIntFromApplicationConfVarValue(String varName) {

		String varValueStr = getStrFromApplicationConfVarValue(varName);
		int varValueInt = Integer.parseInt(varValueStr);
		
		

		return varValueInt;

	}
	
	
	// 20130828.

	public static String getLocalHostName() {

		String localHostName = "UNKNOWN_FQDN";
		try {
			localHostName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return localHostName;

	}

	public static boolean runCronJob() {

		String runCronJobStr = Play.configuration
				.getProperty("agentmaster.cronjob.run");
		boolean runCron = Boolean.parseBoolean(runCronJobStr);
		return runCron;

	}
	
	
	

	public static boolean isRunCronJob() {
		return runCronJob;
	}

	public static void setRunCronJob(boolean runCronJob) {
		ConfUtils.runCronJob = runCronJob;
	}

	

	public static String getLocalhostname() {
		return localHostName;
	}

	public static void main(String[] args) {
		;
	}

	
	
}
