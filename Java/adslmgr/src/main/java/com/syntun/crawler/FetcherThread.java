package com.syntun.crawler;



import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpHost;


import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@SuppressWarnings("deprecation")
public class FetcherThread extends Thread {
	private static Logger logger = LogManager.getLogger(FetcherThread.class.getName());
	private RequestManager _reqMgr = null;
	private static boolean needQuit = false;
	private PoolingHttpClientConnectionManager _connMgr = null;
	private CloseableHttpClient httpclient = null;
	
	
	public PoolingHttpClientConnectionManager getConnMgr() {
		return _connMgr;
	}


	public void setConnMgr(PoolingHttpClientConnectionManager _connMgr) {
		this._connMgr = _connMgr;
	}
	
	public boolean init(){
		SSLContextBuilder builder = new SSLContextBuilder();
		SSLConnectionSocketFactory sslsf = null;
	    try {
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			sslsf = new SSLConnectionSocketFactory(
		            builder.build());
		} catch (NoSuchAlgorithmException  e) {
			// TODO Auto-generated catch block
			logger.error("SSL socker error.");
			e.printStackTrace();
			return false;
		} catch (KeyStoreException e){
			logger.error("SSL socker error.");
			e.printStackTrace();
			return false;
		}catch (KeyManagementException e){
			logger.error("SSL socker error.");
			e.printStackTrace();
			return false;
		}
	    
	    
	    this.httpclient = HttpClients.custom().setSSLSocketFactory(
	            sslsf).setConnectionManager(_connMgr).build();
	    return true;
	}


	@SuppressWarnings("deprecation")
	private FetcherResponse doGet(FetcherRequestInfo reqInfo){
//		SSLContextBuilder builder = new SSLContextBuilder();
//		SSLConnectionSocketFactory sslsf = null;
//	    try {
//			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
//			sslsf = new SSLConnectionSocketFactory(
//		            builder.build());
//		} catch (NoSuchAlgorithmException  e) {
//			// TODO Auto-generated catch block
//			logger.error("SSL socker error.");
//			e.printStackTrace();
//			return null;
//		} catch (KeyStoreException e){
//			logger.error("SSL socker error.");
//			e.printStackTrace();
//			return null;
//		}catch (KeyManagementException e){
//			logger.error("SSL socker error.");
//			e.printStackTrace();
//			return null;
//		}
//	    
//	    
//	    CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(
//	            sslsf).setConnectionManager(_connMgr).build();
	    
		//CloseableHttpClient httpclient = HttpClients.createDefault();
		FetcherResponse response = null;
		HttpContext context = null;
		try {
			logger.debug("Fetching:" + reqInfo.getOrigUrl());
			URI uri = reqInfo.getUri();
			context = reqInfo.getContext();
			if(context == null){
				context = HttpClientContext.create();
				reqInfo.setContext(context);
			}
			
			String newUrl = null;
			if(reqInfo.getOrigUrl().indexOf("mdskip")>=0 || reqInfo.getOrigUrl().indexOf("rate.tmall.com")>=0){
				newUrl = reqInfo.getOrigUrl().replace(uri.getHost(), reqInfo.getRemoteIp().getHostAddress());
			}
			else{
				newUrl = reqInfo.getOrigUrl();
			}
			
			//System.out.println("URL:" + newUrl);
			HttpGet httpGet = new HttpGet(newUrl);
			
		
			RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).setLocalAddress(reqInfo.getLocalIp()).setConnectTimeout(10000).setSocketTimeout(10000).build();
			httpGet.setConfig(config);
			httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Connection", "Keep-Alive");
            if(reqInfo.getOrigUrl().indexOf("mdskip") >= 0 || reqInfo.getOrigUrl().indexOf("rate.tmall.com")>=0){
    			httpGet.addHeader("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
                httpGet.addHeader("Host", uri.getHost());
            }
            else{
                httpGet.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)");
            }
			if(reqInfo.getReferer()!= null){
				httpGet.addHeader("Referer",reqInfo.getReferer());
			}
			
			FetcherResponseHandler rspHandler = new FetcherResponseHandler();
			rspHandler.setKnownCharset(reqInfo.getCharset());
			
			HttpHost host = null;
			if(reqInfo.getOrigUrl().indexOf("mdskip")>=0 || reqInfo.getOrigUrl().indexOf("rate.tmall.com")>=0){
				host = new HttpHost(InetAddress.getByAddress(reqInfo.getRemoteIp().getAddress()),uri.getPort(),uri.getScheme());
			}
			else{
				host = new HttpHost(reqInfo.getRemoteIp(),uri.getHost(),uri.getPort(),uri.getScheme());
			}
			//response = httpclient.execute(host, httpGet, rspHandler, context);
			response = httpclient.execute(host, httpGet, rspHandler);
			//logger.info("EXECUTED:" + reqInfo.getOrigUrl());
		} catch (ClientProtocolException e) {
			
			logger.error("Fetch url error 0. Url:" + reqInfo.getOrigUrl() + "\tMsg:" +e.getMessage());
			//e.printStackTrace();
		} catch (IOException e) {
			logger.error("Fetch url error 1. Url:" + reqInfo.getOrigUrl() + "\tMsg:" +e.getMessage());
			//e.printStackTrace();
		} 
		
		return response;
	}
	
	
	private FetcherResponse doPost(FetcherRequestInfo reqInfo){
		return null;
	}
	
	private FetcherResponse fetch(FetcherRequestInfo request){
		if(request.getMethod() == FetcherRequestInfo.METHOD_GET){
			return doGet(request);
		}
		else if(request.getMethod() == FetcherRequestInfo.METHOD_POST){
			return doPost(request);
		}
		return null;
	}
	
	public void setRequestManager(RequestManager reqMgr){
		_reqMgr = reqMgr;
	}
	
	
	public void run(){
		
		System.out.println("Thread started." + Long.toString(super.getId()));
		
		if(_reqMgr == null){
			logger.error("_reqMgr can not be null.");
			return;
		}
		 
		while(!needQuit){
			FetcherRequestInfo reqInfo = _reqMgr.getRequest();
			
			if(reqInfo == null){
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//logger.info("No new request. wait for 1 second.");
				continue;
			}
			
			logger.debug(String.format("Fetching: %s,Interface:%s,RemoteIp:%s,Host:%s", 
					reqInfo.getOrigUrl(),reqInfo.getLocalIp().toString(),reqInfo.getRemoteIp().toString(),reqInfo.getUri().getHost()));
			
			FetcherResponse rsp = fetch(reqInfo);
			//logger.debug(String.format("Fetching Done: %s,Interface:%s,RemoteIp:%s,Host:%s", reqInfo.getOrigUrl(),reqInfo.getLocalIp().toString(),reqInfo.getRemoteIp().toString(),reqInfo.getUri().getHost()));
			_reqMgr.onResponse(reqInfo, rsp);
		}
	}
}
