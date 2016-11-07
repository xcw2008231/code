package com.syntun.extractor;

import org.json.JSONObject;

public interface Extractor {
	public abstract boolean init(InfoExtractorConfig config);
	public abstract boolean run(JSONObject ctxt);
	public abstract void close();
	public abstract String getName();
}
