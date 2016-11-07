package com.syntun.extractor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.syntun.util.RawPageCompress;

public class PageInfo implements Extractor {
	private static Logger logger = LogManager.getLogger(StatisticExtractor.class.getName());
	PageInfoManager pageInfo = new PageInfoManager();
    private static String extractorName = "PageInfoExtractor";
	
	public boolean init(InfoExtractorConfig config) {
		try {

			boolean flag = pageInfo.load(config.getExtractorConfig("PageInfo").get("templatepath"));
			if (!flag) {
				System.err.println("load templatepath failure");
				return false;
			} 
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public boolean run(JSONObject ctxt) {
        //JSONHelper.getJsonString(ctxt,"rawpage.url"); null
		String baseUri = ctxt.getJSONObject("rawpage").getString("url"); //判断如果是null报错
        if(baseUri == null){ //不存在
            return false;
        }

		String encoding = ctxt.getJSONObject("rawpage").getString("http_content_encoding"); //不一定存在，如果不存在或者是空字符串,http_content不压缩，
        //null isEmpty

		String compressCon = ctxt.getJSONObject("rawpage").getString("http_content"); 
        //判断是否为null,是否为空字符串
		try {
			if (encoding.equals("gzip")) {
				String content = RawPageCompress.Uncompress(compressCon);
				List<Map<String, String>> result = pageInfo.getResult(content, baseUri);
				for (Map<String, String> map : result) {
					for (Entry<String, String> entry : map.entrySet()) {
						if (entry.getValue().isEmpty()) {
							continue;
					    } else {
						    ctxt.put(entry.getKey(), entry.getValue());
					    }
				    }  
			    }
			} else {
				System.err.println("Unrecognize compress type");
				return false;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
