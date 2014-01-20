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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import models.data.providers.AgentDataAggregator;
import models.data.providers.AgentDataProvider;
import models.data.providers.actors.AggregationDirector;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class AggregateData {

	// the initial results are here
	private final HashMap<String, String> fqdnResponseExtractMap = new HashMap<String, String>();
	private final HashMap<String, String> fqdnErrorMsgExtractMap = new HashMap<String, String>();
	private final HashMap<String, Boolean> fqdnIsErrorExtractMap = new HashMap<String, Boolean>();

	private String nodeGroupType;
	private String agentCommandType;

	private final HashMap<String, AggregationValueMetadata> responseToMetadataMap = new HashMap<String, AggregationValueMetadata>();

	public HashMap<String, AggregationValueMetadata> getResponseToMetadataMap() {
		return responseToMetadataMap;
	}

	private Map<String, NodeData> nodeDataMapValid;

	public AggregateData(String nodeGroupType, String agentCommandType,
			Map<String, NodeData> nodeDataMapValid) {
		super();
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
		this.nodeDataMapValid = nodeDataMapValid;
	}

	public Map<String, NodeData> getNodeDataMapValid() {
		return nodeDataMapValid;
	}

	public void setNodeDataMapValid(Map<String, NodeData> nodeDataMapValid) {
		this.nodeDataMapValid = nodeDataMapValid;
	}

	/**
	 * 20131113 used for REST API JSON response; just copy the logic from
	 * getValueCountJSNumber2D of humanReadFriendly is true
	 * 
	 * @return
	 */
	public Map<String, String> getValueCountAggregationMap() {

		Map<String, String> aggregationMap = new TreeMap<String, String>();
		try {

			for (Entry<String, AggregationValueMetadata> entry : responseToMetadataMap
					.entrySet()) {
				String value = entry.getKey();
				String count = Integer.toString(entry.getValue().getNodeList()
						.size());
				aggregationMap.put(value, count);
			}

		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogError("Error in getValueCountAggregationMap "
					+ DateUtils.getNowDateTimeStrSdsm());
		}

		return aggregationMap;

	}

	public String getValueCountJSNumber2D(boolean humanReadFriendly) {

		String jsNumber2dResult = "";
		try {

			List<List<String>> dataArray2D = new ArrayList<List<String>>();

			Map<String, String> aggregationMap = new TreeMap<String, String>();
			for (Entry<String, AggregationValueMetadata> entry : responseToMetadataMap
					.entrySet()) {

				if (humanReadFriendly) {

					String value = entry.getKey();
					String count = Integer.toString(entry.getValue()
							.getNodeList().size());
					aggregationMap.put(value, count);

				} else {
					List<String> list = new ArrayList<String>();

					String value = entry.getKey();
					String count = Integer.toString(entry.getValue()
							.getNodeList().size());
					list.add(value);
					list.add(count);
					dataArray2D.add(list);

				}

			}

			if (humanReadFriendly) {

				jsNumber2dResult = AgentUtils.renderJson(aggregationMap);
			} else {
				jsNumber2dResult = AgentDataAggregator.getDataJSNumber2D(
						dataArray2D, humanReadFriendly);
			}

		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogError("Error in getValueCountJSNumber2D "
					+ DateUtils.getNowDateTimeStrSdsm());
		}

		return jsNumber2dResult;

	}

	public List<AggregationValueMetadata> getResponseToMetadataList() {

		List<AggregationValueMetadata> avmList = new ArrayList<AggregationValueMetadata>();
		try {

			// sorted by number of nodes
			avmList.addAll(responseToMetadataMap.values());
			Collections.sort(avmList);

		} catch (Throwable t) {
			t.printStackTrace();
			models.utils.LogUtils.printLogError("Error in getValueCountJSNumber2D "
					+ DateUtils.getNowDateTimeStrSdsm());
		}

		return avmList;

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

	public HashMap<String, String> getFqdnResponseExtractMap() {
		return fqdnResponseExtractMap;
	}

	public HashMap<String, String> getFqdnErrorMsgExtractMap() {
		return fqdnErrorMsgExtractMap;
	}

	public HashMap<String, Boolean> getFqdnIsErrorExtractMap() {
		return fqdnIsErrorExtractMap;
	}

	/**
	 * KEY generate
	 * 
	 * @param patternStr
	 */
	public void genResponseToMetadataMap(String patternStr) {

		try {

			long startTime = System.currentTimeMillis();
			long endTime = -1L;

			if (VarUtils.IN_DETAIL_DEBUG) {
				 models.utils.LogUtils.printLogError
						 ("1. BEFORE FOR LOOP genResponseToMetadataMap() "
								+ DateUtils.getNowDateTimeStrSdsm());
			}

			AggregationDirector aggregationDirector = new AggregationDirector();
			aggregationDirector.sendAggregationCommandToManager(patternStr,
					this);

			if (VarUtils.IN_DETAIL_DEBUG) {
				 models.utils.LogUtils.printLogError
						 ("2. AFTER FOR LOOP in genResponseToMetadataMap() "
								+ DateUtils.getNowDateTimeStrSdsm());
			}

			genResponseToMetadataMapHelper();

			if (VarUtils.IN_DETAIL_DEBUG) {
				 models.utils.LogUtils.printLogError
						 ("3. AFTER genResponseToMetadataMapHelper() in genResponseToMetadataMap() "
								+ DateUtils.getNowDateTimeStrSdsm());
			}
			endTime = System.currentTimeMillis();

			models.utils.LogUtils.printLogNormal
					 ("!!!!END AGGREGATION REG MATCHING AKKA JOB. genResponseToMetadataMap()."
							+ " time: "
							+ DateUtils.getDateTimeStr(new Date(endTime))
							+ "\n!!!AGGREGATION TOTAL Duration: "
							+ DateUtils.getDurationSecFromTwoDatesDouble(
									new Date(startTime), new Date(endTime))
							+ "sec.");

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}// end func

	/**
	 * From the 3 map: to form the result map: responseToMetadataMap; key is the
	 * extracted value.
	 */
	public void genResponseToMetadataMapHelper() {

		for (Entry<String, String> entry : fqdnResponseExtractMap.entrySet()) {
			String fqdn = entry.getKey();
			String extractedResponse = entry.getValue();
			String errorMsg = fqdnErrorMsgExtractMap.get(fqdn);
			Boolean isError = fqdnIsErrorExtractMap.get(fqdn);

			String responseToMetadataMapKey = (isError) ? errorMsg
					: extractedResponse;

			// safeguard : responseToMetadataMapKey == NULL?
			if (responseToMetadataMapKey == null) {
				responseToMetadataMapKey = VarUtils.NA_NUM;
				 models.utils.LogUtils.printLogError
						 ("responseToMetadataMapKey is null in genResponseToMetadataMapHelper()"
								+ DateUtils.getNowDateTimeStrSdsm());
			}

			if (responseToMetadataMap.containsKey(responseToMetadataMapKey)) {
				// add this node in there
				AggregationValueMetadata avm = responseToMetadataMap
						.get(responseToMetadataMapKey);
				avm.getNodeList().add(fqdn);

			} else {
				// the first time will need to initialize
				List<String> nodeList = new ArrayList<String>();
				nodeList.add(fqdn);

				AggregationValueMetadata avm = new AggregationValueMetadata(
						responseToMetadataMapKey, nodeList, isError);
				responseToMetadataMap.put(responseToMetadataMapKey, avm);

			}
		}
	}

}
