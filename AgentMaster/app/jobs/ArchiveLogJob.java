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

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.data.providers.AgentCommandProvider;
import models.data.providers.AgentDataProvider;
import models.data.providers.LogProvider;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;

/**
 * 
 * @author ypei
 *
 */
// at 0:45am
@On("0 30 0 * * ?")
public class ArchiveLogJob extends Job {

	public void doJob() {
		
//		if(!ConfUtils.runCronJob){
//			models.utils.LogUtils.printLogNormal("Conf set as NOT to run cron job. Now EXIT: " + DateUtils.getNowDateTimeStrSdsm());
//			return;
//		}else{
//			models.utils.LogUtils.printLogNormal("Conf set as to run cron job. Now started: " + DateUtils.getNowDateTimeStrSdsm());
//		}
		
		// will always run
		
		try {

			LogProvider.archiveAppLogsDailyJob();

			models.utils.LogUtils.printLogNormal("Successful ArchiveLogJob " + DateUtils.getNowDateTimeStr());
			
		} catch (Throwable t) {

			models.utils.LogUtils.printLogError("Error occured in ArchiveLogJob "  + DateUtils.getNowDateTimeStr() + " with error" + t.getLocalizedMessage());
		}

		
		/**
		 * 20131009
		 */
		try {

			LogProvider.deleteAppLogsDailyJob();

			models.utils.LogUtils.printLogNormal("Successful deleteAppLogsDailyJob " + DateUtils.getNowDateTimeStr());
			
		} catch (Throwable t) {

			models.utils.LogUtils.printLogError("Error occured in deleteAppLogsDailyJob "  + DateUtils.getNowDateTimeStr() + " with error" + t.getLocalizedMessage());
		}
	}

}
