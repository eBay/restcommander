package models.agent.batch.commands.message;

import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;

import models.data.AgentCommandMetadata;
import models.data.NodeDataCmdType;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;

/**
 * Receive count from the manager to asst manager; for flow control
 * 
 * @author ypei
 * 
 */
public class ResponseCountToBatchSenderAsstManager {

	public final int responseCount;

	public ResponseCountToBatchSenderAsstManager(int responseCount) {
		super();
		this.responseCount = responseCount;
	}

	public int getResponseCount() {
		return responseCount;
	}

}
