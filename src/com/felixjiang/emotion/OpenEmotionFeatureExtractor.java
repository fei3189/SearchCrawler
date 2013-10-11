package com.felixjiang.emotion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.felixjiang.segmentation.Segmentation;
import com.felixjiang.segmentation.SegmentationException;

public class OpenEmotionFeatureExtractor implements EmotionFeatureExtractor {
	Map<String, Integer> des2index = new HashMap<String, Integer>();
	Map<String, String[]> word2type = new HashMap<String, String[]>();
	Map<String, Integer[]> word2strength = new HashMap<String, Integer[]>();
	Map<String, double[]> word2externalDict = new HashMap<String, double[]>();
	Map<String, String> type2emo = new HashMap<String, String>();
	Set<String> negations = new HashSet<String>();
	Segmentation seg = null;
	final int EMOTION_NUM = 8;
//	Map<String, String[]> word2hitType = new HashMap<String, String[]>();
//	Map<String, Integer[]> word2hitStrength = new HashMap<String, Integer[]>();
	
	Map<String, Integer> emoticons2index = new HashMap<String, Integer>();
	Pattern emoticons = Pattern.compile("\\[.+?\\]");
	
	int featureNum = 0;
	public OpenEmotionFeatureExtractor(Segmentation s, String resourcesDir) throws IOException, SegmentationException {
//		seg = new ICTSegmentation("/home/jiangfei/lib/", 1);
		seg = s;
		if (!resourcesDir.endsWith("/"))
			resourcesDir += "/";
		System.out.println(seg);
//		System.out.println(seg.importUserDict("/home/jiangfei/evaluation/NLPCC2013/src/task2/user.dict"));
		String []types = new String[] { "PA", "PE", "PD", "PH", "PG", "PB", "PK", "NA", "NB", "NJ", "NH",
									     "PF", "NI", "NC", "NG", "NE", "ND", "NN", "NK", "NL", "PC"};
		String []pos = new String[] {"v", "a", "i", "n", "o"};
		String []emotions = new String[] { "happiness", "sadness", "anger", "like", "surprise", "disgust", "fear"};
		type2emo.put("PA", "happiness");
		type2emo.put("PE", "happiness");
		type2emo.put("PD", "like");
		type2emo.put("PH", "like");
		type2emo.put("PG", "like");
		type2emo.put("PB", "like");
		type2emo.put("PK", "like");
		type2emo.put("NA", "anger");
		type2emo.put("NB", "sadness");
		type2emo.put("NJ", "sadness");
		type2emo.put("NH", "sadness");
		type2emo.put("PF", "sadness");
		type2emo.put("NI", "fear");
		type2emo.put("NC", "fear");
		type2emo.put("NG", "fear");
		type2emo.put("NE", "disgust");
		type2emo.put("ND", "disgust");
		type2emo.put("NN", "disgust");
		type2emo.put("NK", "disgust");
		type2emo.put("NL", "disgust");
		type2emo.put("PC", "surprise");
		int index = 0;
		for (int i = 0; i < emotions.length; ++i) {
			for (int j = 0; j < pos.length; ++j) {
				des2index.put(emotions[i] + pos[j], index);
				++index;
			}
		}
		des2index.put("[<que1>]", index);
		++index;
		des2index.put("[<que2>]", index);
		++index;
		des2index.put("[<exc1>]", index);
		++index;
		des2index.put("[<exc2>]", index);
		++index;

		featureNum = index;
		readDict(resourcesDir + "dlut_senti.txt");  //大连理工词典
		readExternalDict(resourcesDir + "seventypes.dict"); //七种情感的词典
		readEmoticons(resourcesDir + "emoticons.txt");
		readNegations(resourcesDir + "negations.txt");
	}
	
	private void readEmoticons(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		int index = 0;
		for (String line; (line = reader.readLine()) != null; ) {
			line = line.trim();
			if (!line.isEmpty()) {
				String []tokens = line.split("\\s");
				emoticons2index.put(tokens[0], index++);
			}
		}
		reader.close();
	}
	
	private void readExternalDict(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		for (String line; (line = reader.readLine()) != null; ) {
			line = line.trim();
			if (line.startsWith("#"))
				continue;
			if (!line.isEmpty()) {
				String []tokens = line.split("\\s+");
				assert tokens.length == EMOTION_NUM + 1;
				double []score = new double[EMOTION_NUM];
				double sum = 0;
				for (int i = 0; i < EMOTION_NUM; ++i) {
					score[i] = Double.parseDouble(tokens[i + 1]);
					sum += score[i];
				}
				for (int i = 0; i < EMOTION_NUM; ++i) {
					score[i] /= sum;
				}
				word2externalDict.put(tokens[0], score);
			}
		}
		reader.close();
	}
	
	private void readDict(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		for (String line; (line = reader.readLine()) != null; ) {
			line = line.trim();
			if (line.isEmpty())
				continue;
			String []fields = line.split("\\t");
			int len = 0;
			if (fields.length == 7)
				len = 1;
			else if (fields.length == 10)
				len = 2;
			else {
				System.out.println("Error line" + line);
//				System.exit(-1);
				continue;
			}
			
			String[] type = new String[len];
			Integer[] stren = new Integer[len];
			type[0] = fields[4];
			stren[0] = new Integer(fields[5]);
			if (len == 2) {
				type[1] = type2emo.get(fields[7]);
				stren[1] = new Integer(fields[8]);
			}
			word2type.put(fields[0], type);
			word2strength.put(fields[0], stren);
		}
		reader.close();
		System.out.println(word2type.size());
	}
	
	private void readNegations(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		for (String line; (line = reader.readLine()) != null;) {
			line = line.trim();
			if (!line.isEmpty()) {
				negations.add(line);
			}
		}
		reader.close();
	}
	
	int kk = 0;
	public double[] generateFeathers(String text, String tokens) {
		boolean sign = true;
		double []features = new double[featureNum];
		for (int i = 0; i < features.length; ++i)
			features[i] = 0.0;
		String[] fields = tokens.split(" ");
		for (String str : fields) {
			int pos = str.lastIndexOf('/');
			String word, postag;
			if (pos < 0) {
				word = str;
				postag = "";
			} else {
				word = str.substring(0, pos);
				postag = str.substring(pos + 1, pos + 2).toLowerCase();
				if (!postag.equals("v") && !postag.equals("a") && !postag.equals("i") && !postag.equals("n"))
					postag = "o";
			}
			String type = "NONE";
			if (word2type.containsKey(word)) {
				if (sign) {
					++kk;
					sign = false;
				}
				
				String []types = word2type.get(word);
				Integer []stren = word2strength.get(word);
				for (int i = 0; i< types.length; ++i) {
					String des = type2emo.get(types[i]) + postag;
					if (des2index.get(des) != null) {
						features[des2index.get(des)] += stren[i];
					}
				}
			} else {
				String des;
				if (word.startsWith("[") && word.endsWith("]"))
					des = word;
				else
					des = type + postag;
				Integer index = des2index.get(des);
				if (index != null) {
					features[index] += 1;
				}
			}
		}
		
		return features;
	}
	
	public double[] generateExternalFeatures(String segmented) {
		Map<String, Integer> word2polar = GenSuperWord.extractSuperWordFromTokens(segmented, negations, word2externalDict);
		double features[] = new double[EMOTION_NUM * 2];
		for (int i = 0; i < EMOTION_NUM; ++i) 
			features[i] = 0.0 / EMOTION_NUM;
		for (int i = EMOTION_NUM; i < 2 * EMOTION_NUM - 1; ++i)
			features[i] = 0;
		for (String word : word2polar.keySet()) {
			if (word2externalDict.containsKey(word)) {
				double[] score = word2externalDict.get(word);
				if (word2polar.get(word) == 1) { 
					double sum = 0;
					for (int i = 0; i < EMOTION_NUM; ++i) {
						features[i] += score[i];
						sum += features[i];
					}
					if (sum > 0)
						for (int i = 0; i < EMOTION_NUM; ++i) {
							features[i] /= sum;
						}
					for (int i = EMOTION_NUM; i < 2 * EMOTION_NUM; ++i) {
						if (score[i - EMOTION_NUM] > features[i]) {
							features[i] = score[i - EMOTION_NUM];
						}
					}
				}
			}
		}
		return features;
	}
	
	
	private double[] generateEmoticonFeatures(String str) {
		Matcher m = emoticons.matcher(str);
		double []features = new double[emoticons2index.size()];
		while (m.find()) {
			String emo = m.group();
			
			Integer index = emoticons2index.get(emo);
			if (index != null) {
				features[index] += 1;
			}
		}
		return features;
	}
	
	public String filter(String text) {
		text = text.replaceAll("[!！]{2,}", "[<exc2>]");
		text = text.replaceAll("[?？]{2,}", "[<que2>]");
		text = text.replaceAll("[!！]", "[<exc1>]");
		text = text.replaceAll("[?？]", "[<que1>]");
		text = text.replaceAll("\\.{3,}" ,"[<...>]");
		text = text.replaceAll("…+", "[<...>]");
		text = text.replaceAll("。{3,}", "[<...>]");
		return text;
	}
	
	private int count(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length(); ++i)
			if (str.charAt(i) == c)
				++count;
		return count;
	}
	
	private int countRegex(String str, Pattern p) {
		Matcher m = p.matcher(str);
		int count = 0;
		while (m.find()) {
			++count;
		}
		return count;
	}
	
	private double[] generateSignFeatures(String str) {
		str = str.toLowerCase();
		double [] features = new double[14];
		features[0] = count(str, '、');
		features[1] = count(str, '%');
		features[2] = count(str, '；');
		features[3] = count(str, ';');
//		features[4] = count(str, '!');
//		features[5] = count(str, '！');
		features[6] = countRegex(str, Pattern.compile("\\d+.\\d+"));
		features[7] = countRegex(str, Pattern.compile("\\d{11}"));
		features[8] = countRegex(str, Pattern.compile("\\d{5,10}"));
		features[9] = count(str, '（');
//		features[10] = count(str, '《');
		features[11] = count(str, '：');
		features[12] = countRegex(str, Pattern.compile("[^0-9a-z]1[^0-9a-z].*?[^0-9a-z]2[^0-9a-z]"));
		features[13] = countRegex(str, Pattern.compile("&[a-z]{2,3};"));
		return features;
	}
	
	@Override
	public double[] extractFeature(String str) throws UnsupportedEncodingException {
		str = filter(str);
		String segmented = seg.segment(str, "UTF8", 1);
		double []closeFeature = extractCloseFeatures(str);
		double []openFeature = generateExternalFeatures(segmented);
		double []allFeatures = new double[closeFeature.length + openFeature.length];
		System.arraycopy(closeFeature, 0, allFeatures, 0, closeFeature.length);
		System.arraycopy(openFeature, 0, allFeatures, closeFeature.length, openFeature.length);
		return allFeatures;
	}
	
	public double[] extractCloseFeatures(String str) throws UnsupportedEncodingException {
		str = filter(str);
		String segmented = seg.segment(str, "UTF8", 1);
		double []closeFeature = generateFeathers(str, segmented);
		double []emoticonFeature = generateEmoticonFeatures(str);
		double []signFeature = generateSignFeatures(str);
		double []feature = new double[closeFeature.length + emoticonFeature.length + signFeature.length];
		System.arraycopy(closeFeature, 0, feature, 0, closeFeature.length);
		System.arraycopy(emoticonFeature, 0, feature, closeFeature.length, emoticonFeature.length);
		System.arraycopy(signFeature, 0, feature, closeFeature.length + emoticonFeature.length, signFeature.length);
		return feature;
	}
	
	public double[][] extractAllCloseFeatures(ArrayList<String> slist) throws UnsupportedEncodingException {
		double [][] features = new double[slist.size()][];
		for (int i = 0; i < features.length; ++i) {
			features[i] = extractCloseFeatures(slist.get(i));
		}
		return features;
	}

	@Override
	public double[][] extractAllFeatures(ArrayList<String> slist) throws UnsupportedEncodingException {
		double [][] features = new double[slist.size()][];
		for (int i = 0; i < features.length; ++i) {
			features[i] = extractFeature(slist.get(i));
		}
		return features;
	}
	
}
