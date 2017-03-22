package com.tlabs.speechalyzer;

import java.util.*;


/**
 * <p>
 * �berschrift:
 * </p>
 * <p>
 * Beschreibung:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Organisation: T-Systems
 * </p>
 * 
 * @author burkhardt
 * @version 1.0
 * @date 10.03.2004
 */
public class EmotionAnalyser {
	private Vector ratings = null;

	static {
		System.loadLibrary("SymEmotionNATIVE");
	}

	public EmotionAnalyser(String sympaConfig) {
		try {

		} catch (Exception e) {
			e.printStackTrace();
		}

		ratings = new Vector();
	}

	public void setRating(double[] speechRate, double wordRate) {
		Rating rating = new Rating(speechRate, wordRate);
		ratings.add(rating);
	}

	public void resetRatings() {
		ratings.removeAllElements();
	}

	public double getNeutral() {
		if (ratings.size() > 0)
			return ((Rating) ratings.lastElement()).getSpeechNERate();
		else
			return 0.0;
	}

	public double getColdAnger() {
		if (ratings.size() > 0)
			return ((Rating) ratings.lastElement()).getSpeechCARate();
		else
			return 0.0;
	}

	public double getHotAnger() {
		if (ratings.size() > 0)
			return ((Rating) ratings.lastElement()).getSpeechHARate();
		else
			return 0.0;
	}

	private class Rating {
		private double speechCARate = 0;

		private double speechHARate = 0;

		private double speechNERate = 0;

		private double wordRate = 0;

		public Rating(double[] speechRate, double wordRate) {
			this.speechNERate = speechRate[0];
			this.speechCARate = speechRate[1];
			this.speechHARate = speechRate[2];
			this.wordRate = wordRate;
		}

		public double getWordRate() {
			return wordRate;
		}

		public String toString() {
			return "Emotional Rating: neutral: " + speechNERate
					+ ", coldAnger: " + speechCARate + ", hotAnger: "
					+ speechHARate + ", words: " + wordRate;
		}

		public double getSpeechCARate() {
			return speechCARate;
		}

		public double getSpeechHARate() {
			return speechHARate;
		}

		public double getSpeechNERate() {
			return speechNERate;
		}
	}

}