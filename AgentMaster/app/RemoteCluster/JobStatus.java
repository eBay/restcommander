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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author chunyang
 * 
 * store & query job status
 * 
 */
public class JobStatus {
	// Job Id to Job Data
	
	public enum State {waiting, processing, finishedNotGathered, gathered};
	
	public class JobInfo{
		public String timeStamp;
		public long startTime;
		public long finishedNotAggregatedTime;
		public long endTime;
		public double aggregationTime;
		public int totJobNum;
		public Map<String, Integer> workerRequestCount = new HashMap<String, Integer>();
		public Map<String, Integer> workerResponseCount = new HashMap<String, Integer>();
		public Map<String, Double> capacityUsage = new HashMap<String, Double>();  
		public String backupId;
		public State state = State.waiting;
		public int collectedNum = 0;
		public int maxConcNum = 0;
		public String jobId;
		public String nodeGroupType;
		public String agentCommandType;
		public JobInfo(String jobId, String nodeGroupType, String agentCommandType) {
			this.jobId = jobId;
			this.nodeGroupType = nodeGroupType;
			this.agentCommandType = agentCommandType;
		}
	}
	
	public Map<String, JobInfo> jobDict = new HashMap<String, JobInfo>();
	public List<JobInfo> jobList = new ArrayList<JobInfo>();
	
	public JobInfo getJobInfo(String jobId) {
		if (jobDict.containsKey(jobId) == false) 
			return null;
		return jobDict.get(jobId);
	}
	
	public void setJobInfo(String jobId, String nodeGroupType, String agentCommandType) {
		jobDict.put(jobId, new JobInfo(jobId, nodeGroupType, agentCommandType));
		jobList.add(jobDict.get(jobId));
	}
	
	public void clearJobInfo(String jobId) {
		jobDict.remove(jobId);
	}
}
