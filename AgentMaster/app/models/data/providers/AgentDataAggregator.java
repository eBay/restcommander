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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.data.AggregateData;
import models.data.NodeData;
import models.data.RawDataSourceType;
import models.utils.DateUtils;
import models.utils.VarUtils;

/**
 * Singleton: to display sth from the agent command
 * 
 * @author ypei
 * 
 */
public class AgentDataAggregator {

	public static final AgentDataAggregator ada = AgentDataAggregator
			.getInstance();

	private static final AgentDataAggregator instance = new AgentDataAggregator();

	public static AgentDataAggregator getInstance() {
		return instance;
	}

	private AgentDataAggregator() {
	}

	// regex "\"fatalAgentCount\":([0-9]+),"
	// "mem-usage(KB)": "71400"}
	public String aggregateMetrics(String nodeGroupType, String agentCommandType) {

		List<List<Double>> dataArray2D = new ArrayList<List<Double>>();

		try {
			AgentDataProvider adp = AgentDataProvider.getInstance();
			Map<String, NodeData> nodeDataMapValid = adp.allAgentData.get(
					nodeGroupType).getNodeDataMapValid();

			int i = 0;
			for (Entry<String, NodeData> entry : nodeDataMapValid.entrySet()) {

				// String fqdn = entry.getKey();
				// Pattern patternMetric = Pattern.compile(
				// ".*\"mem-usage\\(KB\\)\":\\s\"([^\"]*)\".*",
				// Pattern.MULTILINE);

				String patternStr = adp.aggregationMetadatas
						.get(VarUtils.AGGREGATION_DEFAULT_METRIC);
				double metricValue = -1.0;
				String response = entry.getValue().getDataMap()
						.get(agentCommandType).getResponseContent()
						.getResponse();
				String output = AgentDataAggregator.stringMatcherByPattern(
						response, patternStr);

				if (VarUtils.IN_DETAIL_DEBUG) {

					models.utils.LogUtils.printLogNormal(" stringMatcherByPattern output: "
							+ output);
				}
				try {

					metricValue = Double.parseDouble(output);
				} catch (NumberFormatException nfe) {

					models.utils.LogUtils.printLogNormal
							 ("  NumberFormatException caught: this is not a double Number: "
									+ output);
					metricValue = output.hashCode();
				}

				List<Double> list = new ArrayList<Double>();
				list.add(new Double(i));
				list.add(metricValue);

				// models.utils.LogUtils.printLogNormal(getDataJSNumber(list));
				dataArray2D.add(list);

				// models.utils.LogUtils.printLogNormal(getDataJSNumber2D(dataArray2D));

				++i;
			}

		} catch (Throwable t) {

			t.printStackTrace();
		}

		boolean humanReadFriendly = false;
		return getDataJSNumber2D(dataArray2D, humanReadFriendly);

	}

	/**
	 * Becareful of rawDataSourceType ; this must be set . 
	 * 
	 * rawDataSourceType is LOG file; also 
	 * LOGIC convert from JSON to Java Object of the responses 
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param timeStamp
	 * @param rawDataSourceType
	 * @param patternStr 
	 * @return
	 */
	public AggregateData aggregateMetricsWithGroupingNew(String nodeGroupType,
			String agentCommandType,  String timeStamp, String rawDataSourceType, String patternStr) {

		AggregateData aggregateData = null;

		try {
			
			AgentDataProvider adp = AgentDataProvider.getInstance();
			Map<String, NodeData> nodeDataMapValid = null;
			
			// default rawDataSourceType is ALL_AGENT_DATA
			if(rawDataSourceType == null || rawDataSourceType.length() ==0){
				rawDataSourceType = RawDataSourceType.ALL_AGENT_DATA.toString();
			}
			
			if(rawDataSourceType.equalsIgnoreCase(RawDataSourceType.ALL_AGENT_DATA.toString())){
				
				nodeDataMapValid = adp.allAgentData.get(
						nodeGroupType).getNodeDataMapValid();
			}else 	if(rawDataSourceType.equalsIgnoreCase(RawDataSourceType.ADHOC_AGENT_DATA.toString())){
				
				nodeDataMapValid = adp.adhocAgentData.get(
						nodeGroupType).getNodeDataMapValid();
			}else if(rawDataSourceType.equalsIgnoreCase(RawDataSourceType.LOG_FILE.toString())){
				
				if(timeStamp==null){
					models.utils.LogUtils.printLogError("time stamp is NULL in aggregateMetricsWithGroupingNew.");
				}
				
				// from log
				nodeDataMapValid = LogProvider.readJsonLogToNodeDataMap(nodeGroupType, agentCommandType, timeStamp);
			}
			
			
			aggregateData = new AggregateData(nodeGroupType, agentCommandType, nodeDataMapValid);

			/**
			 * KEY STEP
			 */
			aggregateData.genResponseToMetadataMap(patternStr);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return aggregateData;
	}


	public HashMap<String, String> aggregateDataset(
			HashMap<String, String> inputMap) {
		HashMap<String, String> resultMap = new HashMap<String, String>();
		for (Entry<String, String> entry : inputMap.entrySet()) {
			String key = entry.getValue();
			resultMap.put(
					key,
					resultMap.get(key) == null ? "1" : String.valueOf(Integer
							.valueOf(resultMap.get(key)) + 1));
		}
		return resultMap;
	}

	public static String stringMatcherByPattern(String input, String patternStr) {

		String output = VarUtils.NA_NUM;

		//20140105: fix the NPE issue
		if(patternStr==null){
			
			if(VarUtils.IN_DETAIL_DEBUG){
				models.utils.LogUtils.printLogError("patternStr is NULL! (Expected when the aggregation rule is not defined at " + DateUtils.getNowDateTimeStrSdsm());
			}
			return output;
		}
		
		
		// remove line break
		
		if(input==null){
			
			if(VarUtils.IN_DETAIL_DEBUG){
				models.utils.LogUtils.printLogNormal("input (Expected when the response is null and now try to match on response) is NULL in stringMatcherByPattern() at " + DateUtils.getNowDateTimeStrSdsm());
			}
			return output;
		}else{
			input = input.replace("\n", "").replace("\r", "");
		}
		if (VarUtils.IN_DETAIL_DEBUG) {

			models.utils.LogUtils.printLogNormal("input: " + input);
			models.utils.LogUtils.printLogNormal("patternStr: " + patternStr);
		}

		Pattern patternMetric = Pattern.compile(patternStr, Pattern.MULTILINE);

		final Matcher matcher = patternMetric.matcher(input);
		if (matcher.matches()) {
			output = matcher.group(1);
		}
		return output;
	}

	public static <T> String getDataJSNumber2D(List<List<T>> dataList, 	boolean humanReadFriendly) {
		StringBuilder sb = new StringBuilder();

		int size = dataList.size();
		int i = 0;
		sb.append("[");
		if(humanReadFriendly){
			sb.append("\n");
		}
		for (List<T> dataList1D : dataList) {
			
			if(humanReadFriendly){
				sb.append("\t");
			}
			
			sb.append(getDataJSNumber(dataList1D));
			i++;
			if (i < size) {
				sb.append(",");
			}
			if(humanReadFriendly){
				sb.append("\n");
			}
		}
		sb.append("]");
		
		
		return sb.toString();
	}
	


	

	
	public static <T> String getDataJSNumber(List<T> dataList) {
		StringBuilder sb = new StringBuilder();

		int size = dataList.size();
		int i = 0;
		sb.append("[");
		for (T xData : dataList) {
			if (i % 2 == 0)
				sb.append("'"); // add quote to allow string type data
			sb.append(xData);
			if (i % 2 == 0)
				sb.append("'"); // add quote to allow string type data
			i++;
			if (i < size) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static void main(String[] args) {

	}

}
