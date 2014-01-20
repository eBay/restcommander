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

import models.data.providers.AgentDataProvider;
import models.monitor.MonitorProvider;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
/**
 * 
 * @author ypei
 *
 */
@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {
       //do stuff
    	AgentDataProvider adp = AgentDataProvider.getInstance();
    	adp.updateConfigFromAllFiles();
    	
    	MonitorProvider mp= MonitorProvider.getInstance();
    	mp.getJVMMemoryUsage();
    	mp.getFreeDiskspace();
    	
    }    
}