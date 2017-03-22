package com.tlabs.speechalyzer.featureextract;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.Util;
import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.Constants;
import com.tlabs.speechalyzer.RecFile;
import com.tlabs.speechalyzer.Speechalyzer;

public class PraatExtractor implements IExtractor {
	private String _praatScriptName;
	private String _praatCommandName;
	private String _tmpFeatName;
	private String _arffHeader;
	private String _trainFileName;
	private String _testFileName;
	private Logger _logger;
	private KeyValues _config;

	public String getInfo() {
		return "Praat extractor with Polzehl Features";
	}

	public PraatExtractor(KeyValues config) {
		_config = config;
		_logger = Logger
				.getLogger("com.tlabs.speechalyzer.featureextract.PraatExtractor");
		_praatScriptName = _config.getString("praatScriptFile");
		_praatCommandName = _config.getString("praatCommand");
		_tmpFeatName = _config.getAbsPath("tempFeatFile");
		try {
			_arffHeader = FileUtil.getFileText(_config
					.getString("arffHeaderFile"));
		} catch (Exception e) {
			e.printStackTrace();
			_logger.error(e.getMessage());
		}
		_trainFileName = _config.getString("trainFile");
		_testFileName = _config.getString("testFile");
	}

	public void extractFeatures(String testFileName) {
		String command = _praatCommandName + " " + _praatScriptName + " "
				+ testFileName + " " + Constants.CLASS_NO_CLASS_STRING + " "
				+ _tmpFeatName;
		System.out.println(command);
		Process genProc = null;
		String inline = "";
		try {
			genProc = Runtime.getRuntime().exec(command);
			BufferedReader bfr = new BufferedReader(new InputStreamReader(
					genProc.getErrorStream()));
			while ((inline = bfr.readLine()) != null) {
				_logger.debug(inline);
			}
		} catch (Exception e) {
			genProc.destroy();
			e.printStackTrace();
			_logger.error(e.getMessage());
		}
		try {
			String fileLines = FileUtil.getFileText(_tmpFeatName);
			FileUtil.writeFileContent(_testFileName, _arffHeader + fileLines);
			new File(_tmpFeatName).delete();
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public void extractAllFeatures(AudioFileManager afm, boolean append) {
		long timeTaken = 0;
		Vector<RecFile> audioFiles;
		if (append) {
			audioFiles = afm.getAudioFilesWithoutPredictions();
		} else {
			new File(_tmpFeatName).delete();
			audioFiles = afm.getAudioFiles();
		}
		for (Iterator<RecFile> iterator = audioFiles.iterator(); iterator
				.hasNext();) {
			RecFile recFile = (RecFile) iterator.next();
			String command = _praatCommandName + " " + _praatScriptName + " "
					+ recFile.getTrainingFormat() + " " + _tmpFeatName;
			try {
				long startTime = System.currentTimeMillis();
				Util.execCmd(command);
				timeTaken += System.currentTimeMillis() - startTime;
			} catch (Exception e) {
				e.printStackTrace();
				_logger.error(e.getMessage());
			}
			System.out.print(".");
		}

		try {
			String fileLines = FileUtil.getFileText(_tmpFeatName);
			FileUtil.writeFileContent(_trainFileName, _arffHeader + fileLines);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println();
		_logger.info("featureExtraction of " + audioFiles.size()
				+ " files took " + timeTaken / 1000.0 + " seconds");

	}
}