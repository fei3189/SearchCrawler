package com.felixjiang.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.felixjiang.ml.SVMTool;
import com.felixjiang.segmentation.ICTSegmentation;
import com.felixjiang.segmentation.SegmentationException;
import com.felixjiang.emotion.*;

/**
 * Compute the probability of each class one microblog message belongs to.
 * The classes includes: neutral(none), happiness, sadness, anger, like, fear, disgust, surprise
 * @author jiangfei
 *
 */
public class EmotionRater {
	EmotionFeatureExtractor extractor = null;
	SVMTool subjectivity = null;
	SVMTool emotion = null;
	
	/**
	 * @param segLibHome. Essential segmentation resources for ICTSeg
	 * @param resDir. Resources for analysis, such as segmentation, emotion lexicon
	 * @param subjM, emoM. svm model for subjectivity and emotion classification
	 * @throws SegmentationException
	 * @throws IOException
	 * @throws Exception
	 */
	public EmotionRater(String segLibHome, String resDir, String subjM, String emoM) throws IOException, SegmentationException {
		extractor = new OpenEmotionFeatureExtractor(new ICTSegmentation(segLibHome, 1), resDir);
		subjectivity = new SVMTool();
		emotion = new SVMTool();
		subjectivity.load(subjM);
		emotion.load(emoM);
	}
	
	public EmotionRater(String segLibHome, String resDir) throws IOException, SegmentationException {
		extractor = new OpenEmotionFeatureExtractor(new ICTSegmentation(segLibHome, 1), resDir);
	}
	
	/**
	 * Predict the probability of each class.
	 * @param weibo
	 * @return a map whose keys are labels and values are probabilities.
	 * @throws IOException
	 */
	public Map<Integer, Double> predict(String weibo) throws IOException {
		double[] features = extractor.extractFeature(weibo);
		Map<Integer, Double> subj = subjectivity.predictProbability(features);
		Map<Integer, Double> emo = emotion.predictProbability(features);
		Map<Integer, Double> ret = new HashMap<Integer, Double>();
		ret.put(0, subj.get(0));
		for (Integer i : emo.keySet()) {
			ret.put(i, emo.get(i) * subj.get(1));
		}
		return ret;
	}
	
	public void train(ArrayList<String> weiboList, ArrayList<Integer> labelList, String modelFile) throws IOException {
		double[][] features = extractor.extractAllFeatures(weiboList);
		ArrayList<Integer> subjLabel = new ArrayList<Integer>();
		ArrayList<double[]> subjFeature = new ArrayList<double[]>();
		if (features.length == 0)
			return;
		for (int i = 0; i < labelList.size(); ++i) {
			subjLabel.add(labelList.get(i) == 0 ? 0 : 1);
			subjFeature.add(features[i]);
		}
		subjectivity = new SVMTool();
		subjectivity.train(subjFeature, subjLabel);
		
		ArrayList<Integer> emoLabel = new ArrayList<Integer>();
		ArrayList<double[]> emoFeature = new ArrayList<double[]>();
		emotion = new SVMTool();
		for (int i = 0; i < labelList.size(); ++i) {
			int label = labelList.get(i);
			if (label != 0) {
				emoLabel.add(label);
				emoFeature.add(features[i]);
			}
		}
		emotion.train(emoFeature, emoLabel);
		if (modelFile != null && !modelFile.isEmpty()) {
			subjectivity.save(modelFile + ".emosubj");
			emotion.save(modelFile + ".emo");
		}
	}
}
