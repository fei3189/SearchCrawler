package com.felixjiang.config;
import java.util.HashMap;

/**
 * city and Beijing's district codes
 * 
 * @author felix
 * 
 */
public class CityCode {
	// 34 provinces
	public static HashMap<String, String> provinceMap = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("34", "安徽");
			put("11", "北京");
			put("50", "重庆");
			put("35", "福建");
			put("62", "甘肃");
			put("44", "广东");
			put("45", "广西");
			put("52", "贵州");
			put("46", "海南");
			put("13", "河北");
			put("23", "黑龙江");
			put("41", "河南");
			put("42", "湖北");
			put("43", "湖南");
			put("15", "内蒙古");
			put("32", "江苏");
			put("36", "江西");
			put("22", "吉林");
			put("21", "辽宁");
			put("64", "宁夏");
			put("63", "青海");
			put("14", "山西");
			put("37", "山东");
			put("31", "上海");
			put("51", "四川");
			put("12", "天津");
			put("54", "西藏");
			put("65", "新疆");
			put("53", "云南");
			put("33", "浙江");
			put("61", "陕西");
			put("71", "台湾");
			put("81", "香港");
			put("82", "澳门");
		}
	};

	// 18 districts of Beijing
	public static HashMap<String, String> beijingMap = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("1", "东城区");
			put("2", "西城区");
			put("3", "崇文区");
			put("4", "宣武区");
			put("5", "朝阳区");
			put("6", "丰台区");
			put("7", "石景山区");
			put("8", "海淀区");
			put("9", "门头沟区");
			put("11", "房山区");
			put("12", "通州区");
			put("13", "顺义区");
			put("14", "昌平区");
			put("15", "大兴区");
			put("16", "怀柔区");
			put("17", "平谷区");
			put("28", "密云县");
			put("29", "延庆县");
		}
	};
}