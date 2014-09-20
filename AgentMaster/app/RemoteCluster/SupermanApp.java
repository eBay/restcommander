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
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import scala.concurrent.Await;
import scala.concurrent.Future;
import akka.pattern.Patterns;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.GenericResponseFromDirector;
import models.data.NodeGroupDataMap;
import models.utils.ConfUtils;
import RemoteCluster.CommunicationMessages.queryMonitorProgressMessage;

/**
 * 
 * @author chunyang
 *
 */

public final class SupermanApp {

	private static SupermanActorSystem TransformLocalSystem = null;
	public static String SystemPort = "";
  
	public static synchronized void initClusterSystem(String port, String masterUrl) {
		if(!isClusterExist()) {	
			TransformLocalSystem = new SupermanActorSystem(port, masterUrl);
			SystemPort = port;
			// Sleep to detect all actor systems. 
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void shutdownSystem() {
		if (TransformLocalSystem != null)
			TransformLocalSystem.shutdownSystem();
		TransformLocalSystem = null;
	}
	
	public static void enableClusterMember(String url, String port) {
		if (ClusterState.memberStates.containsKey(url + ":" + port))
			ClusterState.memberStates.get(url + ":" + port).enable = true;
	}
	
	public static void disableClusterMember(String url, String port) {
		if (ClusterState.memberStates.containsKey(url + ":" + port))
			ClusterState.memberStates.get(url + ":" + port).enable = false;
	}
	
	public static void resetScheduler() {
		if (TransformLocalSystem!=null)
			TransformLocalSystem.resetScheduler();
	}
	
	public static Map<String, ClusterState.State> getClusterList() {
		if (isClusterExist()) {
			return ClusterState.memberStates;
		} else 
			return null;
	}
	
	public static String getMasterUrl() {
		if (isClusterExist()) {
			return TransformLocalSystem.masterUrl;
		} else 
			return "";
	}
	
	public static GenericResponseFromDirector sendAgentCommandToManager(
			String nodeGroupType,  String agentCommandType, Map<String, NodeGroupDataMap> dataStore, boolean localMode, boolean failOver, int maxConcNum, boolean asyncMode) {
		return TransformLocalSystem.aysncCommandDirectorWrapper(nodeGroupType, agentCommandType, dataStore, localMode, failOver, maxConcNum, asyncMode);
	}
	
	public static boolean isClusterExist(){
		return (TransformLocalSystem != null);
	}
	
	public static double queryJobProgress(String jobId) {
		if (TransformLocalSystem == null)
			return 0;
		else {
			JobStatus.JobInfo jobInfo = TransformLocalSystem.jobStatus.getJobInfo(jobId);
			if ( jobInfo == null)
				return 0;
			if (jobInfo.collectedNum >= jobInfo.totJobNum && jobInfo.totJobNum > 0)
					return 1;
			int totResponse = 0;
			for (Entry<String, Integer> e : jobInfo.workerResponseCount.entrySet()) {
					totResponse += e.getValue();
			}
			return (double)(totResponse)/jobInfo.totJobNum;
		}
	}
	
	public static double queryElapsedTime(String jobId) {
		if (TransformLocalSystem == null)
			return 0;
		else 
			return (System.currentTimeMillis() - TransformLocalSystem.jobStatus.getJobInfo(jobId).startTime)/1000.0;	
	}
	
	public static JobStatus.JobInfo getJobInfo(String jobId) {
		if (TransformLocalSystem != null)
			return TransformLocalSystem.jobStatus.getJobInfo(jobId);
		else 
			return null;
	}
	
	public static JobStatus getJobStatus() {
		if (TransformLocalSystem != null)
			return TransformLocalSystem.jobStatus;
		else 
			return null;
	}
}
