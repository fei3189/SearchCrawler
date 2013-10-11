package info.caq9.parser;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.wbsenti.WeiboSA;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class MongoStorage implements Storage {
	Mongo mongoConn;
	DB db;
	DBCollection msgColl;
	WeiboSA wsa;

	public MongoStorage() throws IOException {
		mongoConn = new Mongo(SearchConfig.serverHost, SearchConfig.serverPort);
		db = mongoConn.getDB(SearchConfig.dbName);
		msgColl = db.getCollection(SearchConfig.dbCollectionMessage);
		wsa = WeiboSA.getInstance();
	}

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
	@Override
	public void store(WeiboMessage message,
			Map<String, String> crawlParameters, boolean original)
			throws JSONException {
		DBObject dbobj = message.toDBObject();

		// change the viptypes array of the user to a hashtable
		DBObject user = (BasicDBObject) dbobj.get("user");
		BasicDBList vipTypesList = (BasicDBList) user.get("vipTypes");
		if (vipTypesList != null) {
			DBObject vipTypes = new BasicDBObject();
			for (int i = 0; i < vipTypesList.size(); i++) {
				vipTypes.put((String) vipTypesList.get(i), true);
			}
			user.put("vipTypes", vipTypes);
			dbobj.put("user", user);
		}

		dbobj.put("_id", dbobj.get("mid"));

		// check if exists a message of the same id
		DBObject query = new BasicDBObject("_id", dbobj.get("mid"));
		DBObject dbresult = msgColl.findOne(query);
		DBObject keywords = null;
		boolean lastOriginal = false;
		if (dbresult != null) {
			keywords = (BasicDBObject) dbresult.get("keywords");
			if (dbresult.get("original") != null) {
				lastOriginal = (Boolean) dbresult.get("original");
			}
			if (keywords.containsField(crawlParameters.get("keyword").replaceAll("\\.", "(dot)")))
				return;
		}
		if (keywords == null) {
			keywords = new BasicDBObject();
		}

		// add the current crawl info to the previous sets
		keywords.put(crawlParameters.get("keyword").replaceAll("\\.", "(dot)"), true);
		dbobj.put("keywords", keywords);
		dbobj.put("region", crawlParameters.get("region"));
		dbobj.put("original", (!original) ? false : lastOriginal);
		// original: false has higher priority than true

		// check the original message, replace the object
		if (message.originalMessage != null) {
			this.store(message.originalMessage, crawlParameters, true);
			DBObject idOnlyMsg = new BasicDBObject();
			idOnlyMsg.put("_id", message.originalMessage.mid);
			dbobj.put("originalMessage", idOnlyMsg);
		}

		// sentiment analysis
		Map<String, Object> scores = wsa.getAnalyzedScores((String) dbobj
				.get("plainContent"));
		for (String key : scores.keySet()) {
			Object o = scores.get(key);
			if (o instanceof Integer)
				dbobj.put(key, (Integer)o);
			else
				dbobj.put(key, (Double)o);
		}

		msgColl.save(dbobj);

	}
}
