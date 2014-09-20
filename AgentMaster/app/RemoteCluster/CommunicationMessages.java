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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.agent.batch.commands.message.InitialRequestToManager;
import models.data.NodeData;
import models.data.NodeGroupDataMap;
import akka.actor.ActorRef;
import akka.actor.Address;
/**
 * 
 * @author chunyang
 *
 */
//#messages
public interface CommunicationMessages {
	
	public class RestartNode implements Serializable {
		
	}
	
	public class initOpMessage implements Serializable {
		public String host;
		public String hostUniform;  
		public String httpHeaderType;
		public int agentPort; 
		public String protocol;
		public int maxTries; 
		public long retryIntervalMillis;
		public long pollIntervalMillis;
		public int maxOperationTimeSeconds;
		public int statusChangeTimeoutSeconds;
		public String resourcePath;
		public String requestContent;
		public String httpMethod;
		public boolean pollable;
		public long pauseIntervalBeforeSendMillis;
		//final Map<String, String> httpHeaderMap, 
		public String agentCommandType;
		public ActorRef manager;
		
		public initOpMessage(final String host,
				final String hostUniform,  final String httpHeaderType,
				final int agentPort, final String protocol,
				final int maxTries, final long retryIntervalMillis,
				final long pollIntervalMillis, final int maxOperationTimeSeconds,
				final int statusChangeTimeoutSeconds, final String resourcePath,
				final String requestContent, final String httpMethod, final boolean pollable, 
				final long pauseIntervalBeforeSendMillis,
				//final Map<String, String> httpHeaderMap, 
				final String agentCommandType,
				final ActorRef manager) {
			this.host = host;
			this.hostUniform = hostUniform;
			this.httpHeaderType = httpHeaderType;
			this.agentPort = agentPort;
			this.protocol = protocol;
			this.maxTries = maxTries;
			this.retryIntervalMillis = retryIntervalMillis;
			this.pollIntervalMillis = pollIntervalMillis;
			this.maxOperationTimeSeconds = maxOperationTimeSeconds;
			this.statusChangeTimeoutSeconds = statusChangeTimeoutSeconds;
			this.resourcePath = resourcePath;
			this.requestContent = requestContent;
			this.httpMethod = httpMethod;
			this.pollable = pollable;
			this.pauseIntervalBeforeSendMillis = pauseIntervalBeforeSendMillis;
			this.agentCommandType = agentCommandType;
			this.manager = manager;
		}
	}

	public class querySlaveProgressMessage implements Serializable {
		
	}
	
	public class slaveProgressMessage implements Serializable {
		public int requestCount;
		public int responseCount;
		public boolean completed;
		public double capacityPercent;
		public slaveProgressMessage(int requestCount, int responseCount, boolean completed, double capacityPercent) {
			this.requestCount = requestCount;
			this.responseCount = responseCount;
			this.completed = completed;
			this.capacityPercent = capacityPercent;
		}
	}
	
	public class queryMonitorProgressMessage implements Serializable {
		
	}
	
	public class monitorProgressMessage implements Serializable {
		public double progress;
		public monitorProgressMessage(double progress) {
			this.progress = progress;
		}
	}
	
	public class endOfRequest implements Serializable {
		
	}
	
	public class gatherResponse implements Serializable {
		public ActorRef monitor;
		public int totResponse;
		public gatherResponse(ActorRef monitor, int totResponse) {
			this.monitor = monitor;
			this.totResponse = totResponse;
		}
	}
	
	public class collecterCheckResponse implements Serializable {
		
	}
	
	public class queryMonitorForResponse implements Serializable {
		
	}
	
	public class streamRequestToManager implements Serializable {
		public int index;
		public streamRequestToManager(int index) {
			this.index = index;
		}
	}
	
	public class JobInitInfo {
		public String nodeGroupType;
		public String agentCommandType;
		public Map<String, NodeGroupDataMap> dataStore;
		public String directorJobUuid;
		public boolean localMode;
		public boolean failOver;
		public int maxConcNum;
		public boolean asyncMode;
		public JobStatus.JobInfo jobInfo;
		public int totJobNum;
		public JobInitInfo(String nodeGroupType, String agentCommandType, boolean localMode, boolean failOver, 
				Map<String, NodeGroupDataMap> dataStore, int maxConcNum, boolean asyncMode, String directorJobUuid, int totJobNum) {
			this.nodeGroupType = nodeGroupType;
			this.agentCommandType = agentCommandType;
			this.localMode = localMode;
			this.failOver = failOver;
			this.maxConcNum = maxConcNum;
			this.asyncMode = asyncMode;
			this.directorJobUuid = directorJobUuid;
			this.jobInfo = null;
			this.dataStore = dataStore;
			this.totJobNum = totJobNum;
		}
	}
	
	public class CheckSchedule {
		
	}
	
	public class ResetMaxConc implements Serializable {
		public double capacityAllowedPercent;
		public ResetMaxConc(double capacityAllowedPercent) {
			this.capacityAllowedPercent = capacityAllowedPercent;
		}
	}
	
	public class congestionMessage implements Serializable {
		
	}
	
	public class notCongestionSignal implements Serializable {
		
	}
}
//#messages