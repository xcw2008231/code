package com.syntun.extractor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class InfoExtractorConfig {
	
	private HashMap<String,String> mapConfigRawpage = new HashMap<String,String>();
	private HashMap<String, HashMap<String,String>> mapConfigExtractors = new HashMap<String, HashMap<String,String>>();
		
	
	public HashMap<String, String> getMapConfigRawpage() {
		return mapConfigRawpage;
	}

	public void setMapConfigRawpage(HashMap<String, String> mapConfigRawpage) {
		this.mapConfigRawpage = mapConfigRawpage;
	}
	
	public HashMap<String,String> getExtractorConfig(String extractorName){
		if(!mapConfigExtractors.containsKey(extractorName)){
			return null;
		}
		return mapConfigExtractors.get(extractorName);
	}
	
	public HashMap<String,String> parseExtractorConfig(Node extractorNode){
		HashMap<String,String> mapRes = new HashMap<String,String>();
		for(Node cur = extractorNode.getFirstChild(); cur != null; cur = cur.getNextSibling()){
			if(cur.getNodeType() != Node.ELEMENT_NODE){
				continue;
			}
			String name = cur.getNodeName();
			String value = cur.getTextContent();
			mapRes.put(name, value);
		}
		return mapRes;
	}
	
	public boolean parseExtractorsConfig(Document root){
		NodeList list = root.getElementsByTagName("extractors");
		if(list.getLength() != 1){
			System.err.println("Invalid config node: extractors");
			return false;
		}
		Node node = list.item(0);
		for(Node cur = node.getFirstChild(); cur != null; cur = cur.getNextSibling()){
			if(cur.getNodeType() != Node.ELEMENT_NODE){
				continue;
			}
			if(!cur.getNodeName().equals("extractor")){
				continue;
			}
			NamedNodeMap mapAttr = cur.getAttributes();
			Node nodeName = mapAttr.getNamedItem("name");
			if(nodeName == null){
				System.err.println("No named extractor.");
				return false;
			}
			String name = nodeName.getNodeValue();

			HashMap<String,String> value = parseExtractorConfig(cur);
			if(value == null){
				System.err.println("Invlaid extractor config. Extractor:" + name);
				return false;
			}
			mapConfigExtractors.put(name, value);
		}
		return true;
	}

	public boolean parseRawpageConfig(Document root){
		NodeList list = root.getElementsByTagName("rawpage");
		if(list.getLength() != 1){
			System.err.println("Invalid config node: rawpage");
			return false;
		}
		Node rpNode = list.item(0);
		for(Node cur = rpNode.getFirstChild(); cur != null; cur=cur.getNextSibling()){
			if(cur.getNodeType() != Node.ELEMENT_NODE){
				continue;
			}
			String name = cur.getNodeName();
			String value = cur.getTextContent();
			mapConfigRawpage.put(name, value);
		}
		if(!mapConfigRawpage.containsKey("type")){
			System.err.println("Invalid rawpage input type.");
			return false;
		}
		if(!mapConfigRawpage.containsKey("zookeeper")){
			System.err.println("Invalid rawpage zookeeper.");
			return false;
		}
		if(!mapConfigRawpage.containsKey("topic")){
			System.err.println("Invalid rawpage kafka topic.");
			return false;
		}
		if(!mapConfigRawpage.containsKey("group")){
			System.err.println("Invalid rawpage kafka group.");
			return false;
		}
		return true;
	}
	
	public boolean loadConfig(String filename){
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		Document root = null;
		try{
			 DocumentBuilder domBuilder=domFactory.newDocumentBuilder();
			 InputStream is=new FileInputStream(filename);
			 root = domBuilder.parse(is);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		if(!parseRawpageConfig(root)){
			System.err.println("Invalid rawpage config.");
			return false;
		}
		
		if(!parseExtractorsConfig(root)){
			System.err.println("Invalid extractors config.");
			return false;
		}
		
		return true;
	}
}
