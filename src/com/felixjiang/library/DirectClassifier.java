package com.felixjiang.library;

import java.io.IOException;
import java.util.ArrayList;

import com.felixjiang.ml.SVMTool;
import com.felixjiang.segmentation.ICTSegmentation;
import com.felixjiang.sentiment.FeatureExtractor;
import com.felixjiang.sentiment.FeatureExtractorV8;

/**
 * Perform the triple class classification with one direct step,
 * using the internal multi-class classification mechanism in libsvm.
 * @author jiangfei
 *
 */
public class DirectClassifier implements SentimentClassifier {
	FeatureExtractor extractor = null;
	SVMTool model = null;
	
	/**
	 * @param segLibHome. Essential segmentation resources for ICTSeg 
	 * @param sentiDictName. subjectivityMent dictionary
	 * @param negationName. Negation words
	 * @param model. svm model for classification
	 * @throws Exception
	 */
	public DirectClassifier(String segLibHome, String sentiDictName, 
			String negationName, String modelPath) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
		model = new SVMTool();
		model.load(modelPath);
	}
	
	public DirectClassifier(String segLibHome, String sentiDictName, 
			String negationName) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
	}
	
	@Override
	public int classify(String weibo) throws IOException {
		double[] features = extractor.extractFeatures(weibo);
		int label = model.predict(features);
		return label;
	}
	
	@Override
	public void train(ArrayList<String> weiboList, ArrayList<Integer> labelList, String modelFile) throws IOException {
		ArrayList<double[]> features = extractor.extractFeaturesAll(weiboList);
		model = new SVMTool();
		model.train(features, labelList);
		if (modelFile != null && !modelFile.isEmpty()) {
			model.save(modelFile + ".triclass");
		}
	}
}
