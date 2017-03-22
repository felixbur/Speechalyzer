package com.tlabs.speechalyzer.classifier;

public interface IClassifier {
	public final static String TYPE_SMO = "smo";
	public final static String TYPE_NAIVE_BAYEYS = "naiveBayes";
	public final static String TYPE_J48 = "j48";

	public abstract void trainModel();

	public abstract void loadModel();

	public abstract void loadModel(String path);

	public abstract String getModelFileName();

	public abstract String getInfo();

	public abstract ClassificationResult classify();

	public abstract String evaluate();

	public abstract void setClassifierType(String classifierType);
}
