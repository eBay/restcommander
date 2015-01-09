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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.Play;
import play.vfs.VirtualFile;

import models.asynchttp.response.GenericAgentResponse;
import models.data.AgentCommandMetadata;
import models.data.HttpHeaderMetadata;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeGroupSourceType;
import models.data.NodeReqResponse;
import models.data.NodeReqResponse.ResponseContent;
import models.utils.AgentUtils;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.MyHttpUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.CONFIG_FILE_TYPE;

/**
 * Singleton: all config is here :-)
 * 
 * @author ypei
 * 
 */
public class AgentConfigProvider {

	public static final AgentDataProvider adp = AgentDataProvider.getInstance();

	private static final AgentConfigProvider instance = new AgentConfigProvider();

	public static AgentConfigProvider getInstance() {
		return instance;
	}

	private AgentConfigProvider() {

	}


	public static void main(String[] args) {
		test();

	}

	public static void test() {
	}

	/**
	 * In the end: will always call
	 * updateAllAgentDataFromNodeGroupSourceMetadatas() to initialize
	 * NodeGroupDataMap object if needed in allAgentData
	 */
	public String readConfigFile(CONFIG_FILE_TYPE configFile) {

		if (configFile == null) {
			return "ERROR reading config: configFile is empty.";
		}

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.nodegroup.conf.file.location");

		StringBuilder sb = new StringBuilder();

		// in test
		String nodeGroupConfFileLocation = "conf/"
				+ configFile.toString().toLowerCase(Locale.ENGLISH) + ".conf";
		try {

			VirtualFile vf = VirtualFile
					.fromRelativePath(nodeGroupConfFileLocation);
			File realFile = vf.getRealFile();

			FileReader fr = new FileReader(realFile);
			BufferedReader reader = new BufferedReader(fr);
			String line = null;

			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}

			models.utils.LogUtils.printLogNormal("Completed readConfigFile with size: "
					+ sb.toString().length() / 1024.0 + " KB at "
					+ DateUtils.getNowDateTimeStr());

		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in readConfigFile."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

		return sb.toString();

	} // end func.

	public void saveConfigFile(CONFIG_FILE_TYPE configFile,
			String configFileContent) {

		if (configFile == null) {
			models.utils.LogUtils.printLogError("ERROR reading config: configFile is empty.");
		}

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.nodegroup.conf.file.location");

		// in test
		String nodeGroupConfFileLocation = "conf/"
				+ configFile.toString().toLowerCase(Locale.ENGLISH) + ".conf";
		try {

			VirtualFile vf = VirtualFile
					.fromRelativePath(nodeGroupConfFileLocation);
			File realFile = vf.getRealFile();

			boolean append = false;
			FileWriter fw = new FileWriter(realFile, append);
			fw.write(configFileContent);

			fw.close();
			models.utils.LogUtils.printLogNormal("Completed saveConfigFile with size: "
					+ configFileContent.length() + " at "
					+ DateUtils.getNowDateTimeStr());

		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in saveConfigFile."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

	} // end func.

	/**
	 * In the end: will always call
	 * updateAllAgentDataFromNodeGroupSourceMetadatas() to initialize
	 * NodeGroupDataMap object if needed in allAgentData
	 */
	public synchronized void updateNodeGroupSourceMetadatasFromConf() {

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.nodegroup.conf.file.location");

		// in test
		try {

			VirtualFile vf = VirtualFile
					.fromRelativePath(ConfUtils.nodeGroupConfFileLocation);
			File realFile = vf.getRealFile();

			FileReader fr = new FileReader(realFile);
			BufferedReader reader = new BufferedReader(fr);
			String line = null;

			String nodeGroupType = null;
			String dataSourceType = null;
			String statehubUrlPrefix = null;
			String envStr = null;

			adp.nodeGroupSourceMetadatas.clear();
			while ((line = reader.readLine()) != null) {

				// trim the comments or empty lines
				if (line.trim().length() == 0
						|| (line.trim().length() >= 1 && line.trim()
								.startsWith("%"))
						|| line.trim().startsWith("```")) {
					continue;
				}

				String[] wordsInLine = null;
				wordsInLine = line.split(" ");
				if (wordsInLine.length >= 4) {
					dataSourceType = wordsInLine[0];

						if (dataSourceType
							.equalsIgnoreCase(NodeGroupSourceType.ADHOC
									.toString())) {
						nodeGroupType = wordsInLine[1];
						statehubUrlPrefix = wordsInLine[2];
						envStr = wordsInLine[3];

						NodeGroupSourceMetadata ngsm = new NodeGroupSourceMetadata(
								dataSourceType, nodeGroupType,
								statehubUrlPrefix, envStr);

						List<String> nodeList = new ArrayList<String>();

						boolean startTagParsed = false;
						boolean endTagParsed = false;
						while ((line = reader.readLine()) != null) {
							if (line.equalsIgnoreCase(VarUtils.NODEGROUP_CONF_TAG_ADHOC_NODE_LIST_START)) {
								startTagParsed = true;
							} else if (line
									.equalsIgnoreCase(VarUtils.NODEGROUP_CONF_TAG_ADHOC_NODE_LIST_END)) {
								endTagParsed = true;
								break;
							} else if (startTagParsed == true
									&& endTagParsed == false) {
								// fixed bug: when fqdn has a space in the end.
								// Assuming FQDN dont have a space in the middle
								nodeList.add(line.trim());
							}
						}

						// now should completes
						if (startTagParsed == false || endTagParsed == false) {
							models.utils.LogUtils.printLogError("ERROR in parsing");
						}

						// filtering duplicated nodes:
						int removedDuplicatedNodeCount = AgentUtils
								.removeDuplicateNodeList(nodeList);

						models.utils.LogUtils.printLogNormal(" Removed duplicated node #: "
								+ removedDuplicatedNodeCount);
						models.utils.LogUtils.printLogNormal(" Total node count after removing duplicate : "
										+ nodeList.size());

						ngsm.addNodesToNodeList(nodeList);

						if (nodeGroupType != null) {

							adp.nodeGroupSourceMetadatas.put(nodeGroupType,
									ngsm);
						} else {
							models.utils.LogUtils.printLogError("Have an NULL nodeGroupType when initialize  in updateNodeGroupSourceMetadatasFromConf()"
											+ DateUtils.getNowDateTimeStrSdsm());
						}
					}// end else


				}
			}


			models.utils.LogUtils.printLogNormal
					 ("Completed updateNodeGroupSourceMetadatasFromConf with node group count: "
							+ adp.nodeGroupSourceMetadatas.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());

			reader.close();
			fr.close();
		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in updateNodeGroupSourcesFromConf."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

		initAllAgentDataFromNodeGroupSourceMetadatas(
				AgentDataProvider.allAgentData,
				AgentDataProvider.nodeGroupSourceMetadatas);

	} // end func.


	public synchronized void updateAgentCommandMetadatasFromConf() {

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.agentcommand.conf.file.location");

		// in test
		try {

			VirtualFile vf = VirtualFile
					.fromRelativePath(ConfUtils.agentCommandConfFileLocation);
			File realFile = vf.getRealFile();

			FileReader fr = new FileReader(realFile);
			BufferedReader reader = new BufferedReader(fr);
			String line = null;
			String requestContentLine = null;
			String requestContentTemplate = null;
			String requestContentTemplateAfterReplacementByVarMap = null;

			String requestProtocol = null; // e.g. https for agent
			String requestPort = null; // e.g. 12020 for agent

			String agentCommandType = null;
			String httpMethod = null;
			String requestUrlPostfix = null;
			String requestUrlPostfixAfterReplacementByVarMap = null;

			String httpHeaderType = VarUtils.HTTP_HEADER_TYPE_GLOBAL1;

			long pauseIntervalWorkerMillis = VarUtils.PAUSE_INTERVAL_WORKER_MILLIS_DEFAULT;

			int maxConcurrency = -1;
			int responseExtractIndexStart = -1;
			int responseExtractIndexEnd = -1;

			Map<String, String> varMap = new HashMap<String, String>();
			String varName = null;
			String varContent = null;

			adp.agentCommandMetadatas.clear();
			while ((line = reader.readLine()) != null) {

				// trim the comments or empty lines
				if (line.trim().length() == 0
						|| (line.trim().length() >= 1 && line.trim()
								.startsWith("%"))
						|| line.trim().startsWith("```")) {
					continue;
				}

				// first try to get the metrics
				if (line.trim().equalsIgnoreCase(
						VarUtils.AGENT_CMD_CONF_VARIABLES_PREDEFINED)) {

					boolean startTagParsed = false;
					boolean endTagParsed = false;
					while ((line = reader.readLine()) != null) {
						if (line.equalsIgnoreCase(VarUtils.AGENT_CMD_CONF_VARIABLES_PREDEFINED_LIST_START)) {
							startTagParsed = true;
						} else if (line
								.equalsIgnoreCase(VarUtils.AGENT_CMD_CONF_VARIABLES_PREDEFINED_LIST_END)) {
							endTagParsed = true;
							break;
						} else if (line.trim().length() == 0) {
							continue;

						} else if (startTagParsed == true
								&& endTagParsed == false) {

							Pattern patternMetric = Pattern.compile(
									"(.*)=(.*)", Pattern.MULTILINE);

							final Matcher matcher = patternMetric.matcher(line);
							if (matcher.matches()) {
								varName = matcher.group(1);
								varContent = matcher.group(2);
							}

							if (varName != null) {
								varMap.put(varName, varContent);
							}
						}
					}

					// now should completes
					if (startTagParsed == false || endTagParsed == false) {
						models.utils.LogUtils.printLogError("ERROR in parsing");
					}

					// 20131216 now line is
					// AGENT_CMD_CONF_VARIABLES_PREDEFINED_LIST_END; so need to
					// go until the next meaning line for the request all parts.
					while ((line = reader.readLine()).length() == 0
							
							|| (line.trim().length() >= 1 && line.trim()
							.startsWith("%"))
							
							) {
						;
					}
				}

				// assuming the next line is the request content: parse it to
				// get the post content
				requestContentLine = reader.readLine();
				requestContentTemplate = requestContentLine.replace("```", "");

				if (requestContentTemplate == null) {
					models.utils.LogUtils.printLogError("ERROR requestContentTemplate is NULL "
							+ requestContentLine);
				}

				// now do replacement
				requestContentTemplateAfterReplacementByVarMap = AgentUtils
						.replaceByVarMap(varMap, requestContentTemplate);

				String[] wordsInLine = null;
				wordsInLine = line.split(" ");

				if (wordsInLine.length >= VarUtils.MIN_COMMAND_FIELD_LENGTH) {
					agentCommandType = wordsInLine[0];

					httpMethod = wordsInLine[1];
					requestProtocol = wordsInLine[2];
					requestPort = wordsInLine[3];
					requestUrlPostfix = wordsInLine[4];

					requestUrlPostfixAfterReplacementByVarMap = requestUrlPostfix;

					requestUrlPostfixAfterReplacementByVarMap = AgentUtils
							.replaceByVarMap(varMap, requestUrlPostfix);

					maxConcurrency = Integer.parseInt(wordsInLine[5]);
					responseExtractIndexStart = Integer
							.parseInt(wordsInLine[6]);
					responseExtractIndexEnd = Integer.parseInt(wordsInLine[7]);

					/**
					 * 20131211: add http header type; httpHeaderType =
					 * VarUtils.HTTP_HEADER_TYPE_GLOBAL;
					 */
					if (wordsInLine.length >= VarUtils.MIN_COMMAND_FIELD_LENGTH + 1
							&& wordsInLine[VarUtils.MIN_COMMAND_FIELD_LENGTH] != null) {
						httpHeaderType = wordsInLine[VarUtils.MIN_COMMAND_FIELD_LENGTH]
								.trim();
					} else {
						httpHeaderType = VarUtils.HTTP_HEADER_TYPE_GLOBAL1;
					}

					/**
					 * 20131013; deprecate this pauseIntervalWorkerMillis. if
					 * needed can resume setting this value from the config
					 */
					pauseIntervalWorkerMillis = 0L;

					AgentCommandMetadata acm = new AgentCommandMetadata(
							agentCommandType, httpMethod,
							requestUrlPostfixAfterReplacementByVarMap,
							requestContentTemplateAfterReplacementByVarMap,
							requestProtocol, requestPort, maxConcurrency,
							pauseIntervalWorkerMillis,
							responseExtractIndexStart, responseExtractIndexEnd,
							httpHeaderType);
					adp.agentCommandMetadatas.put(agentCommandType, acm);
				}

			}

			models.utils.LogUtils.printLogNormal
					 ("Completed updateAgentCommandMetadatasFromConf with size: "
							+ adp.nodeGroupSourceMetadatas.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());
			reader.close();
			fr.close();
		} catch (Throwable e) {
			 models.utils.LogUtils.printLogError
					 ("Error in setting Agent Command Config. updateAgentCommandMetadatasFromConf."
							+ e.getLocalizedMessage());
			e.printStackTrace();
		}


	} // end func.

	public synchronized void updateAggregationMetadatasFromConf() {

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.aggregation.conf.file.location");

		// in test
		try {

			VirtualFile vf = VirtualFile
					.fromRelativePath(ConfUtils.aggregationConfFileLocation);
			File realFile = vf.getRealFile();

			FileReader fr = new FileReader(realFile);
			BufferedReader reader = new BufferedReader(fr);
			String line = null;
			String requestContentLine = null;
			String requestContentTemplate = null;

			String aggregationType = null;

			adp.aggregationMetadatas.clear();
			while ((line = reader.readLine()) != null) {

				// trim the comments or empty lines
				if (line.trim().length() == 0
						|| (line.trim().length() >= 1 && line.trim()
								.startsWith("%"))
						|| line.trim().startsWith("```")) {
					continue;
				}

				aggregationType = line.trim();

				// assuming the next line is the request content: parse it to
				// get the post content
				// fix: trim ending / beging white spaces
				requestContentLine = reader.readLine().trim();
				requestContentTemplate = requestContentLine.replace("```", "");

				adp.aggregationMetadatas.put(aggregationType,
						requestContentTemplate);
			}

			models.utils.LogUtils.printLogNormal
					 ("Completed updateAggregationMetadatasFromConf with size: "
							+ adp.aggregationMetadatas.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());
			reader.close();
			fr.close();
		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in updateAggregationMetadatasFromConf."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

	} // end func.


	/**
	 * For common http headers
	 * 
	 * TODO
	 */
	public synchronized void updateCommonHttpHeaderFromConf() {

		// in test
		try {

			VirtualFile vf = VirtualFile
					.fromRelativePath(ConfUtils.httpHeaderConfFileLocation);
			File realFile = vf.getRealFile();

			FileReader fr = new FileReader(realFile);
			BufferedReader reader = new BufferedReader(fr);
			String httpHeaderType = null;
			String line = null;
			String httpHeaderKey = null;
			String httpHeaderValue = null;
			String httpHeaderValueLine = null;
			// HttpHeaderMetadata
			adp.headerMetadataMap.clear();

			while ((line = reader.readLine()) != null) {

				// trim the comments or empty lines
				if (line.trim().length() == 0
						|| (line.trim().length() >= 1 && line.trim()
								.startsWith("%"))
						|| line.trim().startsWith("```")) {
					continue;
				}

				Map<String, String> headerMap = new HashMap<String, String>();

				httpHeaderType = line.trim();

				boolean startTagParsed = false;
				boolean endTagParsed = false;
				while ((line = reader.readLine()) != null) {
					if (line.equalsIgnoreCase(VarUtils.HTTPHEADER_CONF_TAG_HTTP_HEADER_LIST_START1)) {
						startTagParsed = true;
					} else if (line
							.equalsIgnoreCase(VarUtils.HTTPHEADER_CONF_TAG_HTTP_HEADER_LIST_END1)) {
						endTagParsed = true;
						break;
					} else if (startTagParsed == true && endTagParsed == false) {
						// fixed bug: when fqdn has a space in the end.
						// Assuming FQDN dont have a space in the middle
						String lineTrimmed = line.trim();
						if (lineTrimmed != null && !lineTrimmed.isEmpty()) {

							httpHeaderKey = lineTrimmed;
							httpHeaderValueLine = reader.readLine().trim();
							httpHeaderValue = httpHeaderValueLine.replace(
									"```", "");
							headerMap.put(httpHeaderKey, httpHeaderValue);
						} else {
							continue; // read next line
						}

					}// end else if
				}// end while

				HttpHeaderMetadata httpHeaderMetadata = new HttpHeaderMetadata(
						httpHeaderType, headerMap);
				adp.headerMetadataMap.put(httpHeaderType, httpHeaderMetadata);
			}

			models.utils.LogUtils.printLogNormal
					 ("Completed updateCommonHttpHeaderFromConf headerMap size: "
							+ adp.headerMetadataMap.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());
			reader.close();
			fr.close();
		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in updateCommonHttpHeaderFromConf.."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

	} // end func.

	/**
	 * initialize the NodeGroupDataMap inside of AllAgentData
	 */
	public void initAllAgentDataFromNodeGroupSourceMetadatas(
			Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore) {

		try {

			// make sure allAgentData has at least nodeGroup as
			// NodeGroupSourceMetadata
			for (Entry<String, NodeGroupSourceMetadata> entry : nodeGroupStore
					.entrySet()) {

				NodeGroupSourceMetadata ngsm = entry.getValue();
				if (!dataStore.containsKey(ngsm.getNodeGroupType())
						|| dataStore.get(ngsm.getNodeGroupType()) == null) {
					NodeGroupDataMap ngdm = new NodeGroupDataMap(
							ngsm.getNodeGroupType());

					dataStore.put(ngsm.getNodeGroupType(), ngdm);
				}

			}

			// make sure allAgentData has no more nodeGroup as
			// NodeGroupSourceMetadata
			// remove the pair in allAgentData where nodeGroup does not have.
			for (Entry<String, NodeGroupDataMap> entry : dataStore.entrySet()) {
				String nodeGroupType = entry.getKey();
				if (!nodeGroupStore.containsKey(nodeGroupType)) {
					dataStore.remove(nodeGroupType);
				}

			}

			models.utils.LogUtils.printLogNormal
					 ("Completed updateAllAgentDataFromNodeGroupSourceMetadatas: allAgentData with size: "
							+ adp.allAgentData.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());

		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in updateNodeListFromNodeGroupSource."
					+ e.getLocalizedMessage());
		}

	}// end func
	
	
	/**
	 * 201501 optimize for adhoc case initialize the NodeGroupDataMap inside of
	 * AllAgentData
	 */
	public void addAdhocAgentDataFromNodeGroupSourceMetadatas(
			Map<String, NodeGroupDataMap> dataStore,
			Map<String, NodeGroupSourceMetadata> nodeGroupStore,
			String nodeGroupType

	) {

		try {
			NodeGroupSourceMetadata ngsm = nodeGroupStore.get(nodeGroupType);

			if (ngsm == null) {
				models.utils.LogUtils
						.printLogError("Error in addAdhocAgentDataFromNodeGroupSourceMetadatas. ngsm is null for node group "
								+ nodeGroupType);
				return;
			}

			if (!dataStore.containsKey(ngsm.getNodeGroupType())
					|| dataStore.get(ngsm.getNodeGroupType()) == null) {
				NodeGroupDataMap ngdm = new NodeGroupDataMap(
						ngsm.getNodeGroupType());

				dataStore.put(ngsm.getNodeGroupType(), ngdm);
			}

			models.utils.LogUtils
					.printLogNormal("Completed addAdhocAgentDataFromNodeGroupSourceMetadatas: dataStore with size: "
							+ dataStore.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());

		} catch (Throwable e) {
			e.printStackTrace();
			models.utils.LogUtils
					.printLogError("Error in addAdhocAgentDataFromNodeGroupSourceMetadatas."
							+ e.getLocalizedMessage());
		}

	}// end func

}
