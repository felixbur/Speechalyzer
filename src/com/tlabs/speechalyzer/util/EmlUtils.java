package com.tlabs.speechalyzer.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.log4j.Logger;

import com.felix.util.FileUtil;
import com.felix.util.StringUtil;
import com.felix.util.Util;
import com.tlabs.speechalyzer.Speechalyzer;

public class EmlUtils {
	private static Logger logger = Logger.getLogger(EmlUtils.class);

	public static String recognizeFile(String filePath) {
		String result = "";
		String fileName = new File(filePath).getName();
		String tmpFileName = Speechalyzer._config
				.getString("recognitionTmpDir")
				+ fileName;

		String cmd = "sox " + filePath + " -U -r8000 " + tmpFileName;
		if (fileName.endsWith(".raw")) {
			String sr = Speechalyzer._config.getString("sampleRate");
			// assuming linear 16bit pcm
			cmd = "sox -r "+sr+" -s -b16 " + filePath + " -U -r8000 " + tmpFileName;
		}
		try {
			Util.execCmd(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		FileUtil.waitForFile(tmpFileName, false);
		result = EmlUtils.postAudioToEml(tmpFileName, Speechalyzer._config
				.getString("emlUrl"));
		try {
			FileUtil.delete(tmpFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static String postAudioToEml(String filename, String emlURL) {
		String result = "";
		try {
			logger.debug("requesting: " + emlURL + " with file: " + filename);
			ClientHttpRequest chr = new ClientHttpRequest(emlURL);
			chr.setParameter("audio", new File(filename));
			InputStream is = chr.post();
			final char[] buffer = new char[0x10000];
			StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(is, FileUtil.ENCODING_ISO8859_1);
			int read;
			do {
				read = in.read(buffer, 0, buffer.length);
				if (read > 0) {
					out.append(buffer, 0, read);
				}
			} while (read >= 0);
			result = out.toString();
			logger.debug("result: " + result);
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		return result;
	}
}
