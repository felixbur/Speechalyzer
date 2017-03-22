package com.tlabs.speechalyzer.featureextract;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.felix.util.AudioUtil;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;
import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.Constants;
import com.tlabs.speechalyzer.RecFile;

public class OpenEarExtractor implements IExtractor {
	private String _openEarConfigName;
	private String _openEarCommandName;
	private String _tmpWavName;
	// private String _arffHeader;
	private String _trainFileName;
	private String _testFileName;
	private Log4JLogger _logger;
	private KeyValues _config;

	public String getInfo() {
		return "OpenEAR extractor with features: " + _openEarConfigName;
	}

	public OpenEarExtractor(KeyValues config) {
		_config = config;
		_logger = new Log4JLogger(
				Logger.getLogger("com.tlabs.speechalyzer.featureextract.OpenEatExtractor"));
		_openEarConfigName = _config.getString("openEarConfig");
		_openEarCommandName = _config.getString("openEarCommand");
		_trainFileName = _config.getString("trainFile");
		_testFileName = _config.getString("testFile");
		_tmpWavName = _config.getString("tmpWavFile");
	}

	public void extractFeatures(String fileName) {
		String audioFile = checkFileName(fileName);
		try {
			FileUtil.delete(_testFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String command = _openEarCommandName + " -C " + _openEarConfigName
				+ " -I " + audioFile + " -O " + _testFileName + " -classlabel "
				+ Constants.CLASS_NO_CLASS_STRING;
		try {
			Util.execCmd(command, _logger);
		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	public void extractAllFeatures(AudioFileManager afm, boolean append) {
		long timeTaken = 0;
		Vector<RecFile> audioFiles;
		if (append) {
			audioFiles = afm.getAudioFilesWithoutPredictions();
		} else {
			audioFiles = afm.getAudioFiles();
			new File(_trainFileName).delete();
		}
		for (Iterator<RecFile> iterator = audioFiles.iterator(); iterator
				.hasNext();) {
			RecFile recFile = (RecFile) iterator.next();
			String audioFile = checkFileName(recFile._path);
			String command = _openEarCommandName + " -C " + _openEarConfigName
					+ " -I " + audioFile + " -O " + _trainFileName
					+ " -classlabel " + recFile.getStringLabel();
			Util.printOut(".", false);
			try {
				long startTime = System.currentTimeMillis();
				Util.execCmd(command, _logger);
				timeTaken += System.currentTimeMillis() - startTime;
			} catch (Exception e) {
				e.printStackTrace();
				_logger.error(e.getMessage());
			}
			System.out.print(".");
		}
		System.out.println();
		_logger.info("featureExtraction of " + audioFiles.size()
				+ " files took " + timeTaken / 1000.0 + " seconds");
	}

	private String checkFileName(String audioFile) {
		if (!audioFile.endsWith(".wav")) {
			_logger.info("not wav file (" + audioFile + "), needs conversion");
			if (audioFile.endsWith(".raw")) {
				try {
					new File(_tmpWavName).delete();
					byte[] data = FileUtil.getFileContentAsByteArray(audioFile);
					AudioUtil.writeAudioToWavFile(data,
							AudioUtil.FORMAT_PCM_8KHZ, _tmpWavName);
					audioFile = _tmpWavName;
				} catch (Exception e) {
					Util.errorOut(e, _logger);
				}
			}
		}
		return audioFile;
	}
}