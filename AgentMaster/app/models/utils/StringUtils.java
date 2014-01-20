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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import models.data.providers.AgentDataAggregator;

public class StringUtils {

	// jeff
	final static int maxErrorMessageSize = 250;

	/**
	 * Parses the string to an integer if it the string numeric, return the
	 * defaultValue otherwise.
	 * 
	 * @param str
	 * @param defaultValue
	 * @return
	 */
	public static int intValue(String str, int defaultValue) {
		if (!isNullOrEmpty(str)
				&& org.apache.commons.lang.StringUtils.isNumeric(str)) {
			return Integer.parseInt(str);
		}
		return defaultValue;
	}


	public static String getTrimmedStr(String str) {
		if (str.length() > maxErrorMessageSize) {
			return str.substring(0, maxErrorMessageSize);
		} else {
			return str;
		}
	}

	public static String getFirstFew(String str, int length) {
		if (isNullOrEmpty(str))
			return str;
		if (str.length() > length)
			return str.substring(0, length);

		return str;
	}

	public static String removeTrailingQuotes(String str) {
		if (str == null)
			return "";

		return str.substring(1, str.length() - 1);
	}

	public static String getRandomHostname() {
		String randomStr = UUID.randomUUID().toString();
		randomStr = randomStr.substring(0, 12);
		return randomStr;
	}


	public static String convertCollectionStringToCSV(Collection<String> coll,
			boolean valueInSingleQuotes) {

		StringBuffer sb = new StringBuffer();
		int count = 1;
		for (String value : coll) {
			if (valueInSingleQuotes) {
				sb.append("'");
			}
			sb.append(value);
			if (valueInSingleQuotes) {
				sb.append("'");
			}
			if (coll.size() > count) {
				sb.append(",");
			}
			count++;
		}
		return sb.toString();
	}

	public static String escapeSquareBrackets(String str) {

		String result = str.replaceAll("[", "\\[");
		result = result.replaceAll("]", "\\]");
		return result;
	}

	public static String replaceSquareBrackets(String str) {

		String result = str.replace('[', ' ');
		result = result.replace(']', ' ');
		result = result.replace(';', ' ');
		result = result.replace('|', ' ');
		return result;
	}

	public static String replaceBackSlashes(String str) {
		String result = str.replace("\\\"", "\"");
		return result;
	}

	public static boolean areEqual(String str1, String str2) {

		boolean result = false;
		if (str1 == null || str2 == null)
			return result;

		if (str1 != null && str2 != null && str1.equals(str2)) {
			result = true;
		}

		return result;
	}

	public static boolean isNullOrEmpty(String str) {

		if (str == null)
			return true;
		if (str.isEmpty())
			return true;

		return false;
	}

	public static String purgeNull(Object str) {
		if (str == null)
			return "";
		return str.toString();
	}

	public static String purgeNull(String str) {
		return purgeNull(str, "");
	}

	public static String purgeNull(String str, String defaultValue) {
		if (defaultValue == null) {
			throw new IllegalArgumentException("default value cannot be null");
		}
		if (str == null)
			return defaultValue;
		return str.toString();
	}

	/**
	 * Get the first n characters of a string
	 * 
	 * @param args
	 */

	public static String getFirstNChar(String str, int n) {
		str = purgeNull(str);
		if (n > str.length())
			return str;

		return str.substring(0, n);
	}

	/**
	 * Trims
	 * 2012-05-03|CreateComputeParentJob.121e20af1370a0275f377ef7fffffe1c|IAAS
	 * to 121e20af1370a0275f377ef7fffffe1c
	 * 
	 * @param jobId
	 * @return
	 */
	public static String getSimpleJobId(String jobId) {

		jobId = purgeNull(jobId);
		int index = jobId.indexOf(".");
		if (index > 0) {
			jobId = jobId.substring(index + 1);
		}

		index = jobId.indexOf("|");
		if (index > 0) {
			jobId = jobId.substring(0, index);
		}

		return jobId;

	}

	/**
	 * 
	 * @param jobId
	 * @return
	 */
	public static String getIaasJobUrl(String jobId) {
		StringBuffer sb = new StringBuffer("<a href='/provision/jobstatus/"
				+ jobId + "'>");
		sb.append(getSimpleJobId(jobId));
		sb.append("</a>");
		return sb.toString();
	}

	/**
	 * 
	 * @param list
	 * @return
	 */
	public static String getDisplayListInNewLine(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String item : list) {
			sb.append(item + "</br>");
		}

		return sb.toString();
	}

	public static String getPercentageHtml(String val) {

		StringBuilder sb = new StringBuilder(" <div class=\"progress\">");
		sb.append("<div class=\"bar\" style=\"width: " + val + "%;\">")
				.append(val).append("%").append("</div>").append("</div>");
		// Logger.debug("Returning percentage str : " + sb.toString());
		return sb.toString();
	}

	public static void main(String[] args) {

		// String str = "\"text\"";
		// models.utils.LogUtils.printLogNormal(removeTrailingQuotes(str));

		models.utils.LogUtils.printLogNormal("Escape Text : "
				+ replaceSquareBrackets("test   [Hello]"));
		String str = "\\\"hello";
		models.utils.LogUtils.printLogNormal("Replace back slashes : " + replaceBackSlashes(str));

	}

}
