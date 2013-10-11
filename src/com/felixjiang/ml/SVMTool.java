package com.felixjiang.ml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class SVMTool {
	svm_node [][]data = null;
	double [] label = null;
	svm_model model = null;
	
	public SVMTool() {
	}
	
	public void train(ArrayList<double[]> vfeatures, ArrayList<Integer> vlabel) {
		loadTrainingData(vfeatures, vlabel);
		svm_problem problem = new svm_problem();
		problem.l = data.length;
		problem.x = data;
		problem.y = label;
		svm_parameter param = new svm_parameter();
		param.cache_size = 1000;
		param.kernel_type = 0;
		param.eps = 0.001;
		param.C = 1;
		param.degree = 1;
		param.coef0 = 0;
		param.gamma = 0.1;
		param.probability = 1;
		if (svm.svm_check_parameter(problem, param) != null) {
			System.out.println(svm.svm_check_parameter(problem, param));
			System.exit(-1);
		}
		model = svm.svm_train(problem, param);
	}
	
	public void crossValidation() {
		svm_problem problem = new svm_problem();
		problem.l = data.length;
		problem.x = data;
		problem.y = label;
		svm_parameter param = new svm_parameter();
		param.cache_size = 1000;
		param.kernel_type = 0;
		param.eps = 0.001;
		param.C = 1;
		param.degree = 1;
		param.coef0 = 0;
		param.gamma = 0.0051;
//		param.probability = 1;
		if (svm.svm_check_parameter(problem, param) != null) {
			System.out.println(svm.svm_check_parameter(problem, param));
			System.exit(-1);
		}
		model = svm.svm_train(problem, param);
		
		
		double []target = new double[problem.l];
		svm.svm_cross_validation(problem, param, 5, target);
		
		int count = 0;
		for (int i = 0; i < target.length; ++i) {
			System.out.println(label[i] + " " + target[i]);
			if (target[i] == label[i])
				++count;
		}
		System.out.println("Count = " + count);
		System.out.println("Total = " + target.length);
		System.out.println("Accuracy" + 1.0 * count / target.length);
		
		int corr = 0, gold = 0, pro = 0;
		for (int i = 0; i < target.length; ++i) {
			if (target[i] == 1 && label[i] == 1)
				corr++;
			if (target[i] == 1)
				pro++;
			if (label[i] == 1)
				gold++;
		}
		double pre = 1.0 * corr / pro;
		double rec = 1.0 * corr / gold;
		
		int one = 0, zero = 0;
		for (int i = 0; i < label.length; ++i)
			if (label[i] > 0.5)
				++one;
			else 
				++zero;
		System.out.println("Zero = " + zero + ", one = " + one);
		System.out.println("Zero = " + 1.0 * zero / (zero + one) + ", one = " + 1.0 * one / (zero + one));
		System.out.println("Precision = " + pre);
		System.out.println("Recall = " + rec);
		System.out.println("F1 = " + 2.0 * pre * rec / (pre + rec));
		
		corr = 0;
		gold = 0;
		pro = 0;
		for (int i = 0; i < target.length; ++i) {
			if (target[i] == -1 && label[i] == -1)
				corr++;
			if (target[i] == -1)
				pro++;
			if (label[i] == -1)
				gold++;
		}
		pre = 1.0 * corr / pro;
		rec = 1.0 * corr / gold;
		
		one = 0;
		zero = 0;
		for (int i = 0; i < label.length; ++i)
			if (label[i] < -0.5)
				++one;
			else 
				++zero;
		System.out.println("Zero = " + zero + ", one = " + one);
		System.out.println("Zero = " + 1.0 * zero / (zero + one) + ", one = " + 1.0 * one / (zero + one));
		System.out.println("Precision = " + pre);
		System.out.println("Recall = " + rec);
		System.out.println("F1 = " + 2.0 * pre * rec / (pre + rec));
	}
	
	public ArrayList<Integer> predict(ArrayList<double[]> features) {
		ArrayList<Integer> target = new ArrayList<Integer>();
		for (int i = 0; i < features.size(); ++i) {
			svm_node []vec = new svm_node[features.get(i).length];
			for (int j = 0; j < vec.length; ++j) {
				vec[j] = new svm_node();
				vec[j].index = j + 1;
				vec[j].value = features.get(i)[j];
			}
			target.add((int)(svm.svm_predict(model, vec) * 1.1));
		}
		return target;
	}
	
	@Deprecated
	public double[][] predictTwo(double [][]features) {
		double [][]target = new double[features.length][];
		for (int i = 0; i < target.length; ++i) {
			svm_node []vec = new svm_node[features[i].length];
			double []prob = new double[8];
			for (int j = 0; j < vec.length; ++j) {
				vec[j] = new svm_node();
				vec[j].index = j + 1;
				vec[j].value = features[i][j];
			}
			svm.svm_predict_probability(model, vec, prob);
			for (int j = 0; j < prob.length; ++j)
				System.out.println(prob[j] + " ");
			System.out.println();
		}
		return target;
	}
	
	public double[] predictProb(double []feature) {
		svm_node []vec = new svm_node[feature.length];
		double []prob = new double[model.label.length];
		for (int j = 0; j < vec.length; ++j) {
			vec[j] = new svm_node();
			vec[j].index = j + 1;
			vec[j].value = feature[j];
		}
		svm.svm_predict_probability(model, vec, prob);
		return prob;
	}
	
	public Map<Integer, Double> predictProbability(double []feature) {
		double []prob = predictProb(feature);
		Map<Integer, Double> ret = new TreeMap<Integer, Double>();
		int []labels = new int[prob.length];
		svm.svm_get_labels(model, labels);
		for (int i = 0; i < prob.length; ++i)
			ret.put(labels[i], prob[i]);
		return ret;
	}
	
	public int predict(double []feature) {
		svm_node []vec = new svm_node[feature.length];
		for (int j = 0; j < vec.length; ++j) {
			vec[j] = new svm_node();
			vec[j].index = j + 1;
			vec[j].value = feature[j];
		}
		double type = svm.svm_predict(model, vec);
		return (int)(type * 1.1);
	}
	
	private void loadTrainingData(ArrayList<double[]> vfeature, ArrayList<Integer> vlabel) {
		assert vfeature != null && vfeature.size() > 0 && vfeature.get(0) != null;
		assert vlabel != null;
		assert vfeature.size() == vlabel.size();
		
		label = new double[vlabel.size()];
		for (int i = 0; i < vlabel.size(); ++i)
			label[i] = vlabel.get(i);
		data = new svm_node[vfeature.size()][vfeature.get(0).length];
		for (int i = 0; i < vfeature.size(); ++i) {
			for (int j = 0; j < vfeature.get(i).length; ++j) {
				data[i][j] = new svm_node();
				data[i][j].index = j + 1;
				data[i][j].value = vfeature.get(i)[j];
			}
		}
	}
	
	public void save(String filename) throws IOException {
		svm.svm_save_model(filename, model);
	}
	
	public void load(String filename) throws IOException {
		model = svm.svm_load_model(filename);
	}
}
