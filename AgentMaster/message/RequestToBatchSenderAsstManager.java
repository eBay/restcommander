package models.agent.batch.commands.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import akka.actor.ActorRef;
import models.data.AgentCommandMetadata;
import models.data.NodeData;
import models.data.NodeDataCmdType;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;

public class RequestToBatchSenderAsstManager {

	public final String directorJobId;

	public final int maxConcurrency;

	public final List<ActorRef> workers;

	public final ActorRef sender;
	
	public final List<String> jobIdQ;
	
	public final List<NodeData> nodeDataQ;
	
	public InitialRequestToManager request;

	public RequestToBatchSenderAsstManager(String directorJobId,
			List<ActorRef> workers, ActorRef sender, int maxConcurrency, InitialRequestToManager request) {
		super();
		this.directorJobId = directorJobId;
		this.workers = workers;
		this.sender = sender;
		this.maxConcurrency = maxConcurrency;
		this.jobIdQ = new ArrayList<String>();
		this.nodeDataQ = new ArrayList<NodeData>();
		for(Entry<String, NodeData> entry : request.dataStore.get(request.getNodeGroupType()).getNodeDataMapValid().entrySet()) {
			jobIdQ.add(entry.getKey());
			nodeDataQ.add(entry.getValue());
		}
		this.request = request;
		//nodeDataMapValidSafe = request.dataStore.get(request.getNodeGroupType()).getNodeDataMapValid();
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public String getDirectorJobId() {
		return directorJobId;
	}

	public List<ActorRef> getWorkers() {
		return workers;
	}

	public ActorRef getSender() {
		return sender;
	}

}
