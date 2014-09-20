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
package models.data.providers;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.agent.batch.commands.message.BatchResponseFromManager;
import models.data.AgentCommandMetadata;
import models.data.JsonResult;
import models.data.NodeGroupDataMap;
import models.data.NodeGroupSourceMetadata;
import models.data.NodeGroupSourceType;
import models.data.StrStrMap;
import models.utils.AgentUtils;
import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;
/**
 * 
 * @author ypei
 *
 */
public class AgentCommandProviderHelperForWholeJob {

	private static final AgentCommandProviderHelperForWholeJob instance = new AgentCommandProviderHelperForWholeJob();

	public static AgentCommandProviderHelperForWholeJob getInstance() {
		return instance;
	}

	private AgentCommandProviderHelperForWholeJob() {

	}
	



}
