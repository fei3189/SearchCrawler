package com.felixjiang.search;

import info.caq9.parser.WeiboParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;

import com.felixjiang.proxy.WebProxy;

/**
 * @author felix
 * Crawling thread. For proxies may not be reliable, and it takes several seconds for 
 * an proxy to return a result page. Parallelism is essential for faster crawling speed. 
 */
public class CrawlThread extends Thread {
	private InetSocketAddress proxyAddr = null;
	private String requestUrl = null;
	private SearchInstance searchIns = null;
	private boolean forbidden = false;
	private String host = null;
	private String type = null;

	public CrawlThread(WebProxy proxy, SearchInstance cc) {
		proxyAddr = new InetSocketAddress(proxy.host,
				Integer.parseInt(proxy.port));
		requestUrl = cc.getRequestUrl();
		searchIns = cc;
		host = proxy.host;
		type = proxy.type;
		this.setDaemon(true);
	}

	private void SetRequestHeader(HttpURLConnection connection)
			throws ProtocolException {
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Host", "s.weibo.com");
		connection
				.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
		connection
				.setRequestProperty("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		connection.setRequestProperty("Accept-Language",
				"zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
	}

	/**
	 * Crawling thread
	 */
	public void run() {
		boolean valid = false;
		String htmlText = "";
		if (requestUrl == null)
			return;
		if (searchIns.html == null) {
			try {
				URL url = new URL(requestUrl);
				HttpURLConnection connection = null;
				if (type.equalsIgnoreCase("SOCKS"))
					connection = (HttpURLConnection) url.openConnection(new Proxy(
							Proxy.Type.SOCKS, proxyAddr));
				else
					connection = (HttpURLConnection) url.openConnection(new Proxy(
							Proxy.Type.HTTP, proxyAddr));
				SetRequestHeader(connection);
				connection.setConnectTimeout(20000);
				connection.setReadTimeout(30000);
	
				String line;
				BufferedReader input = new BufferedReader(new InputStreamReader(
						connection.getInputStream(), "utf-8"));
				while ((line = input.readLine()) != null) {
					htmlText += line + "\n";
				}
				input.close();
				connection.disconnect();
				valid = WeiboParser.isValid(htmlText);
				forbidden = !valid;
			} catch (IOException e) {
			}
		}
		if (searchIns.html == null) {
			synchronized(searchIns) {
				if (valid) {
					searchIns.proxy = proxyAddr.getAddress() + ":" + proxyAddr.getPort();
					searchIns.html = htmlText;
				}
				searchIns.notify();
			}
		}
	}
	
	public SearchInstance GetSearchInstance() {
		return this.searchIns;
	}
	
	public boolean IsProxyForbidden() {
		return forbidden;
	}
	
	public String GetProxyHost() {
		return host;
	}
}