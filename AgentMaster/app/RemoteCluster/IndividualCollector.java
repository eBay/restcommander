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

import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger; 
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.asynchttp.response.GenericAgentResponse;
import RemoteCluster.CommunicationMessages.streamRequestToManager;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
/**
 * 
 * @author chunyang
 *
 */
public class IndividualCollector extends UntypedActor{
	
	public Queue<ActorRef> slaveQ;
	private AtomicInteger atomic;
	
	public IndividualCollector(ConcurrentLinkedQueue slaveQ, AtomicInteger atomic) {
		this.slaveQ = slaveQ;
		this.atomic = atomic;
	}
	
	@Override
	public void onReceive(Object message) {
		if (message instanceof String) {
			getSender().tell(streamResponse(), getSelf());
		}
	}
	
	private BatchResponseFromManager streamResponse() {
		BatchResponseFromManager response = new BatchResponseFromManager();
		ActorRef manager = slaveQ.poll();
		while (manager != null) {
			FiniteDuration duration = Duration.create(
					60, TimeUnit.SECONDS);
			streamRequestToManager streamRequest = new streamRequestToManager(0);
			Future<Object> futureResponse = Patterns.ask(manager, streamRequest, new Timeout(duration));
			System.out.println(manager.path().address().toString());
			
			try {
				BatchResponseFromManager partResponse = (BatchResponseFromManager) Await.result(futureResponse, duration);
				while (partResponse.getResponseMap().size() > 0) {
					collectResult(response, partResponse);
					streamRequest.index += partResponse.getResponseMap().size();
					futureResponse = Patterns.ask(manager, streamRequest, new Timeout(duration));
					partResponse = (BatchResponseFromManager) Await.result(futureResponse, duration);
				}
			} catch (Exception e) {
				System.out.println("Gather Response Timeout");
			}
			
			atomic.decrementAndGet();
			manager = slaveQ.poll();
		}
		
		return response;
	}
	
	private void collectResult(BatchResponseFromManager tot, BatchResponseFromManager part) {
		for (Entry<String, GenericAgentResponse> e : part.responseMap.entrySet()) {
			tot.responseMap.put(e.getKey(), e.getValue());
		}
	}
}
