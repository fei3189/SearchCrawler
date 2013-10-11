package com.felixjiang.proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProxyRpcClient {
	private XmlRpcClient client = new XmlRpcClient();
	private int errorcode = 0;
	private String errorMsg = "";

	public ProxyRpcClient(String rpcServerHost, String rpcServerPort) throws MalformedURLException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL("http://" + rpcServerHost + ":" + rpcServerPort));
		config.setEnabledForExtensions(true);
		config.setConnectionTimeout(5 * 1000);
		config.setReplyTimeout(5 * 1000);

		client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		client.setConfig(config);
	}
	
	public int GetErrorcode() {
		return errorcode;
	}
	
	public String GetErrorMsg() {
		return errorMsg;
	}
	
	public ProxyRpcClient() throws MalformedURLException {
		this("127.0.0.1", "8080");
	}

	public Vector<WebProxy> FetchProxy(int number)
			throws XmlRpcException, MalformedURLException,
			NoProxyFoundException {
		try {
			JSONObject request = new JSONObject();
			request.put("number", new Integer(number));
			JSONObject response = new JSONObject((String) client.execute(
					"RpcService.FetchProxy",
					new Object[] { request.toString() }));
			errorcode = response.getInt("errorcode");
			errorMsg = response.getString("errormsg");
			if (errorcode == 0) {
				JSONArray array = response.getJSONArray("content");
				Vector<WebProxy> vec = new Vector<WebProxy>();
				for (int i = 0; i < array.length(); ++i) {
					JSONObject item = array.getJSONObject(i);
					vec.add(new WebProxy(item.getString("host"), item
							.getString("port"), item.getString("type")));
				}
				return vec;
			} else if (errorcode == 2) {
				throw new MalformedURLException();
			} else if (errorcode == 4) {
				throw new NoProxyFoundException();
			} else
				return new Vector<WebProxy>();
		} catch (JSONException e) {
			errorcode = -1;
			return new Vector<WebProxy>();
		}
	}
}