package com.tlabs.speechalyzer.classifier;

public class CategoryResult {
	private String cat;
	private double probability;

	public CategoryResult(String cat, double probability) {
		super();
		this.cat = cat;
		this.probability = probability;
	}

	public String toString() {
		return cat + " (" + probability + ")";
	}

	public String getCat() {
		return cat;
	}

	public void setCat(String cat) {
		this.cat = cat;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}
}
