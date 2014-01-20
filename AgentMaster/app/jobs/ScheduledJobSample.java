package jobs;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.data.providers.AgentCommandProvider;
import models.data.providers.AgentCommandProviderHelperForWholeJob;
import models.data.providers.AgentDataProvider;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;

// disabled; this is a sample; uncomment the cron expression line to enable;

// at 4am
//@On("0 0 4 * * ?")
// every minute
@On("0 * * * * ?")
public class ScheduledJobSample extends Job {

	public void doJob() {

		if(!ConfUtils.runCronJob){
			models.utils.LogUtils.printLogNormal("Conf set as NOT to run cron job. Now EXIT: " + DateUtils.getNowDateTimeStrSdsm());
			return;
		}else{
			models.utils.LogUtils.printLogNormal("Conf set as to run cron job. Now started: " + DateUtils.getNowDateTimeStrSdsm());
		}
		
		try {

			
			String nodeGroupType = VarUtils.NODEGROUP_CONF_NODEGROUP_ADHOC_NODE_LIST_TOP100WEBSITES;
			String agentCommandType = VarUtils.AGENT_CMD_KEY_GET_FRONT_PAGE;
			AgentCommandProvider.generateUpdateSendAgentCommandToNodeGroupPredefined(nodeGroupType,
					agentCommandType);
			
			Thread.sleep(VarUtils.PAUSE_TIME_LONG_MILLIS);

			
			
		} catch (Throwable t) {

			models.utils.LogUtils.printLogError("Error occured in HadoopNodeMonitorDeployWholeJob: " + t.getLocalizedMessage());
		}

	}

}
