package com.felixjiang.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration for the crawler and storage
 * 
 * @author felix, caq
 * 
 */
public class SearchConfig {
	public static String serverHost;
	public static Integer serverPort;
	public static String dbName;
	public static String searchInstances;
	public static String resources = "resources/"; // Path for resources, some as data for segmentation.
	public static String proxyServer;
	public static Integer proxyPort;
	
	public static String dbCollectionMessage = "messages",
			dbCollectionSearchSource = "searchSource",
			dbCollectionSearchTime = "time",
			searchTime = "time";

	public static int parameterInterval = 60, parameterCycle = 1800;
	public static boolean proxyEnabled = false;
	public static String proxyLog = "log.txt";
	public static JSONArray queries = new JSONArray();
	public static Integer MAX_PAGES = 50;
	public static boolean nodup = true;
	
	public static String proxyFile = null;
	public static boolean proxyFromWebsite = false;
	
	/**
	 * read the json file. lines starting with # are notes. multiple lines
	 * allowed.
	 * 
	 * @param fileName
	 *            config file name
	 */
	public static void loadConfig(String fileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName), "utf-8"));
			String line;
			StringBuffer jsonStr = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				if (line.trim().startsWith("#")) {
					continue;
				}
				jsonStr.append(line);
				// try if the json string is complete
				JSONObject jConfig = new JSONObject();
				try {
					jConfig = new JSONObject(jsonStr.toString());
				} catch (JSONException je) {
					// not complete yet, continue to read the remaining lines
					continue;
				}
				if (jConfig.has("server")) {
					JSONObject jServer = jConfig.getJSONObject("server");
					if (jServer.has("host")) {
						serverHost = jServer.getString("host");
					}
					if (jServer.has("port")) {
						serverPort = jServer.getInt("port");
					}
				}
				if (jConfig.has("db")) {
					JSONObject jDb = jConfig.getJSONObject("db");
					if (jDb.has("name")) {
						dbName = jDb.getString("name");
					}
					if (jDb.has("collection")) {
						JSONObject jDbCollection = jDb
								.getJSONObject("collection");
						if (jDbCollection.has("message")) {
							dbCollectionMessage = jDbCollection
									.getString("message");
						}
						if (jDbCollection.has("searchSource")) {
							dbCollectionSearchSource = jDbCollection
									.getString("searchSource");
						}
					}
				}
				if (jConfig.has("parameter")) {
					JSONObject jParameter = jConfig.getJSONObject("parameter");
					if (jParameter.has("interval")) {
						parameterInterval = jParameter.getInt("interval");
					}
					if (jParameter.has("cycle")) {
						parameterCycle = jParameter.getInt("cycle");
					}
				}
				if (jConfig.has("page")) {
					MAX_PAGES = jConfig.getInt("page");
				}
				if (jConfig.has("proxy")) {
					JSONObject jProxy = jConfig.getJSONObject("proxy");
					if (jProxy.has("enabled")) {
						proxyEnabled = jProxy.getBoolean("enabled");
					}
					if (jProxy.has("server")) {
						proxyServer = jProxy.getString("server");
					}
					if (jProxy.has("port")) {
						proxyPort = jProxy.getInt("port");
					}
					if (jProxy.has("log")) { 
						proxyLog = jProxy.getString("log");
						System.out.println(proxyLog);
					}
				}		
				if (jConfig.has("searchInstances")) {
					searchInstances = jConfig.getString("searchInstances");
				}
				if (jConfig.has("nodup")) {
					nodup = jConfig.getBoolean("nodup");
				}
				if (jConfig.has("resources")) {
					resources = jConfig.getString("resources");
					if (!resources.endsWith("/"))
						resources += "/";
				}
				if (jConfig.has("proxyFile"))
					proxyFile = jConfig.getString("proxyFile");
				if (jConfig.has("proxyFromWebsite"))
					proxyFromWebsite = jConfig.getBoolean("proxyFromWebsite");
			}
		} catch (JSONException je) {
			Logger.getLogger("SearchConfig").log(Level.SEVERE,
					"JSON parsing error");
			je.printStackTrace();
			System.exit(-1);
		} catch (IOException ioe) {
			Logger.getLogger("SearchConfig").log(Level.SEVERE,
					"IOException");
			ioe.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		Logger.getLogger("SearchConfig").log(Level.INFO, "Config: " + info());
		loadQueries(SearchConfig.searchInstances);
	}

	private static String info() {
		StringBuffer result = new StringBuffer();
		Field[] fields = SearchConfig.class.getFields();
		for (Field field : fields) {
			try {
				result.append(field.getName() + ": " + field.get(null) + " \n");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return result.toString();
	}
	
	/**
	 * Load the queries from file.
	 * @param fileName
	 */
	private static void loadQueries(String fileName) {
		try {
			queries = new JSONArray();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			String line;
			JSONObject obj;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					obj = new JSONObject(line);
					queries.put(obj);
				}
			}
			reader.close();
			System.out.println("Load queries: " + queries.length());
		} catch (Exception e) {
		}
	}
	
	private static void check() {
		if (serverHost == null || serverPort == null) {
			System.out.println("DB server host and port should be provided");
			System.exit(-1);
		}
		if (dbName == null) {
			System.out.println("DB name should be provided");
			System.exit(-1);
		}
		if (searchInstances == null) {
			System.out.println("File path of seach instances should be provided");
			System.exit(-1);
		}
		if (resources == null) {
			System.out.println("Directory of resources of should be provided");
			System.exit(-1);
		}
		if (proxyServer == null || proxyPort == null) {
			System.out.println("Proxy server host and port should be provided");
			System.exit(-1);
		}
	}
}