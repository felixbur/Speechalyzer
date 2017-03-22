package com.tlabs.speechalyzer.emotions;

import java.net.URI;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import com.felix.util.FileUtil;
import com.felix.util.KeyValue;
import com.felix.util.KeyValues;
import com.felix.util.StringUtil;
import com.felix.util.logging.Log4JLogger;
import com.felix.util.logging.LoggerInterface;
import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.RecFile;

public class EmotionMLManager {
private AudioFileManager _afm;
private KeyValues _config;
private VocabManager _vocabManager;
private LoggerInterface _logger;
public EmotionMLManager(AudioFileManager _afm, KeyValues config, LoggerInterface logger) {
	super();
	this._afm = _afm;
	_config = config;
	_logger = logger;
}

public void loadEmotionMLElement(Element root) {
	try {
		Attribute versionAtt = root
				.getAttribute(Emotion.EMOTIONML_ATT_VERSION);
		if (versionAtt != null) {
			String version = versionAtt.getValue();
			if (version.compareTo("1.0") != 0) {
				System.err.println("wrong EmotionML Version: " + version
						+ ", should be 1.0");
				System.exit(-1);
			}
		} else {
			System.err
					.println("Missing version attribute in <emotionml>, should be version=\"1.0\"");
			System.exit(-1);
		}

		String categoryVocabRoot = null, appraisalVocabRoot = null, dimensionVocabRoot = null, actionTendencyVocabRoot = null;
		Attribute categorySetAttribute = root
				.getAttribute(Emotion.EMOTIONML_ATT_CATEGORYSET);
		if (categorySetAttribute != null) {
			categoryVocabRoot = categorySetAttribute.getValue();
		}
		Attribute dimensionSetAttribute = root
				.getAttribute(Emotion.EMOTIONML_ATT_DIMENSIONSET);
		if (dimensionSetAttribute != null) {
			dimensionVocabRoot = dimensionSetAttribute.getValue();
		}
		Attribute appraisalSetAttribute = root
				.getAttribute(Emotion.EMOTIONML_ATT_APPRAISALSET);
		if (appraisalSetAttribute != null) {
			appraisalVocabRoot = appraisalSetAttribute.getValue();
		}
		Attribute actiontendencySetAttribute = root
				.getAttribute(Emotion.EMOTIONML_ATT_ACTIONTENDENCYSET);
		if (actiontendencySetAttribute != null) {
			actionTendencyVocabRoot = actiontendencySetAttribute.getValue();
		}

		_vocabManager = new VocabManager(root);
		String defaultOriginator = "";
		Element infoElem = root.getChild(Emotion.EMOTIONML_ELEM_INFO,
				root.getNamespace());
		if (infoElem != null) {
			String test = infoElem.getTextTrim();
			if (StringUtil.isFilled(test)) {
				String originatorIdentifier = _config
						.getString("originatorIdentifier");
				String originTest = StringUtil.getStringBetween(test,
						originatorIdentifier + ":", Emotion.INFO_SEPARATOR);
				if (originTest != null) {
					defaultOriginator = originTest;
				}
			}
		}

		List<Element> namedChildren = root.getChildren(
				Emotion.EMOTIONML_ELEM_EMOTION, root.getNamespace());
		for (Element emoElem : namedChildren) {
			System.out.println(emoElem.getName());
			String name = emoElem.getName();
			Emotion emotion = null;
			String categoryVocab = categoryVocabRoot, appraisalVocab = appraisalVocabRoot, dimensionVocab = dimensionVocabRoot, actionTendencyVocab = actionTendencyVocabRoot;

			categorySetAttribute = emoElem
					.getAttribute(Emotion.EMOTIONML_ATT_CATEGORYSET);
			if (categorySetAttribute != null) {
				categoryVocab = categorySetAttribute.getValue();
				URI catSetUri = new URI(categoryVocab);
				if (StringUtil.isFilled(catSetUri.getScheme())) {
					_vocabManager.loadVocabs(catSetUri);
				}
			}
			dimensionSetAttribute = emoElem
					.getAttribute(Emotion.EMOTIONML_ATT_DIMENSIONSET);
			if (dimensionSetAttribute != null) {
				dimensionVocab = dimensionSetAttribute.getValue();
				URI dimSetUri = new URI(dimensionVocab);
				if (StringUtil.isFilled(dimSetUri.getScheme())) {
					_vocabManager.loadVocabs(dimSetUri);
				}
			}
			appraisalSetAttribute = emoElem
					.getAttribute(Emotion.EMOTIONML_ATT_APPRAISALSET);
			if (appraisalSetAttribute != null) {
				appraisalVocab = appraisalSetAttribute.getValue();
				URI appSetUri = new URI(appraisalVocab);
				if (StringUtil.isFilled(appSetUri.getScheme())) {
					_vocabManager.loadVocabs(appSetUri);
				}

			}
			actiontendencySetAttribute = emoElem
					.getAttribute(Emotion.EMOTIONML_ATT_ACTIONTENDENCYSET);
			if (actiontendencySetAttribute != null) {
				actionTendencyVocab = actiontendencySetAttribute.getValue();
				URI actSetUri = new URI(actionTendencyVocab);
				if (StringUtil.isFilled(actSetUri.getScheme())) {
					_vocabManager.loadVocabs(actSetUri);
				}
			}

			Element catElem = emoElem.getChild(Emotion.EMOTIONML_ELEM_CAT,
					root.getNamespace());
			if (catElem != null) {
				String emoName = catElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_NAME);
				System.out.println("\t\tother name: " + emoName);
				String value = catElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_VALUE);
				String confidence = catElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_CONFIDENCE);
				if (StringUtil.isEmpty(categoryVocabRoot)) {
					System.err.println("no vocabulary set given for "
							+ emoName);
					System.exit(-1);
				}

				emotion = new Emotion(_config, emoName,
						Emotion.EMOTIONML_ELEM_CAT, value, "", confidence,
						categoryVocab);
			}
			Element dimensionElem = emoElem.getChild(
					Emotion.EMOTIONML_ELEM_DIMENSION, root.getNamespace());
			if (dimensionElem != null) {
				String emoName = dimensionElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_NAME);
				System.out.println("\t\tother name: " + emoName);
				String value = dimensionElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_VALUE);
				String confidence = dimensionElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_CONFIDENCE);
				if (StringUtil.isEmpty(dimensionVocabRoot)) {
					System.err.println("no vocabulary set given for "
							+ emoName);
					System.exit(-1);
				}

				emotion = new Emotion(_config, emoName,
						Emotion.EMOTIONML_ELEM_DIMENSION, value, "",
						confidence, dimensionVocab);
			}
			Element appraisalElem = emoElem.getChild(
					Emotion.EMOTIONML_ELEM_APPRAISAL, root.getNamespace());
			if (appraisalElem != null) {
				String emoName = appraisalElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_NAME);
				System.out.println("\t\tother name: " + emoName);
				String value = appraisalElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_VALUE);
				String confidence = appraisalElem
						.getAttributeValue(Emotion.EMOTIONML_ATT_CONFIDENCE);
				if (StringUtil.isEmpty(appraisalVocabRoot)) {
					System.err.println("no vocabulary set given for "
							+ emoName);
					System.exit(-1);
				}

				emotion = new Emotion(_config, emoName,
						Emotion.EMOTIONML_ELEM_APPRAISAL, value, "",
						confidence, appraisalVocab);
			}
			Element actionTendencyElement = emoElem.getChild(
					Emotion.EMOTIONML_ELEM_ACTIONTENDENCY,
					root.getNamespace());
			if (actionTendencyElement != null) {
				String emoName = actionTendencyElement
						.getAttributeValue(Emotion.EMOTIONML_ATT_NAME);
				System.out.println("\t\tother name: " + emoName);
				String value = actionTendencyElement
						.getAttributeValue(Emotion.EMOTIONML_ATT_VALUE);
				String confidence = actionTendencyElement
						.getAttributeValue(Emotion.EMOTIONML_ATT_CONFIDENCE);
				if (StringUtil.isEmpty(actionTendencyVocabRoot)) {
					System.err.println("no vocabulary set given for "
							+ emoName);
					System.exit(-1);
				}

				emotion = new Emotion(_config, emoName,
						Emotion.EMOTIONML_ELEM_ACTIONTENDENCY, value, "",
						confidence, actionTendencyVocab);
			}
			if (emotion == null) {
				System.err.println("no emotion set");
				System.exit(-1);
			}
			if (!_vocabManager.isInVocab(emotion.get_vocabId(),
					emotion.get_name())) {
				System.err.println("Name not in vocabulary for "
						+ emotion.toString());
				System.exit(-1);
			}
			Element refElem = emoElem.getChild(
					Emotion.EMOTIONML_ELEM_REFERENCE, root.getNamespace());
			if (refElem != null) {
				URI uri = new URI(
						refElem.getAttributeValue(Emotion.EMOTIONML_ATT_URI));
				String path = uri.getPath();
				// String path = new File(uri).getPath();
				path = path.substring(1);
				// path="recordings/2011.01.11-10.27.10.wav";

				if (path.endsWith("." + _afm.get_audioExtension())) {
					if (!FileUtil.existFile(path))
						_logger.warn("AFM: file: " + path
								+ ", doesnt exist.");

					// String transcription = StringUtil.getRestOfLine(st);
					if (_afm.isAudioFileContained(path)) {
						RecFile recFile = _afm.findAudioFile(path);
						infoElem = emoElem.getChild(
								Emotion.EMOTIONML_ELEM_INFO,
								Emotion.EMOTIONML_NAMESPACE);
						recFile.addInfoAndEmotion(infoElem, emotion);
					} else {
						RecFile recFile = new RecFile(path, _config);
						if (StringUtil.isFilled(defaultOriginator))
							recFile.setDefaultOriginator(defaultOriginator);
						recFile.removeAnnotationFile();
						recFile.initAnnotations();
						infoElem = emoElem.getChild(
								Emotion.EMOTIONML_ELEM_INFO,
								Emotion.EMOTIONML_NAMESPACE);
						recFile.addInfoAndEmotion(infoElem, emotion);
						_afm.addAudioFile(recFile);
					}
				}
			}
		}
		_afm.set_vocabManager(_vocabManager);
	} catch (Exception e) {
		e.printStackTrace();
	}
}

}
