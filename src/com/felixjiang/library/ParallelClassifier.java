package com.felixjiang.library;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.felixjiang.ml.SVMTool;
import com.felixjiang.segmentation.ICTSegmentation;
import com.felixjiang.segmentation.SegmentationException;
import com.felixjiang.sentiment.FeatureExtractor;
import com.felixjiang.sentiment.FeatureExtractorV8;

/**
 * Perform triple class classification with two stages.
 * First, positive vs non-positive;Second negative vs non-negative.
 * @author jiangfei
 *
 */
public class ParallelClassifier implements SentimentClassifier {
	FeatureExtractor extractor = null;
	SVMTool positiveM = null; // Model for positive VS non-positive
	SVMTool negativeM = null; // Model for negative VS non-negative
	
	/**
	 * @param segLibHome. Essential segmentation resources for ICTSeg
	 * @param sentiDictName. positiveMent dictionary
	 * @param negationName. Negation words
	 * @param model1. positive vs non-positive model
	 * @param model2. negative vs non-negative model
	 * 		   Please DO NOT confuse the order between model1 and model2.
	 * @throws SegmentationException 
	 * @throws IOException 
	 * @throws Exception
	 */
	public ParallelClassifier(String segLibHome, String sentiDictName, 
			String negationName, String model1, String model2) throws IOException, SegmentationException  {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
		positiveM = new SVMTool();
		negativeM = new SVMTool();
		positiveM.load(model1);
		negativeM.load(model2);
	}
	
	public ParallelClassifier(String segLibHome, String sentiDictName, 
			String negationName) throws Exception {
		extractor = new FeatureExtractorV8(new ICTSegmentation(segLibHome, 1), sentiDictName, negationName);
	}
	
	@Override
	public int classify(String weibo) throws IOException {
		double[] features = extractor.extractFeatures(weibo);
		int ifPos = positiveM.predict(features), ifNeg = negativeM.predict(features);
		if (ifPos == 1 && ifNeg == 0)
			return 1;
		else if (ifPos == 0 && ifNeg == 1)
			return -1;
		else
			return 0;
	}
	
	@Override
	public void train(ArrayList<String> weiboList, ArrayList<Integer> labelList, String model) throws IOException {
		ArrayList<double[]> features = extractor.extractFeaturesAll(weiboList);
		ArrayList<double[]> posFeature = new ArrayList<double[]>();
		ArrayList<double[]> negFeature = new ArrayList<double[]>();
		ArrayList<Integer> posLabel = new ArrayList<Integer>();
		ArrayList<Integer> negLabel = new ArrayList<Integer>();
		for (int i = 0; i < labelList.size(); ++i) {
			int label = labelList.get(i);
			posFeature.add(features.get(i));
			negFeature.add(features.get(i));
			if (label == 0) {
				posLabel.add(0);
				negLabel.add(0);
			} else if (label == 1) {
				posLabel.add(1);
				negLabel.add(0);
			} else {
				posLabel.add(0);
				negLabel.add(1);
			}
		}
		positiveM = new SVMTool();
		negativeM = new SVMTool();
		positiveM.train(posFeature, posLabel);
		negativeM.train(negFeature, negLabel);
		
		if (model != null && !model.isEmpty()) {
			positiveM.save(model + ".pos");
			negativeM.save(model + ".neg");
		}
	}
}
