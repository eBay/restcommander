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

import models.asynchttp.NingClientFactory;


/**
 * All static var, parameters...
 * 
 * @author ypei
 * 
 */
public class VarUtils {
	
	
	//20140314: must use 
	// POTENTIAL BUG: CANNOT USE LOCATION THAT IS +XX00; MUST USE -XX00 ; since + must be URL encoded. Currently only allow -XX00
	public static String STR_LOG_TIME_ZONE =ConfUtils.getStrFromApplicationConfVarValue("LOG_TIME_ZONE");    
			//"America/New_York";
	
	//20140102: for using logger or not. even use it; may redirect to systemout
	public static boolean useLogger = true;
	
	public static final String ADHOC = "ADHOC";
	public static final String ADHOCDATASTORE = ADHOC + "DATASTORE";

	public static final String HTTP_HEADER_TYPE_GLOBAL1 = "SUPERMAN_GLOBAL";
	public static int MIN_COMMAND_FIELD_LENGTH = 8;
	// 20131024 enroll
	public static String EMPTY_STATE_HUB = "null";

	public static String SUPERMAN_COMMENT_PREFIX = "//[SUPERMANSPECIAL]";

	public static String AGGREGATION_SUPERMAN_SPECIAL_STATUS_CODE = "SUPERMAN_SPECIAL_STATUS_CODE";

	public static String AGGREGATION_SUPERMAN_SPECIAL_RESPONSE_TIME = "SUPERMAN_SPECIAL_RESPONSE_TIME";
	public static String SUPERMAN_NOT_FIRE_REQUEST = "SUPERMANSPECIAL_REQUEST_NOT_FIRED.NO_RESPONSE.NOT_ERROR.CHECK_REQUEST_PARAMETERS_FOR_DETAILS.";


	
	public static final String NODEGROUP_CONF_NODEGROUP_ADHOC_NODE_LIST_TOP100WEBSITES = "ADHOC_NODE_LIST_TOP100WEBSITES";
	public static final String AGENT_CMD_KEY_GET_FRONT_PAGE = "GET_FRONT_PAGE";
	// 20130828: added for enable generic var replacement. Not only from SREPO;
	// but also from
	public static enum VAR_REPLACEMENT_TYPE {
		WISB_SREPO, ADHOC_FROM_API
	};

	public static enum CONFIG_FILE_TYPE {
		AGENTCOMMAND, NODEGROUP, NOTES, AGGREGATION, WISBVAR, HTTPHEADER, ALL
	};

	// 20130924:
	public static enum ADHOC_COMPONENT_TYPE {
		AGGREGATION_RULE, NODE_GROUP, COMMAND
	};

	/**
	 * 20130807 not apply to ad hoc list
	 */
	public static boolean useColoAsClusterTag = false;

	public static final boolean IN_DEBUG = true;
	public static final boolean IN_DETAIL_DEBUG = false;
	public static final long PAUSE_INTERVAL_WORKER_MILLIS_DEFAULT = 1L;

	public static String STR_UNKNOWN1 = "UNKNOWN_SUPERMAN";
	public static final long LONG_UNKNOWN = -1L;

	public static final String INPUT_STREAM_READ_HTTP_TEXT_ENCODING_UTF = "utf-8";


	public static final int AGNET_RESPONSE_MAX_RESPONSE_DISPLAY_BYTE1 = 128;


	public static final String CONFIG_FILE_FOLDER_WITH_SLASH = "conf/";

	public static final String TAG_GENERATOR_POST_FIX = "/*";

	// ********************AKKA ACTOR / NING START********************
	
	/**
	 * KEY part: from application.conf
	 */
	public static int ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT = ConfUtils.getIntFromApplicationConfVarValue("ACTOR_MAX_OPERATION_TIME_SECONDS_DEFAULT");
	public static final int NING_SLOWCLIENT_REQUEST_TIMEOUT_MS = ConfUtils.getIntFromApplicationConfVarValue("NING_SLOWCLIENT_REQUEST_TIMEOUT_MS");
	public static final int NING_SLOWCLIENT_CONNECTION_TIMEOUT_MS = ConfUtils.getIntFromApplicationConfVarValue("NING_SLOWCLIENT_CONNECTION_TIMEOUT_MS");
	public static final int NING_FASTCLIENT_REQUEST_TIMEOUT_MS = ConfUtils.getIntFromApplicationConfVarValue("NING_FASTCLIENT_REQUEST_TIMEOUT_MS");
	public static final int NING_FASTCLIENT_CONNECTION_TIMEOUT_MS = ConfUtils.getIntFromApplicationConfVarValue("NING_FASTCLIENT_CONNECTION_TIMEOUT_MS");
	public static int MAX_CONCURRENT_SEND_SIZE = ConfUtils.getIntFromApplicationConfVarValue("MAX_CONCURRENT_SEND_SIZE");;

	
	
	// this is the director; cannot easily send cancel to the workers
	public static long TIMEOUT_ASK_AGGREGATION_MANAGER_SCONDS = 30; // 30 sec
	public static long TIMEOUT_ASK_MANAGER_SCONDS = 2520; // 42MIN
	public static long TIMEOUT_IN_MANAGER_SCONDS = 2400; // 40MIN
	public static final String ACTOR_SYSTEM = "AgentMasterActorSystem";

	public static long ACTOR_BATCH_JOB_SLEEP_INTERVAL_MILLIS = 20000L;
	
	public static int CONVERSION_1024 = 1024;

	public static long RETRY_INTERVAL_MILLIS = 500L; // 1 sec
	
	
	// 20131205 add slow client for any new command defined. 
	public static final String AGENT_CMD_KEY_SLOW_CLIENT_SUBSTR = "SLOWNINGCLIENT";
	// ********************AKKA ACTOR END********************

	// ********************ADHOC COMPONENTS START********************
	public static String ADHOC_COMPONENTS_DIR = "adhoc_components";
	public static String ADHOC_COMPONENTS_SUBDIR_AGGREGATION_RULES = "aggregation_rules";
	public static String ADHOC_COMPONENTS_SUBDIR_COMMANDS = "commands";
	public static String ADHOC_COMPONENTS_SUBDIR_NODE_GROUPS = "node_groups";
	// *******************ADHOC COMPONENTS END********************

	// ********************RELIABILITY GC/MEM START********************
	// explictly GC call should normally be forbiddened; unless a must case.
	public static double MEMORY_MB_TRIGGER_GC_THRESHOLD_MB = 8 * 1024.0;
	// *******************RELIABILITY END********************

	// 20130801
	// ********************CMS START********************
	// *******************CMS END

	public static final String OPERATION_SUCCESSFUL = "OPERATION_SUCCESSFUL";

	public static final int ARCHIVE_LOG_DATE_BASE3 = 2;
	public static final int DELETE_LOG_DATE_BASE4 = 5;

	public static final String FILE_NAME_APP_LOG_EMPTY = "empty.txt";

	public static final String NA = "NA";
	public static final String NA_NUM = "-1";
	public static final int INVALID_NUM = -1;


	
	/**
	 * 20130923: NOTE all adhoc components viewer in UI makes the ASSUMPTION:
	 * ADHOCDATASTORE_JSONOBJNAME_TIMESTAMP
	 * 
	 * This hard 3 parts is easier for regular expression matching to match out
	 * stuff.
	 * 
	 * POTENTIAL BUG NOT displaying properly:
	 */
	public static final String ADHOC_NODEGROUP_PREFIX = ADHOCDATASTORE
			+ "_NODEGROUP_";

	public static final String ADHOC_CMS_WISB_WIRI_MAP_PREFIX = ADHOCDATASTORE
			+ "_CMSWISBWIRIMAP_";

	public static final String AGENT_CMD_KEY_INVALID = "INVALID";



	public static final String EMAIL_SOURCE_ADDRESS1 = "RESTSuperman <RESTSupermanDoNotReply@myhost.com>";
	public static boolean EMAIL_SENT_MAIL_ONLY_SELF = true;

	// log save
	public static final String STRING_SLASH = "/";
	public static final String LOG_FOLDER_NAME_APP = "app_logs";
	public static final String LOG_FOLDER_NAME_APP_WITH_SLASH = LOG_FOLDER_NAME_APP
			+ "/";

	public static final String LOG_FOLDER_NAME_ADHOC = "app_logs_adhoc";
	public static final String LOG_FOLDER_NAME_ADHOC_WITH_SLASH = LOG_FOLDER_NAME_ADHOC
			+ "/";
	public static final String LOG_FOLDER_NAME_NONESTARDARD = "app_logs_none_standard";
	public static final String LOG_FOLDER_NAME_NONESTARDARD_WITH_SLASH = LOG_FOLDER_NAME_NONESTARDARD
			+ "/";
	public static final String LOG_FOLDER_NAME_ADHOC_COMPONENTS = "adhoc_components";
	public static final String LOG_FOLDER_NAME_ADHOC_COMPONENTS_AGGREGATION_RULES = LOG_FOLDER_NAME_ADHOC_COMPONENTS
			+ "/" + "aggregation_rules";
	public static final String LOG_FOLDER_NAME_ADHOC_COMPONENTS_COMMANDS = LOG_FOLDER_NAME_ADHOC_COMPONENTS
			+ "/" + "commands";
	public static final String LOG_FOLDER_NAME_ADHOC_COMPONENTS_NODE_GROUPS = LOG_FOLDER_NAME_ADHOC_COMPONENTS
			+ "/" + "node_groups";

	public static final String LOG_FILE_NAME_EXT1 = ".jsonlog";
	public static final String LOG_FILE_NAME_EXT2 = ".jsonlog.txt";

	// for smart upgrade

	public static final int AGENT_VERSION_INVALID = -1;


	public static final String AGENT_CMD_CONF_VARIABLES_PREDEFINED = "VARIABLES_PREDEFINED";
	public static final String AGENT_CMD_CONF_VARIABLES_PREDEFINED_LIST_START = "```VARIABLES_PREDEFINED_LIST_START";
	public static final String AGENT_CMD_CONF_VARIABLES_PREDEFINED_LIST_END = "```VARIABLES_PREDEFINED_LIST_END";


	// BECAREFUL. this is a special case that require the prefix.
	// where the general search and replace cannot happen because this is on
	// target node field. Because we cannot put into the original fqdn of the
	// same VAR string.
	public static final String VAR_NAME_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR_WHEN_CHECK = "REPLACE-VAR_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR";
	public static final String VAR_NAME_APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR_WHEN_INSERT = "APIVARREPLACE_SUPERMANSPECIAL_TARGET_NODE_VAR";
	public static final String VAR_NAME_SUPERMAN_UNIFORM_TARGET_MIDFIX = "_SUPERMANUNIFORMTARGET_";

	public static final String VAR_NAME_APIVARREPLACE_CMS_OID_VAR = "APIVARREPLACE_CMS_OID_VAR";
	public static final String VAR_NAME_APIVARREPLACE_CMS_AGENT_HEALTH_VAR = "APIVARREPLACE_CMS_AGENT_HEALTH_VAR";

	// END HWPATH ADHOC API BASED REPLACEMENT 20130828

	// START CMS RELATED CALLS 20130916
	public static final String AGENT_CMD_KEY_CMS_UPDATE_AGENT_HEALTH_QA = "CMS_UPDATE_AGENT_HEALTH_QA";
	public static final String AGENT_CMD_KEY_CMS_UPDATE_AGENT_HEALTH_PROD = "CMS_UPDATE_AGENT_HEALTH_PROD";
	public static final String AGENT_CMD_KEY_CMS_UPDATE_AGENT_HEALTH_PREPROD = "CMS_UPDATE_AGENT_HEALTH_PREPROD";
	// END CMS RELATED CALLS 20130916

	public static final String AGENT_CMD_KEY_UPDATE_METADATA_TAG_APP_COLO_RACK_TAG_APPLICATION_NAME_HADOOP = "titan";

	public static boolean AGENT_TAG_HADOOP_INCLUDE_APPLICATION = false;

	public static final String NODE_RESPONSE_INIT = "UNKNOWN_RESONSE_NOT_RECEIVED_YET";

	// Aggregation Regular Expression Patterns
	public static final String AGGREGATION_DEFAULT_METRIC = "DEFAULT_METRIC";
	public static final String AGGREGATION_PATTERN_EXTRACT_EXCEPTION_SUMMARY_FROM_ERROR_MSG = "PATTERN_EXTRACT_EXCEPTION_SUMMARY_FROM_ERROR_MSG";

	public static final String NODE_REQUEST_FULL_CONTENT_TYPE = "AM_FULL_CONTENT";

	// whether or not will execute this command
	public static final String NODE_REQUEST_WILL_EXECUTE = "NODE_REQUEST_WILL_EXECUTE";

	public static final String NODE_REQUEST_EXECUTE_MSG = "NODE_REQUEST_EXECUTE_MSG";

	public static final String NODE_REQUEST_EXECUTE_MSG_DETAIL_TYPE_AGENT_UPGRADE_INVALID = "!!! SAFEGUARD: REQUEST WILL NOT EXECUTE: because  agent version or OS type are with not supported values. (e.g. windows os) or (agent version 0.1.SNAPSHOT).";

	public static final String NODE_REQUEST_EXECUTE_MSG_DETAIL_TYPE_AGENT_UPGRADE_SKIP_VERSION_IDENTICAL = ":-) REST TIME! Target agent upgrade version is IDENTICAL to current agent version. Reduce SREPO load and now SKIP this agent upgrade.";

	public static final String NODE_REQUEST_EXECUTE_MSG_DETAIL_TYPE_WISB_NOT_EXIST = "!!! SAFEGUARD: NOTE: REQUEST WILL NOT EXECUTE: because  WISB value cannot find for this node group. Replacement cannot be execute. Please check the wisb files and add the value for this node group.";

	public static final String NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_VALUE_NA = "!!! SAFEGUARD: NOTE: REQUEST WILL NOT EXECUTE: because NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_VALUE_NA.";

	public static final String NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_KEY_OR_VALUE_NULL = "!!! SAFEGUARD: NOTE: REQUEST WILL NOT EXECUTE: because NODE_REQUEST_EXECUTE_MSG_DETAIL_REPLACEMENT_VAR_KEY_OR_VALUE_NULL.";

	public static final String NODE_REQUEST_PREFIX_REPLACE_VAR = "REPLACE-VAR_";

	// pool level WISB related
	public static final String WISB_VAR_NAME_PREFIX = "$WISB_VERSION_";
	public static final String WISB_COMMAND_POSTFIX = "_BYWISB";
	// 2013 0828: API BASED ADHOC VAR REPLACEMENT
	public static final String APIVARREPLACE_PREFIX = "APIVARREPLACE_";


	// this is the request content after the replacement of agent version.
	public static final String NODE_REQUEST_TRUE_CONTENT1 = "TRUE_CONTENT";
	public static final String NODE_REQUEST_TRUE_URL1 = "TRUE_URL";
	public static final String NODE_REQUEST_TRUE_PORT1 = "TRUE_PORT";
	public static final String NODE_REQUEST_HTTP_METHOD1 = "HTTP_METHOD";
	public static final String NODE_REQUEST_HTTP_HEADER_TYPE = "HEADER_TYPE";
	public static final String NODE_REQUEST_PREPARE_TIME1 = "PREPARE_TIME";
	public static final String NODE_REQUEST_TRUE_TARGET_NODE1 = "TRUE_TARGET_NODE";

	

	public static final String NULL_URL_VAR = "$NULL_URL";

	public static final String NODEGROUP_CONF_DATA_SOURCE_TYPE_ADHOC_NODE_LIST = "ADHOC";

	// this is deprecated.

	public static final String NODEGROUP_CONF_TAG_ADHOC_NODE_LIST_START = "```ADHOC_NODE_LIST_START";
	public static final String NODEGROUP_CONF_TAG_ADHOC_NODE_LIST_END = "```ADHOC_NODE_LIST_END";

	//20131213
	public static final String HTTPHEADER_CONF_TAG_HTTP_HEADER_LIST_START1 = "```HTTP_HEADER_LIST_START";
	public static final String HTTPHEADER_CONF_TAG_HTTP_HEADER_LIST_END1 = "```HTTP_HEADER_LIST_END";

	
	public static final String NODEGROUP_CONF_TAG_CLUSTER_LIST_START = "```CLUSTER_LIST_START";
	public static final String NODEGROUP_CONF_TAG_CLUSTER_LIST_END = "```CLUSTER_LIST_END";


	public static NingClientFactory ningClientFactory = new NingClientFactory();

	// AGENT COMMAND
	public static final String AGENT_COMMAND_VAR_DEFAULT_REQUEST_CONTENT = "$AM_FULL_CONTENT";
	public static final String STR_EMPTY = "";

	public static final long PAUSE_TIME_LONG_MILLIS = 2000L;
	public static final long PAUSE_TIME_BEFORE_ACTIVATE_MANIFEST_LONG_MILLIS = 60 * 1000L;

	public static final long PAUSE_TIME_BEFORE_ACTIVATE_MANIFEST_LONG_MILLIS_EACHNODE_INCREMENT = 10L;

	// 20130925 for delete logs
	public static String SUPERMAN_PIN = "ThinkAgain";

	public static void printSysErrWithTimeAndOptionalReason(
			String errorLocation, String errorDetailsInput) {

		String errorDetails = (errorDetailsInput == null || errorDetailsInput
				.isEmpty()) ? " " : " with details:  " + errorDetailsInput;

		models.utils.LogUtils.printLogError("ERROR IN " + errorLocation + errorDetails + " at "
				+ DateUtils.getNowDateTimeStrSdsm());
	}

	public static void tryCatchBlock() {
		
		String funcName = "tryCatchBlock" + "()";
		try {
			;
		} catch (Throwable t) {
			t.printStackTrace();
			VarUtils.printSysErrWithTimeAndOptionalReason(
					funcName, t.getLocalizedMessage());
			
		}
	}
	
	public static void objNullPrintError(Object obj, String errorLocation, String objName ){
		String errorDetails = (objName == null || objName
				.isEmpty()) ? " " : " with OBJ Name:  " + objName;
			
			models.utils.LogUtils.printLogError("OBJ is NULL IN " + errorLocation + errorDetails + " at "
					+ DateUtils.getNowDateTimeStrSdsm());
	}// end func

}
