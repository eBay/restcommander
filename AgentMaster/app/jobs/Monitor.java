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
package jobs;

import java.lang.management.ThreadInfo;

import models.monitor.MonitorProvider;
import models.monitor.MonitorProvider.PerformUsage;

import org.apache.log4j.Logger;

import play.jobs.Every;
import play.jobs.Job;
/**
 * 
 * @author ypei
 *
 */
@Every("5mn")
public class Monitor extends Job {

	private static Logger log = Logger.getLogger(Monitor.class);
	private static final int THRESHOLD_PERCENT = 90;
	
	
	@Override
	public void doJob() throws Exception {
		models.utils.LogUtils.printLogNormal("Logging JVM Stats");
		MonitorProvider mp = MonitorProvider.getInstance();
		PerformUsage perf = mp.getJVMMemoryUsage();
		
		// get disk usage
		mp.getFreeDiskspace();
		
		log.info(perf.toString());
		if(perf.memoryUsagePercent >= THRESHOLD_PERCENT) {
			log.info("========= Live Threads List=============");
			log.info(mp.getThreadUsage().toString());
			log.info("========================================");
			log.info("========================JVM Thread Dump====================");
			ThreadInfo[] threadDump = mp.getThreadDump();
			for(ThreadInfo threadInfo : threadDump) {
				log.info(threadInfo.toString());
			}
			log.info("===========================================================");
		}
		models.utils.LogUtils.printLogNormal("Logged JVM Stats");
	}

}
