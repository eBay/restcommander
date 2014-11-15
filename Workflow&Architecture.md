
Workflow & Architecture : restructure via AKKA to achieve horizontal scalability
==========
[Overview](#a_overview) | [Actors](#a_actors) |  [Messages](#a_messages)

<a name="a_overview"></a>
##Overview

**The main workflow of distributed REST Commander is shown as below.**

![Work Flow](/workflow.jpg)

**Distributed REST Commander is based on AKKA.  Each functionality component in the pictrue above is implemented as an AKKA actor (except Job Manager).  The whole system is based on message passing model.  In the following sections, we will introduce each actor and messages used by actors to communicate.**

<a name="a_actors"></a>
##Actors

* [Actors Run on Master Node](#a_actors_of_master)
	* Job Scheduler
	* Guard
	* Job Manager (not real actor)
	* Monitor
	* Job Dispatcher
	* Response Collector
	* Individual Collector
* [Actors Run on Salve Node](#a_actors_of_slave)
	* Local Manager
	* Worker
	* Assistant Manager

<a name="a_actors_of_master"></a>
###Actors Run on Master node

#### Job Scheduler

* Job Scheduler maintains job queue.  When user submits a task, it will be pushed to this queue.
* Every 0.5s, Job Scheduler will check the head of this queue.  If at least one slave have enough capacity to execute this job,  Job Scheduler will pop it and send it to Job Manager.  Otherwise, Job Scheduler will not pop it.
* The life span of Job Scheduler is the same as Superman Cluster.

#### Guard

* One of Guard's responsibility is to maintain current state of the superman cluster
* Guard listens to message when node join cluster and leave cluster and modify the state of cluster.
* Guard's another important responsibility is to restart nodes who is enabled and currently left cluster. At the beginning of building cluster, Guard will restart all the other nodes to trigger them join the cluster.
* The life span of Guard is the same as Superman Cluster.

#### Job Manager (not real actor)

* Job Manager is not a real actor.  It's just function.  In this function, it will implement the major workflow of a single job.  
* Moreover, it will also set job information and create all necessary actors and stop them when the job is finished.  As a result, all the actors created by Job Manager has the same life as the job.
* The actors created by Job Manager are : Monitor, Job Dispatcher, Response Collector, Local Manager.

#### Monitor

* Monitor is created by Job Manager at the beginning of the job and stopped when the job is finished.  The major functions of Monitor are as following:
	* Query Local Manager for job progress every 0.5s.  When all the Local Mangers which has not left the cluster have finished their part of job,  Monitor will change job state from processing to finishedNotGathered.
	* Give Response Collector a list of Local Manager which has finished the job but has not given the response back to Response Collector.

#### Job Dispatcher

* Job Dispatchers are created by Job Manager which responsibility is to dispatch job information to Local Managers.
* Each Job Dispatcher will send job to Local Manger chunk by chunk in a synchronized way, which means that after Job Dispatcher sends a chunk of job to Local Manager it will not send next chunk until receive acknowledgement from Local Manager.  The reason why Job Dispatcher needs to send job in this way is that we need avoid network congestion.  If Job Dispatcher sends all the job once, it will cause network congestion which will cause web service of REST Commander not responding and may cause AKKA Remote error, since the message may be too large.  If Job Dispatcher sends job asynch, it will also cause network congestion.  Thus, we need send job to Local Manager under control using a synchronized chunk by chunk way.
* Job Manager will create at most 3 Job Dispatchers to dispatch job.  The reason why I use multi Job Dispatchers is that because we send job to Local Manager in a synchronized way, the speed to send job may be too slow and the network card will be idle when waiting the acknowledgement from Local Manager.  When we use multi Job Dispatchers, they can use network card in turn and avoid network card idle.

#### Response Collector

* Response Collector is created by Job Manager and the responsibility of it are as following:
	* Create Individual Collectors.
	* Query Monitor to get Local Managers which have finished the job but have not given response back.  After Response Collector get those Local Managers, it will tell Individual Collectors to collect response from them.
	* When there is no Local Managers to collect response and the job state is finishedNotGathered, it will aggregate the result and send it back to Job Manager.

#### Individual Collector

* Individual Collectors are created by Response Collector.  It will collect response from Local Manager chunk by chunk in a synchronized way.
* The reason why we collect response chunk by chunk in a synchronized way is that we need to collect response under control to avoid network congestion and AKKA Remote error.
* The reason why we use multi Individual Collectors is that we need to speed up the process by avoiding network card idle.

<a name="a_actors_of_slave"></a>
###Actors Run on Slave node

Major function of these actors below are the same as before.  The new one is dynamically change throttling according to network condition.

#### Local Manager

* Local Managers are created by Job Manager remotely.  Its responsibility is to create Assistant Manager, gather result from workers, dynamically change throttling and interaction between other actors introduced above.

#### Worker

* Workers are created by Assistant Manager.  Each worker will execute one small task.  It will send a HTTP request to a single node.

#### Assistant Manager

* Create Workers and trigger them to execute with throttling.


<a name="a_messages"></a>
##Messages

#### RestartNode

* Sent by Guard to itself.  **Purpose**: trigger Guard to restart nodes which are enabled and currently left cluster every 2s.

#### initOpMessage

* Sent by Assistant Manager to Worker.  **Purpose**: create Worker with job information.

#### querySlaveProgressMessage

* Sent by Monitor to Local Manager.  **Purpose**: query Local Manager progress.

#### slaveProgressMessage

* Sent by Local Manager to Monitor. **Purpose**: progress information of Local Manager.

#### queryMonitorProgressMessage

* Sent by Monitor to itself.  **Purpose**: trigger Monitor to query progress to Local Managers every 0.5s.

#### endOfRequest

* Sent by Job Dispatcher to Local Manager. **Purpose**: tell Local Manager all job has been sent, it can start to execute job.

#### gatherResponse

* Sent by Job Manager to Response Collector. **Purpose**: trigger Response Collector to initialize and create Individual Collectors.

#### collecterCheckResponse

* Sent by Response Collector to itself.  **Purpose**: trigger Response Collector to ask Monitor for Local Managers which have finished job but not collected every 2s.

#### queryMonitorForResponse

* Sent by Response Collector to Monitor. **Purpose**: get list of Local Managers which have finished job but not given response back.

#### streamResquestToManager

* Sent by Individual Collector to Local Manager. **Purpose**: ask Local Manager for response.

#### JobInitInfo

* Sent by Superman Actor System to Job Scheduler.  **Purpose**: put job info in to job scheduling queue.

#### CheckSchedule

* Sent by Job Scheduler to itself.  **Purpose**: trigger Job Scheduler to check whether there is enough capacity to execute job every 0.5s.

#### ResetMaxConc

* Sent by Local Manager to Assistant Manager. **Purpose**: dynamically change throttling.

#### congestionMessage

* Sent by Local Manager to itself.  **Purpose**: trigger Local Manager to check the condition of network congestion and dynamically change throttling according to that.

#### notCongestionSignal

* Sent by Monitor to Local Manger. **Purpose**: tell Local Manager there is no network congestion between Master and itself.
