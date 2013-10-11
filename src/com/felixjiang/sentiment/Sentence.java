package com.felixjiang.sentiment;

import java.util.ArrayList;
import java.util.List;

public class Sentence {
	public String text;
	public String punctuation;
	public double score;
	
	private static char splitPunctuationTable[] = new char[] {
		'，',	'。', 	'；',	'！', 	'？',	'：',	'…',	// SBC case
		',',	'.',	';',	'!',	'?',	':',	//DBC case
	};
	
	private enum State { START, FINISH, TEXT, PUNC };
	
	public Sentence(String t, String p, double s) {
		text = t;
		punctuation = p;
		score = s;
	}
	
	private static boolean isSplitPunctuation(char c) {
		for (int i = 0; i < splitPunctuationTable.length; ++i)
			if (c == splitPunctuationTable[i])
				return true;
		return false;
	}
	
	public static Sentence[] extractSentence(String weibo) {
		String newStr = "";
		for (int i = 0; i < weibo.length(); ++i) {
			newStr += weibo.charAt(i);
			if (isSplitPunctuation(weibo.charAt(i)) && 
					(i + 1 >= weibo.length() || !isSplitPunctuation(weibo.charAt(i + 1))))
				newStr += "\t";
		}
		String[] strList = newStr.split("\\s+");
		Sentence[] senList = new Sentence[strList.length];
		int senCount = 0;
		for (int i = 0; i < strList.length; ++i) {
			if (strList[i].isEmpty())
				continue;
			int end = strList[i].length() - 1;
			while (end >= 0 && isSplitPunctuation(strList[i].charAt(end)))
				--end;
			++end;
			senList[senCount++] = new Sentence(strList[i].substring(0, end),
					strList[i].substring(end), 0);
		}
		Sentence[] ret = new Sentence[senCount];
		System.arraycopy(senList, 0, ret, 0, senCount);
		
		return ret;
	}
}