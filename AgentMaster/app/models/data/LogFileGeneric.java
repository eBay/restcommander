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
package models.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.utils.DateUtils;
import models.utils.FileIoUtils;
import models.utils.VarUtils;

/**
 * 20130923 for generic, log; not assuming to have ~ ~ strcture; but it does
 * have
 * 
 * ADHOCDATASTORE-0700
 * ADHOCDATASTORE_CMS_WISB_WIRI_MAP_20130923105323716-0700.jsonlog.txt
 * 
 * 
 * 20130923: NOTE all adhoc components viewer in UI makes the ASSUMPTION:
 * ADHOCDATASTORE_JSONOBJNAME_TIMESTAMP
 * 
 * This hard 3 parts is easier for regular expression matching to match out
 * stuff.
 * 
 * POTENTIAL BUG NOT displaying properly:
 * 
 * @author ypei
 * 
 */
public class LogFileGeneric implements Comparable<LogFileGeneric> {

	private String timeStamp;
	private String jsonObjectType;

	public String getJsonObjectType() {
		return jsonObjectType;
	}

	public void setJsonObjectType(String jsonObjectType) {
		this.jsonObjectType = jsonObjectType;
	}

	@Override
	public int compareTo(LogFileGeneric o) {

		String timeStampOther = ((LogFileGeneric) o).timeStamp;

		// ascending order
		return this.timeStamp.compareTo(timeStampOther);

	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public String getTimeStampFormatSdsm() {

		Date date = DateUtils.getDateFromConciseStr(timeStamp);
		return DateUtils.getDateTimeStrSdsm(date);
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public LogFileGeneric(String timeStamp, String jsonObjectType) {
		super();
		this.timeStamp = timeStamp;
		this.jsonObjectType = jsonObjectType;
	}

	// 20130510105239921-0700.ADHOC_NODE_LIST_2_BK.GET_VI.jsonlog.txt
	// 20130510111550089-0700.ADHOC_NODEGROUP_20130510111546633-0700.GET_VI.jsonlog.txt

	public String generateFileName() {
		return VarUtils.ADHOCDATASTORE + "_" + jsonObjectType + "_" + timeStamp
				+ ".jsonlog.txt";
	}

	// TODO get the folder from the log file name itself
	public String genLogFilePath() {

		String logFileName = generateFileName();
		String logFilePath = FileIoUtils.getLogFilePathPrefix(logFileName)
				+ "/" + logFileName;
		return logFilePath;

	}

	public LogFileGeneric(String fileName) {

		// remove line break
		fileName = fileName.replace("\n", "").replace("\r", "");

		// 20130521 fixed: for name like this:
		// 20130521142209907-0700~ADHOC_NODEGROUP_20130521142206242-0700~AGENT_SMART_UPGRADE.jsonlog
		// needs to hvae an -: [A-Za-z0-9_-]*? for the 2nd ();
		String patternStr = "ADHOCDATASTORE_([A-Za-z0-9_-]*?)_([0-9]*-[0-9]*)\\.jsonlog\\.txt";
		Pattern patternMetric = Pattern.compile(patternStr, Pattern.MULTILINE);
		if (VarUtils.IN_DETAIL_DEBUG) {

			models.utils.LogUtils.printLogNormal("fileName: " + fileName);
			models.utils.LogUtils.printLogNormal("patternStr: " + patternStr);
		}

		final Matcher matcher = patternMetric.matcher(fileName);
		if (matcher.matches()) {
			this.jsonObjectType = matcher.group(1);
			this.timeStamp = matcher.group(2);
		} else {
			 models.utils.LogUtils.printLogError
					 ("Error in making log file object form file name. REG EXP match fails. with fileName: "
							+ fileName);
		}

	}

	public static LogFileGeneric stringMatcherByPatternAppLog(String fileName) {

		// remove line break
		fileName = fileName.replace("\n", "").replace("\r", "");

		// 20130521 fixed: for name like this:
		// 20130521142209907-0700~ADHOC_NODEGROUP_20130521142206242-0700~AGENT_SMART_UPGRADE.jsonlog
		// needs to hvae an -: [A-Za-z0-9_-]*? for the 2nd ();
		String patternStr = "ADHOCDATASTORE_~([A-Za-z0-9_-]*?)_([0-9]*-[0-9]*)\\.jsonlog\\.txt";
		Pattern patternMetric = Pattern.compile(patternStr, Pattern.MULTILINE);
		if (VarUtils.IN_DETAIL_DEBUG) {

			models.utils.LogUtils.printLogNormal("fileName: " + fileName);
			models.utils.LogUtils.printLogNormal("patternStr: " + patternStr);
		}

		String jsonObjectType = null;
		String timeStamp = null;

		final Matcher matcher = patternMetric.matcher(fileName);
		if (matcher.matches()) {
			jsonObjectType = matcher.group(1);
			timeStamp = matcher.group(2);
		} else {
			 models.utils.LogUtils.printLogError
					 ("Error in making log file object form file name. REG EXP match fails. with fileName: "
							+ fileName);
		}

		LogFileGeneric logFileGeneric = new LogFileGeneric(jsonObjectType,
				timeStamp);

		return logFileGeneric;

	}

	@Override
	public String toString() {
		return "LogFileGeneric [timeStamp=" + timeStamp + ", jsonObjectType="
				+ jsonObjectType + "]";
	}

}
