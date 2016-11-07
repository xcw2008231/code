package com.syntun.extractor;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONHelper {
	
	public static String[] getJSONPath(String path){
		return path.split("\\.");
	}
	
	public static JSONObject getJsonObject(JSONObject obj, String[] path){
		JSONObject curObj = obj;
		for(String curName : path){
			if(!curObj.has(curName)){
				return null;
			}
			try{
				curObj = curObj.getJSONObject(curName);
			}
			catch(JSONException e){
				return null;
			}
		}
		return curObj;
	}
	
	public static JSONObject getJsonObject(JSONObject obj, String pathStr){
		String[] path = getJSONPath(pathStr);
		return getJsonObject(obj,path);
	}
	
	public static String getJsonString(JSONObject obj, String[] path){
		JSONObject curObj = obj;
		for(int i = 0; i < path.length; ++i){
			String curName = path[i];
			if(!curObj.has(curName)){
				return null;
			}
			if(i == path.length - 1){
				try{
					String res = curObj.getString(curName);
					return res;
				}
				catch(JSONException e){
					return null;
				}
			}
			try{
				curObj = curObj.getJSONObject(curName);
			}
			catch(JSONException e){
				return null;
			}
		}
		return null;
	}
	
	public static String getJsonString(JSONObject obj, String pathStr){
		String[] path = getJSONPath(pathStr);
		return getJsonString(obj,path);
	}
	
	public static Boolean getJsonBoolean(JSONObject obj, String[] path){
		JSONObject curObj = obj;
		for(int i = 0; i < path.length; ++i){
			String curName = path[i];
			if(!curObj.has(curName)){
				return null;
			}
			if(i == path.length - 1){
				try{
					boolean res = curObj.getBoolean(curName);
					return res;
				}
				catch(JSONException e){
					return null;
				}
			}
			try{
				curObj = curObj.getJSONObject(curName);
			}
			catch(JSONException e){
				return null;
			}
		}
		return null;
	}
	
	public static Boolean getJsonBoolean(JSONObject obj, String pathStr){
		String[] path = getJSONPath(pathStr);
		return getJsonBoolean(obj,path);
	}
}
