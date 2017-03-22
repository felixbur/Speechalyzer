package com.tlabs.speechalyzer.classifier;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import com.felix.util.ArrayUtil;
import com.felix.util.FileUtil;
import com.felix.util.KeyValue;
import com.felix.util.KeyValues;
import com.felix.util.StatsUtil;
import com.felix.util.StringUtil;
import com.felix.util.Util;
import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.RecFile;

public class EvaluatorThread extends Thread {
	private boolean isRunning = false;
	private AudioFileManager _afm;
	private Categories _categories;
	private String _summary = "";
	private Vector<Evaluatable> _evaluatables;

	/**
	 * Constructor from inside with AudioFilemanager and Categories.
	 * 
	 * @param afm
	 * @param categories
	 */
	public EvaluatorThread(AudioFileManager afm, Categories categories) {
		super();
		_afm = afm;
		_categories = categories;
		_evaluatables = new Vector<Evaluatable>();
		Vector<RecFile> audioFiles = _afm.getAudioFiles();
		for (Iterator<RecFile> iterator = audioFiles.iterator(); iterator
				.hasNext();) {
			RecFile recFile = (RecFile) iterator.next();
			Evaluatable e = new Evaluatable(recFile.getStringLabel(), recFile
					.getClassificationResult().getWinner().getCat());
			_evaluatables.add(e);
		}
	}

	/**
	 * Constructor from outside with categories String descriptor and file with
	 * samples.
	 * 
	 * @param fileName
	 *            Samples, format, one sample each line: <samplename> <truth>
	 *            <hypothesis>
	 * @param categories
	 */
	public EvaluatorThread(String fileName, String categories) {
		super();
		_categories = new Categories(categories);
		_evaluatables = new Vector<Evaluatable>();
		try {
			Vector<String> lines = FileUtil
					.getFileLinesWithoutComments(fileName);
			for (Iterator<String> iterator = lines.iterator(); iterator
					.hasNext();) {
				String string = (String) iterator.next();
				String[] a = StringUtil.stringToArray(string);
				String truth = _categories.getCategoryForJudgement(Double
						.parseDouble(a[1]));
				Evaluatable e = new Evaluatable(truth.trim(), a[2]);
				_evaluatables.add(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public EvaluatorThread(String truthFileName, String testFileName,
			String categories) {
		super();
		_categories = new Categories(categories);
		_evaluatables = new Vector<Evaluatable>();
		try {
			Vector<String> testlines = FileUtil
					.getFileLinesWithoutComments(testFileName);
			Vector<TestCase> cases = new Vector<EvaluatorThread.TestCase>();
			for (String line : testlines) {
				String[] a = StringUtil.stringToArray(line);
				TestCase tc = new TestCase();
				tc.name = a[0];
				tc.test = a[1];
				cases.add(tc);
			}
			System.out.println("loaded " + cases.size() + " tests");
			Vector<String> truthlines = FileUtil
					.getFileLinesWithoutComments(truthFileName);
			int numberNotFound = 0;
			for (String line : truthlines) {
				String[] a = StringUtil.stringToArray(line);
				String name = a[0];
				boolean found = false;
				for (TestCase tc : cases) {
					if (tc.name.compareTo(name) == 0) {
						found = true;
						tc.truth = a[1];
						break;
					}
				}
				if (!found) {
					numberNotFound++;
				}
			}
			System.out.println("and ignored " + numberNotFound + " cases");

			for (TestCase tc:cases) {
				String truth = _categories.getCategoryForJudgement(Double
						.parseDouble(tc.truth));
				Evaluatable e = new Evaluatable(truth, tc.test);
				_evaluatables.add(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		isRunning = true;
		try {
			int catNum = _categories.getCatNumber() - 1;
			// get the category names
			String[] catArray = Util.subStringArray(
					_categories.getCategoryArray(), 1, catNum + 1);
			// initialize a square confusion matrix including "non available
			// values
			int[][] _confMatrix = ArrayUtil.getZeroQuadraticArray(catNum + 1);
			// fill the confusion matrix
			for (Iterator<Evaluatable> iterator = _evaluatables.iterator(); iterator
					.hasNext();) {
				Evaluatable e = (Evaluatable) iterator.next();
				_confMatrix[_categories.getCategoryIndex(e._truth)][_categories
						.getCategoryIndex(e._hypothesis)]++;
			}
			// ignore the "NA" values
			_confMatrix = ArrayUtil
					.subMatrix(_confMatrix, 1, catNum, 1, catNum);
			// number of samples
			int allNum = ArrayUtil.sum(_confMatrix);
			// number of correct samples
			int correctNum = ArrayUtil.diagSum(_confMatrix);
			_summary += "Recall\n";
			double allRecall = 0;
			_summary += Util.arrayToString(catArray).replace(" ", "\t") + "\n";
			for (int i = 0; i < catNum; i++) {
				double catRecall = (double) _confMatrix[i][i]
						/ ArrayUtil.rowSum(_confMatrix, i);
				_summary += Util.cutDouble(catRecall) + "\t";
				allRecall += catRecall;
			}
			_summary += "\n\nUnweighted Average Recall (UAR): ";
			double uar = Util.cutDouble((allRecall / catNum) * 100);
			_summary += uar + "\n";
			double averageAccuracy = Util
					.cutDouble(((double) correctNum / allNum) * 100);
			_summary += "\nWAR (weighted average recall: DIV(correct,all)): ";
			_summary += averageAccuracy + "\n";

			_summary += "\nNumber of samples (all/correct): " + allNum + "/"
					+ correctNum + "\n";
			_summary += "\nDistribution of classes (abs/rel):\n";
			_summary += Util.arrayToString(catArray).replace(" ", "\t") + "\n";
			for (int i = 0; i < catNum; i++) {
				_summary += ArrayUtil.rowSum(_confMatrix, i) + "\t";
			}
			_summary += "\n";
			for (int i = 0; i < catNum; i++) {
				_summary += ArrayUtil.percent(ArrayUtil.rowSum(_confMatrix, i),
						allNum) + "\t";
			}
			_summary += "\nConfusion Matrix (abs/rel)";
			_summary += "\n"
					+ ArrayUtil.toStringConfMatrix(_confMatrix, catArray);
			_summary += "\n\n"
					+ ArrayUtil.toStringRelativeConfMatrix(_confMatrix,
							catArray);
			_summary += "\n";
			_summary += "\nPrecision\n";
			double allPrecision = 0;
			_summary += Util.arrayToString(catArray).replace(" ", "\t") + "\n";
			for (int i = 0; i < catNum; i++) {
				double catPrecision = (double) _confMatrix[i][i]
						/ ArrayUtil.colSum(_confMatrix, i);
				_summary += Util.cutDouble(catPrecision) + "\t";
				allPrecision += catPrecision;
			}
			_summary += "\nF1\n";
			double allF1 = 0;
			_summary += Util.arrayToString(catArray).replace(" ", "\t") + "\n";
			for (int i = 0; i < catNum; i++) {
				double catPrecision = (double) _confMatrix[i][i]
						/ ArrayUtil.colSum(_confMatrix, i);
				double catRecall = (double) _confMatrix[i][i]
						/ ArrayUtil.rowSum(_confMatrix, i);
				double catF1 = StatsUtil.f1(catRecall, catPrecision);
				_summary += Util.cutDouble(catF1) + "\t";
				allF1 += catF1;
			}
			_summary += "\n\nUnweighted Average F1: ";
			_summary += allF1 / catNum + "\n";
			_summary += "\nUnweighted Average Precision: ";
			_summary += allPrecision / catNum + "\n";
			System.out.println(_summary);
		} catch (Exception e) {
			e.printStackTrace();
		}
		isRunning = false;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public String getSummary() {
		return _summary;
	}

	public static void main(String[] args) {
		String useage = "usage: <progname> <samplefile> <catdesc>\n\tsamplefile format <id> <truth> <pred>\n\tcatdesc format <num_1>,<cat_1>;...;<num_n>,<cat_n> NOTE that a -1,NA field is expected for garbage samples!";
		if (args.length == 2) {
			try {
				EvaluatorThread et = new EvaluatorThread(args[0], args[1]);
				et.start();
				while (et.isRunning) {
					Thread.sleep(1000);
				}
				System.out.println(et.getSummary());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (args.length == 3) {
			try {
				EvaluatorThread et = new EvaluatorThread(args[0], args[1], args[2]);
				et.start();
				while (et.isRunning) {
					Thread.sleep(1000);
				}
				System.out.println(et.getSummary());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println(useage);
		}

	}

	private class TestCase {
		String name;
		String truth;
		String test;
	}

	public class Evaluatable {
		public String _truth;
		public String _hypothesis;

		public Evaluatable(String truth, String hypothesis) {
			super();
			_truth = truth.trim();
			_hypothesis = hypothesis.trim();
		}

		public Evaluatable(String truth) {
			super();
			_truth = truth.trim();
		}

		public boolean isRight() {
			if (_truth.compareTo(_hypothesis) == 0)
				return true;
			return false;
		}

		public boolean isRight(String hypothesis) {
			if (_truth.compareTo(hypothesis) == 0)
				return true;
			return false;
		}
	}

}
