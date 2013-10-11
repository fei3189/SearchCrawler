package com.felixjiang.proxypool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class VerifyThread extends Thread {
	private boolean result = false;
	private boolean finished = false;
	public String host;
	public String port;
	public String type;
	VerifyThread(String host, String port, String type) {
		this.host = host;
		this.port = port;
		this.type = type;
	}
	
	private String GetHtml(String urlName) throws IOException {
		InetSocketAddress proxyAddr = new InetSocketAddress(host, new Integer(port));
		URL url = new URL(urlName);
		HttpURLConnection connection;
		if (type.equalsIgnoreCase("SOCKS"))
			connection = (HttpURLConnection)url.openConnection(new Proxy(Proxy.Type.SOCKS, proxyAddr));
		else
			connection = (HttpURLConnection)url.openConnection(new Proxy(Proxy.Type.HTTP, proxyAddr));
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(10000);
		String content = "", line;
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF8"));
		while ((line = reader.readLine()) != null) {
			content += line;
		}
		reader.close();
		return content;
	}
	
	private boolean VerifyBaidu() {
		try {
			String html = GetHtml("http://www.baidu.com");
			return html.contains("百度一下，你就知道");
			
		} catch (IOException e) {
			return false;
		}
	}
	
	private boolean VerifySogou() {
		try {
			String html = GetHtml("http://www.sogou.com");
			return html.contains("搜狗搜索引擎");
		} catch (IOException e) {
			return false;
		}
	}
	
	@Override
	public void run() {
		result = VerifyBaidu() || VerifySogou();
		finished = true;
	}
	
	public boolean IsFinished() {
		return finished;
	}
	
	public boolean GetResult() {
		return result;
	}
}