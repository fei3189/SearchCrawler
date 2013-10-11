package com.felixjiang.proxypool;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.felixjiang.proxy.WebProxy;

public class ProxyParser {
	private static Pattern pNode = Pattern.compile("<tr .*?</tr>", Pattern.DOTALL);
	private static Pattern style = Pattern.compile("<style>.*?</style>", Pattern.DOTALL);
	private void setHeader(HttpURLConnection connection) throws ProtocolException {
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
	
	public String getHtml(String loc) throws IOException {
		URL url = new URL(loc);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8087)));
		setHeader(connection);
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String content = "";
		for (String line; (line = reader.readLine()) != null; ) {
			content += line + "\n";
		}
		return content;
	}
	
	private List<String> getNodes(String html) {
		List<String> nodes = new ArrayList<String>();
		Matcher m = pNode.matcher(html);
		while (m.find()) {
			nodes.add(m.group());
		}
		return nodes;
	}
	
	public List<WebProxy> parse(String html) {
		List<String> nodes = getNodes(html);
		List<WebProxy> proxies = new ArrayList<WebProxy>();
		WebProxy tmp;
		for (String nodeStr : nodes) {
			if ((tmp = parseProxy(nodeStr)) != null && Integer.parseInt(tmp.port) < 65536)
				proxies.add(tmp);
		}
		return proxies;
	}
	
	private WebProxy parseProxy(String nodeStr) {
		Matcher ms = style.matcher(nodeStr);
		Map<String, String> class2display = new HashMap<String, String>();
		if (ms.find()) {
			String ss = ms.group();
			String []lines = ss.split("\\n");
			for (String l : lines) {
				l = l.trim();
				if (l.startsWith(".")) {
					class2display.put(l.substring(1, l.indexOf('{')), 
							l.substring(l.indexOf(':') + 1, l.indexOf('}')));
				}
			}
			int ipStart = nodeStr.indexOf("</style>") + "</style>".length();
			int ipEnd = nodeStr.indexOf("</span></td>", ipStart) + "</span>".length();
			String ipStr = nodeStr.substring(ipStart, ipEnd);
			ipStr = ipStr.replaceAll("<div", "<span");
			ipStr = ipStr.replaceAll("</div>", "</span>");
			
			String ip = "";
			int pos = 0;
			while (pos < ipStr.length()) {
				if (ipStr.charAt(pos) == '<') {
					int newPos = ipStr.indexOf("</span>", pos) +  + "</span>".length();
					String seg = ipStr.substring(pos, newPos);
					String raw = seg.replaceAll("<.*?>", "");
					if (seg.indexOf("\"") >= 0) {
						String desc = seg.substring(seg.indexOf("\"") + 1, seg.lastIndexOf("\""));
						if (desc.contains("display")) {
							if (desc.contains("inline"))
								ip += raw;
						} else if (!class2display.containsKey(desc) || class2display.get(desc).endsWith("inline"))
							ip += raw;
					}
					pos = newPos;
				} else {
					ip += ipStr.charAt(pos);
					++pos;
				}
			}

			int portBeg = nodeStr.indexOf("<td>", ipEnd) + "<td>".length();
			int portEnd = nodeStr.indexOf("</td>", portBeg);
			String port = nodeStr.substring(portBeg, portEnd).trim();
			
			String type = nodeStr.substring(portEnd).toLowerCase().contains("socks") ? "SOCKS" : "HTTP";
			
			return new WebProxy(ip, port, type);
			
		}
		return null;
	}

	public static void main(String args[]) throws IOException, InterruptedException {
		ProxyParser p = new ProxyParser();
		String baseHtml = "http://hidemyass.com/proxy-list/";
		for (int i = 1; i < 30; ++i) {
			String url = baseHtml + i;
			String html = p.getHtml(url);
			List<WebProxy> ll = p.parse(html);
			for (WebProxy pp : ll) {
				System.out.println(pp);
			}
			Thread.sleep(5000);
		}
	}
}