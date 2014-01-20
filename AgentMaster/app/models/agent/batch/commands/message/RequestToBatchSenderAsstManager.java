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
package models.agent.batch.commands.message;

import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;

import models.data.AgentCommandMetadata;
import models.data.NodeGroupDataMap;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class RequestToBatchSenderAsstManager {

	public final String directorJobId;

	public final int maxConcurrency;

	public final List<ActorRef> workers;

	public final ActorRef sender;

	public RequestToBatchSenderAsstManager(String directorJobId,
			List<ActorRef> workers, ActorRef sender, int maxConcurrency) {
		super();
		this.directorJobId = directorJobId;
		this.workers = workers;
		this.sender = sender;
		this.maxConcurrency = maxConcurrency;
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
