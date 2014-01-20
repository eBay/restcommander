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
package models.asynchttp.actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.data.AgentCommandMetadata;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.providers.AgentDataProvider;
import models.data.providers.LogProvider;
import models.utils.DateUtils;
import models.utils.VarUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.pattern.Patterns;
import akka.util.Timeout;

/**
 * 
 * This is NOT an AKKA actor. This is the starting point to create the manager
 * and pass the command to the manager.
 * 
 * 20130430: when command is AGENT_CMD_KEY_AGENT_SMART_UPGRADE: will do invoke
 * the AgentUpgradeManager
 * 
 * @author ypei
 * 
 */
public class CommandDirector {

	// private static final AgentCommandDirector instance = new
	// AgentCommandDirector();
	//
	// public static AgentCommandDirector getInstance() {
	// return instance;
	// }
	//
	// private AgentCommandDirector() {
	// super();
	// }

	public CommandDirector() {
		super();
	}

	/**
	 * Here dont need the nodeGroupMetaData.Assuming the request content has
	 * already been ready as in the keys of the hashmap with all the target
	 * nodes. So just need a string to have the nodeGroupType is fine.
	 * 
	 * @param nodeGroupType
	 * @param agentCommandType
	 * @param dataStore
	 * @return
	 */
	public BatchResponseFromManager sendAgentCommandToManager(
			String nodeGroupType, String agentCommandType,
			Map<String, NodeGroupDataMap> dataStore) {

		BatchResponseFromManager agentCommandResponseFromManager = null;
		ActorRef agentCommandManager = null;
		try {
			//Start new job
			ActorConfig.runningJobCount.incrementAndGet();
			
			String directorJobUuid = UUID.randomUUID().toString();

			models.utils.LogUtils.printLogNormal("!!STARTED sendAgentCommandToManager : "
					+ directorJobUuid + " at " + DateUtils.getNowDateTimeStr());

			//20140120: double check NPE for command
			AgentCommandMetadata agentCommandMetadata = AgentDataProvider.agentCommandMetadatas
					.get(agentCommandType);
			if(agentCommandMetadata==null){
				models.utils.LogUtils.printLogError("!!ERROR in  sendAgentCommandToManager : "
						+ directorJobUuid + " agentCommandType is NULL!! return. at " + DateUtils.getNowDateTimeStr());
				return agentCommandResponseFromManager;
			}
			
			
			// Get the singleton actor system


			// create the master
			agentCommandManager = ActorConfig.getActorSystem().actorOf(new Props(
					new UntypedActorFactory() {
						private static final long serialVersionUID = 1L;

						public UntypedActor create() {
								return new CommandManager();
						}
					}), "AgentCommandManager-" + UUID.randomUUID().toString());

			final FiniteDuration duration = Duration.create(
					VarUtils.TIMEOUT_ASK_MANAGER_SCONDS, TimeUnit.SECONDS);
			// Timeout timeout = new
			// Timeout(FiniteDuration.parse("300 seconds"));
			Future<Object> future = Patterns.ask(agentCommandManager,
					new InitialRequestToManager(nodeGroupType,
							agentCommandType, directorJobUuid, dataStore),
					new Timeout(duration));

			agentCommandResponseFromManager = (BatchResponseFromManager) Await
					.result(future, duration);
			models.utils.LogUtils.printLogNormal("GenericAgentResponseMap in future size: "
					+ agentCommandResponseFromManager.getResponseMap().size());

			models.utils.LogUtils.printLogNormal("!!COMPLETED sendAgentCommandToManager : "
					+ directorJobUuid + " at " + DateUtils.getNowDateTimeStr());

			// !!! KEY Save to files: Save command data as LOG
			LogProvider.saveAgentDataInLog(nodeGroupType, agentCommandType,
					dataStore);

		} catch (Throwable ex) {
			models.utils.LogUtils.printLogError("Exception in sendAgentCommandToManager : "
					+ ex.getLocalizedMessage());
			ex.printStackTrace();
		} finally {

			// stop the manager:agentCommandManager
			if (agentCommandManager != null) {
				ActorConfig.getActorSystem().stop(agentCommandManager);
			}
			
			agentCommandManager = null;
			

			// now regard job is down; no longer a running job that requires actor system (can safe shutdown)
			ActorConfig.runningJobCount.decrementAndGet();
			//TODO MUST ROLLBACK
			//ActorConfig.shutDownActorSystemWhenNoJobRunning();
			
			
			
		}
		return agentCommandResponseFromManager;
	}// end func.

}
