/*
 * Created on 10.05.2005
 * 
 *
 * @author Felix Burkhardt
 */
package com.tlabs.speechalyzer;

import java.io.File;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

import com.felix.util.Constants;
import com.felix.util.FileUtil;
import com.felix.util.KeyValue;
import com.felix.util.KeyValues;
import com.felix.util.NumberToWord;
import com.felix.util.Preprocessor;
import com.felix.util.StringUtil;
import com.tlabs.speechalyzer.classifier.Categories;
import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.emotions.Emotion;
import com.tlabs.speechalyzer.emotions.VocabManager;
import com.tlabs.speechalyzer.synthesizer.EmofiltSynthesizer;
import com.tlabs.speechalyzer.synthesizer.ISynthesizer;
import com.tlabs.speechalyzer.synthesizer.IvonaSynthesizer;
import com.tlabs.speechalyzer.synthesizer.Mary50Synthesizer;
import com.tlabs.speechalyzer.synthesizer.SvoxSynthesizer;
import com.tlabs.speechalyzer.util.EmlUtils;

/**
 * @author Burkhardt.Felix
 * 
 *         Description:
 * 
 */
public class RecFile {
	public File _file;
	public String _dialog = "", _name = "", _path = "", _transcript = "",
			_recognized = "", _emotionMLDimensionSet = "",
			_emotionMLCategorySet = "", _emotionMLAppraisalSet = "",
			_emotionMLActionTendencySet = "";
	public ClassificationResult _classificationResult;
	public Emotion[] _lab = null;
	public long _size;
	private KeyValues _config, _annotations;
	private Logger _logger;
	private Categories _categories;
	private final String _transcriptIdentifier;
	private final String _recognitionIdentifier;
	private String _defaultOriginator = "";
	private double _meanEmotionValue = -1;
	private double _angerBorder = -1;
	
	
	/**
	 * @param _file
	 * @param _dialog
	 * @param _name
	 */
	public RecFile(String path, KeyValues config) {
		_config = config;
		_angerBorder = _config.getDouble("angerBorder");
		_defaultOriginator = _config.getString("defaultUser");
		_transcriptIdentifier = _config.getString("transcriptIdentifier");
		_recognitionIdentifier = _config.getString("recognitionIdentifier");
		_logger = Logger.getLogger("com.tlabs.speechalyzer.RecFile");
		_file = new File(path);
		_size = _file.length();
		File parFile = _file.getParentFile();
		if (parFile != null) {
			_dialog = parFile.getName();
		}
		_path = path;
		_name = _file.getName();
		_categories = new Categories(_config.getString("categories"));
		loadAnnotations();
	}

	public void initAnnotations() {
		_lab = new Emotion[0];
		_annotations = new KeyValues("", "\n", ":", false);
		_transcript = "";
		_recognized = "";
		_classificationResult = null;
	}

	public void setDefaultOriginator(String dfo) {
		_defaultOriginator = dfo;
	}

	public String toString() {
		String ret = "";
		String pathS = _path != null ? _path : "null";
		String sizeS = "0";
		if (_file != null)
			sizeS = String.valueOf(_file.length());
		String transS = _transcript != null ? _transcript : "null";
		String labS = _lab != null ? labToString() : "null";
		String predS = _classificationResult != null ? _classificationResult
				.toString() : "null";

		ret = "_path: " + pathS + "; _size: " + sizeS + "; _transcript: "
				+ transS + "; _lab: " + labS + " cr: " + predS;
		return ret;
	}

	public boolean hasLabel() {
		if (_lab != null && _lab.length > 0)
			return true;
		return false;
	}

	public boolean hasPrediction() {
		if (_classificationResult != null)
			return true;
		return false;
	}

	public void addInfoAndEmotion(Element infoElem, Emotion emotion) {
		emotion.set_originator(_defaultOriginator);
		if (infoElem != null) {
			String test = infoElem.getTextTrim();
			if (StringUtil.isFilled(test)) {
				String originatorIdentifier = _config
						.getString("originatorIdentifier");
				String originTest = StringUtil.getStringBetween(test,
						originatorIdentifier + ":", Emotion.INFO_SEPARATOR);
				if (originTest != null && emotion != null) {
					emotion.set_originator(originTest);
				}
				String transcriptTest = StringUtil.getStringBetween(test,
						_transcriptIdentifier + ":", Emotion.INFO_SEPARATOR);
				if (transcriptTest != null) {
					storeTranscript(transcriptTest);
				}
				String recoTest = StringUtil.getStringBetween(test,
						_recognitionIdentifier + ":", Emotion.INFO_SEPARATOR);
				if (recoTest != null) {
					storeRecongnition(recoTest);
				}
			}
		}
		addEmotion(emotion);
	}

	public void set_emotionMLDimensionSet(String _emotionMLDimensionSet) {
		this._emotionMLDimensionSet = _emotionMLDimensionSet;
	}

	public void set_emotionMLCategorySet(String _emotionMLCategorySet) {
		this._emotionMLCategorySet = _emotionMLCategorySet;
	}

	public void set_emotionMLAppraisalSet(String _emotionMLAppraisalSet) {
		this._emotionMLAppraisalSet = _emotionMLAppraisalSet;
	}

	public void set_emotionMLActionTendencySet(
			String _emotionMLActionTendencySet) {
		this._emotionMLActionTendencySet = _emotionMLActionTendencySet;
	}

	public Collection<Element> getEmotionMLElement() {
		Vector<Element> returnVec = new Vector<Element>();
		if (hasLabel()) {
			for (int i = 0; i < _lab.length; i++) {
				Element emoElem = new Element(Emotion.EMOTIONML_ELEM_EMOTION,
						Emotion.EMOTIONML_NAMESPACE);
				if (StringUtil.isFilled(_emotionMLCategorySet))
					emoElem.setAttribute(Emotion.EMOTIONML_ATT_CATEGORYSET,
							_emotionMLCategorySet);
				if (StringUtil.isFilled(_emotionMLDimensionSet))
					emoElem.setAttribute(Emotion.EMOTIONML_ATT_DIMENSIONSET,
							_emotionMLDimensionSet);
				if (StringUtil.isFilled(_emotionMLAppraisalSet))
					emoElem.setAttribute(Emotion.EMOTIONML_ATT_APPRAISALSET,
							_emotionMLAppraisalSet);
				if (StringUtil.isFilled(_emotionMLActionTendencySet))
					emoElem.setAttribute(
							Emotion.EMOTIONML_ATT_ACTIONTENDENCYSET,
							_emotionMLActionTendencySet);
				Element refElem = Emotion.getExpressedByReference();
				refElem.setAttribute(Emotion.EMOTIONML_ATT_URI, "file:///"
						+ _path);
				emoElem.addContent(refElem);
				Emotion emotion = (Emotion) _lab[i];
				Element elem = null;
				if (emotion.isCategory()) {
					elem = Emotion.getCategory();
				} else if (emotion.isDimension()) {
					elem = Emotion.getDimension();
				} else if (emotion.isAppraisal()) {
					elem = Emotion.getAppraisal();
				} else if (emotion.isActionTendency()) {
					elem = Emotion.getActionTendemcy();
				}
				elem.setAttribute(new Attribute(Emotion.EMOTIONML_ATT_NAME,
						emotion.get_name()));
				elem.setAttribute(new Attribute(Emotion.EMOTIONML_ATT_VALUE,
						emotion.get_value()));
				elem.setAttribute(new Attribute(
						Emotion.EMOTIONML_ATT_CONFIDENCE, emotion
								.get_confidence()));
				emoElem.addContent(elem);
				Element infolem = Emotion.getInfo();
				String infoS = "";
				String transcriptIdentifier = _config
						.getString("transcriptIdentifier");
				String recognitionIdentifier = _config
						.getString("recognitionIdentifier");
				String originatorIdentifier = _config
						.getString("originatorIdentifier");
				if (hasTranscript()) {
					infoS += transcriptIdentifier + ":" + _transcript
							+ Emotion.INFO_SEPARATOR;
				}
				if (hasRecognition()) {
					infoS += recognitionIdentifier + ":" + _recognized
							+ Emotion.INFO_SEPARATOR;
				}
				infoS += originatorIdentifier + ":" + emotion.get_originator()
						+ Emotion.INFO_SEPARATOR;
				infolem.addContent(infoS);
				emoElem.addContent(infolem);
				returnVec.add(emoElem);
			}

			return returnVec;
		}
		return null;
	}

	public boolean hasTranscript() {
		if (StringUtil.isFilled(_transcript))
			return true;
		return false;
	}

	public boolean hasRecognition() {
		if (StringUtil.isFilled(_recognized))
			return true;
		return false;
	}

	public String getTrainingFormat() {
		double val = computeLab(_lab);
		String ret = singleLabelToString(val);
		if (ret == null) {
			ret = "NA";
		}
		try {
			return new File(_path).getAbsolutePath() + " " + ret;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Return string value unified from numeric label values.
	 * 
	 * @return The value.
	 */
	public String getStringLabel() {
		double val = computeLab(_lab);
		String ret = singleLabelToString(val);
		if (ret == null) {
			ret = com.tlabs.speechalyzer.Constants.CLASS_NO_CLASS_STRING;
		}
		return ret;
	}

	/**
	 * Try to recognize the words of an audio and store as transcript.
	 * 
	 * @return The recognition result.
	 */
	public String recognize() {
		String result = EmlUtils.recognizeFile(_path);
		storeRecongnition(result);
		return result;
	}

	/**
	 * Return one blank-separated string value for each numeric label.
	 * 
	 * @return
	 */
	public String getStringLabels() {
		String ret = "";
		if (_lab == null || _lab.length == 0) {
			return null;
		}
		try {
			for (int i = 0; i < _lab.length; i++) {
				double val = ((Emotion) _lab[i]).getValueAsDouble();
				ret += singleLabelToString(val) + " ";
			}
			return ret.trim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public ClassificationResult getClassificationResult() {
		return _classificationResult;
	}

	/**
	 * Decide whether a numeric label is angry.
	 * 
	 * @param val
	 *            The numeric label.
	 * @return "A" for anger, "N" for non-anger, "U" for unsure and "G" for
	 *         garbage / non-applicable.
	 */
	public String singleLabelToString(double val) {
		return _categories.getCategoryForJudgement(val);
	}

	/**
	 * Return transcript and label, e.g. recs/rec.wav "bla bla" -3
	 * 
	 * @return transcript and label or null of either isn't set.
	 */
	public String getTranscritpionAndLabel() {
		if (_transcript.length() > 0 && _lab != null)
			return _path + " " + "\"" + _transcript + "\"" + " "
					+ computeLab(_lab);
		else
			return null;
	}

	/**
	 * Normalize (and store) the transcription automatically following the tlabs
	 * transcription rules.
	 * 
	 * @return The normalized transciption.
	 */
	public String normalizeTranscription() {
		String ret = "";
		Preprocessor p = new Preprocessor(_config.getString("normalizeRules"),
				_config.getString("normalizeVocab"));
		ret = p.process(_transcript);
		ret = new NumberToWord().filtNum(ret);
		storeTranscript(ret);
		return ret;
	}

	/**
	 * Compute a unified value from all labels.
	 * 
	 * @param _lab
	 * @return
	 */
	private double computeLab(Emotion lab[]) {
		if (lab == null || lab.length == 0) {
			return -1;
		}
		double sum = 0;
		int sumOfZero = 0;
		;
		for (int i = 0; i < lab.length; i++) {
			double val = ((Emotion) lab[i]).getValueAsDouble();
			sum += val;
			if (val == 0) {
				sumOfZero++;
			}
		}
		// if most labelers judge "NA" return "NA"
		if (sumOfZero > lab.length / 2) {
			return 0;
		}

		return sum / lab.length;
	}

	/**
	 * Print all labels.
	 * 
	 * @return The String.
	 */
	public String labToString() {
		String ret = "";
		if (_lab == null || _lab.length == 0) {
			return "-1";
		}
		for (int i = 0; i < _lab.length; i++) {
			double val = ((Emotion) _lab[i]).getValueAsDouble();
			ret += val + " ";
		}
		return ret.trim();
	}

	/**
	 * Print all labels as integers.
	 * 
	 * @return The String.
	 */
	public String labToIntString() {
		String ret = "";
		if (_lab == null || _lab.length == 0) {
			return "-1";
		}
		for (int i = 0; i < _lab.length; i++) {
			double val = ((Emotion) _lab[i]).getValueAsDouble();
			val = val * _categories.getCatNumber() - 1;
			ret += (int) val + " ";
		}
		return ret.trim();
	}

	/**
	 * Fill all internal fields with transcriptions and labels found in text
	 * _file.
	 */
	public void loadAnnotations() {
		try {
			String labelIdentifier = _config.getString("labelIdentifier");
			String transcriptIdentifier = _config
					.getString("transcriptIdentifier");
			String recognitionIdentifier = _config
					.getString("recognitionIdentifier");
			String predIdentifer = _config.getString("predictionIdentifier");
			String txtS = FileUtil.getNameWithoutExtension(_path) + "."
					+ _config.getString("labelFileExtension");
			_transcript = "";
			if (FileUtil.existFile(txtS)) {
				_annotations = new KeyValues(new File(txtS), ":",
						_config.getString("charEnc"), false);
				String prediction = null;
				if (_annotations.getString(predIdentifer) != null) {
					prediction = _annotations.getString(predIdentifer);
					if (_config.getBool("emotionmlMode")) {
						Emotion e = Emotion.parseEmotion(_config, prediction);
						_classificationResult = new ClassificationResult(e);
					} else {
						_classificationResult = new ClassificationResult(prediction);						
					}
				}
				String labelsS = null;
				if (_annotations.getString(labelIdentifier) != null) {
					labelsS = _annotations.getString(labelIdentifier);
					fillLabelArrayFromString(labelsS);
				}
				if (_annotations.getString(transcriptIdentifier) != null)
					_transcript = _annotations.getString(transcriptIdentifier);
				if (_annotations.getString(recognitionIdentifier) != null)
					_recognized = _annotations.getString(recognitionIdentifier);
			} else {
				// System.out.println("no _transcript for " + txtS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fillLabelArrayFromString(String labelString) {
		StringTokenizer st = new StringTokenizer(labelString, ";");
		_lab = new Emotion[st.countTokens()];
		int i = 0;
		double eVal = 0;
		while (st.hasMoreTokens()) {
			String label = st.nextToken();
			Emotion e = Emotion.parseEmotion(_config, label);
			_lab[i++] = e;
			eVal += e.getValueAsDouble();
			
		}
		_meanEmotionValue = eVal/(double) i;
	}
	
	public boolean isAngry(){
		if (_meanEmotionValue >=_angerBorder) {
			return true;
		}
		return false;
	}
	

	/**
	 * Retrieve the _transcript.
	 * 
	 * @return _transcript or null.
	 */
	public String getTranscript() {
		String transcriptIdentifier = _config.getString("transcriptIdentifier");
		return getAnnotationForLabel(transcriptIdentifier);
	}

	public String getRecognition() {
		return getAnnotationForLabel(_config.getString("recognitionIdentifier"));
	}

	private String getAnnotationForLabel(String label) {
		loadAnnotations();
		return _annotations.getString(label);
	}

	private void checkInitLab() {
		if (_lab == null)
			_lab = new Emotion[0];
	}
	public void addLabel(String category, String newLabel) {
		Emotion e = Emotion.getDefaultEmotion(_config);
		e.set_value(newLabel);
		e.set_name(category);
		addEmoLabel(e.toString());
	}
		
	/**
	 * Adds a label to existing ones.
	 * 
	 * @param label
	 */
	public void addEmoLabel(String emoDescriptor) {
		checkInitAnnotationKeyValues();
		checkInitLab();
		try {
			String labelIdentifier = _config.getString("labelIdentifier");
			String labelsS = "";
			if (_annotations.getString(labelIdentifier) != null)
				labelsS = _annotations.getString(labelIdentifier);
			int i = 0;
			String newString = labelsS + emoDescriptor;
			Emotion[] newlab = new Emotion[_lab.length + 1];
			for (Emotion e : _lab) {
				newlab[i++] = e;
			}

			newlab[i] = Emotion.parseEmotion(_config,
					emoDescriptor.substring(0, emoDescriptor.length() - 1));

			_annotations.setValue(labelIdentifier, newString);
			_lab = newlab;
			storeAnnotationFile();
		} catch (Exception e) {
			e.printStackTrace();
			_logger.error("error adding label: " + emoDescriptor + " at " + _path);
		}

	}

	public void addEmotion(Emotion e) {
		if (e == null)
			return;
		addEmoLabel(e.toString());
	}

	
	public void replaceLabel(String category, String label) {
		Emotion e = Emotion.getDefaultEmotion(_config);
		e.set_value(label);
		e.set_name(category);
		replaceLabel(e.toString());

	}
	/**
	 * Replaces a label, overwrites if already existed.
	 * 
	 * @param label
	 */
	public void replaceLabel(String emoDescriptor) {
		checkInitAnnotationKeyValues();
		try {
			String labelIdentifier = _config.getString("labelIdentifier");
			_annotations.setValue(labelIdentifier, emoDescriptor);
			fillLabelArrayFromString(emoDescriptor);
			storeAnnotationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	
	/**
	 * Remove the last label that is stored in the _file.
	 */
	public void removeLastLabel() {
		checkInitAnnotationKeyValues();
		try {
			String labelIdentifier = _config.getString("labelIdentifier");
			String labelsS = _annotations.getString(labelIdentifier);
			StringTokenizer st = new StringTokenizer(labelsS, ";");
			int i = 0, sum = _lab.length;
			String newString = "";
			Emotion[] newlab = new Emotion[_lab.length - 1];
			while (i < sum - 1) {
				String label = st.nextToken();
				newString += label + ";";
				newlab[i++] = Emotion.parseEmotion(_config, label);
			}
			_annotations.setValue(labelIdentifier, newString);
			_lab = newlab;
			storeAnnotationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Remove the annotation _file from disk.
	 */
	public void removeAnnotationFile() {
		try {
			String path = _file.getAbsolutePath();
			String txtS = FileUtil.getNameWithoutExtension(path) + "."
					+ _config.getString("labelFileExtension");
			if (FileUtil.existFile(txtS)) {
				new File(txtS).delete();
			} else {
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remove the annotation and audio file from disk.
	 */
	public void removeFiles() {
		try {
			String path = _file.getAbsolutePath();
			String txtS = FileUtil.getNameWithoutExtension(path) + "."
					+ _config.getString("labelFileExtension");
			if (FileUtil.existFile(txtS)) {
				new File(txtS).delete();
			} else {
				_logger.info("no annotation file for deletion");
			}
			if (FileUtil.existFile(path)) {
				new File(path).delete();
			} else {
				_logger.info("no audio file for deletion");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Remove the recording from disk.
	 */
	public void removeAudioFile() {
		try {
			if (FileUtil.existFile(_path)) {
				new File(_path).delete();
			} else {
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Remove all labels from annotation _file.
	 */
	public void removeLabels() {
		if (_annotations == null)
			return;
		try {
			String identifer = _config.getString("labelIdentifier");
			_annotations.removeKeyValue(identifer);
			storeAnnotationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Remove all labels from annotation _file.
	 */
	public void removePrediction() {
		if (_annotations == null)
			return;
		try {
			String predIdentifer = _config.getString("predictionIdentifier");
			_annotations.removeKeyValue(predIdentifer);
			storeAnnotationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Store a new _transcript, i.e. replace an existing one.
	 * 
	 * @param label
	 */
	public void storeTranscript(String transcript) {
		String label = _config.getString("transcriptIdentifier");
		storeAnnotation(label, transcript);
		_transcript = transcript.trim();
	}

	/**
	 * Store a prediction from automatic emotion detector into annotation _file.
	 * 
	 * @param res
	 */
	public void storePred(ClassificationResult cr) {
		String predIdentifier = _config.getString("predictionIdentifier");
		storeAnnotation(predIdentifier, cr.toString());
	}

	/**
	 * Store a new recognition result, i.e. replace an existing one.
	 * 
	 * @param label
	 */
	public void storeRecongnition(String recognition) {
		String label = _config.getString("recognitionIdentifier");
		storeAnnotation(label, recognition);
		_recognized = recognition.trim();
	}

	public void rename(String newName) {
		try {
			String oldName = _path;
			String oldTextName = FileUtil.getNameWithoutExtension(_path)
					+ ".txt";
			String path = _file.getParentFile().getAbsolutePath();
			String newPath = path + System.getProperty("file.separator")
					+ newName;
			FileUtil.rename(oldName, newPath);
			_path = newPath;
			_file = new File(_path);
			newPath = FileUtil.getNameWithoutExtension(newPath) + ".txt";
			FileUtil.rename(oldTextName, newPath);
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public void copyLabelsFromOtherRecFile(RecFile other) {
		for (Emotion e : other._lab) {
			addEmotion(e);
		}
	}

	private void checkInitAnnotationKeyValues() {
		if (_annotations == null) {
			_annotations = new KeyValues("", "\n", ":");
		}
	}

	private void storeAnnotation(String label, String annotation) {
		if (annotation.trim().length() == 0)
			return;
		checkInitAnnotationKeyValues();
		try {
			_annotations.setValue(label, annotation);
			storeAnnotationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void storeAnnotationFile() {
		checkInitAnnotationKeyValues();
		String path = _file.getAbsolutePath();
		FileUtil.createDir(new File(path).getParent());
		String txtS = path.substring(0, path.length() - 4) + ".txt";
		removeAnnotationFile();
		_annotations.fileStore(txtS, _config.getString("charEnc"));
		if (!FileUtil.existFile(txtS)) {
			_logger.info("no previous transcript for " + path);
		}

	}

	public void generateAudioFile(boolean female, String lang, String voice) throws Exception {
		String rulesFile = _config.getString("ttsRulesFile");
		String vocabFile = _config.getString("ttsVocabFile");
		Preprocessor p = new Preprocessor(rulesFile, vocabFile);
		String text = p.process(_transcript);
		_logger.debug("Synthesizing audio from :" + text);
		if (_config.isString("useTTS", "emofilt")) {
			String emotions = _config.getString("emotions");
			String mbrolaVoices = _config.getString("mbrolaVoices");
			ISynthesizer synthesizer = new EmofiltSynthesizer(_config);
			KeyValues voices = new KeyValues(mbrolaVoices, ";", ",");
			StringTokenizer emoST = new StringTokenizer(emotions);
			while (emoST.hasMoreElements()) {
				String emotion = (String) emoST.nextElement();
				for (int i = 0; i < voices.getKeyValues().length; i++) {
					KeyValue kv = voices.getKeyValues()[i];
					String voc = kv.getKey();
					String sex = kv.getValue();
					String filename = FileUtil.addNamePart(_path, "_" + emotion
							+ "_" + voc);
					RecFile recFile = new RecFile(filename, _config);
					recFile.storeTranscript(_transcript);
					synthesizer.synthesize(text, filename, emotion, sex, voc);
				}
			}
		} else if (_config.isString("useTTS", "svox")) {
			ISynthesizer synthesizer = new SvoxSynthesizer(_config);
			synthesizer.synthesize(text, _path, female, lang, voice);
		} else if (_config.isString("useTTS", "ivona")) {
			ISynthesizer synthesizer = new IvonaSynthesizer(_config);
			String sex = female ? Constants.SEX_FEMALE : Constants.SEX_MALE;
			synthesizer.synthesize(text, _path, sex);
		} else if (_config.isString("useTTS", "mary")) {
			ISynthesizer synthesizer = new Mary50Synthesizer(_config);
			String sex = female ? Constants.SEX_FEMALE : Constants.SEX_MALE;
			synthesizer.synthesize(text, _path, true, "de", voice);
		}
	}

	public String getSize() {
		if (_file != null) {
			return Long.toString(_size);
		}
		return "na";
	}
}
