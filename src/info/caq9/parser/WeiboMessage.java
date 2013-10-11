package info.caq9.parser;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class WeiboMessage {
	public long mid = -1;
	public WeiboUser user = new WeiboUser();
	// 内容的html格式可包含：标红；用户、标签、链接的url信息，表情符号的图片信息
	public String htmlContent = null, plainContent = null; // 内容，html或纯文本格式
	public List<String> imageURLs = null, videoThumbnailURLs = null;
	public List<Map<String, String>> musicDatas = null, videoDatas = null; // 音视频信息
	public int repostCount = 0, commentCount = 0; // 转发数，评论数
	public Date postTime = null, crawlTime = null; // 发表时间，抓取时间
	public String url = null; // 微博地址
	public String[] postSource = null; // 发表来源，[0]为中文名称，[1]为来源服务地址
	public WeiboMessage originalMessage = null; // 转发的原始推文
	public boolean isPopular = false; // 是否热门微博

	public String toString() {
		StringBuffer result = new StringBuffer();
		Field[] fields = this.getClass().getFields();
		for (Field field : fields) {
			result.append(field.getName());
			result.append('\t');
			try {
				if (field.getName().equals("postSource")
						&& this.postSource != null) {
					result.append("[" + this.postSource[0] + ", "
							+ this.postSource[1] + "]");
				} else {
					result.append(field.get(this));
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			result.append('\n');
		}
		return result.toString();
	}

	/**
	 * refine some fields
	 */
	public void refine() {
		if (this.originalMessage != null) {
			if (this.originalMessage.crawlTime == null)
				this.originalMessage.crawlTime = this.crawlTime;
			if (this.originalMessage.mid < 0) {
				String origUrl = this.originalMessage.url;
				this.originalMessage.mid = Util.string2Mid(origUrl
						.substring(origUrl.lastIndexOf('/') + 1));
			}
			if (this.originalMessage.user.id < 0) {
				String origUrl = this.originalMessage.url;
				int slash2 = origUrl.lastIndexOf('/');
				int slash1 = origUrl.lastIndexOf('/', slash2 - 1);
				if (slash2 > slash1 + 1)
					this.originalMessage.user.id = new Long(origUrl.substring(
							slash1 + 1, slash2));
			}
			this.originalMessage.refine();
		}
		if (this.user.vipTypes != null && this.user.vipTypes.size() <= 0)
			this.user.vipTypes = null;
		if (this.imageURLs != null && this.imageURLs.size() <= 0)
			this.imageURLs = null;
		if (this.videoThumbnailURLs != null
				&& this.videoThumbnailURLs.size() <= 0)
			this.videoThumbnailURLs = null;
		if (this.musicDatas != null && this.musicDatas.size() <= 0)
			this.musicDatas = null;
		if (this.videoDatas != null && this.videoDatas.size() <= 0)
			this.videoDatas = null;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

	public WeiboUser getUser() {
		return user;
	}

	public void setUser(WeiboUser user) {
		this.user = user;
	}

	public String getHtmlContent() {
		return htmlContent;
	}

	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}

	public String getPlainContent() {
		return plainContent;
	}

	public void setPlainContent(String plainContent) {
		this.plainContent = plainContent;
	}

	public List<String> getImageURLs() {
		return imageURLs;
	}

	public void setImageURLs(List<String> imageURLs) {
		this.imageURLs = imageURLs;
	}

	public List<String> getVideoThumbnailURLs() {
		return videoThumbnailURLs;
	}

	public void setVideoThumbnailURLs(List<String> videoThumbnailURLs) {
		this.videoThumbnailURLs = videoThumbnailURLs;
	}

	public List<Map<String, String>> getMusicDatas() {
		return musicDatas;
	}

	public void setMusicDatas(List<Map<String, String>> musicDatas) {
		this.musicDatas = musicDatas;
	}

	public List<Map<String, String>> getVideoDatas() {
		return videoDatas;
	}

	public void setVideoDatas(List<Map<String, String>> videoDatas) {
		this.videoDatas = videoDatas;
	}

	public int getRepostCount() {
		return repostCount;
	}

	public void setRepostCount(int repostCount) {
		this.repostCount = repostCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	public Date getPostTime() {
		return postTime;
	}

	public void setPostTime(Date postTime) {
		this.postTime = postTime;
	}

	public Date getCrawlTime() {
		return crawlTime;
	}

	public void setCrawlTime(Date crawlTime) {
		this.crawlTime = crawlTime;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String[] getPostSource() {
		return postSource;
	}

	public void setPostSource(String[] postSource) {
		this.postSource = postSource;
	}

	public WeiboMessage getOriginalMessage() {
		return originalMessage;
	}

	public void setOriginalMessage(WeiboMessage originalMessage) {
		this.originalMessage = originalMessage;
	}

	public boolean isPopular() {
		return isPopular;
	}

	public void setPopular(boolean isPopular) {
		this.isPopular = isPopular;
	}

	public DBObject toDBObject() {
		DBObject dbobj = (DBObject) JSON.parse(new JSONObject(this).toString());
		dbobj.put("postTime", this.postTime);
		dbobj.put("crawlTime", this.crawlTime);
		return dbobj;
	}
}
