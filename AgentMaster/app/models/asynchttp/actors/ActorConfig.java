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
package models.asynchttp.actors;

import java.util.concurrent.atomic.AtomicInteger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import models.utils.DateUtils;
import models.utils.VarUtils;
import akka.actor.ActorSystem;
/**
 * 
 * @author ypei
 *
 */
public final class ActorConfig {

	private static Config conf = null;
	private static ActorSystem actorSystem = null;
	public static AtomicInteger runningJobCount = new AtomicInteger(0);
	static {

		conf = ConfigFactory.load("actorconfig");
		actorSystem = ActorSystem.create(VarUtils.ACTOR_SYSTEM, conf);

	}

	public static ActorSystem getActorSystem() {
		if(actorSystem == null || actorSystem.isTerminated()){
			actorSystem = ActorSystem.create(VarUtils.ACTOR_SYSTEM, conf);
		}
		return actorSystem;
	}
	
	public static void shutDownActorSystemWhenNoJobRunning() {
		if(!actorSystem.isTerminated()  && runningJobCount.get() == 0){
			actorSystem.shutdown();
		}
	}
	
	public static void runGCWhenNoJobRunning() {
		if(runningJobCount.get() == 0){
			System.gc();
		}
	}
}
