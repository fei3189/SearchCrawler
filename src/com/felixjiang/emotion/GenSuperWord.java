package com.felixjiang.emotion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GenSuperWord {
	static private Map<String, Integer> GenerateWordSet(final String[] words, final String[] pos, Set<String> negations, Map<String, double[]> wordDict) {
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
	
	static private void separateWordAndPOS(final String []tokens, String[] words, String[] pos) {
		for (int i = 0; i < tokens.length; ++i) {
			int p = tokens[i].lastIndexOf('/');
			if (p >= 0) {
				words[i] = tokens[i].substring(0, p);
				pos[i] = tokens[i].substring(p + 1);
			} else
				words[i] = tokens[i];
		}
	}
	
	static public Map<String, Integer> extractSuperWordFromTokens(String segmented, Set<String> negations, Map<String, double[]> wordDict) {
		String []tokens = segmented.split("\\s+");
		String []words = new String[tokens.length], pos = new String[tokens.length];
		separateWordAndPOS(tokens, words, pos);
		return GenerateWordSet(words, pos, negations, wordDict);
	}
}