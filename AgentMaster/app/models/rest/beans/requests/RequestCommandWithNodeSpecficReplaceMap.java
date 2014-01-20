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
package models.rest.beans.requests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.data.StrStrMap;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class RequestCommandWithNodeSpecficReplaceMap implements Serializable {

	private final List<String> targetNodes = new ArrayList<String>();

	private String agentCommandType;
	// 20131026
	private Boolean useNewAgentCommand;
	private String newAgentCommandLine;
	private String newAgentCommandContentTemplate;

	// 20131110
	private Boolean willAggregateResponse;
	private Boolean useNewAggregation;
	private String aggregationType;
	private String newAggregationExpression;

	private Map<String, StrStrMap> replacementVarMapNodeSpecific = new HashMap<String, StrStrMap>();

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public void setAgentCommandType(String agentCommandType) {
		this.agentCommandType = agentCommandType;
	}

	public Map<String, StrStrMap> getReplacementVarMapNodeSpecific() {
		return replacementVarMapNodeSpecific;
	}

	public void setReplacementVarMapNodeSpecific(
			Map<String, StrStrMap> replacementVarMapNodeSpecific) {
		this.replacementVarMapNodeSpecific = replacementVarMapNodeSpecific;
	}

	public List<String> getTargetNodes() {
		return targetNodes;
	}

	public RequestCommandWithNodeSpecficReplaceMap() {
		super();
	}

	public Boolean getUseNewAgentCommand() {
		return useNewAgentCommand;
	}

	public void setUseNewAgentCommand(Boolean useNewAgentCommand) {
		this.useNewAgentCommand = useNewAgentCommand;
	}

	public String getNewAgentCommandLine() {
		return newAgentCommandLine;
	}

	public void setNewAgentCommandLine(String newAgentCommandLine) {
		this.newAgentCommandLine = newAgentCommandLine;
	}

	public String getNewAgentCommandContentTemplate() {
		return newAgentCommandContentTemplate;
	}

	public void setNewAgentCommandContentTemplate(
			String newAgentCommandContentTemplate) {
		this.newAgentCommandContentTemplate = newAgentCommandContentTemplate;
	}

	public Boolean getWillAggregateResponse() {
		return willAggregateResponse;
	}

	public void setWillAggregateResponse(Boolean willAggregateResponse) {
		this.willAggregateResponse = willAggregateResponse;
	}

	public Boolean getUseNewAggregation() {
		return useNewAggregation;
	}

	public void setUseNewAggregation(Boolean useNewAggregation) {
		this.useNewAggregation = useNewAggregation;
	}

	public String getAggregationType() {
		return aggregationType;
	}

	public void setAggregationType(String aggregationType) {
		this.aggregationType = aggregationType;
	}

	public String getNewAggregationExpression() {
		return newAggregationExpression;
	}

	public void setNewAggregationExpression(String newAggregationExpression) {
		this.newAggregationExpression = newAggregationExpression;
	}

}
