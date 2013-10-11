package com.felixjiang.library;

import java.io.IOException;
import java.util.ArrayList;

import com.felixjiang.ml.SVMTool;
import com.felixjiang.segmentation.ICTSegmentation;
import com.felixjiang.sentiment.FeatureExtractor;
import com.felixjiang.sentiment.FeatureExtractorV8;

/**
 * Perform triple class classification with two stages.
 * First, neutral vs non-neutral;Second positive vs negative.
 * @author jiangfei
 *
 */
public class HierarchicalClassifier implements SentimentClassifier {
	FeatureExtractor extractor = null;
	SVMTool subjectivityM = null;
	SVMTool polarityM = null;
	
	/**
	 * @param segLibHome. Essential segmentation resources for ICTSeg 
	 * @param sentiDictName. subjectivityMent dictionary
	 * @param negationName. Negation words
	 * @param model1. subjectivity model
	 * @param model2. polarity model. Please DO NOT confuse the order between model1 and model2.
	 * @throws Exception
	 */
	public HierarchicalClassifier(String segLibHome, String sentiDictName, 
			String negationName, String model1, String model2) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
		subjectivityM = new SVMTool();
		polarityM = new SVMTool();
		subjectivityM.load(model1);
		polarityM.load(model2);
	}
	
	public HierarchicalClassifier(String segLibHome, String sentiDictName, 
			String negationName) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
	}
	
	@Override
	public int classify(String weibo) throws IOException {
		double[] features = extractor.extractFeatures(weibo);
		int senti = subjectivityM.predict(features);
		if (senti == 0)
			return 0;
		int polarity = polarityM.predict(features);
		return polarity;
	}
	
	@Override
	public void train(ArrayList<String> weiboList, ArrayList<Integer> labelList, String model) throws IOException {
		ArrayList<double[]> features = extractor.extractFeaturesAll(weiboList);
		ArrayList<double[]> subFeature = new ArrayList<double[]>();
		ArrayList<double[]> senFeature = new ArrayList<double[]>();
		ArrayList<Integer> subLabel = new ArrayList<Integer>();
		ArrayList<Integer> senLabel = new ArrayList<Integer>();
		for (int i = 0; i < labelList.size(); ++i) {
			int label = labelList.get(i);
			if (label == 0) {
				subFeature.add(features.get(i));
				subLabel.add(0);
			} else {
				subFeature.add(features.get(i));
				subLabel.add(1);
				senFeature.add(features.get(i));
				senLabel.add(label);
			}
		}
		subjectivityM = new SVMTool();
		polarityM = new SVMTool();
		subjectivityM.train(subFeature, subLabel);
		polarityM.train(senFeature, senLabel);
		
		if (model != null && !model.isEmpty()) {
			subjectivityM.save(model + ".subjectivity");
			polarityM.save(model + ".polarity");
		}
	}
}
