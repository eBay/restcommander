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
