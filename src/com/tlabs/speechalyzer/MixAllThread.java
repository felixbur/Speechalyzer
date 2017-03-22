package com.tlabs.speechalyzer;

import java.util.Iterator;

import org.apache.log4j.Logger;

import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.SoundMixer;
import com.felix.util.SoundMixer_16bit;
import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.classifier.IClassifier;
import com.tlabs.speechalyzer.featureextract.IExtractor;

/**
 * @version 1.0
 * @author Felix Burkhardt
 */
public class MixAllThread extends Thread {

	AudioFileManager _afm;
	 Logger _logger;
	 KeyValues _config;
	 boolean _isRunning=false;
	 
	public MixAllThread(AudioFileManager afm, KeyValues config) {
		_logger = Logger.getLogger("com.tlabs.speechalyzer.MixAllThread");
		_afm = afm;
		_config = config;
	}

	/**
	 * called by thread.start();
	 */
	public void run() {
		_isRunning = true;
		for (Iterator<RecFile> iter = _afm.getAudioFiles().iterator(); iter
				.hasNext();) {
			RecFile recFile = (RecFile) iter.next();
			String source = recFile._path;
			String mix = _config.getString("mixFile");
			double fac = _config.getDouble("mixFactor");
			String mixExt = _config.getString("mixExtension");
			String out = FileUtil.addNamePart(recFile._path, mixExt);
			new SoundMixer().mix(source, mix, out, fac);
			_logger.debug("mixing "+source+" and "+mix+" with factor "+fac+" to "+out);
		}
		System.out.println("finished mixing all files!");
		_isRunning=false;
	}

	public boolean isRunning() {
		return _isRunning;
	}


	public void destroy() {
	}
}