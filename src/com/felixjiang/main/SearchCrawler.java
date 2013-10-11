/**
 * Fei Jiang
 * Weibo search crawler.
 * Do NOT forgot to turn on proxypool service first
 */

package com.felixjiang.main;
import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.proxy.NoProxyFoundException;
import com.felixjiang.search.SearchManager;

public class SearchCrawler {
	public static void main(String args[]) throws IOException, XmlRpcException, NoProxyFoundException {
		SearchConfig.loadConfig("config.json");
		SearchManager.GetInstance().start(true);
	}
}