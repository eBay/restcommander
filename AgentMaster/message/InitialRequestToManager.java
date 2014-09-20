package models.agent.batch.commands.message;

import java.io.Serializable;
import java.util.Map;

import models.data.AgentCommandMetadata;
import models.data.NodeDataCmdType;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;

public class InitialRequestToManager implements Serializable{

	public final String nodeGroupType;

	public final String directorJobId;
	public final String agentCommandType;

	public final Map<String, NodeGroupDataMap> dataStore;
	public final boolean localMode;
	public final boolean failOver;
	public final int maxConcNum;

	public String getNodeGroupType() {
		return nodeGroupType;
	}

	public String getAgentCommandType() {
		return agentCommandType;
	}

	public String getDirectorJobId() {
		return directorJobId;
	}


	public InitialRequestToManager(String nodeGroupType,
			String agentCommandType, String directorJobId,
			Map<String, NodeGroupDataMap> dataStore, 
			boolean localMode, boolean failOver, int maxConcNum) {
		super();
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
		this.directorJobId = directorJobId;
		this.dataStore = dataStore;
		this.localMode = localMode;
		this.failOver = failOver;
		this.maxConcNum = maxConcNum;
	}

	public Map<String, NodeGroupDataMap> getDataStore() {
		return dataStore;
	}

}
