package com.felixjiang.proxypool;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.proxy.WebProxy;


public class ProxyPool implements Serializable {
	
	private static ProxyPool instance;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ConcurrentHashMap<WebProxy, Integer> available = new ConcurrentHashMap<WebProxy, Integer>();   //Proxies those are alive;
	ConcurrentHashMap<WebProxy, Integer> unavailable = new ConcurrentHashMap<WebProxy, Integer>();  //Proxies those are not alive;
	ConcurrentHashMap<WebProxy, Integer> unknown = new ConcurrentHashMap<WebProxy, Integer>();  //Proxies those are newly added and have not been tested yet
	
	private String proxyURL = "http://hidemyass.com/proxy-list/";
	private String stateFileName = "log/proxylist.txt";
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	// Thread for crawling proxies from website: http://hidemyass.com/
	class ParseProxyThread extends Thread {
		@Override
		public void run() {
			if (!SearchConfig.proxyFromWebsite)
				return;
			
			int MAX_PAGES = 50;
			long INTERVAL = 3 * 60 * 60 * 1000;
			while (true) {
				logger.log(Level.INFO, "parsing thread start running");
				try {
					ProxyParser p = new ProxyParser();
					String baseHtml = proxyURL;
					for (int i = 1; i <= MAX_PAGES; ++i) {
						String url = baseHtml + i;
						String html = p.getHtml(url);
						List<WebProxy> ll = p.parse(html);
						if (ll.isEmpty())
							break;
						for (WebProxy pp : ll) {
							available.put(pp, 0);
							if (unknown.contains(pp))
								unknown.remove(pp);
							if (unavailable.contains(pp))
								unavailable.remove(pp);
						}
						Thread.sleep(5000);
					}
					logger.log(Level.INFO, "parsing thread stop running");
					Thread.sleep(INTERVAL); // Update every 3 hours.
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Thread for Testing unavailable proxies
	class UpdateUnavailableThread extends Thread {
		@Override
		public void run() {
			long INTERVAL = 30 * 60 * 1000;
			try { Thread.sleep(100000); } catch (Exception e) {}
			while (true) {
				try {
					logger.log(Level.INFO, "Unavailable thread start running");
					Enumeration<WebProxy> it = unavailable.keys();
					while (it.hasMoreElements()) {
						WebProxy p = it.nextElement();
						unknown.put(p, 0);
						unavailable.remove(p);
					}
					logger.log(Level.INFO, "Unavailable thread stop running");
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Thread for testing available proxies
	class UpdateAvailableThread extends Thread {
		@Override
		public void run() {
			long INTERVAL = 55 * 60 * 1000; 
			try { Thread.sleep(15000); } catch (Exception e) {}
			while (true) {
				try {
					logger.log(Level.INFO, "Available thread start running");
					Map<WebProxy, Boolean> ret = CheckProxy(available);
					for (WebProxy wp : ret.keySet()) {
						Boolean status = ret.get(wp);
						if (!status) {
							available.remove(wp);
							unavailable.put(wp, 0);
						}
					}
					logger.log(Level.INFO, "Available thread stop running");
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Thread for testing unknown proxies.
	class UpdateUnknownThread extends Thread {
		@Override
		public void run() {
			long INTERVAL = 42 * 60 * 1000; 
			try { Thread.sleep(120000); } catch (Exception e) {}
			while (true) {
				try {
					logger.log(Level.INFO, "Unknown thread start running");
					Map<WebProxy, Boolean> ret = CheckProxy(unknown);
					for (WebProxy wp : ret.keySet()) {
						Boolean status = ret.get(wp);
						if (status)
							available.put(wp, 0);
						else
							unavailable.put(wp, 0);
						unknown.remove(wp);
					}
					logger.log(Level.INFO, "Unknown thread stop running");
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	
	class WriteStateThread extends Thread {
		long lastTimestamp = System.currentTimeMillis(); 
		@Override
		public void run() {
			long INTERVAL = 60 * 60 * 1000;
			try { Thread.sleep(15000); } catch (Exception e) {}
			while (true) {
				try {
					logger.log(Level.INFO, "Save state thread start running");
					long current = System.currentTimeMillis();
					SaveProxyList(stateFileName + "." + current);
					File f = new File(stateFileName + "." + lastTimestamp);
					if (f.exists()) f.delete();
					lastTimestamp = current;
					reportSize();
					logger.log(Level.INFO, "Save state thread stop running");
					Thread.sleep(INTERVAL);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void reportSize() {
		logger.log(Level.INFO, "available " + available.size() + ", unavailable " + unavailable.size()
				+ ", unknown " + unknown.size());
/*		System.out.println("available " + available.size());
		System.out.println("unavailable " + unavailable.size());
		System.out.println("unknown " + unknown.size()); */
	}
	Map<WebProxy, Boolean> CheckProxy(ConcurrentHashMap<WebProxy, Integer> proxies) {
		Map<WebProxy, Boolean> ret = new HashMap<WebProxy, Boolean>();
		Enumeration<WebProxy> en = proxies.keys();
		Set<VerifyThread> threads = new HashSet<VerifyThread>();
		int THREAD_GROUP = 50;
		do {
			Iterator<VerifyThread> it = threads.iterator();
			while (it.hasNext()) {
				VerifyThread vt = it.next();
				if (vt.IsFinished()) {
					ret.put(new WebProxy(vt.host, vt.port, vt.type), vt.GetResult());
					it.remove();
				}
			}
			while (en.hasMoreElements() && threads.size() < THREAD_GROUP) {
				WebProxy wp = en.nextElement();
				VerifyThread vt = new VerifyThread(wp.host, wp.port, wp.type);
				vt.start();
				threads.add(vt);
			}
		} while (!threads.isEmpty());
		return ret;
	}String result = "";
	
	public static ProxyPool GetInstance() {
		if (instance == null)
			instance = new ProxyPool();
		return instance;
	}
	
	private ProxyPool() {
		new ParseProxyThread().start();
		new UpdateUnavailableThread().start();
		new UpdateAvailableThread().start();
		new UpdateUnknownThread().start();
		new WriteStateThread().start();
		
		try {
			logger.addHandler(new FileHandler(SearchConfig.proxyLog));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String CollectProxy(ConcurrentHashMap<WebProxy, Integer> map, String suffix) throws IOException {
		StringBuffer ret = new StringBuffer();
		Enumeration<WebProxy> it = map.keys();
		while (it.hasMoreElements()) {
			WebProxy p = it.nextElement();
			ret.append(p.host).append(" ").append(p.port).append(" ").
				append(p.type).append(" ").append(suffix).append("\n");
		}
		return ret.toString();
	}
	
	public boolean SaveProxyList(String fileName) throws IOException {
		String result = "";
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
		result += CollectProxy(available, "available");
		result += CollectProxy(unavailable, "unavailable");
		result += CollectProxy(unknown, "unknown");
		writer.write(result);
		writer.close();
		return true;
	}
	
	public boolean LoadProxyList(String fileName) {
		System.out.println(System.getProperty("user.dir"));
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#"))
					continue;
				String []tokens = line.split("\\s+");
				if (tokens.length < 4)
					continue;
				String host = tokens[0], port = tokens[1], type = tokens[2], state = tokens[3];
				if (!(host.matches("^(\\d+\\.){3}\\d+$") && port.matches("^\\d+$")))
					continue;
				if (!(type.equalsIgnoreCase("SOCKS")))
					type = "HTTP";
				if (state.equalsIgnoreCase("available"))
					available.put(new WebProxy(host, port, type), 0);
				else if (state.equalsIgnoreCase("unavailable"))
					unavailable.put(new WebProxy(host, port, type), 0);
				else
					unknown.put(new WebProxy(host, port, type), 0);				
			}
			reportSize();
			reader.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	public Vector<WebProxy> GetProxy(int number) {
		Vector<WebProxy> vec = new Vector<WebProxy>();
		ArrayList<WebProxy> proxies = new ArrayList<WebProxy>(available.keySet());
		Collections.shuffle(proxies);
		for (int i = 0; i < number && i < proxies.size(); ++i)
			vec.add(proxies.get(i));
		logger.log(Level.INFO, "Get proxy " + vec.size());
		return vec;
	}
}
