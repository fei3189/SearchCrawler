package com.felixjiang.emotion;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public interface EmotionFeatureExtractor {
	public double[] extractFeature(String str) throws UnsupportedEncodingException;
	public double[][] extractAllFeatures(ArrayList<String> slist) throws UnsupportedEncodingException;
	public double[][] extractAllCloseFeatures(ArrayList<String> slist) throws UnsupportedEncodingException;
	public double[] extractCloseFeatures(String str) throws UnsupportedEncodingException;
}
