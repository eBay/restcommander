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

import java.lang.Math;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Queue;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.GenericResponseFromDirector;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.asynchttp.actors.ActorConfig;
import models.asynchttp.actors.HttpWorker;
import models.asynchttp.actors.LogWorker;
import models.asynchttp.response.GenericAgentResponse;
import models.data.AgentCommandMetadata;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import models.data.providers.AgentDataProvider;
import models.data.providers.AgentDataProviderHelper;
import models.utils.DateUtils;
import models.utils.DistributedUtils;
import models.utils.VarUtils;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import controllers.Agents;
import RemoteCluster.JobStatus.State;
import RemoteCluster.CommunicationMessages.CheckSchedule;
import RemoteCluster.CommunicationMessages.queryMonitorProgressMessage;
import RemoteCluster.CommunicationMessages.endOfRequest;
import RemoteCluster.CommunicationMessages.gatherResponse;
import RemoteCluster.CommunicationMessages.JobInitInfo;
import RemoteCluster.CommunicationMessages.RestartNode;
import RemoteCluster.JobStatus;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Member;
import akka.dispatch.OnSuccess;
import akka.util.Timeout;
import scala.concurrent.duration.*;
import static akka.pattern.Patterns.ask;
import akka.actor.Scheduler;
import akka.actor.Deploy;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;

/**
 * 
 * @author chunyang
 *
 */
public class SupermanActorSystem {

	private ActorSystem system = null;
	private ActorRef personalG = null;
	private ActorRef scheduler = null;
	public JobStatus jobStatus = new JobStatus();
	public String masterUrl;
	public AtomicInteger localLoad;
	
	
	public SupermanActorSystem(String port, String masterUrl) {
    // Override the configuration of the port when specified as program argument
		final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
				withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
				withFallback(ConfigFactory.load("remoteactorconfig"));
		system = ActorSystem.create("ClusterSystem", config);
		localLoad = new AtomicInteger(0);
		
		this.masterUrl = masterUrl;
		ClusterState.memberStates.clear();
		List<String> temp = config.getStringList("akka.cluster.seed-nodes");
		String hostAddress = config.getString("akka.remote.netty.tcp.hostname");
		for (String actorAddress : temp) {
			Pattern pattern = Pattern.compile("@.+:");
			Matcher matcher = pattern.matcher(actorAddress);
			if (matcher.find()) {
				String url = matcher.group().substring(1, matcher.end()-matcher.start()-1);
				
				String eachPort = actorAddress.substring(actorAddress.length()-4);
				if (url.equals(hostAddress) && eachPort.equals(port))
					continue;
				ClusterState clusterState = new ClusterState();
				ClusterState.memberStates.put(url + ":" + eachPort, clusterState.new State(false, true));
				System.out.println("Member : " + url + ":" + eachPort);
			}
		}
		
		personalG = system.actorOf(Props.create(Guard.class, masterUrl), "guard");
		scheduler = system.actorOf(Props.create(JobScheduler.class, this), "scheduler");
		scheduler.tell(new CheckSchedule(), ActorRef.noSender());
		hostAddress += ":" + port;
		if (hostAddress.equals(masterUrl)) {
			System.out.println("I'm Master!");
			DistributedUtils.initHttpPort();
			personalG.tell(new RestartNode(), ActorRef.noSender());
		} else {
			System.out.println("I'm slave.");
		}
	}
	
	public void shutdownSystem() {
		if (personalG != null)
			system.stop(personalG);
		if (scheduler != null)
			system.stop(scheduler);
		system.shutdown();
		ClusterState.memberLoad.clear();
		ClusterState.memberStates.clear();
		System.out.println("Member cleared");
		system = null;
	}
	
	public void resetScheduler() {
		system.stop(scheduler);
		scheduler = null;
		List<JobStatus.JobInfo> newList = new ArrayList<JobStatus.JobInfo>();
		for (JobStatus.JobInfo info: jobStatus.jobList) {
			if (info.state != JobStatus.State.waiting) {
				newList.add(info);
			} else 
				jobStatus.jobDict.remove(info.jobId);
		}
		jobStatus.jobList = newList;
		scheduler = system.actorOf(Props.create(JobScheduler.class, this), "scheduler" + UUID.randomUUID().toString());
		scheduler.tell(new CheckSchedule(), ActorRef.noSender());
	}
	
	public GenericResponseFromDirector aysncCommandDirectorWrapper(
			final String nodeGroupType,  final String agentCommandType, final Map<String, NodeGroupDataMap> dataStore, 
			final boolean localMode, final boolean failOver, final int maxConcNum, final boolean asyncMode
			){
		final String directorJobUuid = UUID.randomUUID().toString();
		jobStatus.setJobInfo(directorJobUuid, nodeGroupType, agentCommandType);
		if (asyncMode) {
			
			scheduler.tell(new JobInitInfo(nodeGroupType, agentCommandType, localMode, failOver, dataStore, maxConcNum, asyncMode, directorJobUuid, dataStore.get(nodeGroupType).getNodeDataMapValid().size()), ActorRef.noSender());
			GenericResponseFromDirector uuid = new GenericResponseFromDirector();
			uuid.directorJobUuid = directorJobUuid;
			return uuid;
			 
		}
			
		scheduler.tell(new JobInitInfo(nodeGroupType, agentCommandType, localMode, failOver, dataStore, maxConcNum, asyncMode, directorJobUuid, dataStore.get(nodeGroupType).getNodeDataMapValid().size()), ActorRef.noSender());
		GenericResponseFromDirector uuid = new GenericResponseFromDirector();
		uuid.directorJobUuid = directorJobUuid;
		
		JobStatus.JobInfo jobInfo = jobStatus.getJobInfo(directorJobUuid);
		while (jobInfo!=null && jobInfo.state != JobStatus.State.gathered) {
			jobInfo = jobStatus.getJobInfo(directorJobUuid);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return uuid; 
	}
	
	public BatchResponseFromManager sendAgentCommandToManagerRemote(
			String nodeGroupType,  String agentCommandType, Map<String, NodeGroupDataMap> dataStore, 
			boolean localMode, boolean failOver, String directorJobUuid, int maxConcNum, Set<Address> nodeList) {
		if (system == null) return null;
		BatchResponseFromManager agentCommandResponseFromManager = null;
	
		AgentCommandMetadata agentCommandMetadata = AgentDataProvider.agentCommandMetadatas
				.get(agentCommandType);
		NodeGroupDataMap ngdm = dataStore.get(nodeGroupType);
					
		if (agentCommandMetadata == null) {
			models.utils.LogUtils
				.printLogError("!!ERROR in  sendAgentCommandToManager : "
							+ directorJobUuid
							+ " agentCommandType is NULL!! return. at "
							+ DateUtils.getNowDateTimeStr());
			return agentCommandResponseFromManager;
		}
					
		if (ngdm == null) {
			models.utils.LogUtils
					.printLogError("!!ERROR in  sendAgentCommandToManager : "
							+ nodeGroupType
							+ " NodeGroupDataMap is NULL!! return. at "
							+ DateUtils.getNowDateTimeStr());
			return agentCommandResponseFromManager;
		}
		
		models.utils.LogUtils
			.printLogNormal("Before Safety Check: total entry count: "
				+ ngdm.getNodeDataMapValid().size());
		Map<String, NodeData> nodeDataMapValidSafe = new HashMap<String, NodeData>();
		
		AgentDataProviderHelper.filterUnsafeOrUnnecessaryRequest(
				ngdm.getNodeDataMapValid(), nodeDataMapValidSafe,
				agentCommandType);
					
		if (localMode || nodeList.size() ==0 )
				localLoad.addAndGet(maxConcNum);
		
		/**
		 * Set jobInfo
		 */
		
		JobStatus.JobInfo jobInfo = jobStatus.getJobInfo(directorJobUuid);
		jobInfo.startTime = System.currentTimeMillis();
		jobInfo.state = State.processing;
		jobInfo.maxConcNum = maxConcNum;
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Timestamp now = new Timestamp(jobInfo.startTime);
		jobInfo.timeStamp = df.format(now);
				
		int totJobNum = nodeDataMapValidSafe.size();
		jobInfo.totJobNum = totJobNum;
		/**
		 * Prepare for create manager
		 */
		
		List<ActorRef> managerList = new ArrayList<ActorRef>();
		Queue<Integer> jobQ = new ConcurrentLinkedQueue<Integer>();
		
		/**
		 * @author chunyang
		 * Create Managers locally or remotely
		 */
		
		ActorRef agentCommandManager = null;
		
		if (localMode || nodeList.size()==0) {
			agentCommandManager =  system.actorOf(
					Props.create(LocalManager.class),"AgentCommandManager-" + UUID.randomUUID().toString()
				);
			jobQ.offer(managerList.size());
			managerList.add(agentCommandManager);
			localMode = true;
		}
		else {
			for (Address m : nodeList) {
				agentCommandManager = system.actorOf(
					Props.create(LocalManager.class).withDeploy(
							new Deploy(
					 				new RemoteScope(
											m
											)
									)),
					"AgentCommandManager-" + UUID.randomUUID().toString()
				);
				
				jobQ.offer(managerList.size());
				managerList.add(agentCommandManager);
				ClusterState.memberLoad.get(m).addAndGet(maxConcNum);
			}
		}
		
		/**
		 * Dispatch Jobs
		 * @author chunyang
		 */
		if (!localMode) {
			List<Map<String, NodeData>> jobGroupList = partDataStore(nodeDataMapValidSafe, nodeList.size()==0?
					totJobNum:Math.min(totJobNum/nodeList.size()+1, 1000));
			List<ActorRef> dispatcherList = new ArrayList<ActorRef>();
			int requestChunckSize = jobGroupList.size()/managerList.size() + 1; // Last one do less
			
			for (int i=0; i<Math.min(3, managerList.size()) ; i++) {
				dispatcherList.add(
						system.actorOf(
								Props.create(JobDispatcher.class, managerList, jobGroupList, jobQ, requestChunckSize,
										nodeGroupType, agentCommandType, directorJobUuid, maxConcNum)
								)
						);
			}
			
			for (ActorRef dispatcher : dispatcherList) {
				dispatcher.tell("start dispatching", null);
			}
		} else {
			Map<String, NodeGroupDataMap> totDataStore = new HashMap<String, NodeGroupDataMap>();
			totDataStore.put(nodeGroupType, new NodeGroupDataMap(nodeGroupType));
			totDataStore.get(nodeGroupType).setNodeDataMapValid(nodeDataMapValidSafe);
			Future<Object> ackFuture = Patterns.ask(managerList.get(0), new InitialRequestToManager(nodeGroupType,
					agentCommandType, directorJobUuid, totDataStore, true, false, maxConcNum), new Timeout(Duration.create(
							15, TimeUnit.SECONDS)));
			try {
				Await.result(ackFuture,  Duration.create(
						15, TimeUnit.SECONDS));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			managerList.get(0).tell(new endOfRequest(), null);
		}
		
		ActorRef monitor = system.actorOf(Props.create(Monitor.class, managerList, directorJobUuid, jobStatus), "ProgressMonitor" + directorJobUuid);
		
		ActorRef collector = system.actorOf(Props.create(ResponseCollector.class, jobInfo), "ResultCollector" + directorJobUuid);
		
		final FiniteDuration gatherResponseDuration = Duration.create(
				3600, TimeUnit.SECONDS);
		
		/**
		 * Gather Result.
		 */
		Future<Object> totResponse = Patterns.ask(collector, new gatherResponse(monitor, totJobNum), new Timeout(gatherResponseDuration));
		
		BatchResponseFromManager responseFromCollecter = null;
		try {
			responseFromCollecter = (BatchResponseFromManager) Await.result(totResponse, gatherResponseDuration);
			System.out.println("Gather Result Back! : " + responseFromCollecter.responseMap.size());
			/**
			 * Slave Fail Over
			 */
			int failCount = 3;
			while (responseFromCollecter.responseMap.size() < totJobNum && failCount >= 0) {
				System.out.println("Response less than request, fail over @@");
				failCount -- ;
				Map<String, NodeData> failOverMap = gatherFailOverData(nodeDataMapValidSafe, responseFromCollecter);
				
				List<Address> failOverNodeList = new ArrayList<Address>();
				
				int failOverTot = failOverMap.size();
				
				for (Address m : nodeList) {
					if (ClusterState.memberLoad.containsKey(m)) {
						failOverNodeList.add(m);
						failOverTot -= 2000;
						if (failOverTot < 0)
							break;
					}
				}	
				
				
				List<ActorRef> failOverManagerList = new ArrayList<ActorRef>();
				Queue<Integer> failOverJobQ = new ConcurrentLinkedQueue<Integer>();
				
				if (localMode || failOverNodeList.size()==0) {
					agentCommandManager =  system.actorOf(
							Props.create(LocalManager.class),"AgentCommandManager-" + UUID.randomUUID().toString()
						);
					failOverJobQ.offer(failOverManagerList.size());
					failOverManagerList.add(agentCommandManager);
					managerList.add(agentCommandManager);
					localMode = true;
				}
				else {
					for (Address m : failOverNodeList) {
						agentCommandManager = system.actorOf(
							Props.create(LocalManager.class).withDeploy(
									new Deploy(
							 				new RemoteScope(
													m
													)
											)),
							"AgentCommandManager-" + UUID.randomUUID().toString()
						);
						
						failOverJobQ.offer(failOverManagerList.size());
						failOverManagerList.add(agentCommandManager);
						managerList.add(agentCommandManager);
					}
				}
				if (!localMode) {
					List<Map<String, NodeData>> failOverJobGroupList = partDataStore(failOverMap, failOverNodeList.size()==0?
							failOverMap.size():Math.min(failOverMap.size()/failOverNodeList.size()+1, 1000));
					List<ActorRef> failOverDispatcherList = new ArrayList<ActorRef>();
					int failOverRequestChunckSize = failOverJobGroupList.size()/failOverManagerList.size() + 1; // Last one do less
					
					for (int i=0; i<Math.min(3, failOverManagerList.size()) ; i++) {
						failOverDispatcherList.add(
								system.actorOf(
										Props.create(JobDispatcher.class, failOverManagerList, failOverJobGroupList, failOverJobQ, failOverRequestChunckSize,
												nodeGroupType, agentCommandType, directorJobUuid, maxConcNum)
										)
								);
					}
					
					for (ActorRef failOverDispatcher : failOverDispatcherList) {
						failOverDispatcher.tell("start dispatching", null);
					}
				} else {
					Map<String, NodeGroupDataMap> failOverDataStore = new HashMap<String, NodeGroupDataMap>();
					failOverDataStore.put(nodeGroupType, new NodeGroupDataMap(nodeGroupType));
					failOverDataStore.get(nodeGroupType).setNodeDataMapValid(failOverMap);
					Future<Object> ackFuture = Patterns.ask(failOverManagerList.get(0), new InitialRequestToManager(nodeGroupType,
							agentCommandType, directorJobUuid, failOverDataStore, true, false, maxConcNum), new Timeout(Duration.create(
									15, TimeUnit.SECONDS)));
					try {
						Await.result(ackFuture,  Duration.create(
								15, TimeUnit.SECONDS));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					failOverManagerList.get(0).tell(new endOfRequest(), null);
				}
				
				jobInfo.state = State.processing;
				
				
				Future<Object> futureFailOverResponse = Patterns.ask(collector, new gatherResponse(monitor, totJobNum), new Timeout(gatherResponseDuration));
				BatchResponseFromManager failOverResponse = (BatchResponseFromManager) Await.result(futureFailOverResponse, gatherResponseDuration);
				System.out.println("FailOver Result Size" + failOverResponse.responseMap.size());
				for (Entry<String, GenericAgentResponse> e : failOverResponse.responseMap.entrySet()) {
					responseFromCollecter.responseMap.put(e.getKey(), e.getValue());
				}
			}
			
			for (Entry<String, GenericAgentResponse> e: responseFromCollecter.getResponseMap().entrySet()) {
				AgentDataProviderHelper.getInstance()
				.updateResponseFromAgentGenericResponse(nodeGroupType,
						agentCommandType, e.getValue(), dataStore);
			}
		} catch (Exception e) {
			System.out.println("Response Collector Timeout");
			responseFromCollecter = new BatchResponseFromManager();
		}
		
		jobInfo.endTime = System.currentTimeMillis();
		jobInfo.aggregationTime = (jobInfo.endTime - jobInfo.finishedNotAggregatedTime)/1000.0;
		jobInfo.state = State.gathered;
		
		System.out.println("Clear actors.");
		
		system.stop(monitor);
		system.stop(collector);
		
		for (ActorRef m : managerList) {
			system.stop(m);
		}
		
		if (localMode || nodeList.size() ==0 )
			localLoad.addAndGet( -maxConcNum);
		
		for (Address m : nodeList) {
			if (ClusterState.memberLoad.containsKey(m))
				ClusterState.memberLoad.get(m).addAndGet(-maxConcNum);
		}
		
		final FiniteDuration durationLogWorker = Duration.create(
				VarUtils.TIMEOUT_ASK_LOGWORKER_SCONDS, TimeUnit.SECONDS);
		
		ActorRef logWorker = ActorConfig.getActorSystem().actorOf(
				Props.create(LogWorker.class,
						nodeGroupType, agentCommandType, dataStore, directorJobUuid),
				"LogWorker-" + UUID.randomUUID().toString()
			);
			
		/**
		 * TODO 20140515: this log worker has internally timeout; and will
		 * suicide if timeout; so should no need to have external timeout
		 * check/kill again
		 * 
		 */
		Patterns.ask(logWorker, HttpWorker.MessageType.PROCESS_REQUEST,
				new Timeout(durationLogWorker));
		return responseFromCollecter;
	}
	
	private List<Map<String, NodeData>> partDataStore(Map<String, NodeData> nodeGroupDataMap, int size) {
		List<Map<String, NodeData>> res = new ArrayList<Map<String, NodeData>>();
		Map<String, NodeData> temp = new HashMap<String, NodeData>();
		
		for (Entry<String, NodeData> e : nodeGroupDataMap.entrySet()) {
			temp.put(e.getKey(), e.getValue());
			if (temp.size()>=size) {
				res.add(temp);
				temp = new HashMap<String, NodeData>();
			}
		}
		if (temp.size()>0) 
			res.add(temp);
		return res;
	}
	
	private Map<String, NodeData> gatherFailOverData(Map<String, NodeData> nodeDataMapValidSafe, BatchResponseFromManager oriResponse) {
		Map<String, NodeData> failoverMap = new HashMap<String, NodeData>();
		for (Entry<String, NodeData> e : nodeDataMapValidSafe.entrySet()){
			if (!oriResponse.responseMap.containsKey(e.getKey())) {
				failoverMap.put(e.getKey(), e.getValue());
			}
		}
		return failoverMap;
	}
}
