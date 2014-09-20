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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;  
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.concurrent.Await;
import scala.concurrent.Future;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.asynchttp.response.GenericAgentResponse;
import akka.actor.ActorRef;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;
import akka.util.Timeout;
import RemoteCluster.JobStatus.State;
import RemoteCluster.CommunicationMessages.collecterCheckResponse;
import RemoteCluster.CommunicationMessages.gatherResponse;
import RemoteCluster.CommunicationMessages.queryMonitorProgressMessage;
import RemoteCluster.CommunicationMessages.queryMonitorForResponse;
import RemoteCluster.CommunicationMessages.streamRequestToManager;

/**
 * 
 * @author chunyang
 *
 */
public class ResponseCollector extends UntypedActor{
	private ActorRef monitor;
	private ActorRef director;
	private BatchResponseFromManager collectedResponse;
	public JobStatus.JobInfo jobInfo;
	private Queue<ActorRef> slaveQ;
	private AtomicInteger atomic;
	private List<ActorRef> individualCollectors;
	
	public ResponseCollector(JobStatus.JobInfo jobInfo) {
		this.jobInfo = jobInfo;
		this.slaveQ = new ConcurrentLinkedQueue<ActorRef>();
		this.atomic = new AtomicInteger(0);  
		this.individualCollectors = new ArrayList<ActorRef>();
		
	}
	
	@Override
	public void onReceive(Object message) {
		if (message instanceof gatherResponse) {
			gatherResponse gR = (gatherResponse) message;
			this.monitor = gR.monitor;
			director = getSender();
			collectedResponse = new BatchResponseFromManager();
			for (int i=0; i<3; i++) {
				individualCollectors.add(getContext().system().actorOf(
						Props.create(IndividualCollector.class, slaveQ, atomic)
					));
			}
			getContext().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
					new Runnable() {
					    @Override
					    public void run() {
					      getSelf().tell(new collecterCheckResponse(), ActorRef.noSender());
					 }
			}, getContext().system().dispatcher());
			
		} else if (message instanceof collecterCheckResponse){
			FiniteDuration duration = Duration.create(
					10, TimeUnit.SECONDS);
			Future<Object> futureList = Patterns.ask(monitor, new queryMonitorForResponse(), new Timeout(duration));
			try {
				List<ActorRef> collectList = (List<ActorRef>) Await.result(futureList, duration);
				if (collectList.size() > 0) {
					for (ActorRef m : collectList) {
						slaveQ.offer(m);
						atomic.incrementAndGet();
					}
					for (ActorRef m : individualCollectors) {
						atomic.incrementAndGet();
						m.tell("start", getSelf());
					}
				}
			} catch (Exception e) {
				System.out.println("Collector ask monitor timeout");
			}
	
			if (atomic.intValue() == 0 && jobInfo.state == State.finishedNotGathered) {
				director.tell(collectedResponse, getSelf());
				for (ActorRef ic : individualCollectors) {
					getContext().stop(ic);
				}
				individualCollectors.clear();
			} else {
				getContext().system().scheduler().scheduleOnce(Duration.create(2, TimeUnit.SECONDS),
						new Runnable() {
						    @Override
						    public void run() {
						      getSelf().tell(new collecterCheckResponse(), ActorRef.noSender());
						 }
				}, getContext().system().dispatcher());
			}
		} else if (message instanceof BatchResponseFromManager) {
			BatchResponseFromManager res = (BatchResponseFromManager) message;
			for (Entry<String, GenericAgentResponse> e : res.responseMap.entrySet()) {
				collectedResponse.responseMap.put(e.getKey(), e.getValue());
			}
			jobInfo.collectedNum += res.responseMap.size();
			atomic.decrementAndGet();
		}
	}
	
}
