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
package controllers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import models.utils.DateUtils;
import models.utils.VarUtils;
import models.utils.VarUtils.CONFIG_FILE_TYPE;
import play.mvc.Controller;
import play.vfs.VirtualFile;

/**
 * 
 *
 */
public class LogConsole extends Controller {
	public static void logs() {
		
		String page = "logs";
		String topnav = "LogConsole";		
		render(page, topnav);
	}
	public static void index() {
		String page = "logs";
		String topnav = "LogConsole";
		render(page, topnav);
	}	
	
	public static void indexAjax() {
	    String fileName = request.params.get("fileName");
	    String numberOfLines = request.params.get("numberOfLines");
        String sb = readConfigFile(fileName, numberOfLines);
	    
        renderText(sb);
    }   
	
	public static String readConfigFile(String fileName, String numLines) {
	    int lineCountForDisplay = 0;
	    if("all".equalsIgnoreCase(numLines)) {
	        lineCountForDisplay = 0;
	    } else {
	        lineCountForDisplay = Integer.parseInt(numLines);
	    }
        if (fileName == null) {
            return "ERROR reading config: configFile is empty.";
        }
        List<String> linesTotal = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        
        String logDir = (fileName.contains("models.utils.LogUtils.printLogNormal")) ? "logs/" : "log/";
        
        String logFileLocation = logDir
                + fileName.toString().toLowerCase(Locale.ENGLISH) ;
        try {
           
            VirtualFile vf = VirtualFile
                    .fromRelativePath(logFileLocation);
            File realFile = vf.getRealFile();
            FileReader fr = new FileReader(realFile);
            BufferedReader reader = new BufferedReader(fr);
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line + "<br />";
                linesTotal.add(line);
            }
        } catch (Throwable e) {
            models.utils.LogUtils.printLogError("Error in readConfigFile."
                    + e.getLocalizedMessage());
           // e.printStackTrace();
        }
        
        if(VarUtils.IN_DETAIL_DEBUG){
        	models.utils.LogUtils.printLogNormal("linesTotal size:"+linesTotal.size());
        	models.utils.LogUtils.printLogNormal("lineCountForDisplay:"+lineCountForDisplay);
        	
        }
        if(lineCountForDisplay == 0) {
            for (int j= 0; j< linesTotal.size(); j++) {
                sb.append(linesTotal.get(j));
            }
        } else if (linesTotal.size() > lineCountForDisplay) {
            for (int j= (linesTotal.size() - lineCountForDisplay); j< linesTotal.size(); j++) {
                sb.append(linesTotal.get(j));
            }
        } else {
            for (int j= 0; j< linesTotal.size(); j++) {
                sb.append(linesTotal.get(j));
            }
        }
        
        if(VarUtils.IN_DETAIL_DEBUG){
        	models.utils.LogUtils.printLogNormal("linesTotal size:"+linesTotal.size());
        	
        }
        return sb.toString();
    } 
	

}
