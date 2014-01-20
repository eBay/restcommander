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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import models.data.NodeData;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * KEY to map json string back to java objects.
 * this is specific to mapping hashmap of nodeData back
 * @author ypei
 *
 */
public class NaturalDeserializer implements JsonDeserializer<Object> {
	  public Object deserialize(JsonElement json, Type typeOfT, 
	      JsonDeserializationContext context) {
	    if(json.isJsonNull()) return null;
	    else if(json.isJsonPrimitive()) return handlePrimitive(json.getAsJsonPrimitive());
	    else if(json.isJsonArray()) return handleArray(json.getAsJsonArray(), context);
	    else return handleObject(json.getAsJsonObject(), context);
	  }
	  private Object handlePrimitive(JsonPrimitive json) {
	    if(json.isBoolean())
	      return json.getAsBoolean();
	    else if(json.isString())
	      return json.getAsString();
	    else {
	      BigDecimal bigDec = json.getAsBigDecimal();
	      // Find out if it is an int type
	      try {
	        bigDec.toBigIntegerExact();
	        try { return bigDec.intValueExact(); }
	        catch(ArithmeticException e) {}
	        return bigDec.longValue();
	      } catch(ArithmeticException e) {}
	      // Just return it as a double
	      return bigDec.doubleValue();
	    }
	  }
	  private Object handleArray(JsonArray json, JsonDeserializationContext context) {
	    Object[] array = new Object[json.size()];
	    for(int i = 0; i < array.length; i++)
	      array[i] = context.deserialize(json.get(i), Object.class);
	    return array;
	  }
	  private Object handleObject(JsonObject json, JsonDeserializationContext context) {
	    Map<String, Object> map = new HashMap<String, Object>();
	    for(Map.Entry<String, JsonElement> entry : json.entrySet())
	      map.put(entry.getKey(), context.deserialize(entry.getValue(), NodeData.class));
	    return map;
	  }
	}
