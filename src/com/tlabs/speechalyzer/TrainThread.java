package com.tlabs.speechalyzer;

import org.apache.log4j.Logger;

import com.felix.util.KeyValues;
import com.tlabs.speechalyzer.classifier.IClassifier;
import com.tlabs.speechalyzer.featureextract.IExtractor;

/**
 * read a stream from client and write it to a _file.
 * 
 * @version 1.0
 * @author Felix Burkhardt
 */
class TrainThread extends Thread {
	private IExtractor _featExtractor;
	private IClassifier _classifier;
	private AudioFileManager _afm;
	private Logger _logger;
	private KeyValues _config;
	private boolean _extract=true;
	private boolean _isRunning=false;

	public TrainThread(IExtractor featExtractor, IClassifier classifier,
			AudioFileManager afm, KeyValues config, boolean extract) {
		super();
		_logger = Logger.getLogger("com.tlabs.speechalyzer.TrainThread");
		_featExtractor = featExtractor;
		_classifier = classifier;
		_afm = afm;
		_config = config;
		_extract = extract;
	}
	/**
	 * called by thread.start().
	 */
	public void run() {
		_isRunning = true;
		if (_extract){
		_logger.info("extracting features with "+_featExtractor.getInfo()+"....");
		_featExtractor.extractAllFeatures(_afm, _config.getBool("additiveTraining"));
		} else {
			_logger.info("skipping feature extraction");
		}
		_logger.info("training model with "+_classifier.getInfo()+"....");
		_classifier.trainModel();
		_isRunning=false;
	}
	public boolean isRunning() {
		return _isRunning;
	}
	
}