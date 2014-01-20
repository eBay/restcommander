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
package models.monitor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileSystemUtils;

import models.utils.DateUtils;
import models.utils.NumberUtils;
import models.utils.VarUtils;

import play.Logger;
import play.Play;
import play.libs.WS;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
/**
 * 
 * @author ypei
 *
 */
public class MonitorProvider {
	
	
	private static MonitorProvider instance = new MonitorProvider();
	
	public static MonitorProvider getInstance(){
		return instance;
	}

	private MonitorProvider(){
		
	}
	
	public PerformUsage currentJvmPerformUsage ;
	
	public DiskUsage currentDiskUsage ;
	
	public static void main(String[] args){
		
		MonitorProvider mp = new MonitorProvider();
		mp.getFreeDiskspace();
	}

	public DiskUsage getFreeDiskspace() {
		
		long freeSpace = -1L;
		try {
			freeSpace = FileSystemUtils.freeSpaceKb("/");
		} catch (IOException e) {
			models.utils.LogUtils.printLogError("Error in getFreeDiskspace() " + e.getLocalizedMessage());
			//e.printStackTrace();
		}
		int gb = 1024*1024;
		DiskUsage usage = new DiskUsage();
		usage.freeSpaceGb = (double)freeSpace/ (double)gb;
		
		if(VarUtils.IN_DETAIL_DEBUG){
			
			models.utils.LogUtils.printLogNormal("Free Space:" + usage.freeSpaceGb + " GB");
		}
		
		currentDiskUsage = usage;
		return usage;
	}
	
	public PerformUsage getJVMMemoryUsage() {
		int mb = 1024*1024;
		Runtime rt = Runtime.getRuntime();
		PerformUsage usage = new PerformUsage();
		usage.totalMemory = (double) rt.totalMemory()/mb;
		usage.freeMemory = (double) rt.freeMemory()/mb;
		usage.usedMemory = (double)rt.totalMemory()/mb - rt.freeMemory()/mb;
		usage.maxMemory = (double) rt.maxMemory()/mb;
		usage.memoryUsagePercent = usage.usedMemory / usage.totalMemory * 100.0;
				
//				new BigDecimal().setScale(2) .divide(new BigDecimal(usage.totalMemory).setScale(2), RoundingMode.DOWN)
//																  .setScale(2)
//																  .multiply(new BigDecimal(100)).intValue();
		
		// update current
		currentJvmPerformUsage = usage;
		return usage;
	}
	
	public ThreadInfo[] getThreadDump() {
		ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
		return threadMxBean.dumpAllThreads(true, true);
	}
	
	public ThreadUsage getThreadUsage() {
		ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
		ThreadUsage threadUsage = new ThreadUsage();
		long [] threadIds = threadMxBean.getAllThreadIds();
		threadUsage.liveThreadCount = threadIds.length;
		
		for(long tId : threadIds) {
			ThreadInfo threadInfo = threadMxBean.getThreadInfo(tId);
			threadUsage.threadData.put(new Long(tId).toString(), new ThreadData(threadInfo.getThreadName(), threadInfo.getThreadState().name(),
																				threadMxBean.getThreadCpuTime(tId)));
			
		}
		return threadUsage;
	}
	
	public static class ThreadUsage extends Jsonable {
		public int liveThreadCount;
		public Map<String, ThreadData> threadData = new HashMap<String, ThreadData>();
	}
	
	public static class ThreadData extends Jsonable {
		
		public String threadName;
		public String threadState;
		public long cpuTimeInNanoSeconds;
		
		public ThreadData(String threadName, String threadState, long cpuTimeInNanoSeconds) {
			this.threadName = threadName;
			this.threadState = threadState;
			this.cpuTimeInNanoSeconds = cpuTimeInNanoSeconds;
		}
		
		
	}
	
	public static class PerformUsage extends Jsonable {
		public String date = DateUtils.getNowDateTimeStrSdsm();
		public double totalMemory;
		public double freeMemory;
		public double usedMemory;
		public double maxMemory;
		public double memoryUsagePercent;
		
		public String getSummary(){
			String summary = 
					NumberUtils.getStringFromDouble(memoryUsagePercent) +"% (" +  
					NumberUtils.getStringFromDouble(usedMemory)
					+"/" +  NumberUtils.getStringFromDouble(totalMemory) 
					+ ") Max " + NumberUtils.getStringFromDouble(maxMemory) 
							;
					
			return summary;
		}
		
		
	}
	
	public static class DiskUsage extends Jsonable {
		public String date = DateUtils.getNowDateTimeStrSdsm();
		public double freeSpaceGb;
		
		
		public String getFreeSpaceGbStr(){
			
			return NumberUtils.getStringFromDouble(freeSpaceGb);
		}
	}
	
	public abstract static class Jsonable {
		
		@Override
		public String toString() {
			return new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC).create().toJson(this);
		}
	}
	
	public abstract static class Cubable extends Jsonable {

		private static String cubeEventPutUrl;
		static {
			cubeEventPutUrl = Play.configuration.getProperty("cubeeventput");
		
		}
		@Override
		public String toString() {
			JsonArray dataArray = new JsonArray();
			JsonObject dataJson = new JsonObject();
			dataJson.addProperty("type", "jvmstats");
			dataJson.addProperty("time", new Date().toString());
			String data = super.toString();
			dataJson.add("data", new Gson().toJsonTree(new Gson().fromJson(data, this.getClass())));
			dataArray.add(dataJson);
			if(cubeEventPutUrl != null) {				
				JsonElement jobResponse = WS.url(cubeEventPutUrl).body(dataArray.toString()).post().getJson();
				Logger.info("Response for post Cube request : " + jobResponse);
			}
			return data;
		}
		
		
	}
}
