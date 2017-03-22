package com.tlabs.speechalyzer.synthesizer;

import org.apache.log4j.Logger;

import com.felix.util.Constants;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.PlayWave;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;

public class IvonaSynthesizer implements ISynthesizer {
	private String cmd;
	private KeyValues _config;
	Log4JLogger _logger;

	public IvonaSynthesizer(KeyValues config) {
		_config = config;
		_logger = new Log4JLogger(
				Logger.getLogger("com.tlabs.speechalyzer.synthesizer.IvonaSynthesizer"));
		cmd = _config.getString("ivonaCmd");
	}

	public void synthesize(String intext, String outfile, String emotion,
			String sex, String voice) {
		synthesize(intext, outfile, null, voice);
	}

	public void synthesize(String intext, String outfile, String sex,
			String voice) {
		String tmpTextFile = _config.getAbsPath("tmpTxtFile");
		try {
			FileUtil.writeFileContent(tmpTextFile, intext,
					FileUtil.ENCODING_UTF_8);
			String execCmd;
			execCmd = cmd + " " + tmpTextFile + " " + outfile;
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public void synthesize(String intext, String outfile, boolean female,
			String lang, String voice) {
		String tmpTextFile = _config.getAbsPath("tmpTxtFile");
		try {
			FileUtil.writeFileContent(tmpTextFile, intext,
					FileUtil.ENCODING_UTF_8);
			String execCmd;
			execCmd = cmd + " " + tmpTextFile + " " + outfile;
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public void synthesize(String intext, String outfile, String sex) {
		String tmpTextFile = _config.getAbsPath("tmpTxtFile");
		try {
			FileUtil.writeFileContent(tmpTextFile, intext,
					FileUtil.ENCODING_UTF_8);
			String execCmd;
			execCmd = cmd + " " + tmpTextFile + " " + outfile;
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	public void synthesize(String intext, String outfile) {
		synthesize(intext, outfile, Constants.SEX_MALE);

	}

}
