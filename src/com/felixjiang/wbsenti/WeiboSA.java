package com.felixjiang.wbsenti;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.felixjiang.config.SearchConfig;
import com.felixjiang.library.EmotionRater;
import com.felixjiang.library.SentimentRater;

public class WeiboSA {
	SentimentRater raterSenti;
	EmotionRater raterEmo;
	private static WeiboSA wsa;
	
	private Map<String, Integer> desc2id = new HashMap<String, Integer>();
	private Map<Integer, String> id2desc = new HashMap<Integer, String>();
	
	private void genMap() {
		desc2id.put("none", 0);
		desc2id.put("happiness", 1);
		desc2id.put("sadness", 2);
		desc2id.put("anger", 3);
		desc2id.put("like", 4);
		desc2id.put("surprise", 5);
		desc2id.put("disgust", 6);
		desc2id.put("fear", 7);
		for (String key : desc2id.keySet())
			id2desc.put(desc2id.get(key), key);
	}
	
	public static WeiboSA getInstance() {
		try {
			if (wsa == null) {
				wsa = new WeiboSA();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return wsa;
	}
	
	private WeiboSA() throws Exception {
		System.out.println(SearchConfig.resources);
		raterSenti = new SentimentRater(SearchConfig.resources, SearchConfig.resources + "sentiment.dict", 
				SearchConfig.resources + "negations.txt", SearchConfig.resources + "model.triclass");
		raterEmo = new EmotionRater(SearchConfig.resources, SearchConfig.resources, 
				SearchConfig.resources + "emotionmodel.emosubj", SearchConfig.resources + "emotionmodel.emo");
		genMap();
	}
	
	public Map<String, Object> getAnalyzedScores(String weibo) {
		Map<String, Object> ret = new HashMap<String, Object>();
		if (weibo == null)
			return ret;
		try {
			Map<Integer, Double> rates = raterSenti.predict(weibo);
			double positive = rates.get(1);
			double negative = rates.get(-1);
			double neutral = rates.get(0);
			double maxSentimentScore = Math.max(positive, negative);
			double sentimentDifference = positive - negative;
			double sentimentDifferenceAbs = Math.abs(sentimentDifference);
			int sentimentPolarity = (positive > negative) ? 
					(neutral > positive ? 0 : 1) : (neutral > negative ? 0 : -1);
			ret.put("positive", positive);
			ret.put("negative", negative);
			ret.put("neutral", neutral);
			ret.put("maxSentimentScore", maxSentimentScore);
			ret.put("sentimentDifference", sentimentDifference);
			ret.put("sentimentDifferenceAbs", sentimentDifferenceAbs);
			ret.put("sentimentPolarity", sentimentPolarity);
			
			rates = raterEmo.predict(weibo);
			int MAX_EMOTION = 5;
			double []scores = new double[MAX_EMOTION];
			scores[0] = rates.get(desc2id.get("happiness"));
			scores[1] = rates.get(desc2id.get("anger"));
			scores[2] = rates.get(desc2id.get("sadness"));
			scores[3] = rates.get(desc2id.get("fear"));
			scores[4] = rates.get(desc2id.get("surprise"));
			double happiness = scores[0];
			double sadness = scores[2];
			double anger = scores[1];
			double surprise = scores[4];
			double fear = scores[3];
			double maxEmotionScore = 0;
			double secondMaxEmotionScore = 1;
			int maxEmotionTypePFDAH = 0;
			for (int i = 0; i < MAX_EMOTION; ++i) { // i = 0 is not counted
				if (scores[i] > maxEmotionScore) {
					secondMaxEmotionScore = maxEmotionScore;
					maxEmotionScore = scores[i];
					maxEmotionTypePFDAH = (1 << i);
				} else if (scores[i] > secondMaxEmotionScore) {
					secondMaxEmotionScore = scores[i];
				}
			}
			ret.put("happiness", happiness);
			ret.put("sadness", sadness);
			ret.put("anger", anger);
			ret.put("surprise", surprise);
			ret.put("fear", fear);
			ret.put("maxEmotionScore", maxEmotionScore);
			ret.put("maxEmotionDifference", maxEmotionScore - secondMaxEmotionScore);
			ret.put("maxEmotionTypePFDAH", maxEmotionTypePFDAH);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void main(String []args) {
		WeiboSA sa = WeiboSA.getInstance();
		System.out.println(sa.getAnalyzedScores("我太高兴啦"));
	}
}