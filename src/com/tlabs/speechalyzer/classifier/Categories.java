package com.tlabs.speechalyzer.classifier;

import java.util.StringTokenizer;

import com.felix.util.KeyValue;
import com.felix.util.KeyValues;
import com.felix.util.StringUtil;
import com.tlabs.speechalyzer.Constants;

public class Categories {
	private String _catString;
	KeyValues _catKeys;
	private int _catNum = 0;
	private String [] _cats;

	/**
	 * Initialize Categories from String of format
	 * "lab_1,cat_1;...;lab_n,cat_n". Numeric labels MUST be in ascending order.
	 * 
	 * @param initString
	 */
	public Categories(String initString) {
		_catKeys = new KeyValues(initString, ";", ",");
		_catString = _catKeys.getUniqValuesAsString();
		_cats = _catKeys.getUniqValuesAsArray();
		_catNum = _cats.length;
	}

	/**
	 * Return the highest category that assigned to the given number.
	 * 
	 * @param judgement
	 * @return
	 */
	public String getCategoryForJudgement(double judgement) {
		String lastCat = _catKeys.getKeyValues()[0].getValue();
		for (int i = 0; i < _catKeys.getKeyValues().length; i++) {
			KeyValue kv = _catKeys.getKeyValues()[i];
			if (judgement < Double.parseDouble(kv.getKey())) {
				return lastCat;
			} else {
				lastCat = kv.getValue();
			}
		}
		return lastCat;
	}

	/**
	 * Return index of category string.
	 * 
	 * @param cat
	 * @return
	 */
	public int getCategoryIndex(String cat) {
		for (int i = 0; i < _cats.length; i++) {
			if (cat.compareTo(_cats[i]) == 0)
				return i;
		}
		return -1;
	}

	/**
	 * Return comma separated list of category names.
	 * 
	 * @return
	 */
	public String toCommaSeparatedCategoryList() {
		return _catString.replace(" ", ",");
	}

	/**
	 * Return blank separated list of category names.
	 * 
	 * @return
	 */
	public String getCatString() {
		return _catString;
	}

	/**
	 * Return blank separated list of numeric labels and categories.
	 */
	public String toString() {
		return _catKeys.toString();
	}

	/**
	 * Get number of label/category pairs.
	 * 
	 * @return
	 */
	public int getNumber() {
		return _catKeys.getSize();
	}

	/**
	 * Get number of categories
	 * 
	 * @return
	 */
	public int getCatNumber() {
		return _catNum;
	}

	/**
	 * Return categorie names as array.
	 * 
	 * @return
	 */
	public String[] getCategoryArray() {
		return StringUtil.stringToArray(_catString);
	}
}
