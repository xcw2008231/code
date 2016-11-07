package com.syntun.extractor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.syntun.util.RawPageCompress;

public class PageInfoExtractor implements Extractor {
	private static Logger logger = LogManager.getLogger(PageInfoExtractor.class.getName());
	private PageInfoManager pageInfo = new PageInfoManager();
    private static String extractorName = "PageInfoExtractor";
	
	public boolean init(InfoExtractorConfig config) {
		try {
            Map<String, String> extractorConfig = config.getExtractorConfig("PageInfo");
            if (extractorConfig == null) {
            	logger.error("Invalid extractor name: PageInfo");
            	return false;
            }
            String path = extractorConfig.get("templatepath");
            if (path == null) {
            	logger.error("Invalid config node: templatepath");
            	return false;
            }
			boolean flag = pageInfo.load(path);
			if (!flag) {
				logger.error("load templatepath failure");
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
		String baseUri = JSONHelper.getJsonString(ctxt, "rawpage.url"); //判断如果是null报错
        if (baseUri == null) { //不存在
            return false;
        }
        
        String encoding = JSONHelper.getJsonString(ctxt, "rawpage.http_content_encoding");//不一定存在，如果不存在或者是空字符串,http_content不压缩，

		String httpContent = JSONHelper.getJsonString(ctxt, "rawpage.http_content"); 
		if (httpContent == null) {
			return false;
		}
        //判断是否为null,是否为空字符串
		if (!(encoding.isEmpty() || encoding == null)) {
			try {
				if (encoding.equals("gzip")) {
					httpContent = RawPageCompress.Uncompress(httpContent);
				} else {
					logger.error("Unrecognize compress type");
					return false;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		
		List<Map<String, String>> result = pageInfo.getResult(httpContent, baseUri);
		if (result == null) {
			return false;
		} else {
			JSONObject objResult = new JSONObject();
			for (Map<String, String> map : result) {
				for (Entry<String, String> entry : map.entrySet()) {
					
					objResult.put(entry.getKey(), entry.getValue());
				}  
			}
			ctxt.put("pageinfo", objResult);
	    }
		return true;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public String getName() {
		// TODO Auto-generated method stub
		return extractorName;
	}

}
