package com.tlabs.speechalyzer;

import java.io.DataOutputStream;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Vector;

import com.felix.util.FileUtil;

/**
 * send the list of recordings in directory to the client.
 * 
 * @version 1.0
 * @author Felix Burkhardt
 */
public class SendFileListThread extends Thread {

	/**
	 * output stream.
	 */
	private DataOutputStream _out;
	/**
	 * array of recordings.
	 */
	RecFile[] files;
	/**
	 * _name of recording.
	 */
	String fileName;
	/**
	 * _size of recording.
	 */
	String fileSize;
	/**
	 * _size of recording as long.
	 */
	long fileLength;
	AudioFileManager _afm;
	private boolean _reloadAFM = false;

	/**
	 * @param _out
	 *            outpur stream
	 * @param directory
	 *            _name of dir of recordings
	 * 
	 */
	public SendFileListThread(DataOutputStream out, AudioFileManager afm,
			boolean reload) {
		_out = out;
		_afm = afm;
		_reloadAFM = reload;
	}

	/**
	 * called by thread.start().
	 */
	public void run() {
		if (_reloadAFM) {
			_afm.reload();
		}
		try {
			_out.writeBytes(String.valueOf(Constants.version)+"\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// init the directory
		Vector<RecFile> tmpVec = _afm.getAudioFiles();
		int i = 0;
		for (Iterator<RecFile> iter = tmpVec.iterator(); iter.hasNext();) {
			RecFile recFile = (RecFile) iter.next();
			// feedback for progress
			if (i++ % 10 == 0)
				System.out.print(".");
			// send it to the client
			try {
				String classResult = "null";
				if (recFile._classificationResult != null)
					classResult = recFile._classificationResult.toString();
				String sendString = "";
				sendString += recFile._path + ';';
				sendString += recFile.getSize() + ';';
				sendString += recFile.labToString() + ';';
				sendString += String.valueOf(classResult) + ';';
				String transcription = recFile._transcript;
				String recognized = recFile._recognized;
				sendString += URLEncoder.encode(transcription + " ", _afm
						.getConfig().getString("charEnc")) + ';';
				sendString += URLEncoder.encode(recognized + " ", _afm
						.getConfig().getString("charEnc"))
						+ ";\n";
				_afm.getLogger().debug(sendString.trim());
				_out.writeBytes(sendString);
			} catch (IOException e) {
				System.out.println("unable to send data " + e);
			}
		}
		System.out.println();
		// tell client _file list is complete.
		try {
			_out.writeBytes("finished" + '\n');
			_out.writeBytes(_afm.getStats() + '\n');
		} catch (IOException e) {
			System.out.println("unable to send data " + e);
		}
	}

}