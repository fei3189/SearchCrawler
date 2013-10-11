package com.felixjiang.proxypool;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.felixjiang.proxy.WebProxy;

public class RpcService {
	ProxyPool pool = ProxyPool.GetInstance();
	Logger logger = Logger.getLogger(this.getClass().getName());
	public String FetchProxy(String request) {
		JSONObject result = new JSONObject();
		try {
			JSONObject object = new JSONObject(request);
			Integer number = null;
			if (object.has("number"))
				number = object.getInt("number");
			if (number == null || number <= 0)
				number = 1;
			Vector<WebProxy> proxies = pool.GetProxy(number);
			JSONArray array = new JSONArray();
			for (int i = 0; i < proxies.size(); ++i) {
				JSONObject item = new JSONObject();
				WebProxy p = proxies.get(i);
				item.put("host", p.host);
				item.put("port", p.port);
				item.put("type", p.type);
				array.put(item);
			}
			if (array.length() != 0) {
				result.put("errorcode", 0);
				result.put("errormsg", "");
				result.put("content", array);
			} else {
				result.put("errorcode", 2);
				result.put("errormsg", "No proxy left");
			}
			logger.log(Level.INFO, "FetchProxy: Request " + number + ", Response " + proxies.size());
			return result.toString();
		} catch (JSONException e) {
			return "{ \"errorcode\" : 1, \"errormsg\" : \"JSONException\" }";
		}
	}
}