package com.tlabs.speechalyzer.synthesizer;

import java.io.BufferedReader;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.felix.util.Constants;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;

public class EmofiltSynthesizer implements ISynthesizer {
	KeyValues _config;
	Log4JLogger _logger;

	public EmofiltSynthesizer(KeyValues config) {
		_config = config;
		_logger = new Log4JLogger(Logger
				.getLogger("com.tlabs.speechalyzer.synthesizer.EmofiltSynthesizer"));
	}
	public void synthesize(String intext, String outfile){
		synthesize(intext, outfile, null, null, null);
	}
	public void synthesize(String intext, String outfile, String sex) {
		synthesize(intext, outfile, null, sex, null);
	}
	public void synthesize(String intext, String outfile, String sex, String voice) {
		synthesize(intext, outfile, null, sex, voice);
	}
	public void synthesize(String intext, String outfile, boolean female, String lang, String voice) {
		String sex=female?Constants.SEX_FEMALE:Constants.SEX_MALE;
		synthesize(intext, outfile, null, sex, lang);
	}
	
	public void synthesize(String intext, String outfile, String emotion,
			String sex, String voice) {
		if (sex == null) {
			sex = Constants.SEX_FEMALE;
		}
		if (voice == null) {
			voice = "de1";
		}
		try {
			// txt2pho
			String txt2pho = _config.getString("txt2pho");
			File tmpTxtFile = _config.getFileHandler("tmpTxtFile");
			String tmpPhoFile = _config.getString("tmpPhoFile");
			FileUtil.writeFileContent(tmpTxtFile, intext);

			String execCmd = txt2pho + sex + " " + tmpTxtFile.getPath() + " "
					+ tmpPhoFile;
			String inline = "";
			_logger.info("executing: " + execCmd);
			Util.execCmd(execCmd, _logger);
			String tmpPhoEmotionFile = _config.getString("tmpPhoEmotionFile");
			if (emotion != null) {
				// emotionalize
				String emofilt = _config.getString("emofilt");
				String emofiltDB = _config.getString("emofiltDB");
				execCmd = emofilt + " " + emofiltDB + " -e " + emotion
						+ " -voc " + voice + " -if " + tmpPhoFile + " -of "
						+ tmpPhoEmotionFile;
				_logger.info("executing: " + execCmd);
				Util.execCmd(execCmd, _logger);
			}
			// mbrolize
			String mbrola = _config.getString("mbrola");
			String mbrolaDB = _config.getString("mbrolaDB");
			String wavGenOutPrefix = _config.getString("wavGenOutPrefix");
			String formatOption = _config.getString("formatOption");
			String mbrolaVoice = voice + "/" + voice;
			if (emotion != null) {
				execCmd = mbrola + " " + mbrolaDB + mbrolaVoice + " "
						+ tmpPhoEmotionFile + " " + wavGenOutPrefix + outfile
						+ " " + formatOption;
			} else {
				execCmd = mbrola + " " + mbrolaDB + mbrolaVoice + " "
						+ tmpPhoFile + " " + wavGenOutPrefix + outfile + " "
						+ formatOption;
			}
			_logger.info("executing: " + execCmd);
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

}
