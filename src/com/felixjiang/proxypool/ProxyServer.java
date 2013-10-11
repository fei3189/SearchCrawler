package com.felixjiang.proxypool;
import java.io.IOException;
import java.net.ServerSocket;

public class ProxyServer {
	ServerSocket serverSocket;
	
	ProxyServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}
	
	public void start() {
		
	}
}