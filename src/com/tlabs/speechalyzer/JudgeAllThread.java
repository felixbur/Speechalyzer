package com.tlabs.speechalyzer;

import java.util.Iterator;

import org.apache.log4j.Logger;

import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.classifier.IClassifier;
import com.tlabs.speechalyzer.featureextract.IExtractor;

/**
 * @version 1.0
 * @author Felix Burkhardt
 */
public class JudgeAllThread extends Thread {

	AudioFileManager _afm;
	IClassifier _classifier;
	IExtractor _extractor;
//	 boolean _extract = true;
	 Logger _logger;
	 boolean _isRunning=false;
	 
	public JudgeAllThread(AudioFileManager afm, IClassifier classifier,
			IExtractor extractor) {
		_logger = Logger.getLogger("com.tlabs.speechalyzer.JudgeAllThread");
		_afm = afm;
		_classifier = classifier;
		_extractor = extractor;
//		_extract = extract;
	}

	/**
	 * called by thread.start();
	 */
	public void run() {
		_isRunning = true;
		for (Iterator<RecFile> iter = _afm.getAudioFiles().iterator(); iter
				.hasNext();) {
			RecFile recFile = (RecFile) iter.next();
			_extractor
					.extractFeatures(recFile._file.getAbsolutePath());
			ClassificationResult cr = _classifier.classify();
//			logger.info("Modus: judge file " + filePath + " emotionally: "
//					+ cr.toString());
			recFile.storePred(cr);
			_afm.updateAudioFile(recFile._path);

		}
		System.out.println("finished judging all files!");
		_isRunning=false;
	}

	public boolean isRunning() {
		return _isRunning;
	}


	public void destroy() {
	}
}