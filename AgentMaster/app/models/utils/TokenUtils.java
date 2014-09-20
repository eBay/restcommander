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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONObject;

/**
 * IAF tokens
 * 
 * 
 * 20140210
 * Integrate with KeyStone for LDAP. requires the 
 * 
 * 
 * @author ypei
 * 
 */
public class TokenUtils {


	public static String iafTokenLbmsUdns = null;
	public static Date tokenUdnsLbmsLastUpdated = new Date(0L);

	

	// for KeyStone LDAP	
	public static String keyStoneTokenSupermanSelf = null;
	public static String keyStoneTokenSupermanSelfExpiredStr= VarUtils.NA; // this is what the return updated
	public static Date keyStoneTokenSupermanSelfLastUpdated = new Date(0L);

	
	// public static get
	public static void main(String [] args){
		//generateNewIafTokenForLbmsUdns();
	
		//String authTokenUser ="9a28226827eb4263a164741a2a514fdc";
		
	}
	
}
