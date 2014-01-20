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
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeGroupSourceType;
import models.data.NodeReqResponse;
import models.data.NodeReqResponse.ResponseContent;
import models.utils.AgentUtils;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.CONFIG_FILE_TYPE;

/**
 * 20131026 for rest APIs ; adhoc adding command/reg expression
 * 
 * @author ypei
 * 
 */
public class AgentConfigProviderHelper {
	public static final AgentDataProvider adp = AgentDataProvider.getInstance();

	/**
	 * 20131026: for adhoc REST API; for stateless; unlike the load from .conf
	 * file; this way of adding does not persistant Potential bug: if memory got
	 * reload (clean up and only load from conf file; these type of command will
	 * be erase; every mid night there is reload confg
	 * 
	 * @param commandLine
	 * @param requestContentTemplate
	 */
	public static String addOrUpdateAgentCommandInMemoryFromString(
			String commandLine, String requestContentTemplate) {

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.agentcommand.conf.file.location");

		String agentCommandTypeNewlyAdded = null;
		
		// in test
		try {
			String requestContentLine = null;
			String requestContentTemplateAfterReplacementByVarMap = null;

			String requestProtocol = null; // e.g. https for agent
			String requestPort = null; // e.g. 12020 for agent

			String agentCommandType = null;
			String httpMethod = null;
			String requestUrlPostfix = null;
			String requestUrlPostfixAfterReplacementByVarMap = null;

			long pauseIntervalWorkerMillis = VarUtils.PAUSE_INTERVAL_WORKER_MILLIS_DEFAULT;

			int maxConcurrency = -1;
			int responseExtractIndexStart = -1;
			int responseExtractIndexEnd = -1;

			String httpHeaderType = VarUtils.HTTP_HEADER_TYPE_GLOBAL1;
			
			Map<String, String> varMap = new HashMap<String, String>();

			// assuming the next line is the request content: parse it to
			// get the post content

			if (requestContentTemplate == null) {
				models.utils.LogUtils.printLogError("ERROR requestContentTemplate is NULL "
						+ requestContentLine);
			}
			requestContentTemplate = requestContentTemplate.trim();

			// now do replacement
			requestContentTemplateAfterReplacementByVarMap = AgentUtils
					.replaceByVarMap(varMap, requestContentTemplate);

			String[] wordsInLine = null;
			wordsInLine = commandLine.split(" ");
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
				responseExtractIndexStart = Integer.parseInt(wordsInLine[6]);
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
				 * 20131013; deprecate this pauseIntervalWorkerMillis. if needed
				 * can resume setting this value from the config
				 */
				pauseIntervalWorkerMillis = 0L;
				

				AgentCommandMetadata acm = new AgentCommandMetadata(
						agentCommandType, httpMethod,
						requestUrlPostfixAfterReplacementByVarMap,
						requestContentTemplateAfterReplacementByVarMap,
						requestProtocol, requestPort, maxConcurrency,
						pauseIntervalWorkerMillis, responseExtractIndexStart,
						responseExtractIndexEnd
						, httpHeaderType
						);

				// add or replace
				adp.agentCommandMetadatas.put(agentCommandType, acm);
			}
			
			agentCommandTypeNewlyAdded = agentCommandType;

			models.utils.LogUtils.printLogNormal
					 ("Added command into memory; now  agentCommandMetadatas size: "
							+ adp.agentCommandMetadatas.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());
		} catch (Throwable e) {
			 models.utils.LogUtils.printLogError
					 ("Error in Added command into memory;. addOrUpdateAgentCommandInMemoryFromString."
							+ e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		return agentCommandTypeNewlyAdded;

	} // end func.

	
	/**
	 * 20131110
	 * From REST Calls the regular expression, due to too many *.?\ ; hard to form valid jsons.
	 * @param aggregationType
	 * @param aggregationExpression
	 */
	public static void addOrUpdateAggregationMetadataInMemoryFromString(
			String aggregationType, String aggregationExpression) {

		// String nodeGroupConfFileLocation =
		// Play.configuration.getProperty("agentmaster.aggregation.conf.file.location");

		// in test
		try {

			if(aggregationType==null){
				models.utils.LogUtils.printLogError("aggregationType is NULL? Not passed in?? now use default AGGREGATION_DEFAULT_METRIC");
				aggregationType = VarUtils.AGGREGATION_DEFAULT_METRIC;
			}

			aggregationType = aggregationType.trim();
			aggregationExpression = aggregationExpression.trim();
			adp.aggregationMetadatas.put(aggregationType,
					aggregationExpression);
			models.utils.LogUtils.printLogNormal
					 ("Completed addOrUpdateAggregationMetadataInMemoryFromString with size: "
							+ adp.aggregationMetadatas.size()
							+ " at "
							+ DateUtils.getNowDateTimeStr());
		} catch (Throwable e) {
			models.utils.LogUtils.printLogError("Error in addOrUpdateAggregationMetadataInMemoryFromString."
					+ e.getLocalizedMessage());
			e.printStackTrace();
		}

	} // end func.

}
