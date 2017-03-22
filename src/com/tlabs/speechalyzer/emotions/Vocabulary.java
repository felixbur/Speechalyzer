package com.tlabs.speechalyzer.emotions;

import java.util.Vector;

public class Vocabulary {
	private Vector<VocabItem> _items;
	private String _type;
	private String _id;

	public void addItem(VocabItem newItem) {
		if (_items == null)
			_items = new Vector<VocabItem>();
		_items.add(newItem);
	}

	public Vector<VocabItem> get_items() {
		return _items;
	}

	public void set_items(Vector<VocabItem> _items) {
		this._items = _items;
	}

	public boolean isItemContained(String name) {
		if (_items == null)
			return false;
		for (VocabItem item : _items) {
			if (item.get_name().compareTo(name) == 0)
				return true;
		}
		return false;
	}

	public String get_type() {
		return _type;
	}

	public void set_type(String _type) {
		this._type = _type;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public Vocabulary(String _type, String _id) {
		super();
		this._type = _type;
		this._id = _id;
	}

}
