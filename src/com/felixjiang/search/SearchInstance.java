package com.felixjiang.search;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * a search instance (query)
 * 
 * @author Fei Jiang
 */

public class SearchInstance implements Cloneable {
	private String baseUrl = "http://s.weibo.com/weibo/";
	public String keywords = "";
	public String province = "";
	public String district = "";
	public String startTime = "";
	public String endTime = "";
	public String html = null;
	public int page = 1;
	public String proxy = "";

	/**
	 * 
	 * @param kws: Search keywords, different keywords should be separated with space.
	 * @param prov: code of province, i.e. "11" for "Beijing"
	 * @param dist: code of district. 
	 * @param st, et: Searching results in time period [st, et]
	 * 
	 * kws is essential, others are optional.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public SearchInstance(String kws, String prov, String dist, String st,
			String et) throws UnsupportedEncodingException {
		keywords = kws;
		province = prov;
		district = dist;
		startTime = st;
		endTime = et;
		refine();
	}

	public void setPeriod(String st, String et) {
		startTime = st;
		endTime = et;
		refine();
	}
	
	private void refine() {
		if (keywords == null)
			keywords = "";
		if (province == null)
			province = "";
		if (district == null)
			district = "";
		if (startTime == null)
			startTime = "";
		if (endTime == null)
			endTime = "";
	}

	public String getProvince() {
		return province;
	}

	public String getDistrict() {
		return district;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setRegion(String prov, String dist) {
		province = prov;
		district = dist;
	}

	private String getRequestParams() throws UnsupportedEncodingException {
		String parameters = keywords;
		parameters = URLEncoder.encode(keywords, "utf-8");
		parameters = parameters.replaceAll("\\+", "%20");
		parameters = URLEncoder.encode(parameters, "utf-8");
		if (province != null && province != "") {
			if (district == null || district == "")
				district = "1000";
			parameters += "&region=custom:" + province + ":" + district;
		}
		if (startTime != null && startTime != "" || endTime != null
				&& endTime != "") {
			parameters += "&timescope=custom:" + startTime + ":" + endTime;
		}
		return parameters;
	}

	/**
	 * Generate request url. Result type is NODUP.
	 * @return
	 */
	public String getRequestUrl() {
		try {
			String parameters = getRequestParams();
			parameters += "&nodup=1&page=" + new Integer(page).toString();
			return baseUrl + parameters;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/**
	 * Generate request url.
	 * @param nodup: If true, simplified results if provided. Full results has many redundant messages
	 * 				  and spams.
	 * @return
	 */
	public String getRequestUrl(boolean nodup) {
		try {
			String parameters = getRequestParams();
			parameters += "&nodup=" + (nodup ? 1 : 0) + "&page=" + new Integer(page).toString();
			return baseUrl + parameters;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/**
	 * Get description of this search instance.
	 * @return
	 */
	public String getDesc() {
		return keywords + province + district + startTime + endTime;
	}

	@Override
	public Object clone() {
		SearchInstance o = null;
		try {
			o = (SearchInstance)super.clone();
			o.html = null;
			o.page = page;
			o.baseUrl = baseUrl;
			o.keywords = keywords;
			o.province = province;
			o.district = district;
			o.startTime = startTime;
			o.endTime = endTime;
		} catch (CloneNotSupportedException e) {
		}
		return o;
	}
	
	/**
	 * Notice, html is not considered when comparing two search instances.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SearchInstance))
			return false;
		SearchInstance ins = (SearchInstance)o;
		if (ins.keywords.equals(keywords) &&
			ins.province.equals(province) &&
			ins.district.equals(district) &&
			ins.startTime.equals(startTime) &&
			ins.endTime.equals(endTime) &&
			ins.page == page)
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return keywords.hashCode() * 107 + province.hashCode() * 97 + district.hashCode() * 29 +
				startTime.hashCode() * 17 + endTime.hashCode() * 2 + page;
	}
}