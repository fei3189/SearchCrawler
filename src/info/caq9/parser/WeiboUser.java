package info.caq9.parser;

import java.lang.reflect.Field;
import java.util.List;

public class WeiboUser {
	public long id = -1; // 用户id
	public String avatarURL = null; // 头像图片地址
	public String screenName = null; // 昵称
	public String profileURL = null; // 个人页面地址
	public List<String> vipTypes = null; // 身份认证：新浪机构认证/新浪个人认证/微博会员/微博达人

	public String toString() {
		StringBuffer result = new StringBuffer();
		Field[] fields = this.getClass().getFields();
		for (Field field : fields) {
			result.append(field.getName());
			result.append('\t');
			try {
				result.append(field.get(this));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			result.append('\n');
		}
		return result.toString();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAvatarURL() {
		return avatarURL;
	}

	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	public String getProfileURL() {
		return profileURL;
	}

	public void setProfileURL(String profileURL) {
		this.profileURL = profileURL;
	}

	public List<String> getVipTypes() {
		return vipTypes;
	}

	public void setVipTypes(List<String> vipTypes) {
		this.vipTypes = vipTypes;
	}
}
