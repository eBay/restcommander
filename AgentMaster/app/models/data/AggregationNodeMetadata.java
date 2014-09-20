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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 2014: for converting back to node --> value pair
 * value is the regex extracted string.
 * @author ypei
 *
 */
public class AggregationNodeMetadata implements
		Comparable<AggregationNodeMetadata> {

	private String node;

	private String value;
	private boolean isError;

	public AggregationNodeMetadata(String node, String value, boolean isError) {
		super();
		this.node = node;
		this.value = value;
		this.isError = isError;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	@Override
	public int compareTo(AggregationNodeMetadata o) {
		String node = ((AggregationNodeMetadata) o).getNode();
		// //ascending order
		return this.getNode().compareTo(node);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isError() {
		return isError;
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}
	
	/**
	 * generate just the value is equal to the node fqnd; and then no error; just to fake a such aggregation map to feed the single targer server api
	 * 20140111
	 * @param nodeList
	 * @return
	 */
	public static Map<String, AggregationNodeMetadata> generateAggregationNodeMetadataMapFromNodeList(List<String> nodeList){
		Map<String,AggregationNodeMetadata> nodeValueMap = new LinkedHashMap<String,AggregationNodeMetadata>();
		
		for(String node: nodeList){
				boolean isError = false;
				AggregationNodeMetadata nodeMetadata = new  AggregationNodeMetadata(node, node, isError);
				nodeValueMap.put(node, nodeMetadata);
		}
		
		return nodeValueMap;
		
	}

}
