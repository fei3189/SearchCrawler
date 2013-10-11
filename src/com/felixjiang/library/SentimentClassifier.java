package com.felixjiang.library;

import java.io.IOException;
import java.util.ArrayList;

public interface SentimentClassifier {
	/**
	 * Classify one weibo message
	 * @param weibo
	 * @return positive=1, negative=-1, neutral=0
	 * @throws IOException
	 */
	public int classify(String weibo) throws IOException;
	
	/**
	 * Training a model for sentiment classification.
	 * @param weiboList array of weibo messages
	 * @param labelList label of each message
	 * @param modelFile model file to be saved. If null, no file will be saved.
	 * @throws IOException
	 */
	public void train(ArrayList<String> weiboList, ArrayList<Integer> labelList, String modelFile) throws IOException;
}