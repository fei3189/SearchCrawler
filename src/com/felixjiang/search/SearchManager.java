package com.felixjiang.search;

import info.caq9.parser.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.proxy.NoProxyFoundException;
import com.felixjiang.proxy.ProxyRpcClient;
import com.felixjiang.proxy.WebProxy;

public class SearchManager {
	private int MAX_PAGES = SearchConfig.MAX_PAGES;
	private ProxyRpcClient proxyClient = null;
	private Logger logger = null;
	private static SearchManager instance;
	CrawlTimeManager ctm = new CrawlTimeManager();
	Logger loggerCon = Logger.getLogger(this.getClass().getName());
	
	private SearchManager() {
		try {
			logger = Logger.getLogger(this.getClass().getName());
			proxyClient = new ProxyRpcClient(SearchConfig.proxyServer, ""
					+ SearchConfig.proxyPort);
			logger.addHandler(new FileHandler(SearchConfig.proxyLog));
		} catch (MalformedURLException e) {
		} catch (SecurityException e) {
		} catch (IOException e) {
		}
	}

	public static SearchManager GetInstance() {
		if (instance == null)
			instance = new SearchManager();
		return instance;
	}

	public void Sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	// Map search instances to proxies, one proxy per thread. Then run threads.
	@SuppressWarnings("unchecked")
	private LinkedList<SearchInstance> runThread(LinkedList<SearchInstance> searchList, 
			LinkedList<CrawlThread> runningTList, Vector<WebProxy> proxyList) {
		LinkedList<SearchInstance> runningSList = new LinkedList<SearchInstance>();
		for (int index = 0; index < proxyList.size();) {
			Iterator<SearchInstance> it = searchList.iterator();
			while (it.hasNext() && index < proxyList.size()) {
				SearchInstance tmp = it.next();
				CrawlThread thread = new CrawlThread(
						proxyList.get(index++), tmp);
				runningTList.add(thread);
				thread.start();
			}
		}
		if (searchList.size() <= proxyList.size()) {
			runningSList = (LinkedList<SearchInstance>)searchList.clone();
			searchList.clear();
		} else {
			for (int i = 0; i < proxyList.size(); ++i) {
				runningSList.add(searchList.remove());
			}
		}
		return runningSList;
	}
	
	// Check whether the threads have finished, if 80% of the threads is finished,
	// then stop.
	private void checkAndWait(LinkedList<CrawlThread> runningTList) {
		int count = 0, size = runningTList.size();
		while (true) {
			try { Thread.sleep(2000); } catch (InterruptedException e) { }
			count = 0;
			Iterator<CrawlThread> it = runningTList.iterator();
			while (it.hasNext()) {
				if (it.next().getState()
						.equals(Thread.State.TERMINATED))
					++count;
			}
			if (count >= 0.8 * size) //Do not need to wait until all finished, thus saving time
				break;
		}
	}
	
	public void start(boolean continuous) throws IOException, XmlRpcException, NoProxyFoundException {
		if (proxyClient == null) {
			logger.log(Level.SEVERE, "No proxy client");
			return;
		}
		CrawlInstanceManager.getInstance().loadQueries(SearchConfig.queries);
		LinkedList<CrawlThread> runningTList = new LinkedList<CrawlThread>();
		LinkedList<SearchInstance> searchList = new LinkedList<SearchInstance>(CrawlInstanceManager.getInstance()
				.getSearchInstance());
		long lastIterTime = System.currentTimeMillis();
		Storage storage = new MongoStorage();
		Map<String, Long> query2latest = new HashMap<String, Long>(); // The latest post by each search instance. 
		
		while (true) {
			long start = System.currentTimeMillis();
			// For continuous crawling, every cycle, we should load the queries.
			if (continuous && searchList.isEmpty()) {
				if (start - lastIterTime > SearchConfig.parameterCycle
						&& searchList.isEmpty()) {
					lastIterTime = start;
					query2latest.clear();
					
					Vector<SearchInstance> newSearch = CrawlInstanceManager.getInstance()
							.getSearchInstance();
					logger.log(Level.INFO,
							"Get SearchInstance number = " + newSearch.size());
					for (int i = 0; i < newSearch.size(); ++i)
						searchList.add(newSearch.get(i));
				}
			}

			if (searchList.isEmpty()) {
				if (!continuous) break;
				Sleep(10000);
				continue;
			}
			
			logger.log(Level.INFO, "Total Search number = " + searchList.size());
			Vector<WebProxy> proxyList = null;
			proxyList = proxyClient.FetchProxy(2000);
			logger.log(Level.INFO,
						"Get proxies number = " + proxyList.size());
			if (!searchList.isEmpty() && proxyList != null) {
				long requestTime = System.currentTimeMillis();
				LinkedList<SearchInstance> runningSList = runThread(searchList, runningTList, proxyList);
				checkAndWait(runningTList);
				logger.log(Level.INFO, "Thread running = " + runningTList.size());
				
				// For each search instance, check whether it is successfully crawled.
				int countFailure = 0, countSuccess = 0;
				Set<SearchInstance> set = new HashSet<SearchInstance>();
				for (Iterator<SearchInstance> it = runningSList.iterator(); it.hasNext();) {
					SearchInstance searchIns = it.next();
					
					if (searchIns.html == null) { // Failed
						SearchInstance copy = (SearchInstance) searchIns.clone();
						if (!set.contains(copy)) {
							searchList.addFirst(copy);
							set.add(copy);
						}
						countFailure++;
					} else { // Succeeded.
						countSuccess++;
						List<WeiboMessage> messages = null;
						messages = WeiboParser.parse(searchIns.html, requestTime);
						
						storeMessage(storage, messages, searchIns);

						boolean hasNextPage = true;
						if (isTheLastPage(messages, searchIns)) {
							hasNextPage = false;
						}
						if (continuous && (searchIns.endTime == null || searchIns.endTime.isEmpty())) {
							if (!ManagePostTime(messages, searchIns, query2latest)) {
								hasNextPage = false;
							}
						}
						
						// If the query has next page, put the next page into list, continue crawling.
						if (hasNextPage) {
							SearchInstance copy = (SearchInstance) searchIns.clone();
							++copy.page;
							if (!set.contains(copy)) {
								searchList.addFirst(copy);
								set.add(copy);
							}
						}
					}
				}
				logger.log(Level.INFO, "Iteration Total = " + runningTList.size() + ", Search number = " + runningSList.size()
								+ ", Success = " + countSuccess + ", Failure = " + countFailure);
				runningTList.clear();
			}
			
			long end = System.currentTimeMillis();
			long interval = end - start;
			logger.log(Level.INFO, "Iteration cost " + (1.0 * interval / 1000)
					+ " seconds");
			if (interval < SearchConfig.parameterInterval) {
				Sleep(SearchConfig.parameterInterval - interval);
			}
		}
	}

	// Store messages into database.
	private void storeMessage(Storage storage, List<WeiboMessage> messages,
			SearchInstance searchIns) {
		Map<String, String> storeParameters = new HashMap<String, String>();
		storeParameters.put("startTime", searchIns.startTime);
		storeParameters.put("region", searchIns.province +searchIns.district);
		storeParameters.put("keyword", searchIns.keywords);
		if (storage != null && messages != null) {
			for (WeiboMessage msg : messages) {
				try {
					storage.store(msg, storeParameters, false);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else if (messages == null) {
			logger.log(Level.WARNING, "Parse exception" + searchIns.getRequestUrl(SearchConfig.nodup));
		}
	}

	// If the page number reaches the MAX page, or the items is less than 10, 
	// this page is the last page of the query.
	private boolean isTheLastPage(List<WeiboMessage> messages, SearchInstance search) {
		if (messages == null || messages.size() < 10) {
			logger.log(Level.WARNING, "Parse result = 0:" + search.getRequestUrl(SearchConfig.nodup));
			return true;
		}
		if (search.page >= MAX_PAGES)
			return true;
		return false;
	}

	// Manage post time, return true if next page should be crawled, false otherwise.
	private boolean ManagePostTime(List<WeiboMessage> messages,
			SearchInstance search, Map<String, Long> query2latest) {
		if (messages == null) {
			messages = new LinkedList<WeiboMessage>();
		}
		boolean ret = true;
		String key = search.getDesc();
		long time = ctm.GetLatestTime(key), earliest = Long.MAX_VALUE, latest = Long.MIN_VALUE;
		for (int i = 0; i < messages.size(); ++i) {
			if (messages.get(i).isPopular())  // Ignore hot posts, for they are not organized in time order.  
				continue;
			Date pt = messages.get(i).getPostTime();
			if (pt == null) continue;
			long tmp = pt.getTime();
			if (tmp < earliest) earliest = tmp;
			if (tmp > latest) latest = earliest;
		}
		
		// Record the latest post time of current search instance.
		if (!query2latest.containsKey(key) || latest > query2latest.get(key)) {
			query2latest.put(key, latest);
		}
		
		// Reaches the last page, or the earliest post of current page 
		// is earlier than the latest post crawled before, stop crawling.
		if (isTheLastPage(messages, search) || time > earliest) {
			ctm.SetLatestTime(key, query2latest.get(key));
			ret = false;
		}
		
		return ret;
	}
}