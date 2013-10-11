package com.felixjiang.main;
import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.proxypool.ProxyPool;
import com.felixjiang.proxypool.RpcService;

public class StartService {
	private ProxyPool pool = ProxyPool.GetInstance();

	public void start(int port) throws XmlRpcException, IOException {
		if (SearchConfig.proxyFile != null && !SearchConfig.proxyFile.isEmpty()) {
			pool.LoadProxyList(SearchConfig.proxyFile);
		}
		WebServer webServer = new WebServer(port);
		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		phm.addHandler("RpcService", RpcService.class);
		xmlRpcServer.setHandlerMapping(phm);
		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
				.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);

		webServer.start();
		System.out.println("Succeed");
	}

	public static void main(String args[]) {
		SearchConfig.loadConfig("config.json");
		try {
			new StartService().start(8085);
		} catch (Exception e) {
			System.out.println("Failed");
		}
	}
}