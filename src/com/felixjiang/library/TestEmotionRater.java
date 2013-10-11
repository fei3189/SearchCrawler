package com.felixjiang.library;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.felixjiang.segmentation.SegmentationException;

public class TestEmotionRater {
	private ArrayList<String> weibo = new ArrayList<String>();
	private ArrayList<Integer> label = new ArrayList<Integer>();
	private Map<String, Integer> desc2id = new HashMap<String, Integer>();
	private Map<Integer, String> id2desc = new HashMap<Integer, String>();
	
	private TestEmotionRater() {
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
	
	public void loadTrainingData(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		for (String line; (line = reader.readLine()) != null; ) {
			line = line.trim().toLowerCase();
			if (!line.isEmpty()) {
				String []tokens = line.split("\\t");
				if (tokens.length == 3) {
					assert desc2id.containsKey(tokens[1]);
					weibo.add(tokens[2]);
					label.add(desc2id.get(tokens[1]));
				} else {
					assert false;
				}
			}
		}
		reader.close();
	}
	
	public static void main(String args[]) throws IOException, SegmentationException {
		TestEmotionRater test = new TestEmotionRater();
//		test.loadTrainingData("/home/jiangfei/evaluation/NLPCC2013/src/task2/weibo.txt");
		EmotionRater rater = new EmotionRater("resources/", "resources/", "resources/emotionmodel.emosubj", "resources/emotionmodel.emo");
		EmotionClassifier classifier = new EmotionClassifier("resources/", "resources/", "resources/emotionmodel.emosubj", "resources/emotionmodel.emo");
//		rater.train(test.weibo, test.label, "resources/emotionmodel");
		System.out.println(classifier.predict("尼玛，我真生气!"));
	}
}
