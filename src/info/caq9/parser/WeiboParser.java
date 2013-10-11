package info.caq9.parser;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.DefinitionList;
import org.htmlparser.tags.DefinitionListBullet;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.json.JSONException;
import org.json.JSONObject;

public class WeiboParser {
	/**
	 * given the html, parse it, extract the weibo messages
	 * 
	 * @param str
	 *            the html
	 * @param crawlTimestamp
	 *            the millisecond timestamp of the crawling time
	 * 
	 * @return an array of Weibo messages. null if validation code is required;
	 *         an empty list if no results
	 * @throws ParserException
	 * @throws UnsupportedEncodingException
	 * @throws JSONException
	 */
	public static List<WeiboMessage> parse(String str, long crawlTimestamp) {

		try {
			// get the "pl_weibo_feedlist" part
			String html = null;
			String[] lines = str.split("\n");
			for (String line : lines) {
				if (!line.startsWith("<script") || !line.endsWith("</script>")) {
					continue;
				}
				if (!line.contains("pl_weibo_feedlist"))
					continue;
				String jsonstr = line.substring(line.indexOf("<script") + 8,
						line.lastIndexOf("</script>"));
				jsonstr = jsonstr.substring(jsonstr.indexOf('(') + 1,
						jsonstr.lastIndexOf(')'));
				// {"pid":"pl_weibo_feedlist",...}
				JSONObject jobj = new JSONObject(jsonstr);
				if (!jobj.getString("pid").equals("pl_weibo_feedlist")) {
					continue;
				}
				html = jobj.getString("html");
			}
			
			if (html == null) { // usually caused by validation code requirement
				return null;
			}
			
			List<WeiboMessage> messages = new ArrayList<WeiboMessage>();
			Parser hp = new Parser();
			hp.setInputHTML(html);
	
			// if no result:
			// - div class=pl_noresult
			// - - div class=search_noresult
			// - - - p class=noresult_tit
			// ...
	
			
			// we don't use the Parser method to avoid disrupt the later extractions
			if (html.indexOf("pl_noresult") >= 0) {
				return messages; // empty
			}
			// NodeList noresultList = hp
			// .extractAllNodesThatMatch(new HasAttributeFilter("class",
			// "pl_noresult"));
			// if (noresultList != null && noresultList.size() > 0) {
			// return messages;
			// }
	
			// structure:
			// - div class=search_feed
			// - - div node-type=feed_list
			// - - - dl class=feed_list
			// - - - - dt class=face
			// - - - - dd class=content
			// - - div class="search_tips clearfix"
	
			// each dl is a message
			SimpleNodeIterator dlNodeIter = hp.extractAllNodesThatMatch(
					new HasAttributeFilter("class", "feed_list")).elements();
			while (dlNodeIter.hasMoreNodes()) {
				DefinitionList dlNode = (DefinitionList) dlNodeIter.nextNode();
				Long mid = new Long(dlNode.getAttribute("mid"));
	
				// extract user's id, avatarURL, screenName, profileURL
				DefinitionListBullet dtNode = (DefinitionListBullet) Util
						.getFirstChildByTagName(dlNode, "dt");
				WeiboUser user = parseAvatar((LinkTag) Util.getFirstChildByTagName(
						dtNode, "a"));
	
				DefinitionListBullet ddNode = (DefinitionListBullet) Util
						.getFirstChildByTagName(dlNode, "dd");
				WeiboMessage msg = parseContent(ddNode);
				msg.mid = mid;
				msg.crawlTime = new Date(crawlTimestamp);
	
				if (msg.user.id < 0)
					msg.user.id = user.id;
				if (msg.user.avatarURL == null)
					msg.user.avatarURL = user.avatarURL;
				if (msg.user.screenName == null)
					msg.user.screenName = user.screenName;
				if (msg.user.profileURL == null)
					msg.user.profileURL = user.profileURL;
	
				msg.refine();
	
				messages.add(msg);
			}
			return messages;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * parse an "a" block of the avatar: profileURL, screenName and optional:
	 * user's id, avatarURL
	 * 
	 * @param dtNode
	 * @return a user structure
	 * @throws UnsupportedEncodingException
	 * @throws NumberFormatException
	 */
	static WeiboUser parseAvatar(LinkTag aNode) throws NumberFormatException,
			UnsupportedEncodingException {
		WeiboUser user = new WeiboUser();
		// dt
		// - a href=, title=, suda-data="key=...&value=weibo_feed_3:1953392292"
		// or usercard="id=1760267054"
		// - - img src=
		user.profileURL = aNode.getLink();
		user.screenName = aNode.getAttribute("title");
		String suda_data = aNode.getAttribute("suda-data");
		String usercard = aNode.getAttribute("usercard");
		if (suda_data != null) { // from the new post
			String suda_data_value = Util.parseParameters(suda_data).get(
					"value");
			if (suda_data_value.lastIndexOf(':') >= 0)
				user.id = new Long(suda_data_value.substring(suda_data_value
						.lastIndexOf(':') + 1));
		} else if (usercard != null) { // from the original post
			user.id = new Long(Util.parseParameters(usercard).get("id"));
		}

		ImageTag imgNode = (ImageTag) Util.getFirstChildByTagName(aNode, "img");
		if (imgNode != null)
			user.avatarURL = imgNode.getAttribute("src");

		return user;
	}

	/**
	 * parse a ul block of the pic list
	 * 
	 * @param ulNode
	 * @return a message structure
	 * @throws UnsupportedEncodingException
	 */
	static WeiboMessage parsePiclist(BulletList ulNode)
			throws UnsupportedEncodingException {
		WeiboMessage msg = new WeiboMessage();
		List<String> imageURLs = new ArrayList<String>(), videoThumbnailURLs = new ArrayList<String>();
		List<Map<String, String>> musicDatas = new ArrayList<Map<String, String>>(), videoDatas = new ArrayList<Map<String, String>>();

		NodeList liList = Util.getChildrenByTagName(ulNode, "li");
		SimpleNodeIterator liIter = liList.elements();
		while (liIter.hasMoreNodes()) {
			Bullet liNode = (Bullet) liIter.nextNode();
			// image
			SimpleNodeIterator imageIter = liNode
					.getChildren()
					.extractAllNodesThatMatch(
							new HasAttributeFilter("action-type",
									"feed_list_media_img")).elements();
			while (imageIter.hasMoreNodes()) {
				ImageTag imgNode = (ImageTag) imageIter.nextNode();
				imageURLs.add(imgNode.getAttribute("src"));
			}
			// music
			SimpleNodeIterator musicIter = liNode
					.getChildren()
					.extractAllNodesThatMatch(
							new HasAttributeFilter("action-type",
									"feed_list_media_music")).elements();
			while (musicIter.hasMoreNodes()) {
				LinkTag musicNode = (LinkTag) musicIter.nextNode();
				musicDatas.add(Util.parseParameters(musicNode
						.getAttribute("action-data")));
			}
			// video
			SimpleNodeIterator videoIter = liNode
					.getChildren()
					.extractAllNodesThatMatch(
							new HasAttributeFilter("action-type",
									"feed_list_media_video")).elements();
			while (videoIter.hasMoreNodes()) {
				ImageTag videoNode = (ImageTag) videoIter.nextNode();
				ImageTag videoThumbnailNode = null;
				if (!videoNode.getPreviousSibling().getClass().toString()
						.endsWith(".tags.ImageTag"))
					videoThumbnailNode = (ImageTag) videoNode
							.getPreviousSibling().getPreviousSibling();
				else
					videoThumbnailNode = (ImageTag) videoNode
							.getPreviousSibling();
				videoThumbnailURLs.add(videoThumbnailNode.getAttribute("src"));
				videoDatas.add(Util.parseParameters(videoNode
						.getAttribute("action-data")));
			}
		}

		msg.imageURLs = imageURLs;
		msg.musicDatas = musicDatas;
		msg.videoThumbnailURLs = videoThumbnailURLs;
		msg.videoDatas = videoDatas;

		return msg;
	}

	/**
	 * parse a dd or p block of the weibo info: repost count, comment count,
	 * post time, url, post source
	 * 
	 * @param infoNode
	 * @return a message structure
	 * @throws UnsupportedEncodingException
	 */
	static WeiboMessage parseInfo(Node infoNode) throws UnsupportedEncodingException {
		WeiboMessage msg = new WeiboMessage();

		Span spanNode = (Span) Util.getFirstChildByTagName(infoNode, "span");
		SimpleNodeIterator aNodeIter = Util.getChildrenByTagName(spanNode, "a")
				.elements();
		while (aNodeIter.hasMoreNodes()) {
			LinkTag aNode = (LinkTag) aNodeIter.nextNode();
			String innerText = aNode.getLinkText();
			if (innerText.indexOf("转发(") >= 0) {
				msg.repostCount = new Integer(innerText.substring(
						innerText.indexOf("(") + 1, innerText.indexOf(")")));
			} else if (innerText.indexOf("评论(") >= 0) {
				msg.commentCount = new Integer(innerText.substring(
						innerText.indexOf("(") + 1, innerText.indexOf(")")));
			}
		}
		LinkTag aNode = (LinkTag) infoNode
				.getChildren()
				.extractAllNodesThatMatch(
						new AndFilter(new TagNameFilter("a"),
								new HasAttributeFilter("class", "date")))
				.elementAt(0);
		if (aNode.getAttribute("date") != null
				&& aNode.getAttribute("date").length() > 0)
			msg.postTime = new Date(new Long(aNode.getAttribute("date")));
		msg.url = aNode.getLink();

		Node aSourceNode = aNode.getNextSibling();
		while (!aSourceNode.getClass().toString().endsWith(".tags.LinkTag")) {
			aSourceNode = aSourceNode.getNextSibling();
			if (aSourceNode == null)
				break;
		}
		if (aSourceNode != null) {
			LinkTag aSourceTag = (LinkTag) aSourceNode;
			String[] postSource = new String[] { aSourceTag.getLinkText(),
					aSourceTag.getLink() };
			msg.postSource = postSource;
		}

		return msg;
	}

	/**
	 * parse a dd block of the message content
	 * 
	 * @param ddNode
	 * @return a message structure
	 * @throws UnsupportedEncodingException
	 * @throws ParserException 
	 */
	static WeiboMessage parseContent(DefinitionListBullet ddNode)
			throws UnsupportedEncodingException, ParserException {
		WeiboMessage message = new WeiboMessage();
		WeiboUser user = new WeiboUser();

		// dd class=content
		// - p node-type=feed_list_content
		// - - a (user's information)
		// suda-data="key=tblog_search_v4.1&value=weibo_feed_other:1220765964",
		// usercard="id=1220765964&usercardkey=weibo_mp", title, href,
		// nick-name
		// - - a (link to the VIPs) -- optional, multiple
		// - - - img title=新浪机构认证/新浪个人认证/微博会员/...
		// - - em (content)

		Node outerNode = (ParagraphTag) Util
				.getFirstChildByTagName(ddNode, "p");
		if (outerNode == null) {
			// in the original message, the ddNode is already the outer one
			outerNode = ddNode;
		}
		NodeList aList = Util.getChildrenByTagName(outerNode, "a");
		if (aList.size() == 0) {
			// the original message has been deleted
			return message;
		}
		List<String> viptypes = new ArrayList<String>();
		// search all the a until we meet the em
		TagNode emNode = (TagNode) Util.getFirstChildByTagName(outerNode, "em");
		// the first "a" tag (user info)
		user = parseAvatar((LinkTag) aList.elementAt(0));

		for (int idx = 1; idx < aList.size(); idx++) {
			LinkTag aNode = (LinkTag) aList.elementAt(idx);
			if (aNode == null)
				continue;
			if (aNode.getStartPosition() >= emNode.getStartPosition())
				break;
			ImageTag imgNode = (ImageTag) Util.getFirstChildByTagName(aNode,
					"img");
			viptypes.add(imgNode.getAttribute("title").trim());
		}
		user.vipTypes = viptypes;
		message.user = user;

		if (emNode == null) {
			return message;
		}
		Node emSibling = emNode.getNextSibling();
		StringBuffer html = new StringBuffer(), plain = new StringBuffer();
		while (emSibling != null
				&& !emSibling.toHtml().equalsIgnoreCase("</em>")) {
			html.append(emSibling.toHtml());
/*			System.out.println(emSibling.toHtml());
			Parser parser = new Parser()
//			Node[] nl = emSibling.getChildren().toNodeArray();
			for (int nl_i = 0; nl_i < nl.length; ++nl_i) {
				if (nl[nl_i] instanceof ImageTag)
					plain.append(((ImageTag)nl[nl_i]).getAttribute("title"));
				else
					plain.append(nl[nl_i].toPlainTextString());
			}*/
//			plain.append(emSibling.toPlainTextString());
			emSibling = emSibling.getNextSibling();
		}
		message.htmlContent = html.toString();
		
		Parser parser = new Parser();
		parser.setInputHTML(html.toString());
		NodeIterator it = parser.elements();
		while (it.hasMoreNodes()) {
			Node node = it.nextNode();
			if (node instanceof ImageTag)
				plain.append(((ImageTag)node).getAttribute("title"));
			else
				plain.append(node.toPlainTextString());
		}
		message.plainContent = plain.toString();

		// still under dd class=content:
		// - ul class=piclist, node-type=feed_list_media_prev -- optional
		// - - li
		// - - - img src=, class=bigcursor, action-type=feed_list_media_img
		// - - li
		// - - - img src= (thumbnail of the video)
		// - - - img class=video_play, action-type=feed_list_media_video, ...
		// action-data=
		// - - li
		// - - - a action-type=feed_list_media_music, action-data=, href=

		BulletList ulNode = (BulletList) Util.getFirstChildByTagName(ddNode,
				"ul");
		if (ulNode != null) { // has pic list
			WeiboMessage ulMessage = parsePiclist(ulNode);
			message.imageURLs = ulMessage.imageURLs;
			message.musicDatas = ulMessage.musicDatas;
			message.videoThumbnailURLs = ulMessage.videoThumbnailURLs;
			message.videoDatas = ulMessage.videoDatas;
		}

		// still under dd class=content:
		// - dl class="comment W_textc W_linecolor W_bgcolor" -- optional
		// - - dd class="arrow W_bgcolor_arrow"
		// - - dt node-type=feed_list_forwardContent
		// - - - (same as inside the "p node-type=feed_list_content")
		// - - dd
		// - - - ul (same as the piclist) -- optional
		// - - dd class=expand style="display:none;" (ignored)
		// - - dd class="info W_linkb W_textb"
		// - - - (same as inside the "p class='info W_linkb W_textb'")

		NodeList dlList = ddNode.getChildren().extractAllNodesThatMatch(
				new AndFilter(new TagNameFilter("dl"), new HasAttributeFilter(
						"class", "comment W_textc W_linecolor W_bgcolor")));
		if (dlList.size() > 0) { // reposted
			new WeiboMessage();

			DefinitionList dlNode = (DefinitionList) dlList.elementAt(0);
			DefinitionListBullet dtNode = (DefinitionListBullet) (DefinitionListBullet) Util
					.getFirstChildByTagName(dlNode, "dt");

			WeiboMessage originalMsg = parseContent(dtNode);

			LinkTag avatarANode = (LinkTag) Util
					.getFirstChildByTagName(dtNode, "a");
			if (avatarANode != null) {
				// the original post hasn't been deleted
				WeiboUser originalUser = parseAvatar(avatarANode);
				if (originalMsg.user.id < 0)
					originalMsg.user.id = originalUser.id;
				if (originalMsg.user.avatarURL == null)
					originalMsg.user.avatarURL = originalUser.avatarURL;
				if (originalMsg.user.screenName == null)
					originalMsg.user.screenName = originalUser.screenName;
				if (originalMsg.user.profileURL == null)
					originalMsg.user.profileURL = originalUser.profileURL;
			}

			DefinitionListBullet ulOuterNode = (DefinitionListBullet) Util
					.getChildrenByTagName(dlNode, "dd").elementAt(1);
			if (ulOuterNode != null
					&& Util.getFirstChildByTagName(ulOuterNode, "ul") != null) {
				// the original post has pic
				ulNode = (BulletList) Util.getFirstChildByTagName(ulOuterNode,
						"ul");
				WeiboMessage ulMessage = parsePiclist(ulNode);
				originalMsg.imageURLs = ulMessage.imageURLs;
				originalMsg.musicDatas = ulMessage.musicDatas;
				originalMsg.videoThumbnailURLs = ulMessage.videoThumbnailURLs;
				originalMsg.videoDatas = ulMessage.videoDatas;
			}

			DefinitionListBullet infoNode = (DefinitionListBullet) dlNode
					.getChildren()
					.extractAllNodesThatMatch(
							new AndFilter(new TagNameFilter("dd"),
									new HasAttributeFilter("class",
											"info W_linkb W_textb")))
					.elementAt(0);
			WeiboMessage infoMsg = parseInfo(infoNode);
			originalMsg.repostCount = infoMsg.repostCount;
			originalMsg.commentCount = infoMsg.commentCount;
			originalMsg.postTime = infoMsg.postTime;
			originalMsg.url = infoMsg.url;
			originalMsg.postSource = infoMsg.postSource;

			message.originalMessage = originalMsg;
		}

		// still under dd class=content:
		// - p class="info W_linkb W_textb"
		// - - span
		// - - - a: 转发(x)
		// - - - i
		// - - - a: 评论(x)
		// - - a class=date suda-data=, node-type=feed_list_item_date,
		// date=timestamp(ms) href=
		// - - 来自
		// - - a href=
		// - - - 来源
		ParagraphTag pInfoNode = (ParagraphTag) ddNode
				.getChildren()
				.extractAllNodesThatMatch(
						new AndFilter(new TagNameFilter("p"),
								new HasAttributeFilter("class",
										"info W_linkb W_textb"))).elementAt(0);
		if (pInfoNode != null) {
			WeiboMessage infoMsg = parseInfo(pInfoNode);
			message.repostCount = infoMsg.repostCount;
			message.commentCount = infoMsg.commentCount;
			message.postTime = infoMsg.postTime;
			message.url = infoMsg.url;
			message.postSource = infoMsg.postSource;
		}

		// still under dd class=content:
		// - div class=hot_feed2 -- optional
		NodeList hotNodeList = ddNode.getChildren().extractAllNodesThatMatch(
				new AndFilter(new TagNameFilter("div"), new HasAttributeFilter(
						"class", "hot_feed2")));
		if (hotNodeList.size() > 0)
			message.isPopular = true;

		return message;
	}
	
	/**
	 * By Fei Jiang.
	 * Judge whether a html page is valid. A html page is not valid is it is not completed
	 * or do not contains the substring "pid":"pl_weibo_feedlist".
	 * @param html
	 * @return
	 */
	public static boolean isValid(String html) {
		if (html == null)
			return false;
		if (html.lastIndexOf("</html>") < html.length() - 20)  //Not a full html document, it seldom occurs, but exactly exists
			return false;
		if (html.indexOf("\"pid\":\"pl_weibo_feedlist\"") > 0)
			return true;
		return false;
	}
}
