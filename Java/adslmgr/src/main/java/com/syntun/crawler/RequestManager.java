package com.syntun.crawler;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.syntun.checker.InspectWebPage;
import com.syntun.util.RawPageCompress;

public class RequestManager {
	private static int MAX_REDIRECT_COUNT = 20;
	private static int MAX_RETRIES = 5;
	private List<FetcherRequestInfo> m_reqList = Collections.synchronizedList(new ArrayList<FetcherRequestInfo>());
	private int m_reqCount = 0;
	private int m_errCount = 0;
	
	//private UrlSelectConsumer m_urlSelect = new UrlSelectConsumer();
	private LinkedList<UrlSelectConsumer> m_urlSelect_list = new LinkedList<UrlSelectConsumer>();
	//private List<JSONObject> m_objects = Collections.synchronizedList(new ArrayList<JSONObject>());
	private int m_minLoad = 200; //最小需要加载的数量
	private static Logger logger = LogManager.getLogger(FetcherThread.class.getName());
	private AdslManager m_adslMgr = new AdslManager();
	private RawPageProducer m_rawPageProducer = new RawPageProducer();
	
	private Integer reqCount = new Integer(0);
	private Integer rspCount = new Integer(0);
	
	private InspectWebPage inspectWebPage = null;
	
	private RedialManager redialMgr = new RedialManager();
	
	private UrlUniq urlUniq = new UrlUniq();
	
	private long removeExpiredTime = 0;
	
	private long removeExpiredStep = (1800*1000);
	
	public boolean init(){
		boolean ret = false;
		//初始化InspectWebPage
		inspectWebPage = InspectWebPage.getInspectObj();
		if(inspectWebPage == null){
			logger.error("Init InspectWebPage failed.");
			return false;
		}
		
		for (Integer patition : CrawlerConfig.URL_SELECTOR_PARTITION_LIST) {
			UrlSelectConsumer m_urlSelect = new UrlSelectConsumer();
			ret = m_urlSelect.init(CrawlerConfig.BROKER_LIST, CrawlerConfig.BROKER_PORT, CrawlerConfig.URL_SELECTOR_TOPIC,patition);
			if(!ret){
				logger.error("Can not init urlSelectConsumer patition:"+patition);
				return false;
			}else
				m_urlSelect_list.add(m_urlSelect);
		}
		ret = m_adslMgr.init();
		if(!ret){
			logger.error("Can not init AdslManager.");
			return false;
		}
		ret = m_rawPageProducer.init();
		if(!ret){
			logger.error("Can not init RawPageProducer.");
			return false;
		}
		m_rawPageProducer.start();
		logger.debug("RawPageProducer started.");
		return ret;
	}
		
	public synchronized FetcherRequestInfo getRequest(){ //获取一个请求
		//判断是不是需要清理uniqTable
		
		
		synchronized(m_reqList){
			long curTime = System.currentTimeMillis();
			if(this.removeExpiredTime + removeExpiredStep < curTime){
				//需要清理
				urlUniq.removeExpired();
				this.removeExpiredTime = curTime;
			}
			while(m_reqList.size() < m_minLoad){
				UrlSelectConsumer m_urlSelect = m_urlSelect_list.pollFirst();
				if(m_urlSelect == null){
					logger.error("urlSelect is null.");
					break;
				}
				List<JSONObject> newMsgs = m_urlSelect.getDataFromKafka();
				m_urlSelect_list.addLast(m_urlSelect);
				if(newMsgs == null){
					logger.error("Can not get msg from kafka.");
					break;
				}
				else{
					
					//m_objects.addAll(newMsgs);
					for(int i = 0; i < newMsgs.size(); ++i){
						JSONObject obj = newMsgs.get(i);
						try{
							FetcherRequestInfo req = new FetcherRequestInfo(obj);
							if (!urlUniq.needCrawl(req.getOrigUrl())) {
								logger.info("Skip_by_uniq:" + req.getOrigUrl());
								continue;
							}
							m_reqList.add(req);
						}catch (URISyntaxException e) {
							logger.error("Invalid url.");
							m_errCount++;
							e.printStackTrace();
						}
					}
					m_reqCount+=newMsgs.size();
					//logger.debug("TotalRequest:" + m_reqCount + " ,ErrorCount:" + m_errCount);
			
					break;
				}
			}
			
			if(m_reqList.size() <= 0){
				return null;
			}
			
			FetcherRequestInfo retInfo = m_adslMgr.getRequest(m_reqList);
			if(retInfo == null){
				logger.debug("Request in list:" + m_reqList.size());
			}
            else{

                synchronized(reqCount){
                    reqCount++;
                }
            }
			return retInfo;
		}
	}
	
	public void onResponse(FetcherRequestInfo reqInfo, FetcherResponse rsp){
		synchronized(rspCount){
			rspCount++;
		}
		long curTime = System.currentTimeMillis();
		String strJson = null;
		
		//去掉busy属性
		m_adslMgr.setAccessTime(reqInfo.getUri().getHost(), reqInfo.getLocalIp(), reqInfo.getRemoteIp());
		m_adslMgr.setBusy(reqInfo.getUri().getHost(), reqInfo.getLocalIp(), reqInfo.getRemoteIp(), false);
		Integer isBan = 0;
		if(rsp != null){
			//判断是否被封禁
			JSONObject reqMsg = reqInfo.getMsg();
			if(reqMsg.has("url_group")){
				int urlGroup = reqMsg.getInt("url_group");
				if(inspectWebPage.inspectPage(rsp.getBody(), urlGroup) != 0){
					logger.error("BAN_URL: " + reqInfo.getOrigUrl());
					isBan = 1;
					//redial
					boolean redial = false;
					if(reqInfo.getOrigUrl().indexOf("alisec.") >= 0){ //天猫评论页验证码地址
						redial = true;
					}
					
					if(redial){
						String ifName = m_adslMgr.getInterfaceNameByAddr(reqInfo.getLocalIp());
						if(ifName != null){
							redialMgr.redial(ifName);
						}
					}
					
				}
			}
			if(rsp.getCode() >=400){//错误计数+1
                logger.debug("INC_ERROR: CODE:" + rsp.getCode());
				m_adslMgr.incErrCount(reqInfo.getUri().getHost(), reqInfo.getLocalIp(), reqInfo.getRemoteIp());
				m_errCount++;
			}
			if(rsp.getCode() == 302 || rsp.getCode() == 301){
				if(reqInfo.getRedirectCount() >= MAX_REDIRECT_COUNT){
					logger.error("Max redirect count reached.");
				}
				else if(rsp.getLocation() == null){
                    logger.debug("INC_ERROR: CODE:" + rsp.getCode());
					m_adslMgr.incErrCount(reqInfo.getUri().getHost(), reqInfo.getLocalIp(), reqInfo.getRemoteIp());
					logger.error("No location for code 302.");
				}
				else{
					//跳转指令，重新插入抓取队列
					URI uri;
					try {
						
						URL absoluteUrl = new URL(reqInfo.getOrigUrl());
						URL locationUrl = new URL(absoluteUrl, rsp.getLocation());	//拼成绝对url
						logger.debug("Redirect to:"+rsp.getLocation() + ",ABSOLUTE_URL:" + locationUrl.toString());
						if(inspectWebPage.checkRedirectPage(locationUrl.toString())!=0){
							//即将跳转到封禁页面，重拨。
							
							String ifName = m_adslMgr.getInterfaceNameByAddr(reqInfo.getLocalIp());
							if(ifName != null){
								logger.warn("BAN_REDIRECT: Redial:" + ifName + ",Location:" + locationUrl.toString());
								redialMgr.redial(ifName);
							}
							if(reqInfo.getRetries()<=5){
								synchronized(m_reqList){
									reqInfo.setRetry(true);
									reqInfo.incRetries();
									m_reqList.add(reqInfo);
									return;
								}
							}
						}
						uri = new URI(locationUrl.toString());
						reqInfo.setUri(uri);
						reqInfo.setReferer(reqInfo.getOrigUrl());
						reqInfo.setOrigUrl(locationUrl.toString());
						if(!reqInfo.getOrigUrl().startsWith("http")){
							logger.error("INVALID_URL:" + reqInfo.getOrigUrl());
						}
						reqInfo.incRedirectCount();
						reqInfo.addToRedirectList(locationUrl.toString());
						synchronized(m_reqList){
							//插入队列头
							reqInfo.setRetry(true);
							m_reqList.add(0,reqInfo);
						}
						return;
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			//输出页面（包含错误页面）
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("http_status", rsp.getCode());
			if(!CrawlerConfig.RAW_PAGE_COMPRESS){
				jsonResponse.put("http_content", rsp.getBody());
				jsonResponse.put("http_content_compress", false);
			}
			else{
				jsonResponse.put("http_content", RawPageCompress.Compress(rsp.getBody()));
				jsonResponse.put("http_content_compress", true);
			}
			jsonResponse.put("error", 0);
			jsonResponse.put("end_time", curTime);
			jsonResponse.put("redirect_route", reqInfo.getRedirectList());
			jsonResponse.put("retries", reqInfo.getRetries());
			jsonResponse.put("is_ban", isBan);
			JSONObject jsonResult = new JSONObject();
			jsonResult.put("url_record", reqInfo.getMsg());
			jsonResult.put("http_response", jsonResponse);
			strJson = jsonResult.toString();
			logger.debug(String.format("Fetching Done: %s,Interface:%s,RemoteIp:%s,Host:%s", reqInfo.getOrigUrl(),reqInfo.getLocalIp().toString(),reqInfo.getRemoteIp().toString(),reqInfo.getUri().getHost()));
			
		}
		else{
			//错误，可能网络io错误，重新插入队列
			m_adslMgr.incErrCount(reqInfo.getUri().getHost(), reqInfo.getLocalIp(), reqInfo.getRemoteIp());
			reqInfo.incRetries();
			if(reqInfo.getRetries() > MAX_RETRIES){//大于重试次数，输出
				logger.debug("RETRIES > " + MAX_RETRIES + ",URL:"+reqInfo.getOrigUrl());
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("http_status", 0);
				jsonResponse.put("http_content", "");
				jsonResponse.put("http_content_compress", false);
				jsonResponse.put("error", 1);
				jsonResponse.put("end_time", curTime);
				jsonResponse.put("retries", reqInfo.getRetries());
				jsonResponse.put("redirect_route", reqInfo.getRedirectList());
				jsonResponse.put("is_ban", isBan);

				JSONObject jsonResult = new JSONObject();
				jsonResult.put("url_record", reqInfo.getMsg());
				jsonResult.put("http_response", jsonResponse);
				strJson = jsonResult.toString();
			}
			else{
				synchronized(m_reqList){
					//插入队列头
					reqInfo.setRetry(true);
					m_reqList.add(reqInfo);
				}
				return;
			}

		}
		
		this.m_rawPageProducer.postRawPage(strJson, reqInfo.getOrigUrl());//使用最后一个url做key
//		System.out.println("Fetch done.");
//		System.out.println("Code:"+ String.valueOf(rsp.getCode()));
//		System.out.println("Body:" + String.valueOf(rsp.getBody().length()));
		//CookieStore cookieStore = (CookieStore) reqInfo.getContext().getAttribute(HttpClientContext.COOKIE_STORE);
		//System.out.println("Cookie:" + cookieStore.toString());
		synchronized(reqCount){
			synchronized(rspCount){
//				logger.debug("Req:"+ reqCount+" Rsp:"+rspCount);
			}
		}
		return;
	}
}
