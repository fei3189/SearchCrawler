package com.felixjiang.search;

import com.felixjiang.config.SearchConfig;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class CrawlTimeManager {
	DBCollection collection = null;
	public CrawlTimeManager() {
		try {
			collection = new Mongo(SearchConfig.serverHost, SearchConfig.serverPort).getDB(SearchConfig.dbName).getCollection(SearchConfig.searchTime);
			collection.ensureIndex(new BasicDBObject("query", 1), "querykey_1", true);
		} catch (UnknownHostException e) {
		}
		
	}
	
	/**
	 * Read the post time of the latest message from database.
	 * @param key
	 * @return
	 */
	public long GetLatestTime(String key) {
		DBObject obj = new BasicDBObject("query" , key);
		DBCursor cursor = collection.find(obj);
		if (cursor.hasNext()) {
			DBObject res = (DBObject) cursor.next();
			return (Long)res.get(SearchConfig.searchTime);
		}
		return 0L;
	}
	
	/**
	 * Write the post time of the latest message to database.
	 * @param key
	 * @param time
	 */
	public void SetLatestTime(String key, long time) {
		DBObject value = new BasicDBObject("query" , key);
		DBCursor cursor = collection.find(value);
		if (cursor.hasNext()) {
			DBObject update = new BasicDBObject(SearchConfig.searchTime, time);
			DBObject set = new BasicDBObject("$set", update);
			collection.update(value, set);
		} else {
			value.put(SearchConfig.searchTime, time);
			collection.save(value);
		}
	}
	
	//Only for testing this module
	public static void main(String args[]) {
		CrawlTimeManager manager = new CrawlTimeManager();
		manager.SetLatestTime("wuwu", 0);
		manager.SetLatestTime("huhu", 6);
	}
}