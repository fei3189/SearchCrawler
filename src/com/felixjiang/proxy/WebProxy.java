package com.felixjiang.proxy;
public class WebProxy {
	public String host;
	public String port;
	public String type;
	public WebProxy(String h, String p, String t) {
		host = h;
		port = p;
		type = t;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this.hashCode() != obj.hashCode()) {
			return false;
		}
		if (obj instanceof WebProxy) {
			WebProxy proxyObj = (WebProxy)obj;
			if (host.equals(proxyObj.host) && port.equals(proxyObj.port) && type.equals(proxyObj.type))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return host + "\t" + port + "\t" + type;
	}
	
	@Override
	public int hashCode() {
		return host.hashCode() * 233 + port.hashCode() * 419 + type.hashCode() * 2;
	}
}