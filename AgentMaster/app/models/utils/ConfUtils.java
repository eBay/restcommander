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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.data.AgentCommandMetadata;
import models.data.HttpHeaderMetadata;
import models.data.providers.AgentDataProvider;
import models.utils.ConfUtils.DAO_TYPE;

import play.Play;

public class ConfUtils {

	public static enum DAO_TYPE {
		FILE, SWIFT;
	}
	
	public static String nodeGroupConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH + "nodegroup.conf";
	public static String agentCommandConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH +"agentcommand.conf";
	public static String aggregationConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH +"aggregation.conf";
	public static String httpHeaderConfFileLocation = VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH +"httpheader.conf";

	// public static DAO_TYPE daoType = DAO_TYPE.SWIFT;
	public static ConfUtils.DAO_TYPE daoType = getDaoTypeFromStr(getStrFromApplicationConfVarValue("DAO_TYPE"));

	public static DAO_TYPE getDaoTypeFromStr(String daoTypeStr) {
		DAO_TYPE daoType = DAO_TYPE.FILE;

		if (daoTypeStr != null && daoTypeStr.equalsIgnoreCase("SWIFT")) {
			daoType = DAO_TYPE.SWIFT;
		}
		return daoType;
	}

	// TODO load from the conf; "prod-iaas";
	// public static String swiftContainerPrefix= "prod-sbe-";
	public static String swiftContainerPrefix = getStrFromApplicationConfVarValue("SWIFT_CONTAINER_PREFIX");

	public static String playConfGetPropertyStr(String propertyKey) {

		String propertyValue = Play.configuration == null ? ""
				: Play.configuration.getProperty(propertyKey);

		return propertyValue;
	}

	// 201405
	public static boolean updatingConf = false;

	public static boolean runCronJob = runCronJob();

	// 20140505 default as false
	public static boolean runCronJobCassiniTags = false;

	// runCronJob();
	// false; for testing with main function

	// 20130909:
	public static boolean enableLoadBalancer = true;

	public static String nodeGroupConfFileName = "nodegroup.conf";
	public static String agentCommandConfFileName = "agentcommand.conf";
	public static String aggregationConfFileName = "aggregation.conf";
	public static String wisbvarConfFileName = "wisbvar.conf";
	public static String httpHeaderConfFileName = "httpheader.conf";

	public static String getConfFilePath(String fileName) {
		return VarUtils.CONFIG_FILE_FOLDER_WITH_SLASH + fileName;
	}

	public static boolean runCmsJob = false;

	// 20140218
	public static boolean runIaasEsxJob = false;

	public static String localHostName = getLocalHostName();

	// 20130828.
	public static boolean isRunningDaisyDeploymentMultiplePoolsNow = false;

	public static String serverInstancePrefix = getServerInstancePrefix();

	public static boolean isRunningDaisyDeploymentMultiplePoolsNow() {
		return isRunningDaisyDeploymentMultiplePoolsNow;
	}

	public static void setRunningDaisyDeploymentMultiplePoolsNow(
			boolean isRunningDaisyDeploymentMultiplePoolsNow) {
		ConfUtils.isRunningDaisyDeploymentMultiplePoolsNow = isRunningDaisyDeploymentMultiplePoolsNow;
	}

	/**
	 * 20140522: date
	 * 
	 * @return
	 */
	private static String getServerInstancePrefix() {

		// if disabled just return "";
		// return "";

		String funcName = "getServerInstancePrefix" + "()";

		String prefix = null;

		try {

			// this fqnd must be first loaded; dependency.
			if (ConfUtils.localHostName == null) {
				localHostName = getLocalHostName();
			}

			String httpPort = models.utils.ConfUtils
					.playConfGetPropertyStr("http.port");
			String httpsPort = models.utils.ConfUtils
					.playConfGetPropertyStr("https.port");
			prefix = (httpsPort == null || httpsPort.isEmpty()) ? "http://"
					+ ConfUtils.localHostName + ":" + httpPort : "https://"
					+ ConfUtils.localHostName + ":" + httpsPort;

		} catch (Throwable t) {
			t.printStackTrace();
			VarUtils.printSysErrWithTimeAndOptionalReason(funcName,
					t.getLocalizedMessage());

		}

		return prefix;

	}

	public static void cleanServerInstancePrefix() {
		ConfUtils.serverInstancePrefix = "";
	}

	public static void reloadServerInstancePrefix() {
		ConfUtils.serverInstancePrefix = getServerInstancePrefix();
	}

	/**
	 * 20140111: for dynamically get parameter from conf
	 * 
	 * @param varName
	 * @return
	 */
	public static String getStrFromApplicationConfVarValue(String varName) {

		String varValueStr = models.utils.ConfUtils
				.playConfGetPropertyStr("agentmaster.var." + varName);

		if (VarUtils.IN_DEBUG) {
			models.utils.LogUtils.printLogNormal("GetFromConf varName: "
					+ varName + "\tValue:" + varValueStr);
		}
		return varValueStr;

	}

	/**
	 * Support such as cms point.
	 * 
	 * @param varNameFull
	 * @return
	 */
	public static String getStrFromApplicationConfVarValueFullName(
			String varNameFull) {

		String varValueStr = models.utils.ConfUtils
				.playConfGetPropertyStr(varNameFull);

		if (VarUtils.IN_DEBUG) {
			models.utils.LogUtils.printLogNormal("GetFromConf varName: "
					+ varNameFull + "\tValue:" + varValueStr);
		}
		return varValueStr;

	}

	public static int getIntFromApplicationConfVarValue(String varName) {

		String varValueStr = getStrFromApplicationConfVarValue(varName);
		int varValueInt = Integer.parseInt(varValueStr);
		return varValueInt;

	}

	public static boolean getBooleanFromApplicationConfVarValue(String varName) {

		String varValueStr = getStrFromApplicationConfVarValue(varName);
		boolean varValueBoolean = Boolean.parseBoolean(varValueStr);
		return varValueBoolean;

	}

	public static String getLocalHostName() {

		String localHostName = "UNKNOWN_FQDN";
		try {
			localHostName = java.net.InetAddress.getLocalHost()
					.getCanonicalHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return localHostName;

	}

	public static boolean runCronJob() {

		String runCronJobStr = models.utils.ConfUtils
				.playConfGetPropertyStr("agentmaster.cronjob.run");
		boolean runCron = Boolean.parseBoolean(runCronJobStr);
		return runCron;

	}

	public static String getUrlSrepo() {
		String runCronJobStr = models.utils.ConfUtils
				.playConfGetPropertyStr("agentmaster.url.srepo");
		return runCronJobStr;

	}

	public static boolean isRunCronJob() {
		return runCronJob;
	}

	public static void setRunCronJob(boolean runCronJob) {
		ConfUtils.runCronJob = runCronJob;
	}

	public static boolean isEnableLoadBalancer() {
		return enableLoadBalancer;
	}

	public static void setEnableLoadBalancer(boolean enableLoadBalancer) {
		ConfUtils.enableLoadBalancer = enableLoadBalancer;
	}

	public static String getLocalhostname() {
		return localHostName;
	}

	public static void main(String[] args) {
		;
	}

	public static boolean isRunCmsJob() {
		return runCmsJob;
	}

	public static void setRunCmsJob(boolean runCmsJob) {
		ConfUtils.runCmsJob = runCmsJob;
	}

	public static boolean isRunIaasEsxJob() {
		return runIaasEsxJob;
	}

	public static void setRunIaasEsxJob(boolean runIaasEsxJob) {
		ConfUtils.runIaasEsxJob = runIaasEsxJob;
	}

	public static boolean isRunCronJobCassiniTags() {
		return runCronJobCassiniTags;
	}

	public static void setRunCronJobCassiniTags(boolean runCronJobCassiniTags) {
		ConfUtils.runCronJobCassiniTags = runCronJobCassiniTags;
	}

	public static ConfUtils.DAO_TYPE getDaoType() {
		return daoType;
	}

	public static void setDaoType(String daoTypeStr) {

		if (daoTypeStr.equalsIgnoreCase("SWIFT")) {

			ConfUtils.daoType = DAO_TYPE.SWIFT;
		} else {
			ConfUtils.daoType = DAO_TYPE.FILE;
		}

		AgentDataProvider adp = AgentDataProvider.getInstance();
		adp.updateConfigFromAllFiles();
	}

	public static String getSwiftContainerPrefix() {
		return swiftContainerPrefix;
	}

	/**
	 * note that this also refresh all config loading.
	 * 
	 * @param swiftContainerPrefix
	 */
	public static void setSwiftContainerPrefix(String swiftContainerPrefix) {
		ConfUtils.swiftContainerPrefix = swiftContainerPrefix;

		AgentDataProvider adp = AgentDataProvider.getInstance();
		adp.updateConfigFromAllFiles();
	}

	public static boolean isUpdatingConf() {
		return updatingConf;
	}

	public static void setUpdatingConf(boolean updatingConf) {
		ConfUtils.updatingConf = updatingConf;
	}
	
	

}
