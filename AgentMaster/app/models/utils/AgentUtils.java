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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import models.utils.VarUtils.CONFIG_FILE_TYPE;
import play.vfs.VirtualFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * @author ypei
 * 
 */
public class AgentUtils {


	public static int processMaxConcurrency(int maxConcurrencyInit) {

		return (maxConcurrencyInit <= 0) ? VarUtils.MAX_CONCURRENT_SEND_SIZE
				: maxConcurrencyInit;
	}

	
	public static String genChunkName(int startIndex, int endIndex, String prefix){
		String chunkName = prefix + "_" + startIndex + "_" + endIndex;
		return chunkName;
	}
	
	public static void main(String[] args) {


	}
	
	
	
	public static List<String> getNodeListFromString(String listStr,
			boolean removeDuplicate) {

		List<String> nodes = new ArrayList<String>();

		if (listStr == null || listStr.isEmpty()) {
			models.utils.LogUtils.printLogError("input listStr is NULL or empty"
					+ DateUtils.getNowDateTimeStrSdsm());
		}

		for (String token : listStr.split("[\\r?\\n]+")) {

			// 20131025: fix if fqdn has space in the end.
			if (token != null && !token.trim().isEmpty()) {
				nodes.add(token.trim());

			}
		}

		if (removeDuplicate) {
			removeDuplicateNodeList(nodes);
		}

		return nodes;

	}

	public static List<String> getNodeListFromStringLineSeperateOrSpaceSeperate(
			String listStr, boolean removeDuplicate) {

		List<String> nodes = new ArrayList<String>();

		for (String token : listStr.split("[\\r?\\n| +]+")) {

			// 20131025: fix if fqdn has space in the end.
			if (token != null && !token.trim().isEmpty()) {
				nodes.add(token.trim());

			}
		}

		models.utils.LogUtils.printLogNormal
				 ("got node size from getNodeListFromStringLineSeperateOrSpaceSeperate: "
						+ nodes.size());

		if (removeDuplicate) {
			removeDuplicateNodeList(nodes);
		}

		return nodes;

	}




	public static String generateReplaceVarKeyInRequestParametersHashMap(
			String wisbVar) {

		if (wisbVar == null) {
			 models.utils.LogUtils.printLogError
					 ("wisbVar IS NULL in generateReplaceVarKeyInRequestParametersHashMap "
							+ DateUtils.getNowDateTimeStrSdsm());
		}

		return VarUtils.NODE_REQUEST_PREFIX_REPLACE_VAR + wisbVar;

	}


	public static String concatNodeList(List<String> list) {

		StringBuilder result = new StringBuilder();
		result.append("Total List Count: " + list.size() + "\n\n");
		for (String item : list) {
			result.append(item).append("\n");
		}

		return result.toString();

	}



	public static String readStringFromUrlGeneric(String url)
			throws IOException {
		InputStream is = null;
		try {
			is = new URL(url).openStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String jsonText = readAll(rd);

			return jsonText;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}


	public static int removeDuplicateNodeList(List<String> list) {

		int originCount = list.size();
		// add elements to all, including duplicates
		HashSet<String> hs = new LinkedHashSet<String>();
		hs.addAll(list);
		list.clear();
		list.addAll(hs);

		return originCount - list.size();
	}

	public static boolean existElementInList(List<String> list, String target) {

		HashSet<String> hs = new LinkedHashSet<String>();
		hs.addAll(list);
		if (hs.contains(target)) {
			return true;
		}
		return false;

	}

	public static String printListStrings(List<String> list,
			String listDescription) {

		StringBuilder sb = new StringBuilder();
		sb.append(
				"\nLIST_START of " + listDescription + " count:" + list.size())
				.append("\n[\n");

		for (String s : list) {

			sb.append(s).append("\n");
		}

		sb.append("]\nLIST_END of " + listDescription + " count:" + list.size())
				.append("\n");

		return sb.toString();
	}

	public static List<String> removeEmptyStringFromList(List<String> list) {

		List<String> resultList = new ArrayList<String>();

		for (String s : list) {

			if (s != null && s.trim().length() > 0) {
				resultList.add(s);
			}
		}

		return resultList;
	}

	public static String printMapStrings(Map<String, List<String>> map,
			String mapDescription) {

		StringBuilder sb = new StringBuilder();

		if (map == null) {
			return "Map " + mapDescription + " is NULL";
		}

		sb.append(
				"\nMAP_SUMMARY of " + mapDescription + " with key count:"
						+ map.size()).append(" :\n[");
		for (String key : map.keySet()) {
			sb.append(key).append("\t");
		}
		sb.append("]\n");

		sb.append(
				"\nMAP_START of " + mapDescription + " with key count:"
						+ map.size()).append("\n");

		for (Entry<String, List<String>> entry : map.entrySet()) {

			String key = entry.getKey();
			List<String> list = entry.getValue();

			sb.append(
					"\nLIST_START of " + mapDescription + "[" + key
							+ "] count:" + list.size()).append("\n[\n");

			for (String s : list) {

				sb.append(s).append("\n");
			}

			sb.append(
					"]\nLIST_END: List of " + mapDescription + "[" + key
							+ "] count:" + list.size()).append("\n");

		}

		sb.append(
				"MAP_END of " + mapDescription + " with key count:"
						+ map.size()).append("\n");

		return sb.toString();
	}

	public static String replaceByVarMap(Map<String, String> varMap,
			String originalStr) {

		if (originalStr == null) {
			models.utils.LogUtils.printLogError("ERROR! originalStr is null");
			return null;
		}

		String newString = originalStr;
		String varName = null;
		String varContent = null;
		/**
		 * 20140103; fix the replace VAR in agentcommand.conf cannot handle more than 1 replacement
		 */
		for (Entry<String, String> entry : varMap.entrySet()) {
			varName = entry.getKey();
			varContent = entry.getValue();

			if (newString.contains(varName)) {
				newString = newString.replace(varName, varContent);
			}
			
		}

		return newString;
	}

	public static <E> String toJson(List<E> list) {

		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		String jsonArray = gson.toJson(list);

		models.utils.LogUtils.printLogNormal(jsonArray);

		return jsonArray;

	}

	public static String getStackTraceStr(Throwable t) {

		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		t.printStackTrace(printWriter);

		return "Time: " + DateUtils.getNowDateTimeStrSdsm() + " Stacktrace: "
				+ result.toString();

	}

	public static void saveStringToFile(String filePath, String fileContent) {

		if (filePath == null) {
			models.utils.LogUtils.printLogError("ERROR reading filePath: filePath is empty.");
		}

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.nodegroup.conf.file.location");

		// in test
		try {

			VirtualFile vf = VirtualFile.fromRelativePath(filePath);
			File realFile = vf.getRealFile();

			boolean append = false;
			FileWriter fw = new FileWriter(realFile, append);
			fw.write(fileContent);

			fw.close();
			models.utils.LogUtils.printLogNormal("Completed saveStringToFile with size: "
					+ fileContent.length() / VarUtils.CONVERSION_1024
					+ " KB Path: " + filePath + " at "
					+ DateUtils.getNowDateTimeStr());

		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in saveStringToFile."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

	} // end func.

	public static String renderJson(Object o) {

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOutput = gson.toJson(o);

		return jsonOutput;
	}

	/**
	 * 20131017
	 * 
	 * @param nodeList
	 * @return
	 */
	public static String cleanDisplayStringListLineByLineFromJavaStringList(
			List<String> nodeList) {
		StringBuilder sb = new StringBuilder();

		sb.append("nodeList size: " + nodeList.size() + "\n");

		for (String fqdn : nodeList) {
			sb.append(fqdn + "\n");
		}

		return sb.toString();
	}

}
