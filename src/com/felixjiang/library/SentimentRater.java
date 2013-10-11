package com.felixjiang.library;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.felixjiang.ml.SVMTool;
import com.felixjiang.segmentation.ICTSegmentation;
import com.felixjiang.sentiment.FeatureExtractor;
import com.felixjiang.sentiment.FeatureExtractorV8;

/**
 * Compute the probability for each class(1, -1 or 0).
 * The scorer series only has one-stage three-class version
 * as the other two are hard for probability estimation. 
 * @author jiangfei
 *
 */
public class SentimentRater {
	FeatureExtractor extractor = null;
	SVMTool model = null;
	
	/**
	 * @param segLibHome. Essential segmentation resources for ICTSeg 
	 * @param sentiDictName. subjectivityMent dictionary
	 * @param negationName. Negation words
	 * @param model. svm model for classification
	 * @throws Exception
	 */
	public SentimentRater(String segLibHome, String sentiDictName, 
			String negationName, String modelPath) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
		model = new SVMTool();
		model.load(modelPath);
	}
	
	public SentimentRater(String segLibHome, String sentiDictName, 
			String negationName) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
	}
	
	/**
	 * Predict the probability of each class.
	 * @param weibo
	 * @return a map whose keys are labels and values are probabilities.
	 * @throws IOException
	 */
	public Map<Integer, Double> predict(String weibo) throws IOException {
		double[] features = extractor.extractFeatures(weibo);
		return model.predictProbability(features);
	}
	
	public void train(ArrayList<String> weiboList, ArrayList<Integer> labelList, String modelFile) throws IOException {
		ArrayList<double[]> features = extractor.extractFeaturesAll(weiboList);
		model = new SVMTool();
		model.train(features, labelList);
		if (modelFile != null && !modelFile.isEmpty()) {
			model.save(modelFile);
		}
	}
}
