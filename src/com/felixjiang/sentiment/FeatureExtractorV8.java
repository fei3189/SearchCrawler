package com.felixjiang.sentiment;

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

public class FeatureExtractorV8 implements FeatureExtractor {
	static String urlPattern = "(我在(这里)?:)?http://t.cn/[0-9a-zA-Z]{5,9}";
	static String fwdPattern = "//\\s*@.+?[:：\\s]";
	static String replyPattern = "回复\\s*@.+?[:：\\s]";
	static String atPattern = "(@.+?\\s)";
	static String at_endPattern = "(@.+?$)";
	static String sharePattern = "\\(分享自.+?\\)";
	static String topicPattern = "#.+?#";
	static String titlePattern = "【.+?】";
	static String puncPattern = ",.;?";
	static String emoPattern = "\\[.+?\\]";
	static Pattern postagPattern = Pattern.compile("/[a-zA-Z]+[0-9]?$");
	static int EMOTION_NUM = 3;
	static int DICT_FEATURE_NUM = EMOTION_NUM * 8;
	static int OTHER_FEATURE_NUM = 8;
	Segmentation tokenizer;
	Map<String, double[]> wordDict = new HashMap<String, double[]>();
	Map<String, String> word2desc = new HashMap<String, String>();
	Map<String, double[]> emoticonDict = new HashMap<String, double[]>();
	Set<String> negations = new HashSet<String>();
	static final int NEG_SCOPE = 2;
	
	/**
	 * @param segLib, directory of segmentation resource files.
	 * @param dictPath, path of sentiment lexicon dictionary.
	 * @param negationPath, path of negation words.
	 * @throws IOException
	 */
	public FeatureExtractorV8(Segmentation segLib, String dictPath, String negationPath) throws IOException {
		tokenizer = segLib;
		readEmotionDict(dictPath);
		readNegations(negationPath);
	}
	
	/*
	 * File format, each line:
	 * word positive_score negative_score neutral_score description
	 */
	private void readEmotionDict(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.isEmpty() && !line.startsWith("#")) {
				String[] tokens = line.split("\\s+");
				assert tokens.length == 5;
				double[] score = new double[tokens.length - 2];	//The first field is word, the last field is desc of word
				for (int i = 0; i < score.length; ++i) {
					score[i] = Double.parseDouble(tokens[i + 1]);
				}
				
				if (tokens[0].startsWith("[") && tokens[0].endsWith("]"))
					emoticonDict.put(tokens[0], score);
				else {
					wordDict.put(tokens[0], score);
					word2desc.put(tokens[0], tokens[tokens.length - 1]);
				}

				assert EMOTION_NUM == score.length;
			}
		}
		reader.close();
	}
	
	/*
	 * File format: each line is a negation word.
	 */
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

	/**
	 * 0-EMOTION_NUM: dict multiply
	 * EMOTION_NUM-2*EMOTION_NUM: dict max
	 * 2*EMOTION_NUM-3*EMOTION_NUM: MDA multiply
	 * 3*EMOTION_NUM-4*EMOTION_NUM: MDA max
	 * 4*EMOTION_NUM-5*EMOTION_NUM: single-character word multiply
	 * 5*EMOTION_NUM-6*EMOTION_NUM: single-character word max
	 */
	
	public double[] extractFeatureSingleWeibo(String weibo, String keyword) throws UnsupportedEncodingException {
		double [] structFeatures = extractStructuralFeature(weibo);
		double [] scoreEmo = analysisEmoticon(weibo);
		
		weibo = weiboFiltering(weibo);
		double [] scoreWords = analysisSingleWeibo(weibo, keyword);
		String[] marks = new String [] { "[|url|]", "[|reply|]", "[|at|]", "[|share|]",
				"[|topic|]", "[|title|]", "[|exc|]", "[|que|]" };
		double[] feature = new double[DICT_FEATURE_NUM + OTHER_FEATURE_NUM + structFeatures.length];
		System.arraycopy(scoreWords, 0, feature, 0, scoreWords.length);
		System.arraycopy(scoreEmo, 0, feature, scoreWords.length, scoreEmo.length);
		for (int i = 0; i < marks.length; ++i) {
			if (i > -100 && i < 10)
				feature[DICT_FEATURE_NUM + i] = countMarks(weibo, marks[i]);
			else
				feature[DICT_FEATURE_NUM + i] = 0;
		}
		for (int i = 0; i < structFeatures.length; ++i)
			feature[DICT_FEATURE_NUM + OTHER_FEATURE_NUM + i] = structFeatures[i];
		
		int[] zeros = new int[]{8,9,10,11};
		for (int k : zeros)
			feature[k] = 0;
		return feature;
	}
	
	private void keywordPolar(Sentence sen) {
		String text = sen.text;
		if (!text.contains("[|keyword|]"))
			return;
		String[] tokens = text.split("\\s+");
		int pos = -1;
		for (int i = 0; i < tokens.length; ++i) {
			if (tokens[i].equals("[|keyword|]")) {
				pos = i;
				break;
			}
		}
		
		String res = "";
		if (pos > 0 && tokens[pos - 1].startsWith("比")) {
			if (pos < tokens.length - 1) {
				if (tokens[pos + 1].startsWith("更")) {
					if (pos < tokens.length - 2) {
						int index = tokens[pos + 2].indexOf("/");
						if (index >= 0 && index < tokens[pos + 2].length() - 1 && tokens[pos + 2].charAt(index + 1) == 'a')
						{
							tokens[pos - 1] = "";
							tokens[pos + 1] = "不";
							System.out.println(text);
						}
					}
				} else {
					int index = tokens[pos + 1].indexOf("/");
					if (index >= 0 && index < tokens[pos + 2].length() - 1 && tokens[pos + 2].charAt(index + 1) == 'a')
					{
						tokens[pos - 1] = "";
						tokens[pos] += " 不 ";
						System.out.println(text);
					}
				}
			}
		}
		for (int i = 0; i < tokens.length; ++i)
			res += tokens[i] + " ";
		res = res.replaceAll("\\s+", " ");
		res = res.trim();
		sen.text = res;
	}
	
	private int countRegex(String str, Pattern p) {
		Matcher m = p.matcher(str);
		int count = 0;
		while (m.find()) {
			++count;
		}
		return count;
	}
	
	private double[] extractStructuralFeature(String weibo) {
		double []features = new double[12];
		String[] signs = new String[] {"；", "、", ""};
		features[0] = countMarks(weibo, "、");
		features[1] = countMarks(weibo, "%");
		features[2] = countMarks(weibo, "；");
		features[3] = countMarks(weibo, ";");
		features[4] = countRegex(weibo, Pattern.compile("\\d+.\\d+"));
		features[5] = countRegex(weibo, Pattern.compile("\\d{11}"));
		features[6] = countRegex(weibo, Pattern.compile("\\d{5,10}"));
//		features[7] = countMarks(weibo, "（");
//		features[8] = countMarks(weibo, "《");
		features[9] = countMarks(weibo, "：");
		features[10] = countRegex(weibo, Pattern.compile("[^0-9a-z]1[^0-9a-z].*?[^0-9a-z]2[^0-9a-z]"));
		features[11] = countRegex(weibo, Pattern.compile("&[a-z]{2,3};"));
		return features;
	}
	
	private double[] analysisEmoticon(String weibo) {
		// 0 to EMOTION_NUM: multiply, EMOTION_NUM to EMOTION_NUM*2-1 : max
		double []score = new double[EMOTION_NUM * 2];
		
		for (int i = 0; i < EMOTION_NUM; ++i)
			score[i] = 1.0 / EMOTION_NUM;
		for (int i = EMOTION_NUM; i < EMOTION_NUM * 2; ++i)
			score[i] = 0;
		Matcher m = Pattern.compile(emoPattern).matcher(weibo);
		while (m.find()) {
			String emoticon = m.group();
			if (emoticonDict.containsKey(emoticon)) {
				double []s = emoticonDict.get(emoticon);
				for (int i = 0; i < EMOTION_NUM; ++i) {
					score[i] *= s[i];
					score[EMOTION_NUM + i] = Math.max(score[EMOTION_NUM + i], s[i]);
				}
				double sum = dsum(score, 0, EMOTION_NUM);
				for (int i = 0; i < EMOTION_NUM; ++i)
					score[i] /= sum;
			}
		}

		return score;
	}


	private double dsum(double []vec, int start, int end) {
		double sum = 0;
		for (int i = start; i < end; ++i)
			sum += vec[i];
		return sum;
	}
	
	private int countMarks(String weibo, String mark) {
		int pos = 0, count = 0;
		while ((pos = weibo.indexOf(mark, pos) + 1) > 0)
			++count;
		return count;
	}
	
	private double[] nullFeatures() {
		double[] features = new double[DICT_FEATURE_NUM + OTHER_FEATURE_NUM];
		for (int i = 0; i < features.length; ++i)
			features[i] = 0;
		return features;
	}
	
	public ArrayList<double[]> analysisAll(ArrayList<String> weiboList) throws UnsupportedEncodingException {
		ArrayList<double[]> featuresAll = new ArrayList<double[]>();
		for (int i = 0; i < weiboList.size(); ++i)
			featuresAll.add(analysis(weiboList.get(i)));
		return featuresAll;
	}
	
	public ArrayList<double[]> analysisAll(ArrayList<String> weiboList, ArrayList<String> keywordList) throws UnsupportedEncodingException {
		ArrayList<double[]> featuresAll = new ArrayList<double[]>();
		for (int i = 0; i < weiboList.size(); ++i)
			featuresAll.add(analysis(weiboList.get(i), keywordList.get(i)));
		return featuresAll;
	}
	
	public double[] analysis(String weibo) throws UnsupportedEncodingException {
		String []weiboList = weibo.toLowerCase().split(fwdPattern);
		for (int i = 0; i < weiboList.length; ++i) {
			weiboList[i] = weiboList[i].trim();
			if (!weiboList[i].isEmpty()) {
				return extractFeatureSingleWeibo(weiboList[i], "");
			}
		}
		return nullFeatures();
	}
	
	public double[] analysis(String weibo, String keyword) throws UnsupportedEncodingException {
		weibo = weibo.toLowerCase();
		keyword = keyword.toLowerCase();
		String[] weiboList = weibo.split(fwdPattern);
		for (int i = 0; i < weiboList.length; ++i) {
			if (weiboList[i].contains(keyword)) {
				return extractFeatureSingleWeibo(weiboList[i], keyword);
			}
		}

		return nullFeatures();
	}
	
	private String weiboFiltering(String weibo) {
		weibo = weibo.replaceAll(urlPattern, "[|url|]");
		weibo = weibo.replaceAll(replyPattern, "[|reply|]");
		weibo = weibo.replaceAll(atPattern, "[|at|]");
		weibo = weibo.replaceAll(at_endPattern, "[|at|]");
		weibo = weibo.replaceAll("[！!]", "[|exc|]");
		weibo = weibo.replaceAll("[？?]", "[|que|]");
		weibo = weibo.replaceAll(sharePattern, "[|share|]");
		weibo = weibo.replaceAll(topicPattern, "[|topic|]");
		weibo = weibo.replaceAll(titlePattern, "[|title|]");
		return weibo;
	}

	
	private Map<String, Integer> GenerateWordSet(final String[] words, final String[] pos) {
		Map<String, Integer> word2polar = new HashMap<String, Integer>();
		assert words.length == pos.length;
		int p = 0, polar = 1, neg_pos = 0;
		while (p < words.length) {
			if (p - neg_pos > 3)
				polar = 1;
			
			String cur = words[p];
			if (negations.contains(cur)) {
				while (++p < words.length) {
					cur += words[p];
					if (!negations.contains(cur))
						break;
				}
				polar = -polar;
				neg_pos = p;
			} else {
				String postag = pos[p];
				while (++p < words.length && wordDict.containsKey(cur + words[p])) {
					cur += words[p];
					postag = null;
				}
				if (cur.length() >= 1) {
					int cur_polar = 1;
					if (postag == null || postag.startsWith("a") || postag.startsWith("v")) {
						cur_polar = polar;
						polar = 1;
/*						if (polar == -1) {
							cur_polar = -1;
							polar = -2;
//							System.out.println("wokao");
						} else if (polar == -2) {
							cur_polar = -1;
							polar = 1;
//							System.out.println("wokao");
						} else {
							cur_polar = 1;
							polar = 1;
						}*/
					}
					if (word2polar.containsKey(cur)) {
						if (word2polar.get(cur) != cur_polar)
							word2polar.remove(cur);
					} else {
						word2polar.put(cur, cur_polar);
					}
				}
			}
		}
		return word2polar;
	}
	
	private boolean isEndOfSentence(String pun) {
		if (pun.contains("!") || pun.contains("！") || pun.contains("。") || 
				pun.contains("？") || pun.contains("?") || pun.contains("."))
			return true;
		return false;
	}
	private Sentence[] filterSentence(Sentence[] sen, String keyword) {
		if (keyword == null || keyword.isEmpty())// || keyword.length() > 0)
			return sen;
		Sentence []tmp = new Sentence[sen.length];
		int num = 0;
		int sign = 0;
		int last = -1;
//		System.out.println("%%");
		if (keyword != null && !keyword.isEmpty()) {
			for (int i = 0; i < sen.length; ++i) {
//				System.out.println("&&" + sen[i].text + sen[i].punctuation);
				if (sen[i].text.contains(keyword)) {
					
					sign = 1;
					int j = i - 1;
					for (j = i - 1; j > last; --j) {
						if (sen[j].punctuation.equals("。")) {
							break;
						}
					}
//					System.out.println(j + " " + i);
					for (int k = j + 1; k <= i; k++) {
						tmp[num++] = sen[k];
//						System.out.println("$$" + k + " " + num);
					}
					last = i;
				} else if (sign == 1) {
					tmp[num++] = sen[i];
					last = i;
					if (!isEndOfSentence(sen[i].punctuation) && sen[i].text.length() <= 3)
						sign = 1;
					else
						sign = 0;
				}
			}
		}
		Sentence []ret = new Sentence[num];
		System.arraycopy(tmp, 0, ret, 0, num);
		
		return ret;
	}
	
	private double[] analysisSingleWeibo(String weibo, String keyword) throws UnsupportedEncodingException {
		Sentence[] sen = null;
/*		if (weibo.lastIndexOf("但") != -1)
			weibo = weibo.substring(weibo.lastIndexOf("但") + 1);
		else if (weibo.lastIndexOf("不过") != -1)
			weibo = weibo.substring(weibo.lastIndexOf("不过") + 2);
		else if (weibo.lastIndexOf("，可是") != -1)
			weibo = weibo.substring(weibo.lastIndexOf("，可是") + 3);*/
		if (keyword != null && !keyword.isEmpty()) {
			weibo = weibo.replaceAll(keyword, "[|keyword|]");
			sen = filterSentence(Sentence.extractSentence(weibo), "[|keyword|]");
		} else {
			sen = Sentence.extractSentence(weibo);
		}

		double [][] score = new double[sen.length][EMOTION_NUM * 6];
		for (int i = 0; i < sen.length; ++i) {
			score[i] = analysisSentence(sen[i]);
		}
		
		double[] result = new double[EMOTION_NUM * 6];
		for (int i = 0; i < 6 * EMOTION_NUM; ++i) {
			if ((i / EMOTION_NUM) % 2 == 0)
				result[i] = 1.0;
			else
				result[i] = 0;
		}

		for (int i = 0; i < score.length; ++i) {
			for (int j = 0; j < score[i].length; ++j) {
				if ((j / EMOTION_NUM) % 2 == 0) {
					result[j] *= Math.pow(score[i][j], (1 + (0 + 1)/3));
				} else {
					result[j] = Math.max(result[j], score[i][j]);
				}
			}
		}
		normalize(result, 0, EMOTION_NUM);
		normalize(result, 2 * EMOTION_NUM, 3 * EMOTION_NUM);
		normalize(result, 4 * EMOTION_NUM, 5 * EMOTION_NUM);
		return result;
	}

	private void normalize(double []list, int start, int end) {
		double sum = dsum(list, start, end);
		for (int i = start; i < end; ++i)
			list[i] /= sum;
	}

	private void separateWordAndPOS(final String []tokens, String[] words, String[] pos) {
		for (int i = 0; i < tokens.length; ++i) {
			int p = tokens[i].lastIndexOf('/');
			if (p >= 0) {
				words[i] = tokens[i].substring(0, p);
				pos[i] = tokens[i].substring(p + 1);
			} else
				words[i] = tokens[i];
		}
	}
	
	private void reorder(double []s) {
		int start = 0;
		if (true) {
			double tmp = s[0];
			s[0] = s[1];
			s[1] = tmp;
			start = 2;
		}
///		for (int i = start; i < s.length - 1; ++i) {
//			s[i] = s[s.length - 1] / 5;
//		}
	}
	
	private double[] analysisSentence(Sentence sentence) throws UnsupportedEncodingException {
		sentence.text = tokenizer.segment(sentence.text.toLowerCase(), "UTF8", 1);
		keywordPolar(sentence);
		String []tokens = sentence.text.split("\\s+");
		String []words = new String[tokens.length], pos = new String[tokens.length];
		separateWordAndPOS(tokens, words, pos);
		Map<String, Integer> word2polar = GenerateWordSet(words, pos);
		
		/**
		 * Structure of the variable 'score' (double[])
		 * 0-EMOTION_NUM: product of scores of an emotion(currently positive, negative or neutral) of all words.
		 * EMOTION_NUM-2*EMOTION_NUM: max of all.
		 * 2*EMOTION_NUM-3*EMOTION_NUM: product of scores of an emotion(currently positive, negative or neutral) of MDA words.
		 * 3*EMOTION_NUM-4*EMOTION_NUM: max of MDA.
		 * 4*EMOTION_NUM-5*EMOTION_NUM: product of scores of an emotion(currently positive, negative or neutral) of single character.
		 * 5*EMOTION_NUM-6*EMOTION_NUM: max of single-character.
		 */
		double[] score = new double[EMOTION_NUM * 6];
		
		for (int i = 0; i < 6 * EMOTION_NUM; ++i) {
			if ((i / EMOTION_NUM) % 2 == 0)
				score[i] = 1.0 / EMOTION_NUM;  // For product
			else
				score[i] = 0; // For max
		}

		// For sentences ended with '?', ignore them.
		if (sentence.punctuation.endsWith("?") || sentence.punctuation.endsWith("？"))
			return score;
		
		for (String word : word2polar.keySet()) {
			double[] tmp = wordDict.get(word);
			if (tmp == null)
				continue;
			double[] s = new double[tmp.length];
			System.arraycopy(tmp, 0, s, 0, tmp.length);
			if (word2polar.get(word) == -1) {
				reorder(s); //If modified by negation, reverse positive score and negative score.
			}
			
			// For single character.
			if (word.length() == 1) {
				for (int i = 4*EMOTION_NUM; i < 5*EMOTION_NUM; ++i) {
					score[i] *= s[i % EMOTION_NUM];
					score[i+EMOTION_NUM] = Math.max(score[i+EMOTION_NUM], s[i % EMOTION_NUM]);
				}
				double sum = dsum(score, 4*EMOTION_NUM, 5*EMOTION_NUM);
				for (int i = 4*EMOTION_NUM; i < 5*EMOTION_NUM; ++i)
					score[i] /= sum;
			}
			
			// For MDA words.
			String desc = word2desc.get(word);
			if (desc != null && desc.equals("va")) {
				for (int i = 2*EMOTION_NUM; i < 3*EMOTION_NUM; ++i) {
					score[i] *= s[i % EMOTION_NUM];
					score[i+EMOTION_NUM] = Math.max(score[i+EMOTION_NUM], s[i % EMOTION_NUM]);
				}
				double sum = dsum(score, 2*EMOTION_NUM, 3*EMOTION_NUM);
				for (int i = 2*EMOTION_NUM; i < 3*EMOTION_NUM; ++i)
					score[i] /= sum;
			}
			
			// For all words.
			if (true) {
				for (int i = 0; i < EMOTION_NUM; ++i) {
					score[i] *= s[i % EMOTION_NUM];
					score[i+EMOTION_NUM] = Math.max(score[i+EMOTION_NUM], s[i % EMOTION_NUM]);
				}
				double sum = dsum(score, 0, EMOTION_NUM);
				for (int i = 0; i < EMOTION_NUM; ++i) {
					score[i] /= sum;
				}
			}
		}
		return score;
	}

	@Override
	public double[] extractFeatures(String weibo)
			throws UnsupportedEncodingException {
		return analysis(weibo);
	}

	@Override
	public double[] extractFeatures(String weibo, String keyword)
			throws UnsupportedEncodingException {
		return analysis(weibo, keyword);
	}

	@Override
	public ArrayList<double[]> extractFeaturesAll(ArrayList<String> weiboList)
			throws UnsupportedEncodingException {
		return analysisAll(weiboList);
	}

	@Override
	public ArrayList<double[]> extractFeaturesAll(ArrayList<String> weiboList,
			ArrayList<String> keywords) throws UnsupportedEncodingException {
		return analysisAll(weiboList, keywords);
	}
}
