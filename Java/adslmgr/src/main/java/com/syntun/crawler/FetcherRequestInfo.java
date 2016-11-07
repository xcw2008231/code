package com.syntun.crawler;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

public class FetcherRequestInfo {
	public FetcherRequestInfo(JSONObject msg) throws URISyntaxException {
		super();
		this.msg = msg;
		if (!msg.isNull("url_str")) {
			this.origUrl = msg.getString("url_str");
		    this.uri = new URI(this.origUrl);
		    if(!this.origUrl.startsWith("http")){
                System.out.println("INVALID_MSG:" + msg);
            }
		}
		if (!msg.isNull("father_url")) {
			this.referer = msg.getString("father_url");
		}
		if(!msg.isNull("url_charset")){
			setCharset(msg.getString("url_charset"));
		}
		this.method = METHOD_GET;
		this.postData = null;
	}
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;

	private InetAddress localIp = null;
	private InetAddress remoteIp = null;
	private int method = 0;
	private String postData = null;
	private URI uri = null;
	private String origUrl = null;
	private JSONObject msg = null;
	private String referer = null;
	private HttpContext context = null;
	private int redirectCount = 0;
	private List<String> redirectUrlList = new ArrayList<String>();
	private int retries = 0;
	private String charset = null;
	private boolean isRetry = false;
	
	
	
	public boolean isRetry() {
		return isRetry;
	}

	public void setRetry(boolean isRetry) {
		this.isRetry = isRetry;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public int getRetries() {
		return retries;
	}

	public void incRetries() {
		this.retries++;
	}

	public void addToRedirectList(String url){
		redirectUrlList.add(url);
	}
	
	public List<String> getRedirectList(){
		return redirectUrlList;
	}
	
	public String getReferer() {
		return referer;
	}
	public void setReferer(String referer) {
		this.referer = referer;
	}
	public JSONObject getMsg() {
		return msg;
	}
	public void setMsg(JSONObject msg) {
		this.msg = msg;
	}
	public String getOrigUrl() {
		return origUrl;
	}
	public void setOrigUrl(String origUrl) {
		this.origUrl = origUrl;
	}
	public InetAddress getLocalIp() {
		return localIp;
	}
	public void setLocalIp(InetAddress localIp) {
		this.localIp = localIp;
	}
	public InetAddress getRemoteIp() {
		return remoteIp;
	}
	public void setRemoteIp(InetAddress remoteIp) {
		this.remoteIp = remoteIp;
	}
	public int getMethod() {
		return method;
	}
	public void setMethod(int method) {
		this.method = method;
	}
	public String getPostData() {
		return postData;
	}
	public void setPostData(String postData) {
		this.postData = postData;
	}
	public URI getUri() {
		return uri;
	}
	public void setUri(URI uri) {
		this.uri = uri;
	}
	public HttpContext getContext() {
		return context;
	}
	public void setContext(HttpContext context) {
		this.context = context;
	}
	public int getRedirectCount() {
		return redirectCount;
	}
	public void incRedirectCount() {
		this.redirectCount++;
	}
	

}
