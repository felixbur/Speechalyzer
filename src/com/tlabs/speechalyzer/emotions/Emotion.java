package com.tlabs.speechalyzer.emotions;

import java.util.StringTokenizer;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;

import com.felix.util.KeyValues;

/**
 * An Emotion is an assertion on a certain emotional condition, its got a name,
 * a value and an originator, e.g. anger, 0.5, SVM algorithm based on training
 * xy
 * 
 * @author burkhardt.felix
 * 
 */
public class Emotion {
	public static final String INFO_SEPARATOR = ";";
	public static final String TYPE_CATEGORY = "category";
	public static final String TYPE_DIMENSION = "dimension";
	public static final String TYPE_APPRAISAL = "appraisal";
	public static final String TYPE_ACTION_TENDENCY = "action-tendency";
	public static final String EMOTIONML_ELEM_VOCABULARY = "vocabulary";
	public static final String EMOTIONML_ELEM_ITEM = "item";
	public static final String EMOTIONML_ELEM_ROOT = "emotionml";
	public static final String EMOTIONML_ELEM_EMOTION = "emotion";
	public static final String EMOTIONML_ELEM_CAT = "category";
	public static final String EMOTIONML_ELEM_APPRAISAL = "appraisal";
	public static final String EMOTIONML_ELEM_DIMENSION = "dimension";
	public static final String EMOTIONML_ELEM_ACTIONTENDENCY = "action-tendency";
	public static final String EMOTIONML_ELEM_INFO = "info";
	public static final String EMOTIONML_ELEM_REFERENCE = "reference";
	public static final String EMOTIONML_ATT_VERSION = "version";
	public static final String EMOTIONML_ATT_VALUE = "value";
	public static final String EMOTIONML_ATT_CONFIDENCE = "confidence";
	public static final String EMOTIONML_ATT_URI = "uri";
	public static final String EMOTIONML_ATT_TYPE = "type";
	public static final String EMOTIONML_ATT_ID = "id";
	public static final String EMOTIONML_ATT_CATEGORYSET = "category-set";
	public static final String EMOTIONML_ATT_DIMENSIONSET = "dimension-set";
	public static final String EMOTIONML_ATT_APPRAISALSET = "appraisal-set";
	public static final String EMOTIONML_ATT_ACTIONTENDENCYSET = "action-tendency-set";
	public static final String EMOTIONML_ATT_ROLE = "role";
	public static final String EMOTIONML_ATT_NAME = "name";
	public static final String EMOTIONML_VALUE_EXPRESSEDBY = "expressedBy";
	public final static Namespace EMOTIONML_NAMESPACE = Namespace.getNamespace("http://www.w3.org/2009/10/emotionml");
	String _name;
	String _originator;
	String _type;
	String _id;
	String _vocabId;
	String _value;
	String _confidence;
	double _valueDouble;
	double _confidenceDouble;
	KeyValues _config;

	public static Element getRoot() {
		return new Element(EMOTIONML_ELEM_ROOT, EMOTIONML_NAMESPACE);
	}

	public static Element getEmotion() {
		return new Element(EMOTIONML_ELEM_EMOTION, EMOTIONML_NAMESPACE);
	}

	public static Element getCategory() {
		return new Element(EMOTIONML_ELEM_CAT, EMOTIONML_NAMESPACE);
	}

	public static Element getDimension() {
		return new Element(EMOTIONML_ELEM_DIMENSION, EMOTIONML_NAMESPACE);
	}

	public static Element getAppraisal() {
		return new Element(EMOTIONML_ELEM_APPRAISAL, EMOTIONML_NAMESPACE);
	}

	public static Element getActionTendemcy() {
		return new Element(EMOTIONML_ELEM_ACTIONTENDENCY, EMOTIONML_NAMESPACE);
	}

	public static Element getInfo() {
		return new Element(EMOTIONML_ELEM_INFO, EMOTIONML_NAMESPACE);
	}

	public static Element getExpressedByReference() {
		Element elem = new Element(EMOTIONML_ELEM_REFERENCE, EMOTIONML_NAMESPACE);
		elem.setAttribute(new Attribute(EMOTIONML_ATT_ROLE, EMOTIONML_VALUE_EXPRESSEDBY));
		return elem;
	}

	public Emotion(KeyValues config, String name, String type, String value, String originator, String confidence,
			String vocabId) {
		super();
		_config = config;
		this._name = name;
		this._type = type;
		this._value = value;
		this._valueDouble = Double.parseDouble(value);
		this._confidence = confidence;
		this._confidenceDouble = Double.parseDouble(_confidence);
		_originator = originator;
		_vocabId = vocabId;
	}

	public static Emotion parseEmotion(KeyValues config, String emoDescriptor) {
		KeyValues labels = new KeyValues(emoDescriptor, ",", "=");
		return new Emotion(config, labels.getString(config.getString("emotion.label.cat")),
				labels.getString(config.getString("emotion.label.type")),
				labels.getString(config.getString("emotion.label.val")),
				labels.getString(config.getString("emotion.label.orig")),
				labels.getString(config.getString("emotion.label.conf")),
				labels.getString(config.getString("emotion.label.vocabId")));

	}

	public static Emotion getDefaultEmotion(KeyValues config) {
		String defaultCat = config.getString("defaultEmotionName");
		String defaultType = config.getString("defaultEmotionType");
		String defaultUser = config.getString("defaultUser");
		String defaultConf = config.getString("defaultConfidence");
		String defaultVocabId = config.getString("defaultConfidence");
		String emoDescriptor = config.getString("emotion.label.val") + "=0" + ","
				+ config.getString("emotion.label.cat") + "=" + defaultCat + ","
				+ config.getString("emotion.label.orig") + "=" + defaultUser + ","
				+ config.getString("emotion.label.type") + "=" + defaultType + ","
				+ config.getString("emotion.label.conf") + "=" + defaultConf + ","
				+ config.getString("emotion.label.vocabId") + "=" + defaultVocabId;
		return parseEmotion(config, emoDescriptor);

	}

	public String get_originator() {
		return _originator;
	}

	public void set_originator(String _originator) {
		this._originator = _originator;
	}

	public String get_type() {
		return _type;
	}

	public void set_type(String _type) {
		this._type = _type;
	}

	public String get_name() {
		return _name;
	}

	public String get_vocabId() {
		return _vocabId;
	}

	public void set_vocabId(String _vocabId) {
		this._vocabId = _vocabId;
	}

	public String toString() {
		return _config.getString("emotion.label.val") + "=" + _value + "," + _config.getString("emotion.label.cat")
				+ "=" + _name + "," + _config.getString("emotion.label.orig") + "=" + _originator + ","
				+ _config.getString("emotion.label.type") + "=" + _type + "," + _config.getString("emotion.label.conf")
				+ "=" + _confidence + "," + _config.getString("emotion.label.vocabId") + "=" + _vocabId + ";";
	}

	public boolean isCategory() {
		if (_type.compareTo(TYPE_CATEGORY) == 0)
			return true;
		return false;
	}

	public boolean isDimension() {
		if (_type.compareTo(TYPE_DIMENSION) == 0)
			return true;
		return false;
	}

	public boolean isAppraisal() {
		if (_type.compareTo(TYPE_APPRAISAL) == 0)
			return true;
		return false;
	}

	public boolean isActionTendency() {
		if (_type.compareTo(TYPE_ACTION_TENDENCY) == 0)
			return true;
		return false;
	}

	public void set_name(String _name) {
		this._name = _name;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String get_value() {
		return _value;
	}

	public void set_value(String _value) {
		this._value = _value;
		this._valueDouble = Double.parseDouble(_value);
	}

	public void setValueAsDouble(double newVal) {
		_valueDouble = newVal;
		_value = String.valueOf(newVal);
	}

	public double getValueAsDouble() {
		return _valueDouble;
	}

	public String get_confidence() {
		return _confidence;
	}

	public void set_confidence(String confidence) {
		this._confidence = confidence;
		this._confidenceDouble = Double.parseDouble(_confidence);
	}

	public void setConfidenceAsDouble(double newConfidence) {
		_confidenceDouble = newConfidence;
		_confidence = String.valueOf(newConfidence);
	}

	public double getConfidenceAsDouble() {
		return _confidenceDouble;
	}
}
