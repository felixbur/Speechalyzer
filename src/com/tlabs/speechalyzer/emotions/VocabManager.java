package com.tlabs.speechalyzer.emotions;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class VocabManager {
	Vector<Vocabulary> _vocabs;

	public VocabManager(Element root) {
		loadVocabs(root);
	}

	public void loadVocabs(Element root) {
		try {
			if (_vocabs == null)
				_vocabs = new Vector<Vocabulary>();
			List<Element> namedChildren = root.getChildren(
					Emotion.EMOTIONML_ELEM_VOCABULARY, root.getNamespace());
			for (Element vocabElem : namedChildren) {
				String id = "";
				String type = "";
				type = vocabElem.getAttributeValue(Emotion.EMOTIONML_ATT_TYPE);
				id = vocabElem.getAttributeValue(Emotion.EMOTIONML_ATT_ID);
				Vocabulary vocab = new Vocabulary(type, id);
				List<Element> itemChildren = vocabElem.getChildren(
						Emotion.EMOTIONML_ELEM_ITEM, root.getNamespace());
				for (Element itemElem : itemChildren) {
					String name = itemElem
							.getAttributeValue(Emotion.EMOTIONML_ATT_NAME);
					VocabItem item = new VocabItem(name);
					vocab.addItem(item);
				}
				_vocabs.add(vocab);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadVocabs(URI uri) {
		try {
			SAXBuilder parser = new SAXBuilder();
			Document doc = parser.build(uri.toURL());
			Element root = doc.getRootElement();
			loadVocabs(root);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isInVocab(String vocabId, String itemName) {
		Vocabulary vocab = getVocab(vocabId);
		if (vocab != null) {
			if (vocab.isItemContained(itemName))
				return true;
		}
		return false;
	}

	public Vocabulary getVocab(String id) {
		try {
			for (Vocabulary vocab : _vocabs) {
				URI vocabUri = new URI(vocab.get_id());
				URI testUri = new URI(id);
				if (vocabUri.getPath().compareTo(testUri.getFragment()) == 0)
					return vocab;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}
