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
package models.rest.beans.responses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import models.data.AggregationValueMetadata;
/**
 * 
 * @author ypei
 *
 */
public class AggregationResponse implements Serializable {

	private Map<String, String> aggregationMap = new TreeMap<String, String>();

	private List<AggregationValueMetadata> aggregationValueToNodesList = new ArrayList<AggregationValueMetadata>();

	public AggregationResponse(Map<String, String> aggregationMap,
			List<AggregationValueMetadata> aggregationValueToNodesList) {
		super();
		this.aggregationMap = aggregationMap;
		this.aggregationValueToNodesList = aggregationValueToNodesList;
	}

	public List<AggregationValueMetadata> getAggregationValueToNodesList() {
		return aggregationValueToNodesList;
	}

	public void setAggregationValueToNodesList(
			List<AggregationValueMetadata> aggregationValueToNodesList) {
		this.aggregationValueToNodesList = aggregationValueToNodesList;
	}

	public AggregationResponse() {
		super();
	}

	public Map<String, String> getAggregationMap() {
		return aggregationMap;
	}

	public void setAggregationMap(Map<String, String> aggregationMap) {
		this.aggregationMap = aggregationMap;
	}

}
