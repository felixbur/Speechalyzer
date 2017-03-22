package com.tlabs.speechalyzer.classifier;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.Random;

import org.apache.log4j.Logger;

import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.Util;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

public class WEKAClassifier implements IClassifier {
	private String _trainFileName;
	private String _testFileName;
	private String _modelFileName;
	private Logger _logger;
	private KeyValues _config;
	private Categories _categories;
	private Classifier _classifier = null;
	private String _classifierType;

	public String getInfo() {
		return "WEKA classifier with model: " + _classifierType;
	}

	public String getModelFileName() {
		return _modelFileName;
	}

	public WEKAClassifier(KeyValues config) {
		_config = config;
		_logger = Logger
				.getLogger("com.tlabs.speechalyzer.classifier.WEKAClassifier");
		_classifierType = _config.getString("classifier");
		try {
			_trainFileName = _config.getAbsPath("trainFile");
			_testFileName = _config.getAbsPath("testFile");
			_modelFileName = FileUtil.addNamePart(_config
					.getAbsPath("modelFile"), "_" + _classifierType);
			_categories = new Categories(_config.getString("categories"));
			loadModel();
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public void setClassifierType(String classifierType) {
		if (_classifierType.compareTo(classifierType) == 0)
			return;
		_classifierType = classifierType;
		_modelFileName = FileUtil.addNamePart(_config.getAbsPath("modelFile"),
				"_" + _classifierType);
		loadModel();
	}

	public ClassificationResult classify() {
		try {
			String input = FileUtil.getFileText(_testFileName);
			Instances classifyInsts = new Instances(new StringReader(input));
			Instance instance = classifyInsts.instance(0);
			int classIndex = classifyInsts.numAttributes() - 1;
			classifyInsts.setClassIndex(classIndex);
			instance.setClassMissing();
			double[] res = _classifier.distributionForInstance(instance);
			ClassificationResult cr = new ClassificationResult();
			String[] catArray = _categories.getCategoryArray();
			for (int i = 0; i < catArray.length; i++) {
				cr.addResult(_categories.getCategoryArray()[i], res[i]);
			}
			double cls = _classifier.classifyInstance(instance);
			instance.setClassValue(cls);
			String resId = instance.stringValue(classIndex);
			System.out.println("erg: " + resId + ", " + cls);
			return cr;
		} catch (Exception e) {
			_logger
					.error("ERROR classifying. Perhaps model didn't fit test? : "
							+ e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public void trainModel() {
		try {
			_logger.info("training model ...");
			BufferedReader trainReader = new BufferedReader(new FileReader(
					_trainFileName));// File with
			// text
			// examples
			Instances trainInsts = new Instances(trainReader);
			trainInsts.setClassIndex(trainInsts.numAttributes() - 1);
			if (_classifier == null) {
				if (_classifierType.compareTo("smo") == 0)
					_classifier = new SMO();
				else if (_classifierType.compareTo("naiveBayes") == 0)
					_classifier = new NaiveBayes();
				else if (_classifierType.compareTo("j48") == 0)
					_classifier = new J48();
				else
					_logger.error("no/wrong classifier");
			}
			_classifier.buildClassifier(trainInsts);
			_logger.info("training model finished");
			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(new FileOutputStream(
						_modelFileName));
				oos.writeObject(_classifier);
			} catch (Exception e) {
				e.printStackTrace();
			}
			_logger.info("model saved to file: " + _modelFileName);
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public String evaluate() {
		_logger.info("evaluating...");
		try {
			BufferedReader trainReader = new BufferedReader(new FileReader(
					_config.getString("trainFile")));// File with
			// examples
			Instances trainInsts = new Instances(trainReader);
			trainInsts.setClassIndex(trainInsts.numAttributes() - 1);
			Evaluation eval = new Evaluation(trainInsts);
			eval.crossValidateModel(_classifier, trainInsts, 10, new Random());
			// return _classifier.toString()+"\n" + eval.toSummaryString();
			return eval.toSummaryString()+eval.toMatrixString();
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
		return "";
	}

	public void loadModel(String filePath) {
		_modelFileName = filePath;
		loadModel();
	}

	/**
	 * Load the model specified in configuration.
	 */
	public void loadModel() {
		_logger.info("loading model from file " + _modelFileName + "...");
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(_modelFileName)));
			_classifier = (Classifier) ois.readObject();
		} catch (Exception e) {
			reportError("problem opening classifier model: "
							+ e.getMessage());
		}
	}

	public void reportError(String mesg) {
		System.err.println(mesg);
		_logger.error(mesg);
	}
}
