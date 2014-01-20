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
 * 
 * @author ypei
 *
 */
public class LogFile implements Comparable<LogFile> {

	private String timeStamp;
	private String nodeGroupType;

	private String agentCommandType;

	@Override
	public int compareTo(LogFile o) {

		String timeStampOther = ((LogFile) o).timeStamp;

		// ascending order
		if (this != null && this.timeStamp != null && timeStampOther != null) {
			return this.timeStamp.compareTo(timeStampOther);

		} else {
			return 0;
		}
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

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public void setNodeGroupType(String nodeGroupType) {
		this.nodeGroupType = nodeGroupType;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public void setAgentCommandType(String agentCommandType) {
		this.agentCommandType = agentCommandType;
	}

	public LogFile(String timeStamp, String nodeGroupType,
			String agentCommandType) {
		super();
		this.timeStamp = timeStamp;
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
	}

	// 20130510105239921-0700.ADHOC_NODE_LIST_2_BK.GET_VI.jsonlog.txt
	// 20130510111550089-0700.ADHOC_NODEGROUP_20130510111546633-0700.GET_VI.jsonlog.txt

	public String generateFileName() {
		return timeStamp + "~" + nodeGroupType + "~" + agentCommandType
				+ ".jsonlog.txt";

	}

	// TODO get the folder from the log file name itself
	public String genLogFilePath() {

		String logFileName = generateFileName();
		String logFilePath = FileIoUtils.getLogFilePathPrefix(logFileName)
				+ "/" + logFileName;
		return logFilePath;

	}

	public LogFile(String fileName) {

		// remove line break
		fileName = fileName.replace("\n", "").replace("\r", "");

		// 20130521 fixed: for name like this:
		// 20130521142209907-0700~ADHOC_NODEGROUP_20130521142206242-0700~AGENT_SMART_UPGRADE.jsonlog
		// needs to hvae an -: [A-Za-z0-9_-]*? for the 2nd ();
		String patternStr = "([0-9]*-[0-9]*)~([A-Za-z0-9_-]*?)~([A-Za-z0-9_]*?)\\.jsonlog\\.txt";
		Pattern patternMetric = Pattern.compile(patternStr, Pattern.MULTILINE);
		if (VarUtils.IN_DETAIL_DEBUG) {

			models.utils.LogUtils.printLogNormal("fileName: " + fileName);
			models.utils.LogUtils.printLogNormal("patternStr: " + patternStr);
		}

		final Matcher matcher = patternMetric.matcher(fileName);
		if (matcher.matches()) {
			this.timeStamp = matcher.group(1);
			this.nodeGroupType = matcher.group(2);
			this.agentCommandType = matcher.group(3);
		} else {
			 models.utils.LogUtils.printLogError
					 ("Error in making log file object form file name. REG EXP match fails. with fileName: "
							+ fileName);
		}

	}

	public static LogFile stringMatcherByPatternAppLog(String fileName) {

		// remove line break
		fileName = fileName.replace("\n", "").replace("\r", "");

		String patternStr = "([0-9]*-[0-9]*)~([A-Za-z0-9]*?)~([A-Za-z0-9]*?)\\.jsonlog\\.txt";
		Pattern patternMetric = Pattern.compile(patternStr, Pattern.MULTILINE);
		if (VarUtils.IN_DETAIL_DEBUG) {

			models.utils.LogUtils.printLogNormal("fileName: " + fileName);
			models.utils.LogUtils.printLogNormal("patternStr: " + patternStr);
		}

		String timeStamp = null;
		String nodeGroupType = null;
		String agentCommandType = null;

		final Matcher matcher = patternMetric.matcher(fileName);
		if (matcher.matches()) {
			timeStamp = matcher.group(1);
			nodeGroupType = matcher.group(2);
			agentCommandType = matcher.group(3);
		}

		LogFile logFile = new LogFile(timeStamp, nodeGroupType,
				agentCommandType);

		return logFile;

	}

	@Override
	public String toString() {
		return "LogFile [timeStamp=" + timeStamp + ", nodeGroupType="
				+ nodeGroupType + ", agentCommandType=" + agentCommandType
				+ "]";
	}

}
