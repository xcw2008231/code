package com.syntun.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class XpathSelector implements Selector {
	private static Logger logger = LogManager.getLogger(XpathSelector.class.getName());
	private String selectorId;
	private Set<ItemNode> nodes;
	
	@Override
	public String toString() {
		return "Selector [selectorId=" + selectorId + ", nodes=" + nodes + "]";
	}

	public String getSelectorId() {
		return selectorId;
	}

	public Set<ItemNode> getNodes() {
		return nodes;
	}

	public boolean initSelector(Element element) {
		try {
			selectorId = element.attr("id");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.warn("Lack of attribute id");
			return false;
		}
		try {
			for (Element element1 : element.children()) {
				if (this.nodes == null) {
					this.nodes = new HashSet<ItemNode>();
				}
				nodes.add(parseNode(element1));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private ItemNode parseNode(Element element) {
		ItemNode node = new ItemNode();
		try {
			node.setType(element.select("type").text().trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			node.setName(element.select("name").text().trim());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			node.setXpath(element.select("xpath").text().trim());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			String pattern = element.select("regex").text().trim();
			if (pattern.isEmpty()) {
				node.setPattern(null);
			} else {
				Pattern tokSplitter = Pattern.compile(pattern, Pattern.DOTALL);
				node.setPattern(tokSplitter);
			};
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return node;
	}

	public Map<String, String> extractSelectorInfo(String content, String baseUri) {
		// TODO Auto-generated method stub
	    try {
			Document doc = Jsoup.parse(content, baseUri);
			Map<String, String> map = new HashMap<String, String>();
			for (ItemNode obj : nodes) {
				String selectorInfo = selectInfo(doc, obj);
				if (selectorInfo != null) {
			        map.put(obj.getName(), selectorInfo);
				}
			}
			if (map.isEmpty()) {
				logger.warn("Extract selector nothing: " + selectorId);
				return null;
			} else {
			    return map;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private String selectInfo(Document doc, ItemNode node) {
	    try {
			if (node.getType().equals("text")) {
				Elements elements = doc.select(node.getXpath());
				String value = elements.text();
				if (value.isEmpty()) {
					return null;
				}
			    if (node.getPattern() != null) {
			    	value = getToken(value, node.getPattern());
			    }
			    return value;
			} 
			else if (node.getType().equals("img")) {
				Elements elements = doc.select(node.getXpath() + "[src]");
				String value = elements.attr("abs:src");
				if (value.isEmpty()) {
					return null;
				}
				if (node.getPattern() != null) {
					value = getToken(value, node.getPattern());
			    }
				return value;
			} 
			else if (node.getType().equals("html")) {
				Elements elements = doc.select(node.getXpath());
				String value = elements.toString();
				if (value.isEmpty()) {
					return null;
				}
			    if (node.getPattern() != null) {
			    	value = getToken(value, node.getPattern());
			    }
			    return value;
			} else {
				logger.warn("Unrecognize selector node: type");
				return null;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private String getToken(String text, Pattern pattern) {
	    ArrayList<String> tokens = new ArrayList<String>();
		Matcher m = pattern.matcher(text);
		if (m.find()) {
			logger.info("GroupCount = " + m.groupCount());
			if(m.groupCount() >= 1){
				tokens.add(m.group(1));
			}
		}
		return tokens.toString();
	}
}	
