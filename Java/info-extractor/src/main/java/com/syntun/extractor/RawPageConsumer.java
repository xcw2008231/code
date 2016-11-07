package com.syntun.extractor;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.consumer.Whitelist;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;

public class RawPageConsumer implements Runnable{
	private ConsumerConnector consumer = null;
	//private Map<String,List<KafkaStream<byte[],byte[]>>> mapStream = null;
	private boolean needQuit = false;
	private BlockingQueue<String> outputQueue = null;
	private String topic = null;
	private static Logger logger = LogManager.getLogger(RawPageConsumer.class.getName());
	List<KafkaStream<byte[],byte[]>> streamList = null;
	
	public boolean init(String zookeeper, String topic, String groupId, BlockingQueue<String> outputQueue){
		Properties props = new Properties(); 
		props.put("zookeeper.connect", zookeeper);
		props.put("auto.commit.enable", "true");
		props.put("auto.commit.interval.ms", "60000"); 
		props.put("group.id", groupId);
		
		ConsumerConfig consumerConfig = new ConsumerConfig(props);
		consumer = Consumer.createJavaConsumerConnector(consumerConfig);
//		Map<String,Integer> mapTopic = new HashMap<String,Integer>();
//		mapTopic.put(topic, 1);
//		mapStream = consumer.createMessageStreams(mapTopic);
//		if(mapStream == null){
//			return false;
//		}
		Whitelist whitelist = new Whitelist(topic);
		this.streamList = consumer.createMessageStreamsByFilter(whitelist);
		logger.info("Kafka Stream Count:" + this.streamList.size());
		this.outputQueue = outputQueue;
		this.topic = topic;
		return true;
	}
	
	public void quit(){
		needQuit = true;
	}

	public void run() {
//		if(topic == null || mapStream == null || outputQueue == null){
//			logger.error("UrlConsumer init error.");
//			return;
//		}
//
//		List<KafkaStream<byte[],byte[]>> partList = mapStream.get(topic);
//		if(partList == null || partList.size() <= 0){
//			logger.error("Partition list is null.");
//			return;
//		}
		
		KafkaStream<byte[], byte[]> stream = streamList.get(0);
		ConsumerIterator<byte[], byte[]> it = stream.iterator();

		while(it.hasNext()){
			MessageAndMetadata<byte[], byte[]> data = it.next();
			//System.out.println("Current Partition:" + data.partition());
			//System.out.println("Current Offset:" + data.offset());
			//logger.debug("Current Partition:" + data.partition());
			//logger.debug("Current Offset:" + data.offset());
			String payload;
			try {
				payload = new String(data.message(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				logger.warn("Invalid utf8 input message.");
				payload = new String(data.message());
			}
			try {
				outputQueue.put(payload); //可能阻塞
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	

	}
	
}
