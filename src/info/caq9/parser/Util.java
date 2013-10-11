package info.caq9.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

public class Util {

	/**
	 * return the children of a tag name as a nodelist
	 * 
	 * @param node
	 * @param tagName
	 * @return
	 */
	public static NodeList getChildrenByTagName(Node node, String tagName) {
		if (node.getChildren() == null)
			return null;
		return node.getChildren().extractAllNodesThatMatch(
				new TagNameFilter(tagName));
	}

	/**
	 * return the first child of a tag name
	 * 
	 * @param node
	 * @param tagName
	 * @return null if no such child
	 */
	public static Node getFirstChildByTagName(Node node, String tagName) {
		NodeList list = getChildrenByTagName(node, tagName);
		if (list != null && list.size() > 0)
			return list.elementAt(0);
		else
			return null;
	}

	/**
	 * parse the http parameter string into a hashtable
	 * 
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> parseParameters(String str)
			throws UnsupportedEncodingException {
		Map<String, String> map = new HashMap<String, String>();
		String[] fields = str.split("&");
		for (String field : fields) {
			String[] key_value = field.split("=", 2);
			map.put(key_value[0], URLDecoder.decode(key_value[1], "utf-8"));
		}
		return map;
	}

	/**
	 * convert a number from a base to another base (max 64)
	 * 
	 * @param num
	 * @param toBase
	 * @param fromBase
	 * @return null if invalid
	 */
	final static String SYMBOLSHEET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/";

	public static String toBase(String num, int toBase, int fromBase) {
		// js version: http://forums.devnetwork.net/viewtopic.php?f=13&t=77205
		double decimal = 0;
		for (int i = 0; i < num.length(); i++) {
			decimal = decimal
					+ (Math.pow(fromBase, i) * SYMBOLSHEET.indexOf(num
							.toString().charAt(num.length() - (i + 1))));
		}
		String conversion = "";
		if (toBase > SYMBOLSHEET.length() || toBase <= 1) {
			return null;
		}
		while (decimal >= 1) {
			conversion = SYMBOLSHEET.charAt(new Double(
					(decimal - (toBase * Math.floor(decimal / toBase))))
					.intValue())
					+ conversion;
			decimal = Math.floor(decimal / toBase);
		}
		return conversion;
	};

	/**
	 * convert a weibo url string to mid
	 * 
	 * @param str
	 * @return
	 */
	public static long string2Mid(String str) {
		String mid = "";
		while (str.length() > 0) {
			String thisstr;
			if (str.length() < 4)
				thisstr = str;
			else
				thisstr = str.substring(str.length() - 4);

			String s = Util.toBase(thisstr, 10, 62);
			while (s.length() < 7)
				s = "0" + s;
			mid = s + mid;
			if (str.length() >= 4)
				str = str.substring(0, str.length() - 4);
			else
				break;
		}
		return new Long(mid);
	}

	/**
	 * join the array with the given delim
	 * 
	 * @param array
	 * @param delim
	 * @return
	 */
	public static String join(List<String> array, String delim) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (String s : array) {
			if (!first) {
				sb.append(delim);
			}
			sb.append(s);
			first = false;
		}
		return sb.toString();
	}

	/**
	 * convert a Date object to ISO format string
	 * 
	 * @param d
	 * @return
	 */
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public static String dateToISO(Date d) {
		df.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
		return df.format(d);
	}
}
