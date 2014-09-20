package models.agent.batch.commands.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import models.asynchttp.response.GenericAgentResponse;


public class BatchResponseFromManager extends GenericResponseFromDirector implements Serializable{

	public final Map<String, GenericAgentResponse> responseMap = new HashMap<String, GenericAgentResponse>();

	public Map<String, GenericAgentResponse> getResponseMap() {
		return responseMap;
	}

	public BatchResponseFromManager() {
		super();
	};

}
