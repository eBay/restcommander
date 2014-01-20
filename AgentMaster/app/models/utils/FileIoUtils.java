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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.plugins.repository.vfs.VfsRepository;

import models.utils.VarUtils.CONFIG_FILE_TYPE;

import play.Play;
import play.vfs.VirtualFile;

/**
 * 20130509 Auto generate TSDB URL.
 * 
 * 
 * @author ypei
 * 
 */
public class FileIoUtils {

	public static void getAppLogFileNamesInFolder() {
		String appLogsFolderPath = VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH;
		getFileNamesInFolder(appLogsFolderPath);
	}

	public static void main(String[] args) {
		getAppLogFileNamesInFolder();
		// testReadFileToString();
	}

	public static void createFolder(String folderPath) {

		File theDir = new File(folderPath);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			models.utils.LogUtils.printLogNormal("creating directory: " + folderPath);
			boolean result = theDir.mkdir();
			if (result) {
				models.utils.LogUtils.printLogNormal("directory just created now " + folderPath);
			}
		} else {
			models.utils.LogUtils.printLogNormal("directory " + folderPath + " already exist");
		}

	}

	public static void createFolderTestWin() {

		// win: S:\GitSources\AgentMaster\AgentMaster\AgentMaster
		// linux AgentMaster\AgentMaster
		// String applicationPath = Play.applicationPath.getAbsolutePath();
		// models.utils.LogUtils.printLogNormal(applicationPath);

		String directoryPath = "S:\\GitSources\\AgentMaster\\AgentMaster\\AgentMaster\\app_logs\\20130627";
		File theDir = new File(directoryPath);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			models.utils.LogUtils.printLogNormal("creating directory: " + directoryPath);
			boolean result = theDir.mkdir();
			if (result) {
				models.utils.LogUtils.printLogNormal("DIR created");
			}

		}

	}

	/**
	 * TODO this logic better to be changed. when it is NONE standard; need to
	 * have UNIFORM PART. e.g. * NoneStandard*
	 *
	 * EXTENSION UNFRIENDLY
	 * 
	 * Potential bug: when add a new none standard; the link in the webpage may
	 * not work as this function is working and need to explicitly add the new
	 * type
	 * 
	 * @param logFileName
	 * @return
	 */
	public static String getLogFilePathPrefix(String logFileName) {

		String pathPrefix = "";

		if (logFileName == null) {
			models.utils.LogUtils.printLogError("logFileName is NULL for logFileName  "
					+ logFileName);
		}

		else if (logFileName.contains(VarUtils.ADHOC_NODEGROUP_PREFIX)) {
			pathPrefix = VarUtils.LOG_FOLDER_NAME_ADHOC_WITH_SLASH;
		} else if (logFileName
				.contains(VarUtils.LOG_FOLDER_NAME_NONESTARDARD)
		) {
			pathPrefix = VarUtils.LOG_FOLDER_NAME_NONESTARDARD_WITH_SLASH;
		} else {
			pathPrefix = VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH;
		}

		return pathPrefix;

	}

	public static String testReadFileToString() {

		String filePath = VarUtils.LOG_FOLDER_NAME_APP_WITH_SLASH
				+ "20130510111550089-0700-ADHOC_NODEGROUP_20130510111546633-0700-GET_VI.jsonlog.txt";
		return readFileToString(filePath);
	}

	/**
	 * 20130927 Fixed Memory Leak. Dont use line by line, just use apache
	 * commons io!! so simple and easy!
	 * 
	 * @param filePath
	 * @return
	 */
	public static String readFileToString(String filePath) {

		String fileContentString = null;

		try {

			VirtualFile vf = VirtualFile.fromRelativePath(filePath);
			File realFile = vf.getRealFile();
			fileContentString = FileUtils.readFileToString(realFile);

			models.utils.LogUtils.printLogNormal("Completed read file with file size: "
					+ fileContentString.toString().length()
					/ VarUtils.CONVERSION_1024 + " KB. Path: " + filePath
					+ " at " + DateUtils.getNowDateTimeStr());
		} catch (java.io.FileNotFoundException e) {
			models.utils.LogUtils.printLogError("File Not Found exception."
					+ e.getLocalizedMessage());

			fileContentString = "File Not Found exception. This file may have been removed. "
					+ filePath;
		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in readConfigFile."
					+ e.getLocalizedMessage());
			e.printStackTrace();
			fileContentString = "File Not Found exception. This file may have been removed. "
					+ filePath;
		}
		return fileContentString.toString();

	} // end func.

	/**
	 * This will display all files except for empty.txt refined 20130918
	 * 
	 * @param folderName
	 * @return
	 */
	public static List<String> getFileNamesInFolder(String folderName) {

		List<String> fileNameList = new ArrayList<String>();

		try {

			VirtualFile virtualDir = VirtualFile.fromRelativePath(folderName);
			List<VirtualFile> virtualFileList = virtualDir.list();

			if (virtualFileList == null) {
				 models.utils.LogUtils.printLogError
						 ("virtualFileList is NULL! in getFileNamesInFolder()"
								+ DateUtils.getNowDateTimeStrSdsm());
			}

			models.utils.LogUtils.printLogNormal("Under folder: " + folderName
					+ ",  File/dir count is " + virtualFileList.size());

			for (int i = 0; i < virtualFileList.size(); i++) {

				if (virtualFileList.get(i).getRealFile().isFile()) {
					String fileName = virtualFileList.get(i).getName();

					if ((!fileName
							.equalsIgnoreCase(VarUtils.FILE_NAME_APP_LOG_EMPTY))) {

						if (VarUtils.IN_DETAIL_DEBUG) {
							models.utils.LogUtils.printLogNormal("File " + fileName);
						}
						fileNameList.add(fileName);
					}
				} else if (virtualFileList.get(i).getRealFile().isDirectory()) {
					models.utils.LogUtils.printLogNormal("Directory "
							+ virtualFileList.get(i).getName());
				}
			}// end for

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return fileNameList;
	}// end func.

	/**
	 * Output both This will display all files except for empty.txt refined
	 * 20130918
	 * 
	 * @param folderName
	 * @return
	 */
	public static void getFileAndDirNamesInFolder(String folderName,
			List<String> fileNames, List<String> dirNames) {

		if (fileNames == null) {
			fileNames = new ArrayList<String>();
		}

		if (dirNames == null) {
			dirNames = new ArrayList<String>();
		}

		try {

			VirtualFile virtualDir = VirtualFile.fromRelativePath(folderName);
			List<VirtualFile> virtualFileList = virtualDir.list();

			if (virtualFileList == null) {
				 models.utils.LogUtils.printLogError
						 ("virtualFileList is NULL! in getFileNamesInFolder()"
								+ DateUtils.getNowDateTimeStrSdsm());
			}

			models.utils.LogUtils.printLogNormal("Under folder: " + folderName
					+ ",  File/dir count is " + virtualFileList.size());

			for (int i = 0; i < virtualFileList.size(); i++) {

				String fileOrDirName = virtualFileList.get(i).getName();
				if (virtualFileList.get(i).getRealFile().isFile()) {

					if ((!fileOrDirName
							.equalsIgnoreCase(VarUtils.FILE_NAME_APP_LOG_EMPTY))) {

						if (VarUtils.IN_DETAIL_DEBUG) {
							models.utils.LogUtils.printLogNormal("File " + fileOrDirName);
						}
						fileNames.add(fileOrDirName);
					}
				} else if (virtualFileList.get(i).getRealFile().isDirectory()) {
					models.utils.LogUtils.printLogNormal("Directory " + fileOrDirName);
					dirNames.add(fileOrDirName);

				}
			}// end for

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}// end func.

	/**
	 * This will delete all files and folder under the path. Very careful
	 * 20130918
	 * 
	 * SAFE GUARD: only with adhoc logs
	 * 
	 * @param folderName
	 * @return
	 */
	public static boolean deleteAllFileAndDirInFolder(String folderName) {

		boolean success = true;

		// safeguard:
		if (!(folderName.contains("adhoc") || folderName.contains("logs"))) {
			 models.utils.LogUtils.printLogError
					 ("Looks like this folder is not logs folder in deleteAllFileAndDirInFolder(). Safeguard activated. "
							+ "NO OPS on this case. Return. ForderName:"
							+ folderName);
			return success;
		}

		try {

			VirtualFile virtualDir = VirtualFile.fromRelativePath(folderName);
			List<VirtualFile> virtualFileList = virtualDir.list();

			if (virtualFileList == null) {
				 models.utils.LogUtils.printLogError
						 ("virtualFileList is NULL! in getFileNamesInFolder()"
								+ DateUtils.getNowDateTimeStrSdsm());
			}

			models.utils.LogUtils.printLogNormal("Delete: Under folder: " + folderName
					+ ",  File/dir count is " + virtualFileList.size());

			for (int i = 0; i < virtualFileList.size(); i++) {

				if (virtualFileList.get(i).getRealFile().isFile()) {
					String fileName = virtualFileList.get(i).getName();

					if ((!fileName
							.equalsIgnoreCase(VarUtils.FILE_NAME_APP_LOG_EMPTY))) {

						if (VarUtils.IN_DETAIL_DEBUG) {
							models.utils.LogUtils.printLogNormal("File " + fileName);
						}

						FileUtils.forceDelete(virtualFileList.get(i)
								.getRealFile());
					}
				} else if (virtualFileList.get(i).getRealFile().isDirectory()) {
					models.utils.LogUtils.printLogNormal("Directory "
							+ virtualFileList.get(i).getName());

					FileUtils.deleteDirectory(virtualFileList.get(i)
							.getRealFile());
				}
			}// end for

		} catch (Throwable t) {
			t.printStackTrace();
			success = false;
		}

		return success;
	}// end func.

}// end class

