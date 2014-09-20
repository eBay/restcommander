package models.agent.batch.commands.message;

import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;

import models.data.AgentCommandMetadata;
import models.data.NodeDataCmdType;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;

public class ContinueToSendToBatchSenderAsstManager {

	public final int processedWorkerCount;

	public ContinueToSendToBatchSenderAsstManager(int processedWorkerCount) {
		super();
		this.processedWorkerCount = processedWorkerCount;
	}

}
