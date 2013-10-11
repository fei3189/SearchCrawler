package com.felixjiang.search;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import com.felixjiang.config.CityCode;
import com.felixjiang.config.SearchConfig;

public class CrawlInstanceManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static CrawlInstanceManager manager;
	private DBCollection collection = null;
	private ArrayList<SearchInstance> searchList = new ArrayList<SearchInstance>();
	
	public static CrawlInstanceManager getInstance() {
		if (manager == null)
			manager = new CrawlInstanceManager();
		return manager;
	}
	
	private CrawlInstanceManager() {
		try {
			collection = new Mongo(SearchConfig.serverHost,
					SearchConfig.serverPort).getDB(SearchConfig.dbName)
					.getCollection(SearchConfig.dbCollectionSearchSource);
			collection.ensureIndex("desc");
		} catch (UnknownHostException e) {
		}
	}

	public int inject(SearchInstance search) {
		if (collection == null)
			return -1;
		
		DBObject obj = new BasicDBObject();
		obj.put("desc", search.getDesc());
		if (!collection.find(obj).hasNext()) {
			obj.put("keywords", search.keywords);
			obj.put("province", search.province);
			obj.put("district", search.district);
			obj.put("startTime", search.startTime);
			obj.put("endTime", search.endTime);
			collection.save(obj);
		}

		return 0;
	}
	
	public int remove(SearchInstance search) {
		if (collection == null)
			return -1;
		
		DBObject obj = new BasicDBObject();
		obj.put("desc", search.getDesc());
		collection.findAndRemove(obj);
		
		return 0;
	}
	
	
	/**
	 * For special province (Beijing), crawling granularity is district
	 */
	private ArrayList<SearchInstance> addAllRegion(String keywords, String specialProv, String begin,
			String end) throws UnsupportedEncodingException {
		ArrayList<SearchInstance> searchList = new ArrayList<SearchInstance>();
		Iterator<String> iter = CityCode.provinceMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			if (key == specialProv) {
				Iterator<String> it = CityCode.beijingMap.keySet().iterator();
				while (it.hasNext()) {
					searchList.add(new SearchInstance(keywords, "11",
							it.next(), begin, end));
				}
			} else
				searchList.add(new SearchInstance(keywords, key, "1000", begin,
						end));
		}
		return searchList;
	}

	/**
	 * In this setting,crawling granularity is province.
	 */
	private ArrayList<SearchInstance> addDefaultRegion(String keywords, String begin, String end)
			throws UnsupportedEncodingException {
		return addAllRegion(keywords, "12345678", begin, end); // The second parameter should not be a valid city code
	}

	/**
	 * load the queries (keywords and their parameters) from the JSON Array and
	 * generate search instances
	 * 
	 * @param queries
	 *            the JSON Array read from the input config file
	 * @throws UnsupportedEncodingException
	 */
	public void loadQueries(JSONArray queries)
			throws UnsupportedEncodingException {
		for (int idx = 0; idx < queries.length(); idx++) {
			try {
				JSONObject query = queries.getJSONObject(idx);
				String keyword = query.getString("keyword");
				String province = "", district = "";
				String begin = "", end = "";

				if (query.has("startTime"))
					begin = query.getString("startTime");
				if (query.has("endTime"))
					end = query.getString("endTime");

				if (query.has("province")) {
					province = query.getString("province");
					if (query.has("district"))
						district = query.getString("district");
					else
						district = "1000";
					if (province.equals("default")) {
						searchList.addAll(addDefaultRegion(keyword, begin, end));
					} else if (province.equals("all")) { // For CAQ9
						searchList.addAll(addAllRegion(keyword, "11", begin, end));
					} else if (CityCode.provinceMap.containsKey(province))
						searchList.add(new SearchInstance(keyword, province,
								district, begin, end));
					else
						logger.log(Level.WARNING, "Invalid province");
				} else {
					searchList.add(new SearchInstance(keyword, province,
							district, begin, end));
				}
			} catch (JSONException je) {
				logger.log(Level.WARNING, "Query #" + idx + " parse error");
				je.printStackTrace();
			}
		}
		injectQueries();
		
		logger.log(Level.INFO, searchList.size() + " queries added.");
	}
	
	/**
	 * Get search instance from db, meanwhile delete those that endTime is not empty.
	 * @return
	 */
	public Vector<SearchInstance> getSearchInstance() {
		Vector<SearchInstance> vec = new Vector<SearchInstance>();
		if (collection == null)
			return vec;
		DBCursor cursor = collection.find();
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			String keywords = "", province = "", district = "", startTime = "", endTime = "";
			if (obj.containsField("keywords"))
				keywords = (String)obj.get("keywords");
			if (obj.containsField("province"))
				province = (String)obj.get("province");
			if (obj.containsField("district"))
				district = (String)obj.get("district");
			if (obj.containsField("startTime"))
				startTime = (String)obj.get("startTime");
			if (obj.containsField("endTime"))
				endTime = (String)obj.get("endTime");
			try {
				SearchInstance ins = new SearchInstance(keywords, province, district, startTime, endTime);
				vec.add(ins);
			} catch (UnsupportedEncodingException e) {
			}
		}
		BasicDBObject query = new BasicDBObject();
		query.put("endTime", new BasicDBObject("$ne", ""));
		collection.remove(query);
		return vec;
	}
	
	private void injectQueries() {
		for (int i = 0; i < searchList.size(); ++i)
			inject(searchList.get(i));
	}
}