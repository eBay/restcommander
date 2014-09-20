package models.agent.batch.commands.message;

import java.util.Map;

import models.data.AgentCommandMetadata;
import models.data.NodeDataCmdType;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;

public class RequestToLogWorker {

	private final String nodeGroupType;

	private final String directorJobId;
	private final String agentCommandType;

	private final Map<String, NodeGroupDataMap> dataStore;

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public String getDirectorJobId() {
		return directorJobId;
	}

	public RequestToLogWorker(String nodeGroupType,
			String agentCommandType, Map<String, NodeGroupDataMap> dataStore,
			String directorJobId) {
		super();
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
		this.dataStore = dataStore;
		this.directorJobId = directorJobId;
	}

	public Map<String, NodeGroupDataMap> getDataStore() {
		return dataStore;
	}

}
