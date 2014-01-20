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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.utils.DateUtils;
import models.utils.VarUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author ypei
 * 
 */
public class NodeGroupSourceMetadata implements Comparable<NodeGroupSourceMetadata>{ 

	private String nodeGroupType;
	private String dataSourceType;
	private String statehubUrlPrefix;
	private String envStr;

	private int nodeCount;

	private final List<String> nodeList = new ArrayList<String>();

	private final List<String> clusterList = new ArrayList<String>();

	public NodeGroupSourceMetadata(String dataSourceType, String nodeGroupType,
			String statehubUrlPrefix, String envStr) {
		super();
		this.dataSourceType = dataSourceType;
		this.nodeGroupType = nodeGroupType;
		this.statehubUrlPrefix = statehubUrlPrefix;
		this.envStr = envStr;
	}
	
	/**
	 * 20131016: fix the node count is 0
	 * @param dataSourceType
	 * @param nodeGroupType
	 * @param nodeList
	 */
	public NodeGroupSourceMetadata(String dataSourceType, String nodeGroupType,
			List<String> nodeList) {
		super();
		this.dataSourceType = dataSourceType;
		this.nodeGroupType = nodeGroupType;
		this.nodeList.addAll(nodeList);
		this.statehubUrlPrefix = null;
		this.envStr = null;
		// 20131016: fix node count
		this.nodeCount = nodeList.size();
	}
	
	

	/**
	 * Create the ad hoc list
	 * @param list
	 */
	public NodeGroupSourceMetadata(List<String> list) {
		super();
		
		this.nodeList.addAll(list);
		this.nodeCount = list.size();
		this.dataSourceType = VarUtils.ADHOC;
		this.nodeGroupType = VarUtils.ADHOC + "." +  DateUtils.getNowDateTimeStrSdsm();
		this.statehubUrlPrefix = VarUtils.NA;
		this.envStr = VarUtils.NA;
	}
	

	/**
	 * THIS DOES NOT COMPLETELY COPY!! MISSING statehubUrlPrefix,  LIST ELEMENT for easy JSON passing to html template!!
	 * @param other
	 */
	public NodeGroupSourceMetadata(NodeGroupSourceMetadata other) {
		super();
		this.dataSourceType = other.dataSourceType;
		this.nodeGroupType = other.nodeGroupType;
	//	this.statehubUrlPrefix = other.statehubUrlPrefix.replace(":", "\\:");
		
	//	this.envStr = other.envStr;
		this.nodeCount = other.nodeCount;
		// not to copy the list of cluster and env
	}

	public List<String> getClusterList() {
		return clusterList;
	}
	
	public String getClusterListStr() {
		StringBuilder sb = new StringBuilder();
		
		for(String cluster: clusterList){
			sb.append(cluster).append("\n");
		}
		
		return sb.toString();
	}

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public void setNodeGroupType(String nodeGroupType) {
		this.nodeGroupType = nodeGroupType;
	}

	public String getDataSourceType() {
		return dataSourceType;
	}

	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	public String getStatehubUrlPrefix() {
		return statehubUrlPrefix;
	}

	public void setStatehubUrlPrefix(String statehubUrlPrefix) {
		this.statehubUrlPrefix = statehubUrlPrefix;
	}

	public String getEnvStr() {
		return envStr;
	}

	public void setEnvStr(String envStr) {
		this.envStr = envStr;
	}

	public List<String> getNodeList() {
		return nodeList;
	}

	public void addNodesToNodeList(List<String> newList) {
		nodeList.addAll(newList);
		nodeCount = nodeList.size();
	}

	/**
	 * Dont forget to update the node count.
	 * @param newList
	 */
	public void replaceNodesInNodeList(List<String> newList) {
		nodeList.clear();
		nodeList.addAll(newList);
		nodeCount = nodeList.size();
	}
	
	public static List<NodeGroupSourceMetadata> convertMapToList(
			Map<String, NodeGroupSourceMetadata> map) {

		List<NodeGroupSourceMetadata> list = new ArrayList<NodeGroupSourceMetadata>();
		for (Entry<String, NodeGroupSourceMetadata> entry : map.entrySet()) {

			NodeGroupSourceMetadata copyWithoutList = new NodeGroupSourceMetadata(
					entry.getValue());
			list.add(copyWithoutList);
		}

		return list;

	}
	

	public int getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(int nodeCount) {
		this.nodeCount = nodeCount;
	}

	@Override
	public String toString() {
		return "NodeGroupSourceMetadata [nodeGroupType=" + nodeGroupType
				+ ", dataSourceType=" + dataSourceType + ", statehubUrlPrefix="
				+ statehubUrlPrefix + ", envStr=" + envStr + ", nodeCount="
				+ nodeCount + ", nodeList=" + nodeList + ", clusterList="
				+ clusterList + "]";
	}

	@Override
	public int compareTo(NodeGroupSourceMetadata o) {

		int compareQuantity = ((NodeGroupSourceMetadata) o).nodeCount; 
		 
		//ascending  order
		return  this.nodeCount - compareQuantity;
 
	}

}
