package info.caq9.refine;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.wbsenti.WeiboSA;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class RefineSentiment {

	public static void main(String[] args) throws ParseException, IOException {
		Mongo mongoconn = new Mongo("192.168.56.34", 27027);
		DB db = mongoconn.getDB("weibo20121208");
		DBCollection coll = db.getCollection("messages");
		SearchConfig.loadConfig("config.json");
		WeiboSA wsa = WeiboSA.getInstance();

		System.out.println("Records: " + coll.count());

		DBCursor cursor = coll.find();
		int count = 0;
		while (cursor.hasNext()) {
			DBObject dbobj = cursor.next();
			Map<String, Object> sentiRecord = wsa.getAnalyzedScores((String) dbobj
					.get("plainContent"));
			for (String key : sentiRecord.keySet()) {
				if (!key.equals("_id")) {
					Object o = sentiRecord.get(key);
					if (o instanceof Integer)
						dbobj.put(key, (Integer)o);
					else
						dbobj.put(key, (Double)o);
				}
			}
			coll.save(dbobj);
			count += 1;
			if (count % 10000 == 0)
				System.out.println(count);
		}

		mongoconn.close();
		System.out.println("Done: " + count);
	}

}
