package com.tlabs.speechalyzer.emotions;

public class VocabItem {
	String _name = "";

	public String get_name() {
		return _name;
	}

	public void set_name(String _name) {
		this._name = _name;
	}

	public VocabItem(String _name) {
		super();
		this._name = _name;
	}

}
