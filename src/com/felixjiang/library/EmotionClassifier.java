package com.felixjiang.library;

import java.io.IOException;
import java.util.ArrayList;
import com.felixjiang.emotion.EmotionFeatureExtractor;
import com.felixjiang.emotion.OpenEmotionFeatureExtractor;
import com.felixjiang.ml.SVMTool;
import com.felixjiang.segmentation.ICTSegmentation;
import com.felixjiang.segmentation.SegmentationException;

/**
 * @param segLibHome. Essential segmentation resources for ICTSeg
 * @param resDir. Resources for analysis, such as segmentation, emotion lexicon
 * @param subjM, emoM. svm model for subjectivity and emotion classification
 * @throws SegmentationException
 * @throws IOException
 * @throws Exception
 */
public class EmotionClassifier {
	EmotionFeatureExtractor extractor = null;
	SVMTool subjectivity = null;
	SVMTool emotion = null;
	
	/**
	 * Predict the probability of each class.
	 * @param weibo
	 * @return a map whose keys are labels and values are probabilities.
	 * @throws IOException
	 */
	public EmotionClassifier(String segLibHome, String resDir, String subjM, String emoM) throws IOException, SegmentationException {
		extractor = new OpenEmotionFeatureExtractor(new ICTSegmentation(segLibHome, 1), resDir);
		subjectivity = new SVMTool();
		emotion = new SVMTool();
		subjectivity.load(subjM);
		emotion.load(emoM);
	}
	
	public EmotionClassifier(String segLibHome, String resDir) throws IOException, SegmentationException {
		extractor = new OpenEmotionFeatureExtractor(new ICTSegmentation(segLibHome, 1), resDir);
	}
	
	/**
	 * Predict the probability of each class.
	 * @param weibo
	 * @return a map whose keys are labels and values are probabilities.
	 * @throws IOException
	 */
	public int predict(String weibo) throws IOException {
		double[] features = extractor.extractFeature(weibo);
		int subj = subjectivity.predict(features);
		if (subj == 0)
			return 0;
		return emotion.predict(features);
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
