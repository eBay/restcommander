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
package models.utils;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Random;

import org.hamcrest.core.IsNull;

import play.Logger;
import play.libs.WS;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NumberUtils {

	
	
	public static String getStringFromDouble(double number) {
		DecimalFormat df = new DecimalFormat("#.###");
		return df.format(number);
	}
	
	public static int getRandomNumber(int min, int max) {
		Random rand = new Random();		
		int randomNum = rand.nextInt(max - min + 1) + min;
		return randomNum;
	}
	
	public static String getRandomIp() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(getRandomNumber(1,255));
		sb.append(".").append(getRandomNumber(1,255));
		sb.append(".").append(getRandomNumber(1,255));
		sb.append(".").append(getRandomNumber(1,255));
		
		return sb.toString();
	}
	
	public static int strToInt(String str) {
		if (str == null) return 0;
		if (StringUtils.areEqual(str, "")) return 0;
		
		int val = 0;
		try {
			val = Integer.parseInt(str);
		} catch (Exception ex) {
			Logger.error("Exception while converting string to int : " + str);
		}
		
		return val;
	}
	
	public static long strToLong(String str) {
		if (str == null) return 0;
		if (StringUtils.areEqual(str, "")) return 0;
		
		long val = 0;
		try {
			val = Long.parseLong(str);
		} catch (Exception ex) {
			Logger.error("Exception while converting string to int : " + str);
		}
		
		return val;
	}
	
	
	
	public static double strToDouble(String str) {
		
		if (str == null || StringUtils.areEqual(str, "")) return 0.0;
		double d = 0.0;
		try {
			d = Double.parseDouble(str);
		} catch (Exception ex) {
			Logger.error(ex, "Exception while converting str to double");
		}
		return d;
		
	}
	
	/*
	 * For a given size, provide the 95th percentile number
	 */
	public static int getPercentileNumber(int count, int percentile) {
		
		double computedVal = (count*percentile)/100;
		double ceil = Math.ceil(computedVal);
		int result = Math.round((float) ceil);
		
		if (result == 0) result = 1;
		
		return result;
	}
	
	public static void main(String[] args) {
				
		for (int i=0; i<10; i++) {
			int num = (int) (Math.random() * 100);
			models.utils.LogUtils.printLogNormal("Number : " + num + " 95th pct : " + getPercentileNumber(num, 95));
		}
		
	}
	
}
