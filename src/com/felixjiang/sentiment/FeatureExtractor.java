package com.felixjiang.sentiment;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public interface FeatureExtractor {
	/**
	 * Extract features from a single weibo message with no target.
	 * @param weibo
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public double[] extractFeatures(String weibo) throws UnsupportedEncodingException;
	
	/**
	 * Extract features from a single weibo message with target. 
	 * @param weibo
	 * @param keywords
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public double[] extractFeatures(String weibo, String keyword) throws UnsupportedEncodingException;
	
	/**
	 * Extract features from a list of weibo messages.
	 * @param weibo
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public ArrayList<double[]> extractFeaturesAll(ArrayList<String> weibo) throws UnsupportedEncodingException;
	
	/**
	 * Extract features from a list of weibo messages, each of them has a corresponding keyword.
	 * @param weibo
	 * @param keywords
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public ArrayList<double[]> extractFeaturesAll(ArrayList<String> weibo, ArrayList<String> keywords) throws UnsupportedEncodingException;
}