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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;

import scala.Option;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import models.agent.batch.commands.message.BatchResponseFromManager;
import models.agent.batch.commands.message.InitialRequestToManager;
import models.asynchttp.actors.ActorConfig;
import models.asynchttp.actors.HttpWorker;
import models.asynchttp.actors.LogWorker;
import models.utils.DistributedUtils;
import models.utils.VarUtils;
import RemoteCluster.ClusterState;
import RemoteCluster.CommunicationMessages.RestartNode;
import RemoteCluster.CommunicationMessages.queryMonitorProgressMessage;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.Member;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.MemberStatus;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;
import akka.util.Timeout;
/**
 * 
 * @author chunyang
 *
 */
public class Guard extends UntypedActor{

	private Cluster cluster = Cluster.get(getContext().system());
	private String masterUrl;
	private ClusterState clusterState = new ClusterState();
	
	public Guard(String masterUrl) {
		this.masterUrl = masterUrl;
	}
	
	//subscribe to cluster changes, MemberUp, MemberRemoved
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), MemberUp.class, MemberRemoved.class);
	}

	//re-subscribe when restart
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
	}
	  
	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof CurrentClusterState) {
			CurrentClusterState state = (CurrentClusterState) message;
			for (Member member : state.getMembers()) {
				if (member.status().equals(MemberStatus.up())) {
					ClusterState.memberLoad.put(member.address(), new AtomicInteger(0));
					models.utils.LogUtils.printLogNormal("Status Change: Member up " + ClusterState.memberLoad.size());
				}
				if (member.status().equals(MemberStatus.removed())) {
					ClusterState.memberLoad.remove(member.address());
					models.utils.LogUtils.printLogNormal("Status Change: Member Removed " + ClusterState.memberLoad.size());
				}
			}
			
		} else if (message instanceof MemberUp) {
			MemberUp mUp = (MemberUp) message;
			
			if (!mUp.member().address().equals(cluster.selfAddress())) { 
				ClusterState.memberLoad.put(mUp.member().address(), new AtomicInteger(0));
				Pattern pattern = Pattern.compile("@.+:");				
				Matcher matcher = pattern.matcher(mUp.member().address().toString());
				if (matcher.find()) {
					String url = matcher.group().substring(1, matcher.end()-matcher.start()-1);
					String port = mUp.member().address().toString().substring(mUp.member().address().toString().length()-4);
					ClusterState.memberStates.put(url + ":" + port, clusterState.new State(true, true));
				}
				
				models.utils.LogUtils.printLogNormal("MemberUp : " 
						+ ClusterState.memberLoad.size()
						+ "Member Address : "
						+ mUp.member().address().toString());
			}
			
		} else if (message instanceof MemberRemoved) {
			MemberRemoved mRe = (MemberRemoved) message;
			ClusterState.memberLoad.remove(mRe.member().address());
			models.utils.LogUtils.printLogNormal("MemberRemoved " + ClusterState.memberLoad.size());
			
			Pattern pattern = Pattern.compile("@.+:");
			Matcher matcher = pattern.matcher(mRe.member().address().toString());
			if (matcher.find()) {
				String url = matcher.group().substring(1, matcher.end()-matcher.start()-1);
				String port = mRe.member().address().toString().substring(mRe.member().address().toString().length()-4);
				ClusterState.memberStates.get(url + ":" + port).available = false;
			}
	
		} else if (message instanceof RestartNode) {
			System.out.println("Restart removed nodes : ");
			for (Entry<String, ClusterState.State> e : ClusterState.memberStates.entrySet())
				if (!ClusterState.memberStates.get(e.getKey()).available && ClusterState.memberStates.get(e.getKey()).enable) {
					String url = e.getKey().substring(0, e.getKey().length() - 5);
					String port = e.getKey().substring(e.getKey().length() - 4);
					String httpPort = DistributedUtils.httpPort.get(e.getKey());
					URL yahoo = new URL("http://" + url + ":" + httpPort + "/distributedagents/shutdownNode");
					URLConnection yc = yahoo.openConnection();
					yc.getContentLength();
					yahoo = new URL("http://" + url + ":" + httpPort + "/distributedagents/createnode?port=" + port + "&masterUrl=" + masterUrl);
					yc = yahoo.openConnection();
					yc.getContentLength();
					System.out.println("Restart : " + e.getKey());
				}
			
			getContext().system().scheduler().scheduleOnce(Duration.create(60, TimeUnit.SECONDS),
					new Runnable() {
					    @Override
					    public void run() {
					      getSelf().tell(new RestartNode(), ActorRef.noSender());
					 }
			}, getContext().system().dispatcher());
			
		}
	}
}
