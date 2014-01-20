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

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import play.Play;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import models.data.LogFile;
import models.data.LogFileGeneric;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.FileIoUtils;
import models.utils.NaturalDeserializer;
import models.utils.VarUtils;
import models.utils.VarUtils.ADHOC_COMPONENT_TYPE;
/**
 * 
 * @author ypei
 *
 */
public class LogProvider {

	private static final LogProvider instance = new LogProvider();

	public static LogProvider getInstance() {
		return instance;
	}

	private LogProvider() {

	}

	public static void saveAgentDataInLog(String nodeGroupType,
			String agentCommandType, Map<String, NodeGroupDataMap> dataStore) {
		try {

			models.utils.LogUtils.printLogNormal("Start to write log files for nodeGroupType: "
					+ nodeGroupType + " agentCommandType: " + agentCommandType
					+ " at " + DateUtils.getNowDateTimeStrSdsm());
			if (nodeGroupType == null || agentCommandType == null) {
				 models.utils.LogUtils.printLogError
						 ("Error nodeGroupType is null or agentCommandType is null in saveAgentDataInLog. Will not to save logs.. "
								+ DateUtils.getNowDateTimeStrSdsm());
			} else {
				String logFilePath = genLogFilePath(nodeGroupType,
						agentCommandType, dataStore);

				writeObjectAsJsonFile(
						dataStore.get(nodeGroupType)
								.getNodeGroupDataMapValidForSingleCommand(
										agentCommandType), logFilePath);

			}

		} catch (Throwable t) {

			t.printStackTrace();
			 models.utils.LogUtils.printLogError
					 (("Error occured in saveAgentDataInLog: " + AgentUtils
							.getStackTraceStr(t)));
		}

	}

	/**
	 * 20130923 generic save object as json file
	 * 
	 * @param filePath
	 * @param object
	 */
	public static void saveJsonDataInLog(String filePath, Object object) {
		try {

			if (filePath == null) {
				 models.utils.LogUtils.printLogError
						 ("Error filePath is null insaveJsonDataInLog ");
			} else {

				writeObjectAsJsonFile(object, filePath);
			}

		} catch (Throwable t) {

			t.printStackTrace();
			 models.utils.LogUtils.printLogError
					 (("Error occured in saveJsonDataInLog: " + AgentUtils
							.getStackTraceStr(t)));
		}

	}


	public static String readJsonLog(String nodeGroupType,
			String agentCommandType, String timeStamp) {

		String logContent = null;
		try {

			if (nodeGroupType == null || agentCommandType == null
					|| timeStamp == null) {
				 models.utils.LogUtils.printLogError
						 ("Error nodeGroupType is null or agentCommandType or timeStamp is null ");
			} else {
				LogFile logFile = new LogFile(timeStamp, nodeGroupType,
						agentCommandType);

				String filePath = logFile.genLogFilePath();
				logContent = FileIoUtils.readFileToString(filePath);

			}

		} catch (Throwable t) {

			t.printStackTrace();
			models.utils.LogUtils.printLogError(("Error occured in readJsonLog: " + AgentUtils
					.getStackTraceStr(t)));
		}
		return logContent;
	}

	public static void main(String[] args) {

		String nodeGroupType = "qe";
		String agentCommandType = "GET_VI";
		String timeStamp = "20130524121051783-0700";
		readJsonLogToNodeDataMap(nodeGroupType, agentCommandType, timeStamp);

	}

	/**
	 * Read JSON log to the hashmap. From file back to the memory.
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param timeStamp
	 * @return
	 */
	public static Map<String, NodeData> readJsonLogToNodeDataMap(
			String nodeGroupType, String agentCommandType, String timeStamp) {

		String logContent = readJsonLog(nodeGroupType, agentCommandType,
				timeStamp);
		HashMap<String, NodeData> nodeDataMapValid = null;
		if (logContent == null) {
			 models.utils.LogUtils.printLogError
					 ("Error logContent is null in readJsonLogToNodeDataMap ");

			return nodeDataMapValid;
		}

		try {

			// Great solution: very challenging part: 20130523
			// http://stackoverflow.com/questions/2779251/convert-json-to-hashmap-using-gson-in-java
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(Object.class,
					new NaturalDeserializer());
			Gson gson = gsonBuilder.create();
			Object mapObject = gson.fromJson(logContent, Object.class);
			nodeDataMapValid = (HashMap<String, NodeData>) (mapObject);

			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils.printLogNormal(nodeDataMapValid.size()+"");
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}

		return nodeDataMapValid;
	}

	public static List<LogFile> getLogFilesInFolder(String folderPath) {

		List<String> fileNames = FileIoUtils.getFileNamesInFolder(folderPath);

		List<LogFile> logFiles = new ArrayList<LogFile>();

		for (String fileName : fileNames) {
			LogFile logFile = new LogFile(fileName);
			logFiles.add(logFile);

			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils.printLogNormal(logFile.toString());
			}
		}
		Collections.sort(logFiles, Collections.reverseOrder());

		return logFiles;

	}

	public static List<LogFileGeneric> getLogFileGenericsInFolder(
			String folderPath) {

		List<String> fileNames = FileIoUtils.getFileNamesInFolder(folderPath);

		List<LogFileGeneric> logFileGenerics = new ArrayList<LogFileGeneric>();

		for (String fileName : fileNames) {
			LogFileGeneric logFileGeneric = new LogFileGeneric(fileName);
			logFileGenerics.add(logFileGeneric);

			if (VarUtils.IN_DETAIL_DEBUG) {
				models.utils.LogUtils.printLogNormal(logFileGeneric.toString());
			}
		}
		Collections.sort(logFileGenerics, Collections.reverseOrder());

		return logFileGenerics;

	}

	
	public static String generateAppLogAchiveFolderDate(String folderDate,
			String logFolder) {

		// win: S:\GitSources\AgentMaster\AgentMaster\AgentMaster
		// linux AgentMaster\AgentMaster
		String applicationPath = Play.applicationPath.getAbsolutePath();

		// folderDate = "20130627m1";
		String directoryPath = applicationPath + "/" + logFolder + folderDate;
		return directoryPath;
	}

	/**
	 * 20131007; when find /read from the file; get absolute path will not
	 * work!! Must use relative path.
	 * 
	 * @param folderDate
	 * @param logFolder
	 * @return
	 */
	public static String generateAppLogRelativeFolderDate(String folderDate,
			String logFolder) {

		// folderDate = "20130627m1";
		String directoryPath = "/" + logFolder + folderDate;
		return directoryPath;
	}

	// VarUtils.PATH_FOLDER_APP_LOG
	public static String generateAppLogAbsolutePath(String logFolder) {

		String applicationPath = Play.applicationPath.getAbsolutePath();

		String directoryPath = applicationPath + "/" + logFolder;
		return directoryPath;
	}

	/**
	 * 20130924: add for all none standard folder and folder of adhoc components
	 * 
	 * @param dateForArchive
	 * @param logFolder
	 */
	public static void moveFilesForAppLogs(String dateForArchive,
			String logFolder) {

		try {
			String appLogsFolderPath = logFolder;
			List<String> fileNamesForAppLogs = FileIoUtils
					.getFileNamesInFolder(appLogsFolderPath);

			String destDirPath = generateAppLogAchiveFolderDate(dateForArchive,
					logFolder);

			// now create folder
			FileIoUtils.createFolder(destDirPath);

			File destDir = new File(destDirPath);

			for (String appLogFileName : fileNamesForAppLogs) {

				// only move for that date.
				if (!appLogFileName.startsWith(dateForArchive)
				// 201309 for none standard logs and for ADHOCDATASTORE
				// components
						&& !(appLogFileName.startsWith(VarUtils.ADHOCDATASTORE) && appLogFileName
								.contains(dateForArchive))

				) {
					continue;
				}

				String appLogAbsolutePath = generateAppLogAbsolutePath(logFolder);
				String appLogFileFullPath = appLogAbsolutePath + appLogFileName;

				File srcFile = new File(appLogFileFullPath);
				FileUtils.moveFileToDirectory(srcFile, destDir, true);

			}// end for loop

		} catch (Throwable e) {
			e.printStackTrace();
			models.utils.LogUtils.printLogError("Error in moveFilesForAppLogs "
					+ DateUtils.getNowDateTimeStrSdsm());
		}

	}// end

	/**
	 * 20131009: add delete archived date folders
	 * 
	 * @param dateForArchive
	 * @param logFolder
	 */
	public static void deleteFilesForOneLogFolderOnDate(String dateForArchive,
			String logFolder) {

		try {

			String destDirPath = generateAppLogRelativeFolderDate(
					dateForArchive, logFolder);

			FileIoUtils.deleteAllFileAndDirInFolder(destDirPath);

		} catch (Throwable e) {
			e.printStackTrace();
			models.utils.LogUtils.printLogError("Error in deleteFilesForLogsOnDate "
					+ DateUtils.getNowDateTimeStrSdsm());
		}

	}// end

	public static String genLogFileName(String nodeGroupType,
			String agentCommandType) {

		String logFileName = DateUtils.getNowDateTimeStrConcise() + "~"
				+ nodeGroupType + "~" + agentCommandType
				+ VarUtils.LOG_FILE_NAME_EXT2;
		return logFileName;
	}

	public static Map<String, NodeGroupDataMap> getDataStore(String logFileName) {

		if (logFileName.contains(VarUtils.ADHOC_NODEGROUP_PREFIX)) {
			return AgentDataProvider.adhocAgentData;
		} else {
			return AgentDataProvider.allAgentData;
		}

	}

	/**
	 * 20130918: enable more choice
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @return
	 */
	public static String genLogFilePath(String nodeGroupType,
			String agentCommandType, Map<String, NodeGroupDataMap> dataStore) {

		String logFileName = genLogFileName(nodeGroupType, agentCommandType);
		String logFilePath = getLogFolderName(dataStore) + "/" + logFileName;
		return logFilePath;

	}

	// ADHOC_CMS_WISB_WIRI_MAP_PREFIX

	public static String genLogFilePathNoneStandard(String genericObjectFileName) {

		String logFilePath = VarUtils.LOG_FOLDER_NAME_NONESTARDARD + "/"
				+ genericObjectFileName;
		return logFilePath;
	}

	public static String genFilePathAndSaveLogAdhocComponents(
			ADHOC_COMPONENT_TYPE adhocComponentType,
			String adhocComponentFileName, Object object) {

		String filePath = genLogFilePathAdhocComponents(adhocComponentType,
				adhocComponentFileName);

		if (filePath != null) {

			saveJsonDataInLog(filePath, object);
		} else {
			 models.utils.LogUtils.printLogError
					 ("FILE PATH IS NULL in genFilePathAndSaveLogAdhocComponents"
							+ DateUtils.getNowDateTimeStrSdsm());
		}

		return filePath;
	}

	public static String genFilePathAndReadLogAdhocComponents(
			ADHOC_COMPONENT_TYPE adhocComponentType,
			String adhocComponentFileName) {
		String logContent = "INIT_NULL_CONTENT";
		String filePath = genLogFilePathAdhocComponents(adhocComponentType,
				adhocComponentFileName);

		if (filePath != null) {

			logContent = FileIoUtils.readFileToString(filePath);
		} else {
			 models.utils.LogUtils.printLogError
					 ("FILE PATH IS NULL in genFilePathAndSaveLogAdhocComponents"
							+ DateUtils.getNowDateTimeStrSdsm());
		}

		return logContent;
	}

	public static String genLogFilePathAdhocComponents(
			ADHOC_COMPONENT_TYPE adhocComponentType,
			String adhocComponentFileName) {

		String logFilePath = null;
		if (adhocComponentType == ADHOC_COMPONENT_TYPE.NODE_GROUP) {
			logFilePath = VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_NODE_GROUPS
					+ "/" + adhocComponentFileName
					+ VarUtils.LOG_FILE_NAME_EXT2;
		}

		else if (adhocComponentType == ADHOC_COMPONENT_TYPE.AGGREGATION_RULE) {
			logFilePath = VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_AGGREGATION_RULES
					+ "/"
					+ adhocComponentFileName
					+ VarUtils.LOG_FILE_NAME_EXT2;
		}

		else if (adhocComponentType == ADHOC_COMPONENT_TYPE.COMMAND) {
			logFilePath = VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_COMMANDS
					+ "/" + adhocComponentFileName
					+ VarUtils.LOG_FILE_NAME_EXT2;
		}

		return logFilePath;
	}

	

	public static String getLogFolderName(
			Map<String, NodeGroupDataMap> dataStore) {

		String logFolderName = VarUtils.LOG_FOLDER_NAME_NONESTARDARD; // none
																		// standard

		if (dataStore == AgentDataProvider.adhocAgentData) {
			logFolderName = VarUtils.LOG_FOLDER_NAME_ADHOC;
		} else if (dataStore == AgentDataProvider.allAgentData) {
			logFolderName = VarUtils.LOG_FOLDER_NAME_APP;
		}
		return logFolderName;
	}

	private static void writeObjectAsJsonFile(Object obj, String logFilePath) {

		String logFileContentString = AgentUtils.renderJson(obj);

		AgentUtils.saveStringToFile(logFilePath, logFileContentString);

	}

	public static boolean archiveAppLogsDailyJob() {
		boolean success = true;

		try {

			List<String> noneAdhocLogArchiveDates = new ArrayList<String>();
			// keep 3 days
			// redundency here: in case some time the cron did not run.

			noneAdhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.ARCHIVE_LOG_DATE_BASE3 ));
			noneAdhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.ARCHIVE_LOG_DATE_BASE3 + 1));
			noneAdhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.ARCHIVE_LOG_DATE_BASE3 + 2));

			List<String> adhocLogArchiveDates = new ArrayList<String>();
			adhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.ARCHIVE_LOG_DATE_BASE3));
			adhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.ARCHIVE_LOG_DATE_BASE3 + 1));
			adhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.ARCHIVE_LOG_DATE_BASE3 + 2));

			for (String archiveDate : noneAdhocLogArchiveDates) {
				LogProvider.moveFilesForAppLogs(archiveDate,
						VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH);

			}

			for (String archiveDateAdhoc : adhocLogArchiveDates) {
				// 2013094: add other log folders
				LogProvider.moveFilesForAppLogs(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_ADHOC_WITH_SLASH);
				LogProvider.moveFilesForAppLogs(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_NONESTARDARD_WITH_SLASH);

				LogProvider
						.moveFilesForAppLogs(
								archiveDateAdhoc,
								VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_AGGREGATION_RULES
										+ "/");
				LogProvider.moveFilesForAppLogs(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_COMMANDS
								+ "/");
				LogProvider.moveFilesForAppLogs(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_NODE_GROUPS
								+ "/");

			}

			models.utils.LogUtils.printLogNormal("Success archive app archiveAppLogsDailyJob");
		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogNormal("Error occured in archiveAppLogsDailyJob ");

			success = false;
		}
		return success;

	}// end func

	/**
	 * 20131009 add cron to delete logs
	 * 
	 * @return
	 */
	public static boolean deleteAppLogsDailyJob() {
		boolean success = true;

		try {

			List<String> noneAdhocLogArchiveDates = new ArrayList<String>();
			// keep 7 days
			// redundency here: in case some time the cron did not run.

			noneAdhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.DELETE_LOG_DATE_BASE4 ));
			noneAdhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.DELETE_LOG_DATE_BASE4 + 1));
			noneAdhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.DELETE_LOG_DATE_BASE4 + 2));

			List<String> adhocLogArchiveDates = new ArrayList<String>();
			adhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.DELETE_LOG_DATE_BASE4 ));
			adhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.DELETE_LOG_DATE_BASE4 + 1));
			adhocLogArchiveDates.add(DateUtils
					.getNDayBeforeTodayStr(VarUtils.DELETE_LOG_DATE_BASE4 + 2));

			for (String archiveDate : noneAdhocLogArchiveDates) {
				LogProvider.deleteFilesForOneLogFolderOnDate(archiveDate,
						VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH);

			}

			for (String archiveDateAdhoc : adhocLogArchiveDates) {
				// 2013094: add other log folders
				LogProvider.deleteFilesForOneLogFolderOnDate(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_ADHOC_WITH_SLASH);
				LogProvider.deleteFilesForOneLogFolderOnDate(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_NONESTARDARD_WITH_SLASH);

				LogProvider
						.deleteFilesForOneLogFolderOnDate(
								archiveDateAdhoc,
								VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_AGGREGATION_RULES
										+ "/");
				LogProvider.deleteFilesForOneLogFolderOnDate(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_COMMANDS
								+ "/");
				LogProvider.deleteFilesForOneLogFolderOnDate(archiveDateAdhoc,
						VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_NODE_GROUPS
								+ "/");

			}

			models.utils.LogUtils.printLogNormal("Success archive app archiveAppLogsDailyJob");
		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogNormal("Error occured in archiveAppLogsDailyJob ");

			success = false;
		}
		return success;

	}// end func

	public static boolean archiveAppLogsOnDate(String archiveDate) {
		boolean success = true;

		try {
			LogProvider.moveFilesForAppLogs(archiveDate,
					VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH);
			// 2013094: add other log folders
			LogProvider.moveFilesForAppLogs(archiveDate,
					VarUtils.LOG_FOLDER_NAME_ADHOC_WITH_SLASH);
			LogProvider.moveFilesForAppLogs(archiveDate,
					VarUtils.LOG_FOLDER_NAME_NONESTARDARD_WITH_SLASH);

			LogProvider.moveFilesForAppLogs(archiveDate,
					VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_AGGREGATION_RULES
							+ "/");
			LogProvider.moveFilesForAppLogs(archiveDate,
					VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_COMMANDS + "/");
			LogProvider
					.moveFilesForAppLogs(
							archiveDate,
							VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_NODE_GROUPS
									+ "/");

			models.utils.LogUtils.printLogNormal("Success archive app logs into folder "
					+ archiveDate);
		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogNormal("Error occured in archive app logs ");

			success = false;
		}
		return success;

	}// end func

	public static boolean deleteAllAppLogsOnDate(String archiveDate) {
		boolean success = true;

		try {
			LogProvider.deleteFilesForOneLogFolderOnDate(archiveDate,
					VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH);
			// 2013094: add other log folders
			LogProvider.deleteFilesForOneLogFolderOnDate(archiveDate,
					VarUtils.LOG_FOLDER_NAME_ADHOC_WITH_SLASH);
			LogProvider.deleteFilesForOneLogFolderOnDate(archiveDate,
					VarUtils.LOG_FOLDER_NAME_NONESTARDARD_WITH_SLASH);

			LogProvider.deleteFilesForOneLogFolderOnDate(archiveDate,
					VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_AGGREGATION_RULES
							+ "/");
			LogProvider.deleteFilesForOneLogFolderOnDate(archiveDate,
					VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_COMMANDS + "/");
			LogProvider
					.deleteFilesForOneLogFolderOnDate(
							archiveDate,
							VarUtils.LOG_FOLDER_NAME_ADHOC_COMPONENTS_NODE_GROUPS
									+ "/");

			models.utils.LogUtils.printLogNormal
					 ("Success deleteAllAppLogsOnDate app logs with date "
							+ archiveDate);
		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogNormal
					 ("Error occured in deleteAllAppLogsOnDate; ErrMsg: "
							+ t.getLocalizedMessage());

			success = false;
		}
		return success;

	}// end func

}
