package com.tlabs.speechalyzer.synthesizer;

import java.util.Locale;

import javax.sound.sampled.AudioInputStream;

import marytts.MaryInterface;
import marytts.client.RemoteMaryInterface;
import marytts.util.data.audio.MaryAudioUtils;

import org.apache.log4j.Logger;

import com.felix.util.Constants;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;

public class Mary50Synthesizer implements ISynthesizer {
	private KeyValues _config;
	Log4JLogger _logger;
	private final String MARYSERVER_HOST = "localhost";
	private final String MARYSERVER_PORT = "59125";

	public Mary50Synthesizer(KeyValues config) {
		_config = config;
		_logger = new Log4JLogger(
				Logger.getLogger("com.tlabs.speechalyzer.synthesizer.EmofiltSynthesizer"));
	}

	public void synthesize(String intext, String outfile, String emotion,
			String sex, String voice) {
		synthesize(intext, outfile, null, voice);
	}

	public void synthesize(String intext, String outfile, String sex,
			String voice) {
	}

	public void synthesize(String intext, String outfile, String sex) {
	}

	public void synthesize(String intext, String outfile, boolean female,
			String lang, String voice) {
		String tmpTextFile = _config.getAbsPath("tmpTxtFile");
		try {
			FileUtil.writeFileContent(tmpTextFile, intext,
					FileUtil.ENCODING_UTF_8);
			String sentence = "";
			String maryHost = MARYSERVER_HOST;
			int maryPort = Integer.parseInt(MARYSERVER_PORT);
			MaryInterface marytts = new RemoteMaryInterface(maryHost, maryPort);
			marytts.setLocale(new Locale("de"));
			marytts.setVoice(voice);
			// marytts.setStyle("happy");

			AudioInputStream audio = marytts.generateAudio(intext);
			MaryAudioUtils.writeWavFile(
					MaryAudioUtils.getSamplesAsDoubleArray(audio), outfile,
					audio.getFormat());

			// ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// String textFormat = "TEXT_DE";
			// mary.process(input, inputType, defaultVoiceName, player)
			// mary.process(sentence, textFormat, "MBROLA", null,
			// "de", baos);
			// File phoF = new File(tmpPhoFile);
			// FileUtil.writeFileContent(phoF, baos.toString());
			// baos = null;
			marytts = null;
		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	public void synthesize(String intext, String outfile) {
		synthesize(intext, outfile, Constants.SEX_MALE);

	}

}