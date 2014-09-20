Distributed Version R
============

[Overview](#a_overview) | [New Features](#a_new_features) |  [Terminoloy](#a_terminology) |  [New APIs](#a_new_apis) | [Deployment](#a_deployment) 
<a name="a_overview"></a>
## Overview
HTTP/REST/SOAP client as a service.

* Generic discovery, monitoring, software deployment, configuration management
* Fast and scalable concurrent task execution on 100,000+ machines (HTTP and SSH).
* Restructure via AKKA Cluster to make it horizontally scalable.

<a name="a_new_features"></a>
##New Features
### **Speed**

* By restructuring REST Commander to make it distributed and horizontally scalable, it can send request to **100K+** machines in eBay's cloud and gather result back in just **100s** using **5 VM**.

### **Reliability**

* Failover automatically to other slave nodes in case of a failure on slave node.  
* Slave nodes can also automatically adjust speed of sending request to handle network congestion between master and slave.
* One HTTP call to restart the whole distributed cluster and assign new master.

### **Scheduling**

* Since most tasks are quite small, we provide scheduling to handle multitasks to fully use capacity and ensure reliability at the same time.
* Just uses greedy scheduling algorithm in a first-come-first-serve way.

### **Visibility**

* Real time show workload and available capacity of each slave node.
* Real time track task progress.
* Search response according to FQDN.

<a name="a_terminology"></a>
## Terminology

### Job / Task

* Job and task are the same. The input of them is a group of target nodes and a group of commands to them.  Distributed REST Commander will send command to each target node and gather result back.

### Basic Workflow & Architecture

 * Distributed REST Commander adopts master-slave architect.  The basic workflow is as following:
	 1. If there is enough capacity to execute the first task in scheduling job queue, the master node will pop it and dispatch the task to slaves evenly. Otherwise, the task will wait to be executed until there is enough capacity.
	 2. Each slave will start to execute the task until it receive all its part of job from master.
	 3. After finishing all its part of job, the slave will send response to master.

### Job Status
* Waiting : the job is in scheduling queue waiting to be executed.
* Processing : the job is processing now.
* FinishedNotGathered : all the slaves have finished the job and are sending response back to master.
* Gathered : the master have gathered all the result back.

### Capacity & Max Concurrency

* Empirically, we found that each slave (large size, 4 core) can process 2500 HTTP connection at the same time.  Thus, we define the total available capacity of each slave is 2500.  The idea of capacity or max concurrency is similar to throughput.
* For each task we can define max concurrency, which means that how many HTTP request the slave can process at the same time for this task.  Because slave can automatically adjust throttling to handle network congestion, the actual concurrency may be less than the max concurrency.  Moreover, since the total capacity of each slave is 2500, the sum of max concurrency of all the jobs processing on a slave cannot be larger than 2500.  If there is no slave that has available capacity larger or equal to max concurrency of this job, the job will be waiting until there is at least one slave which have enough available capacity.

<a name="a_new_apis"></a>
##New APIs
All APIs can use both GET and POST.  All old version APIs of REST Commander also work for distributed version.
### New API List
* [Execute Job & Track Progress](#a_job_apis)
	* /commands/generateUpdateSendAgentCommandToNodeGroupForAsyncPolling
	* /distributedAgents/queryJobInfo
	* /distributedAgents/queryJobQ
	* /distributedAgents/searchFqdn
* [Cluster Managerment](#a_cluster_apis)
	* /distributedAgents/shutdownNode
	* /distributedAgents/createNode
	* /distributedAgents/enableClusterMember
	* /distributedAgents/disableClusterMember
	* /distributedAgents/getClusterList
	* /distributedAgents/resetScheduler

<a name="a_job_apis"></a>
### Execute Job & Track Progress APIs
#### /commands/generateUpdateSendAgentCommandToNodeGroupForAsyncPolling
* This API is used to process a task with predefined nodegroup and return a job ID. Use this job ID, user can track the progress of the task.

* Request:
```json
{
  "nodeGroupType": "node_list", 
  "agentCommandType": "GET_VI",
  "localMode": false,
  "failOver": true,
  "maxConcNum": 500
}
```
* Response:
```json
  6c4b8302-4370-46c1-a4ab-008c1e1ddddd
```
#### /distributedAgents/queryJobInfo
* Use job ID to get the progress.
* Request:
```json
{
  "jobId": "6c4b8302-4370-46c1-a4ab-008c1e1ddddd"
}
```
* Response:
```json
{
  "timeStamp": "2014-09-15 02:32:43",
  "startTime": 1410748363828,
  "finishedNotAggregatedTime": 1410748430246,
  "endTime": 1410748434311,
  "aggregationTime": 4.065,
  "totJobNum": 40000,
  "workerRequestCount": {
    "akka.tcp://ClusterSystem@slave1*": 10000,
    "akka.tcp://ClusterSystem@slave2*": 10000,
    "akka.tcp://ClusterSystem@slave3*": 10000,
    "akka.tcp://ClusterSystem@slave4*": 10000
  },
  "workerResponseCount": {
    "akka.tcp://ClusterSystem@slave1*": 10000,
    "akka.tcp://ClusterSystem@slave2*": 10000,
    "akka.tcp://ClusterSystem@slave3*": 10000,
    "akka.tcp://ClusterSystem@slave4*": 10000
  },
  "capacityUsage": {
    "akka.tcp://ClusterSystem@slave1:2555": 1,
    "akka.tcp://ClusterSystem@slave2:2551": 1,
    "akka.tcp://ClusterSystem@slave3:2553": 1,
    "akka.tcp://ClusterSystem@slave4:2554": 1
  },
  "state": "gathered",
  "collectedNum": 40000,
  "maxConcNum": 2000,
  "jobId": "6c4b8302-4370-46c1-a4ab-008c1e1ddddd",
  "nodeGroupType": "node_List",
  "agentCommandType": "GET_VI"
}
```
#### /distributedAgents/queryJobQ

* Get the entire job queue.  The state of task in the queue can be waiting, processing, finishedNotGathered or gathered.

* Response:
```json
[
	{
	  "timeStamp": "2014-09-15 02:32:43",
	  "startTime": 1410748363828,
	  "finishedNotAggregatedTime": 1410748430246,
	  "endTime": 1410748434311,
	  "aggregationTime": 4.065,
	  "totJobNum": 40000,
	  "workerRequestCount": {
	     "akka.tcp://ClusterSystem@slave1*": 10000,
	    "akka.tcp://ClusterSystem@slave2*": 10000,
	    "akka.tcp://ClusterSystem@slave3*": 10000,
	    "akka.tcp://ClusterSystem@slave4*": 10000
	  },
	  "workerResponseCount": {
	     "akka.tcp://ClusterSystem@slave1*": 10000,
	    "akka.tcp://ClusterSystem@slave2*": 10000,
	    "akka.tcp://ClusterSystem@slave3*": 10000,
	    "akka.tcp://ClusterSystem@slave4*": 10000
	  },
	  "capacityUsage": {
	    "akka.tcp://ClusterSystem@slave1:2555": 1,
	    "akka.tcp://ClusterSystem@slave2:2551": 1,
	    "akka.tcp://ClusterSystem@slave3:2553": 1,
	    "akka.tcp://ClusterSystem@slave4:2554": 1
	  },
	  "state": "gathered",
	  "collectedNum": 40000,
	  "maxConcNum": 2000,
	  "jobId": "6c4b8302-4370-46c1-a4ab-008c1e1ddddd",
	  "nodeGroupType": "NODE_LIST",
	  "agentCommandType": "GET_VI"
	},
	{
	  "timeStamp": "2014-09-15 02:32:43",
	  "startTime": 1410748363828,
	  "finishedNotAggregatedTime": 1410748430246,
	  "endTime": 1410748434311,
	  "aggregationTime": 4.065,
	  "totJobNum": 40000,
	  "workerRequestCount": {
	     "akka.tcp://ClusterSystem@slave1*": 10000,
	    "akka.tcp://ClusterSystem@slave2*": 10000,
	    "akka.tcp://ClusterSystem@slave3*": 10000,
	    "akka.tcp://ClusterSystem@slave4*": 10000
	  },
	  "workerResponseCount": {
	     "akka.tcp://ClusterSystem@slave1*": 10000,
	    "akka.tcp://ClusterSystem@slave2*": 10000,
	    "akka.tcp://ClusterSystem@slave3*": 10000,
	    "akka.tcp://ClusterSystem@slave4*": 10000
	  },
	  "capacityUsage": {
	    "akka.tcp://ClusterSystem@slave1:2555": 1,
	    "akka.tcp://ClusterSystem@slave2:2551": 1,
	    "akka.tcp://ClusterSystem@slave3:2553": 1,
	    "akka.tcp://ClusterSystem@slave4:2554": 1
	  },
	  "state": "gathered",
	  "collectedNum": 40000,
	  "maxConcNum": 2000,
	  "jobId": "59d23705-e420-4f74-8972-ea55955be752",
	  "nodeGroupType": "NODE_LIST",
	  "agentCommandType": "GET_VI"
	}
]
```
#### /distributedAgents/searchFqdn
* Search response according to FQDN and job ID.

* Request:
```json
{
  "jobId": "59d23705-e420-4f74-8972-ea55955be752", 
  "fqdn": "127.0.0.1", 
}
```
* Response:
```json
{
  "clusterId": "cluster-uuid"
}
```

<a name="a_cluster_apis"></a>
### Cluster Management APIs
#### /distributedAgents/shutdownNode
* Shut down actor system on the node.
* Response:
```json
200
```
#### /distributedAgents/createNode
* Create actor system on the node.  "masterUrl" should be the master node's Url and its AKKA port.  "port" is this node's AKKA port.
* Request:
```json
{
  "port": 2551, 
  "masterUrl": "127.0.0.1:2551", 
}
```
* Response:
```json
200
```
#### /distributedAgents/enableClusterMember
* If we enbale a member in the cluster, the master node will periodically check its status and restart it if it is removed.
* Request:
```json
{
  "url": "127.0.0.1", 
  "port": 2551, 
}
```
* Response:
```json
"127.0.0.1:2551 has been enabled."
```
#### /distributedAgents/disableClusterMember
* If disable a member in the cluster, the master node will not periodically check its status or restart it.
* Request:
```json
{
  "url": "127.0.0.1", 
  "port": 2551, 
}
```
* Response:
```json
"127.0.0.1:2551 has been disabled."
```
#### /distributedAgents/getClusterList
* Get the members' list which includes their states.
* Response:
```json
{
	"masterUrl": [
		"superman2:2552"
	],
	"enable": [
		"true",
		"true",
		"true",
		"true"
	],
	"state": [
		"true",
		"true",
		"true",
		"true"
	],
	"url": [
		"superman1:2551",
		"superman5:2555",
		"superman4:2554",
		"superman3:2553"
	]
}
```
#### /distributedAgents/resetScheduler
* Clear all the tasks which are waiting to be processed.
* Response:
```json
200
```

<a name="a_deployment"></a>
##Deployment
### Config File
To use distributed REST Commander we need to add a config file at "/conf/remoteactorconfig.conf".
For example:
```
akka {
	version = "2.3.3"
	event-handlers = ["akka.event.Logging$DefaultLogger"]
	event-handler-startup-timeout = 300s
	stdout-loglevel = "DEBUG"
	actor {
		provider = "akka.cluster.ClusterActorRefProvider"
		serializers {
	      java = "akka.serialization.JavaSerializer"
	      proto = "akka.remote.serialization.ProtobufSerializer"
	    }
	    serialization-bindings {
	      "java.lang.String" = java
	      "java.lang.Boolean" = java
	      "models.asynchttp.response.GenericAgentResponse" = java
	    }
	}
	remote {
		log-remote-lifecycle-events = off
		netty.tcp {
			hostname = "127.0.0.1"	
		}
		
	}
	cluster {
		seed-nodes = [
			"akka.tcp://ClusterSystem@127.0.0.1:2551",
			"akka.tcp://ClusterSystem@127.0.0.1:2552",
			"akka.tcp://ClusterSystem@127.0.0.1:2553"
			]
		auto-down-unreachable-after = 60s
	}
	supermanHTTPPort {
		akkaAddress = [
			"127.0.0.1:2551",
			"127.0.0.1:2552",
			"127.0.0.1:2553"
		]
		httpPort = [
			"9000",
			"9001",
			"9002"
		]
	}
}
```
**Things need to be modified:**

* akka.remote.netty.tcp.hostname needs to be changed to each machine's fqdn.
* akka.cluster.seed-nodes needs to be changed to include all nodes' url and port as example above.
* akka.supermanHTTPPort.akkaAddress needs to be changed to include all nodes' url and AKKA port as example above.
* akka.supermanHTTPPort.httpPort needs to be changed to include all nodes' url and HTTP port as example above.  Moreover, the order of this should be corresponding to akka.supermanHTTPPort.akkaAddress.  In other words, we need to correspond nodes' AKKA port and HTTP port.
* The reason we need use akka.supermanHTTPPort.httpPort is to support multi instance local test.  For deployment in multi machines, we can just write all httpPort as "8080".  Moreover, for deployment in multi machines, all the akka port number can be the same, such as "2551".

### One Call Build Cluster
1. Change config file according to different machines' fqdn.
2. Install REST Commander on all your machines.
3. Call API /distributedagents/createnode to the master node.  This call will trigger master to send http call to other slave nodes to join the cluster.
4. Wait slaves to join the cluster.

