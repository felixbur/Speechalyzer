package com.tlabs.speechalyzer.classifier;

import java.util.Iterator;

import java.util.StringTokenizer;
import java.util.Vector;

import com.felix.util.KeyValue;
import com.felix.util.KeyValues;
import com.felix.util.Util;
import com.tlabs.speechalyzer.emotions.Emotion;

public class ClassificationResult {
	final static String NULL = "null";
	Vector<CategoryResult> results;
	String _descr = "";
	boolean isNull = false;

	public ClassificationResult() {
	}

	/**
	 * Initialize with a String of the form "cat_1 prob_1 ... cat_n prob_n"
	 * 
	 * @param resultsDescr
	 */
	public ClassificationResult(Emotion emotion) {
		if (emotion == null) {
			isNull = true;
		} else {
			try {
				addResult(emotion.get_name(), emotion.getValueAsDouble());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public ClassificationResult(String description) {
		KeyValues values = new KeyValues(description, ",", "=");
		for (KeyValue kv : values.getKeyValues()) {
			addResult(kv.getKey(), Double.parseDouble(kv.getValue()));
		}
	}

	public Vector<CategoryResult> getResults() {
		return results;
	}

	public void addResult(String cat, double prob) {
		if (results == null) {
			results = new Vector<CategoryResult>();
		}
		results.add(new CategoryResult(cat, prob));
		_descr += cat + "=" + Util.cutDouble(prob) + ",";
	}

	/**
	 * Returns category string with first highest probability.
	 * 
	 * @return
	 */
	public CategoryResult getWinner() {
		if (isNull)
			return null;
		CategoryResult winner = null;
		double highestProb = 0;
		for (Iterator<CategoryResult> iterator = results.iterator(); iterator
				.hasNext();) {
			CategoryResult cr = (CategoryResult) iterator.next();
			if (cr.getProbability() > highestProb) {
				winner = cr;
				highestProb = cr.getProbability();
			}
		}
		return winner;
	}

	/**
	 * Returns result for a given name.
	 * 
	 * @param catName
	 * @return
	 */
	public CategoryResult getResultForName(String catName) {
		if (isNull)
			return null;
		for (Iterator<CategoryResult> iterator = results.iterator(); iterator
				.hasNext();) {
			CategoryResult cr = (CategoryResult) iterator.next();
			if (cr.getCat().compareTo(catName.trim()) == 0) {
				return cr;
			}
		}
		return null;
	}

	public String toString() {
		return _descr.trim();
	}

}
