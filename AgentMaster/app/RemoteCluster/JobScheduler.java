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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import RemoteCluster.CommunicationMessages.*;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.UntypedActor;
import akka.cluster.Member;
/**
 * 
 * @author chunyang
 *
 */
public class JobScheduler extends UntypedActor{
	
	private SupermanActorSystem TransformLocalSystem;
	public Queue<JobInitInfo> scheduleQ;
	
	public JobScheduler(SupermanActorSystem TransformLocalSystem) {
		this.TransformLocalSystem = TransformLocalSystem;
		this.scheduleQ = new ConcurrentLinkedQueue<JobInitInfo>();
	}
	
	@Override
	public void onReceive(Object message){
		if (message instanceof JobInitInfo) {
			JobInitInfo initInfo = (JobInitInfo) message;
			if (initInfo.maxConcNum >= 2500)
				 initInfo.maxConcNum = 2500;
			scheduleQ.offer(initInfo);
		} else if (message instanceof CheckSchedule) {
			final JobInitInfo job = scheduleQ.peek();
			int tot = 0;
			if (job != null){
				final Set<Address> nodeList = new HashSet<Address>();
				for (Entry<Address, AtomicInteger> e : ClusterState.memberLoad.entrySet()) {
					if (job.totJobNum > tot) 
						if (e.getValue().intValue() + job.maxConcNum <= 2500) {
							nodeList.add(e.getKey());
							tot += 2000;
						}
				}
				if (nodeList.size() > 0) {
					scheduleQ.poll();
					Runnable director = new Runnable() {
				    	@Override
				    	public void run() {
				    		TransformLocalSystem.sendAgentCommandToManagerRemote(job.nodeGroupType, job.agentCommandType, job.dataStore, 
				    				job.localMode, job.failOver, job.directorJobUuid, job.maxConcNum, nodeList);
				    	}
					};
					new Thread(director).start();
				} else if (ClusterState.memberLoad.size() == 0 && 
						(TransformLocalSystem.localLoad.intValue() + job.maxConcNum <= 1500 || 
						(job.maxConcNum >1500 && TransformLocalSystem.localLoad.intValue() == 0))) {
					scheduleQ.poll();
					if (job.maxConcNum > 1500)
							job.maxConcNum = 1500;
					Runnable director = new Runnable() {
				    	@Override
				    	public void run() {
				    		TransformLocalSystem.sendAgentCommandToManagerRemote(job.nodeGroupType, job.agentCommandType, job.dataStore, 
				    				job.localMode, job.failOver, job.directorJobUuid, job.maxConcNum, nodeList);
				    	}
					};
					new Thread(director).start();
				}
			}
			getContext().system().scheduler().scheduleOnce((FiniteDuration) Duration.create(0.5, TimeUnit.SECONDS),
					new Runnable() {
					    @Override
					    public void run() {
					      getSelf().tell(new CheckSchedule(), ActorRef.noSender());
					 }
			}, getContext().system().dispatcher());
		}
	}// end of onReceive
}
