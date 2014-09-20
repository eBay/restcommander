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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;
import akka.util.Timeout;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.data.AgentCommandMetadata;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.NodeReqResponse;
import models.data.providers.AgentDataProvider;
import models.data.providers.AgentDataProviderHelper;
import models.utils.VarUtils;
import RemoteCluster.ClusterState;
import RemoteCluster.JobStatus.JobInfo;
import RemoteCluster.JobStatus.State;
import RemoteCluster.CommunicationMessages.queryMonitorForResponse;
import RemoteCluster.CommunicationMessages.querySlaveProgressMessage;
import RemoteCluster.CommunicationMessages.slaveProgressMessage;
import RemoteCluster.CommunicationMessages.queryMonitorProgressMessage;
import RemoteCluster.CommunicationMessages.monitorProgressMessage;
import RemoteCluster.CommunicationMessages.notCongestionSignal;
/**
 * 
 * @author chunyang
 *
 */
public class Monitor extends UntypedActor {

	private List<ActorRef> managerList;
	private String directorJobId;
	private JobStatus jobStatus;
	private List<ActorRef> completedButNotCollectedManager;
	private Map<ActorRef, Boolean> collectedManager;
	private Cluster cluster = Cluster.get(getContext().system());
	private Set<String> removedNode = new HashSet<String>();

	final Map<String, AtomicInteger> eachWorkerResponseCount = new HashMap<String, AtomicInteger>();
	final Map<String, AtomicInteger> eachWorkerRequestCount = new HashMap<String, AtomicInteger>();

	public Monitor(List<ActorRef> managerList, String jobId, JobStatus jobStatus) {
		this.managerList = managerList;
		this.directorJobId = jobId;
		this.jobStatus = jobStatus;
		this.completedButNotCollectedManager = new ArrayList<ActorRef>();
		this.collectedManager = new HashMap<ActorRef, Boolean>();
		getContext()
				.system()
				.scheduler()
				.scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
						new Runnable() {
							@Override
							public void run() {
								getSelf().tell(
										new queryMonitorProgressMessage(),
										ActorRef.noSender());
							}
						}, getContext().system().dispatcher());
	}

	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), MemberRemoved.class);
	}

	// re-subscribe when restart
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof queryMonitorProgressMessage) {
			queryProgress();
			getContext()
					.system()
					.scheduler()
					.scheduleOnce(
							(FiniteDuration) Duration.create(0.5,
									TimeUnit.SECONDS), new Runnable() {
								@Override
								public void run() {
									getSelf().tell(
											new queryMonitorProgressMessage(),
											ActorRef.noSender());
								}
							}, getContext().system().dispatcher());
		} else if (message instanceof queryMonitorForResponse) {
			getSender().tell(completedButNotCollectedManager, getSelf());
			completedButNotCollectedManager = new ArrayList<ActorRef>();
		} else if (message instanceof MemberRemoved) {
			MemberRemoved mRe = (MemberRemoved) message;
			removedNode.add(mRe.member().address().toString());
		}
	}

	public void queryProgress() {
		List<Future<Object>> progressList = new ArrayList<Future<Object>>();
		final FiniteDuration duration = (FiniteDuration) Duration.create(0.5,
				TimeUnit.SECONDS);
		Map<Future<Object>, ActorRef> askMap = new HashMap<Future<Object>, ActorRef>();

		for (ActorRef manager : managerList) {
			if ((ClusterState.memberLoad.containsKey(manager.path().address()) || manager
					.path().address().toString().equals("akka://ClusterSystem"))
					&& !removedNode.contains(manager.path().address()
							.toString())
					&& !collectedManager.containsKey(manager)) {
				Future<Object> progress = Patterns.ask(manager,
						new querySlaveProgressMessage(), new Timeout(duration));
				System.out.println("Query "
						+ manager.path().toString());
				progressList.add(progress);
				askMap.put(progress, manager);
				if (eachWorkerResponseCount.containsKey(manager.path()
						.toString()) == false) {
					eachWorkerResponseCount.put(manager.path()
							.toString(), new AtomicInteger(0));
				}
				if (eachWorkerRequestCount.containsKey(manager.path()
						.toString()) == false) {
					eachWorkerRequestCount.put(manager.path()
							.toString(), new AtomicInteger(0));
				}
			}
		}

		final JobInfo jobInfo = jobStatus.getJobInfo(directorJobId);
		final AtomicBoolean incomplete = new AtomicBoolean(false);
		ArrayList<Thread> queryList = new ArrayList<Thread>();

		for (final Future<Object> progress : progressList) {
			final ActorRef m = askMap.get(progress);
			Runnable temp = new Runnable() {
				@Override
				public void run() {
					try {
						slaveProgressMessage progressMessage = (slaveProgressMessage) Await
								.result(progress, duration);
						m.tell(new notCongestionSignal(), ActorRef.noSender());
						if (progressMessage.completed) {
							if (!collectedManager.containsKey(m)) {
								completedButNotCollectedManager.add(m);
								collectedManager.put(m, true);
							}
						} else {
							incomplete.getAndSet(true);
						}
						eachWorkerResponseCount.get(
								m.path().toString()).getAndAdd(
								progressMessage.responseCount);
						eachWorkerRequestCount.get(
								m.path().toString()).getAndAdd(
								progressMessage.requestCount);
						jobInfo.capacityUsage.put(
								m.path().address().toString(),
								progressMessage.capacityPercent);
					} catch (Exception e) {
						incomplete.getAndSet(true);
						System.out.println("Monitor query timeout.");
					}
				}
			};
			Thread tempThread = new Thread(temp);
			queryList.add(tempThread);
			tempThread.start();
		}

		for (Thread e : queryList) {
			try {
				e.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		for (Entry<String, AtomicInteger> e : eachWorkerResponseCount
				.entrySet()) {
			jobInfo.workerResponseCount.put(e.getKey(), e.getValue().get());
		}

		for (Entry<String, AtomicInteger> e : eachWorkerRequestCount.entrySet()) {
			jobInfo.workerRequestCount.put(e.getKey(), e.getValue().get());
		}

		eachWorkerResponseCount.clear();
		eachWorkerRequestCount.clear();

		if (!incomplete.get() && jobInfo.state == State.processing) {
			jobInfo.state = State.finishedNotGathered;
			jobInfo.finishedNotAggregatedTime = System.currentTimeMillis();
		}
	}
}
