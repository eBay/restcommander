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
package RemoteCluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import RemoteCluster.CommunicationMessages.endOfRequest;
/**
 * 
 * @author chunyang
 *
 */
public class JobDispatcher extends UntypedActor{
	
	private List<ActorRef> managerList;
	private List<Map<String, NodeData>> jobGroupList;
	private int chunckSize;
	private Queue<Integer> jobQ;
	private String nodeGroupType;
	private String agentCommandType;
	private String directorJobUuid;
	private int maxConcNum;
	private final FiniteDuration durationManager = Duration.create(
			15, TimeUnit.SECONDS);
	
	public JobDispatcher(List<ActorRef> managerList, List<Map<String, NodeData>> jobGroupList, ConcurrentLinkedQueue<Integer> jobQ, int chunckSize, 
			String nodeGroupType, String agentCommandType, String directorJobUuid, int maxConcNum) {
		this.managerList = managerList; 
		this.jobGroupList = jobGroupList;
		this.chunckSize = chunckSize;
		this.jobQ = jobQ;
		this.nodeGroupType = nodeGroupType;
		this.agentCommandType = agentCommandType;
		this.directorJobUuid = directorJobUuid;
		this.maxConcNum = maxConcNum;
	}
	
	@Override
	public void onReceive(Object message) {
		if (message instanceof String) {
			Integer index = jobQ.poll();
			while (index != null) {
				System.out.println("Poll Job: " + index);
				ActorRef manager = managerList.get(index);
				streamSendRequest(manager, index);
				index = jobQ.poll();
			}
			getContext().stop(getSelf());
		}
	}
	
	private void streamSendRequest(ActorRef manager, int index) {
		
		Map<String, NodeGroupDataMap> partDataStore = new HashMap<String, NodeGroupDataMap>();
		partDataStore.put(nodeGroupType, new NodeGroupDataMap(nodeGroupType));
		
		int i = index * chunckSize;
		
		if (i >= jobGroupList.size()) {
			managerList.remove(manager);
			getContext().system().stop(manager);
			return;
		}
		
		Map<String, NodeData> partData = jobGroupList.get(i);
		
		partDataStore.get(nodeGroupType).setNodeDataMapValid(partData);
		
		Future<Object> ackFuture = Patterns.ask(manager, new InitialRequestToManager(nodeGroupType,
				agentCommandType, directorJobUuid, partDataStore, true, false, maxConcNum), new Timeout(durationManager));
		try {
			Await.result(ackFuture, durationManager);
			i++;
			while (i<jobGroupList.size() && i<(index+1)*chunckSize ) {
				ackFuture = Patterns.ask(manager, jobGroupList.get(i), new Timeout(durationManager));
				Await.result(ackFuture, durationManager);
				i++;
			}
		} catch (Exception e) {
			System.out.println("Dispatch Job Timeout.");
			e.printStackTrace();
		}
		manager.tell(new endOfRequest(), null);
	}
}
