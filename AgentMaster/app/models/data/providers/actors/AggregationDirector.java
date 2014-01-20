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
package models.data.providers.actors;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.asynchttp.actors.ActorConfig;
import models.asynchttp.actors.CommandManager;
import models.data.AggregateData;
import models.data.NodeGroupDataMap;
import models.data.providers.LogProvider;
import models.utils.DateUtils;
import models.utils.VarUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.pattern.Patterns;
import akka.util.Timeout;
/**
 * 
 * @author ypei
 *
 */
public class AggregationDirector {

	public AggregationDirector() {
		super();
	}

	private ResponseFromAggregationManager responseFromAggregationManager = null;

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
	public void sendAggregationCommandToManager(String patternStr,
			AggregateData aggregateData) {

		ActorRef aggregationManager = null;
		try {
			// Start new job
			String directorJobUuid = UUID.randomUUID().toString();

			models.utils.LogUtils.printLogNormal("!!STARTED sendAggregationCommandToManager : "
					+ directorJobUuid + " at " + DateUtils.getNowDateTimeStr());

			// Get the singleton actor system

			// create the master
			aggregationManager = ActorConfig.getActorSystem().actorOf(
					new Props(new UntypedActorFactory() {
						private static final long serialVersionUID = 1L;

						public UntypedActor create() {
							return new AggregationManager();
						}
					}), "AggregationManager-" + UUID.randomUUID().toString());

			final FiniteDuration duration = Duration.create(
					VarUtils.TIMEOUT_ASK_AGGREGATION_MANAGER_SCONDS,
					TimeUnit.SECONDS);
			Future<Object> future = Patterns.ask(aggregationManager,
					new RequestToAggregationManager(patternStr,
							directorJobUuid, aggregateData), new Timeout(
							duration));

			responseFromAggregationManager = (ResponseFromAggregationManager) Await
					.result(future, duration);

			models.utils.LogUtils.printLogNormal("!!COMPLETED sendAggregationCommandToManager : "
					+ directorJobUuid + " at " + DateUtils.getNowDateTimeStr());

		} catch (Throwable ex) {
			 models.utils.LogUtils.printLogError
					 ("Exception in sendAggregationCommandToManager : "
							+ ex.getLocalizedMessage());
			ex.printStackTrace();
		} finally {

			// stop the manager:agentCommandManager
			if (aggregationManager != null) {
				ActorConfig.getActorSystem().stop(aggregationManager);
			}

			aggregationManager = null;

		}
	}// end func.

	public ResponseFromAggregationManager getResponseFromAggregationManager() {
		return responseFromAggregationManager;
	}

	public void setResponseFromAggregationManager(
			ResponseFromAggregationManager responseFromAggregationManager) {
		this.responseFromAggregationManager = responseFromAggregationManager;
	}

}
