package com.tlabs.speechalyzer.synthesizer;

import org.apache.log4j.Logger;

import com.felix.util.Constants;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.PlayWave;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;

public class SvoxSynthesizer implements ISynthesizer {
	private String svoxCmd;
	private KeyValues _config;
	Log4JLogger _logger;

	public SvoxSynthesizer(KeyValues config) {
		_config = config;
		_logger = new Log4JLogger(Logger
				.getLogger("com.tlabs.speechalyzer.synthesizer.EmofiltSynthesizer"));
		svoxCmd = _config.getString("svoxCmd");
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
			execCmd = svoxCmd + voice + " " + tmpTextFile + " " + outfile;
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	public void synthesize(String intext, String outfile, String sex) {
		String tmpTextFile = _config.getAbsPath("tmpTxtFile");
		try {
			FileUtil.writeFileContent(tmpTextFile, intext,
					FileUtil.ENCODING_UTF_8);
			String execCmd;
			if (sex.compareTo(Constants.SEX_MALE) == 0) {
				execCmd = svoxCmd + _config.getString("svoxde-DEmale") + " " + tmpTextFile + " "
						+ outfile;
			} else {
				execCmd = svoxCmd + _config.getString("svoxde-DEfemale") + " " + tmpTextFile + " "
						+ outfile;
			}
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	public void synthesize(String intext, String outfile, boolean female,
			String lang, String voice) {
		String tmpTextFile = _config.getAbsPath("tmpTxtFile");
		try {
			FileUtil.writeFileContent(tmpTextFile, intext,
					FileUtil.ENCODING_UTF_8);
			String execCmd;
			if (female) {
				if (lang.compareTo("de-DE") == 0) {
					execCmd = svoxCmd + _config.getString("svoxde-DEfemale") + " " + tmpTextFile + " "
							+ outfile;
				} else if (lang.compareTo("en-US") == 0) {
					execCmd = svoxCmd + _config.getString("svoxen-USfemale") + " " + tmpTextFile + " "
					+ outfile;
				} else if (lang.compareTo("zh-CN") == 0) {
					execCmd = svoxCmd + _config.getString("svoxzh-CNfemale") + " " + tmpTextFile + " "
					+ outfile;
				} else {
					_logger.error("undefined language: " + lang);
					return;
				}
			} else {
				if (lang.compareTo("de-DE") == 0) {
					execCmd = svoxCmd + _config.getString("svoxde-DEmale") + " " + tmpTextFile + " "
							+ outfile;
				} else if (lang.compareTo("en-US") == 0) {
					execCmd = svoxCmd + _config.getString("svoxen-USmale") + " " + tmpTextFile + " "
					+ outfile;
				} else {
					_logger.error("undefined language: " + lang);
					return;
				}
			}
			Util.execCmd(execCmd, _logger);

		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	public void synthesize(String intext, String outfile) {
		synthesize(intext, outfile, Constants.SEX_MALE);

	}

}