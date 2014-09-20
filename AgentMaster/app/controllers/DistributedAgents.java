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
package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.Address;
import akka.cluster.Member;

import com.google.gson.Gson;

import models.data.AgentCommandMetadata;
import models.data.JsonResult;
import models.data.LogFile;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.providers.AgentDataProvider;
import models.data.providers.LogProvider;
import models.utils.AgentUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
import play.*;
import play.mvc.*;
import play.mvc.results.Result;
import play.mvc.results.RenderText;
import RemoteCluster.*;
import RemoteCluster.JobStatus.JobInfo;
/**
 * 
 * @author chunyang
 *
 */
public class DistributedAgents extends Controller{
	
	private static SupermanApp transformer;
	public static double tryCount = 0;
	
	public static void home() {
		String topnav = "distributedSuperman";
		render(topnav);
	}
	
	public static void index() {
		String page = "index";
		String topnav = "distributedSuperman";

		try {
			
			AgentDataProvider adp = AgentDataProvider.getInstance();

			List<NodeGroupSourceMetadata> nodeGroupSourceMetadataList = NodeGroupSourceMetadata
					.convertMapToList(adp.getNodegroupsourcemetadatas());

			String nodeGroupSourceMetadataListJsonArray = AgentUtils
					.toJson(nodeGroupSourceMetadataList);

			List<AgentCommandMetadata> agentCommandMetadataList = AgentCommandMetadata
					.convertMapToList(adp.getAgentcommandmetadatas());

			String agentCommandMetadataListJsonArray = AgentUtils
					.toJson(agentCommandMetadataList);
			
			render(page, topnav, nodeGroupSourceMetadataListJsonArray,
					agentCommandMetadataListJsonArray);
		} catch (Throwable t) {
			t.printStackTrace();
			renderJSON("Error occured in index of logs" + t.getMessage());
		}
	}
	
	public static void detail(String jobId) {
		String page = "detail";
		String topnav = "distributedSuperman";
		render(page, topnav, jobId);
	}
	
	public static void createNode(String port, String masterUrl) {
		//transformer = new TransformationApp(port, name);
		SupermanApp.initClusterSystem(port, masterUrl);
	}
	
	public static String enableClusterMember(String url, String port) {
		SupermanApp.enableClusterMember(url, port);
		return url + ":" + port + "has been enabled";
	}
	
	public static String disableClusterMember(String url, String port) {
		SupermanApp.disableClusterMember(url, port);
		return url + ":" + port + "has been disabled";
	}
	
	public static String getClusterList() {
		Map<String, ClusterState.State> clusterList = SupermanApp.getClusterList();
		if (clusterList==null)
			return "";
		Gson gson = new Gson();
		Map<String, List<String>> resList = new HashMap<String, List<String>>();
		List<String> urlList = new ArrayList<String>();
		List<String> stateList = new ArrayList<String>();
		List<String> enableList = new ArrayList<String>();
		for (Entry<String, ClusterState.State> e : clusterList.entrySet()) {
			urlList.add(e.getKey());
			stateList.add(new Boolean(e.getValue().available).toString());
			enableList.add(new Boolean(e.getValue().enable).toString());
		}
		List<String> masterUrl = new ArrayList<String>();
		masterUrl.add(SupermanApp.getMasterUrl());
		resList.put("url", urlList);
		resList.put("state", stateList);
		resList.put("enable", enableList);
		resList.put("masterUrl", masterUrl);
		return gson.toJson(resList);
	}
	
	public static void shutdownNode() {
		System.out.println("start to shutdown");
		transformer.shutdownSystem();
		System.out.println("Shut Down Complete");
		transformer = null;
	}
	
	public static void resetScheduler() {
		SupermanApp.resetScheduler();
	}
	
	public static String searchFqdn(String jobId, String fqdn) {
		if (SupermanApp.getJobInfo(jobId) !=null) {
			
			String nodeGroupType = SupermanApp.getJobInfo(jobId).nodeGroupType;
			String agentCommandType = SupermanApp.getJobInfo(jobId).agentCommandType;
			
			Map<String, NodeGroupDataMap> dataStore = null;
			dataStore = AgentDataProvider.allAgentData;
				
			if (dataStore.get(nodeGroupType) == null) {
				dataStore = AgentDataProvider.adhocAgentData;
			}
			if (dataStore.containsKey(nodeGroupType) 
					&& dataStore.get(nodeGroupType).getNodeGroupDataMapValidForSingleCommand(agentCommandType)!=null 
					&& dataStore.get(nodeGroupType).getNodeGroupDataMapValidForSingleCommand(agentCommandType).containsKey(fqdn))
				return AgentUtils.renderJson(dataStore.get(nodeGroupType).getNodeGroupDataMapValidForSingleCommand(agentCommandType).get(fqdn));
		}
		return "None exist!";
	}
	
	public static double queryProgress(String jobId) {
		//System.out.println("Query" + jobId);
		return SupermanApp.queryJobProgress(jobId);
	}
	
	public static String queryWorkerLoad(String jobId) {
		
		JobStatus.JobInfo jobInfo = SupermanApp.getJobInfo(jobId);
		if (jobInfo == null) return "";
		Map<String, Integer> responseMap = jobInfo.workerResponseCount;
		Map<String, Integer> requestMap = jobInfo.workerRequestCount;
		Gson gson  = new Gson();
		Map<String, ArrayList<String>> res = new HashMap<String, ArrayList<String>>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> loads = new ArrayList<String>();
		ArrayList<String> requests = new ArrayList<String>();
		ArrayList<String> activeOrPassive = new ArrayList<String>();
		String status = "";
		if (responseMap == null) return "";
		for(Entry<String, Integer> e : responseMap.entrySet()) {
			names.add(e.getKey());
			loads.add(e.getValue().toString());
			if (requestMap.containsKey(e.getKey()))
				requests.add(requestMap.get(e.getKey()).toString());
			else 
				requests.add("0");
			status = "passive";
			for (Entry<Address, AtomicInteger> m : ClusterState.memberLoad.entrySet()) {
				if (m.getKey().toString().endsWith(e.getKey())) {
					status = "active";
					break;
				}
			}
			activeOrPassive.add(status);
		}
		res.put("names", names);
		res.put("loads", loads);
		res.put("requests", requests);
		res.put("status", activeOrPassive);
		return gson.toJson(res).toString();
	}
	
	public static String queryClusterState() {
		Gson gson  = new Gson();
		Map<String, ArrayList<String>> res = new HashMap<String, ArrayList<String>>();
		ArrayList<String> names = new ArrayList<String>();
		//ArrayList<String> loads = new ArrayList<String>();
		JobStatus jobStatus = SupermanApp.getJobStatus();
		
		if (jobStatus == null)
			return "";

		for (Entry<Address, AtomicInteger> m : ClusterState.memberLoad.entrySet()) {
			names.add(m.getKey().toString());
			//loads.add(ClusterState.memberLoad.get(m).toString());
		}
		if (names.size() == 0) {
			names.add("akka://ClusterSystem");
		}
		res.put("names", names);
		//res.put("loads", loads);
		
		for (Entry<String, JobStatus.JobInfo> e: jobStatus.jobDict.entrySet()) {
			JobInfo info = e.getValue();
			if (info.state != JobStatus.State.gathered) {
				ArrayList<String> temp = new ArrayList<String>();
				
				for (String name : names) {
					boolean used = false;
					for (Entry<String, Double> ee : info.capacityUsage.entrySet()) {
						if (ee.getKey().contains(name)) {
							temp.add(String.valueOf(info.maxConcNum * info.capacityUsage.get(name)));
							used = true;
							break;
						}
					}
					if (!used)
						temp.add(String.valueOf(0));
				}
				
				res.put(e.getKey(), temp);
			}
		}
		return gson.toJson(res).toString();
	}
	
	public static String queryJobQ() {
		JobStatus jobStatus = SupermanApp.getJobStatus();
		
		if (jobStatus == null)
			return "";
		Gson gson  = new Gson();
		return gson.toJson(jobStatus.jobList).toString();
	}
	
	public static double queryElapsedTime(String jobId) {
		return SupermanApp.queryElapsedTime(jobId);
	}
	
	public static String queryState(String jobId) {
		switch(SupermanApp.getJobInfo(jobId).state) {
			case processing: return "processing";
			case gathered: return "gathered"; 
			case finishedNotGathered : return "finished not gathered"; 
			default : return "waiting";
		}
	}
	
	public static String queryJobInfo(String jobId) {
		Gson gson = new Gson();
		return gson.toJson(SupermanApp.getJobInfo(jobId));
	}
}
