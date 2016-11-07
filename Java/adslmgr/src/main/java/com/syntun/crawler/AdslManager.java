package com.syntun.crawler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class AdslInfo {
	public InetAddress address = null; //接口ip地址
	public String name = null;
//	public HashMap <String, InterfaceDomainInfo> mapDomainInfo = new HashMap <String, InterfaceDomainInfo>();
	public HashMap <String, InterfaceDomainInfo> mapDomainInfo = new HashMap<String,InterfaceDomainInfo>();
	public HashMap <String, InterfaceDomainInfo> mapStaticDomainInfo = null; //初始化已有域名配置抓取策略信息
	private static Logger logger = LogManager.getLogger(FetcherThread.class.getName());
	
	
	public long getRemindTime(String strDomainIp, long curTime){
		if(mapDomainInfo.containsKey(strDomainIp)){
			
			InterfaceDomainInfo info = mapDomainInfo.get(strDomainIp);
			//logger.debug(String.format("Interface:%s,DomainIp:%s,Busy:%b,Ban:%b,ErrCount:%d",name,strDomainIp,info.isBusy,info.isBan,info.errCount));
			if(info.nConn >= info.maxConn){
                //System.out.println("It's Busy");
                //logger.debug("It's busy. Conn:" + info.nConn);
				return Long.MAX_VALUE;
			}
			if(info.isBan){
                //System.out.println("It's Ban");
                //logger.debug("It's ban.");
				return Long.MAX_VALUE;
			}
//			if(info.errCount >= InterfaceDomainInfo.MAX_ERROR_COUNT){
//                //System.out.println("It's error");
//				return Long.MAX_VALUE;
//			}
			if(curTime - info.lastTime < info.stepTime){
                //System.out.println("It's colding down.");
                //logger.debug("It's colding down.");
				return info.lastTime + info.stepTime - curTime;
			}
			else{
				return 0;
			}
		}
		else{
			InterfaceDomainInfo info = new InterfaceDomainInfo();
			String[] fields = strDomainIp.split("_");
			if(fields!=null && fields.length== 2 && mapStaticDomainInfo.containsKey(fields[0])){
				InterfaceDomainInfo sInfo = mapStaticDomainInfo.get(fields[0]);
				info.errCount = sInfo.errCount;
				info.maxConn = sInfo.maxConn;
				info.stepTime = sInfo.stepTime;
			}
			mapDomainInfo.put(strDomainIp, info);
			return 0;
		}
	}
	
	public void setBusy(String strDomainIp, boolean isBusy){
		if(mapDomainInfo.containsKey(strDomainIp)){
			InterfaceDomainInfo info = mapDomainInfo.get(strDomainIp);
			if(isBusy){
				info.nConn++;
			}
			else{
				info.nConn--;
			}
			//info.isBusy = isBusy;
		}
	}
	
	public void setAccessTime(String strDomainIp){
		if(mapDomainInfo.containsKey(strDomainIp)){
			InterfaceDomainInfo info = mapDomainInfo.get(strDomainIp);
			info.lastTime = System.currentTimeMillis();
		}
	}
	
	public void setBan(String strDomainIp, boolean isBan){
		if(mapDomainInfo.containsKey(strDomainIp)){
			InterfaceDomainInfo info = mapDomainInfo.get(strDomainIp);
			info.isBan = isBan;
		}
	}

	public void incErrCount(String strDomainIp){
		if(mapDomainInfo.containsKey(strDomainIp)){
			InterfaceDomainInfo info = mapDomainInfo.get(strDomainIp);
			info.errCount++;
		}
	}
}

public class AdslManager {
	private List<AdslInfo> adsls = Collections.synchronizedList(new ArrayList<AdslInfo>());
	private NameCache dnsCache = new NameCache();
	private static Logger logger = LogManager.getLogger(FetcherThread.class.getName());
	private static Random rand = new Random(System.currentTimeMillis());
	
	
	public boolean init() {
		List<InterfaceConfig> configs = CrawlerConfig.INTERFACE_LIST;
		HashMap <String, InterfaceDomainInfo> mapDomainInfo = DomainStrategyInfo.init();
		if (configs.size() == 0) {
			//没有配置，增加一个默认的端口
			AdslInfo info = new AdslInfo();
			try {
				info.address = InetAddress.getByName("0.0.0.0");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				info.address =  InetAddress.getByName("0.0.0.0");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			info.name = "DEFAULT";
			info.mapDomainInfo = mapDomainInfo;
			info.mapStaticDomainInfo = mapDomainInfo;
			adsls.add(info);
		}
		//HashMap <String, InterfaceDomainInfo> mapDomainInfo = DomainStrategyInfo.init();
		for (int i = 0; i < configs.size(); ++i) {
			AdslInfo info = new AdslInfo();
			InterfaceConfig conf = configs.get(i);
			info.address = conf.ip;
			info.name = conf.name;
			info.mapDomainInfo = mapDomainInfo;
			info.mapStaticDomainInfo = mapDomainInfo;
			adsls.add(info);
		}
		return true;
	}
	
	private void  removeFromReqList(List<FetcherRequestInfo> reqList, List<FetcherRequestInfo> delList){
	
		reqList.removeAll(delList);

	}
	
	//决定使用哪个端口抓取哪个网站
	public FetcherRequestInfo getRequest(List<FetcherRequestInfo> reqList) {
		Iterator<FetcherRequestInfo> it = null;



		//List<Integer> delList = new ArrayList<Integer>();
		List<FetcherRequestInfo> delList = new ArrayList<FetcherRequestInfo>();
		
		for (it = reqList.iterator(); it.hasNext(); ) {
			FetcherRequestInfo info = it.next();
            if (info == null) {
                System.out.println("info == null");
                continue;
            }
            

            if(info.getUri() == null){
                System.out.println("uri == null");
                continue;
            }
            if(info.getUri().getHost() == null){
                System.out.println("host == null, URI:" + info.getOrigUrl() + " MSG:"+info.getMsg());
                delList.add(info); //错误的location url
                continue;
            }
			String host = info.getUri().getHost().toLowerCase();
			InetAddress[] addrs = dnsCache.getAddress(host);
			if (addrs == null || addrs.length <= 0) {
				logger.error("Can not resolve domain:" + host);
				//放入删除队列。
				delList.add(info);
				continue;
			}
			synchronized (adsls) {
				for (int j = 0; j < addrs.length; ++j) {
					String strDomainIp = getDomainIpString(host, addrs[j]);
					Vector<Integer> availableList = new Vector<Integer>(adsls.size());
					for (int k = 0; k < adsls.size(); ++k) {
						AdslInfo adslInfo = adsls.get(k);
						long remindTime = adslInfo.getRemindTime(strDomainIp, System.currentTimeMillis());
						if (remindTime <= 0) {
							availableList.add(k);
						}
					}
					//随机选一个可用的链接，返回。
					if (availableList.size()>0) {
						int randConn = Math.abs(rand.nextInt()) % availableList.size();
						
						AdslInfo adslInfo = adsls.get(availableList.get(randConn));
						adslInfo.setBusy(strDomainIp, true);
						info.setRemoteIp(addrs[j]);
						info.setLocalIp(adslInfo.address);
						delList.add(info);

						removeFromReqList(reqList,delList);
			           
						return info;
					}
				}
			}
		}
		removeFromReqList(reqList,delList);
		return null;
	}
	
	private String getDomainIpString(String domain,InetAddress addr){
		String ip = addr.getHostAddress();
		return domain.toLowerCase() + "_" +ip.toLowerCase();
	}
	
	public void setBusy(String domain, InetAddress localAddr, InetAddress remoteAddr, boolean isBusy){
		String strDomainIp = getDomainIpString(domain,remoteAddr);
		synchronized(adsls){
			for(int i = 0; i < adsls.size(); ++i){
				AdslInfo adslInfo = adsls.get(i);
				if(adslInfo.address.equals(localAddr)){
					adslInfo.setBusy(strDomainIp, isBusy);
					return;
				}
			}
		}
	}
	
	public void setAccessTime(String domain, InetAddress localAddr, InetAddress remoteAddr){
		String strDomainIp = getDomainIpString(domain,remoteAddr);
		synchronized(adsls){
			for(int i = 0; i < adsls.size(); ++i){
				AdslInfo adslInfo = adsls.get(i);
				if(adslInfo.address.equals(localAddr)){
					adslInfo.setAccessTime(strDomainIp);
					return;
				}
			}
		}
	}
	
	public void setBan(String domain, InetAddress localAddr, InetAddress remoteAddr, boolean isBan){
		String strDomainIp = getDomainIpString(domain,remoteAddr);
		synchronized(adsls){
			for(int i = 0; i < adsls.size(); ++i){
				AdslInfo adslInfo = adsls.get(i);
				if(adslInfo.address.equals(localAddr)){
					adslInfo.setBan(strDomainIp, isBan);
					return;
				}
			}
		}
	}
	
	public void incErrCount(String domain, InetAddress localAddr, InetAddress remoteAddr){
		String strDomainIp = getDomainIpString(domain,remoteAddr);
		synchronized(adsls){
			for(int i = 0; i < adsls.size(); ++i){
				AdslInfo adslInfo = adsls.get(i);
				if(adslInfo.address.equals(localAddr)){
					adslInfo.incErrCount(strDomainIp);
					return;
				}
			}
		}
	}
	
	public String getInterfaceNameByAddr(InetAddress localAddr){
		for(InterfaceConfig conf : CrawlerConfig.INTERFACE_LIST){
			if(conf.ip.equals(localAddr)){
				return conf.name;
			}
		}
		return null;
	}
}
