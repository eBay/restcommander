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
/**
 * 
 * @author ypei
 *
 */
public class AggregationValueMetadata implements Comparable<AggregationValueMetadata>{ 

	private String value;
	private List<String> nodeList = new ArrayList<String>();

	private boolean isError;

	public AggregationValueMetadata(String value, List<String> nodeList,
			boolean isError) {
		super();
		this.value = value;
		this.nodeList = nodeList;
		this.isError = isError;
	}


	@Override
	public int compareTo(AggregationValueMetadata o) {

		//int nodeListCount = ((AggregationValueMetadata) o).nodeList.size(); 
		 
		////ascending  order
		//return  this.nodeList.size() - nodeListCount;
 
		String value= ((AggregationValueMetadata) o).getValue();
		 
		////ascending  order
		return  this.getValue().compareTo(value) ;
	}
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<String> getNodeList() {
		return nodeList;
	}

	public void setNodeList(List<String> nodeList) {
		this.nodeList = nodeList;
	}

	
	public int getNodeCount() {
		return this.nodeList.size();
	}

	
	public boolean isError() {
		return isError;
	}
	
	public String nodeListDetailsStr() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("nodeList size: " + nodeList.size() + "\n");
		
		for(String fqdn: nodeList){
			sb.append(fqdn+ "\n");
		}
		
		return sb.toString();
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}

}
