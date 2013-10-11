package info.caq9.parser;

import java.util.Map;

import org.json.JSONException;

public interface Storage {
	
	/**
	 * store this weibo message, together with its original post (if this is a
	 * repost). override if already exists, but the crawl parameters are added
	 * as a new entry in the crawlInfo collection
	 * 
	 * @param message
	 *            the current WeiboMessage
	 * @param crawlParameters
	 * @param original
	 *            true if the current message is an original post within a
	 *            repost
	 * @throws JSONException
	 */
	public void store(WeiboMessage message, Map<String, String> crawlParameters, boolean original)
			throws JSONException;
}