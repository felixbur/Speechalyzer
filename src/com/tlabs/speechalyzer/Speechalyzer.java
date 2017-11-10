package com.tlabs.speechalyzer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.felix.util.AudioUtil;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.NLPUtil;
import com.felix.util.StringUtil;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;
import com.felix.util.logging.LoggerInterface;
import com.tlabs.speechalyzer.classifier.Categories;
import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.classifier.EvaluatorThread;
import com.tlabs.speechalyzer.classifier.IClassifier;
import com.tlabs.speechalyzer.classifier.WEKAClassifier;
import com.tlabs.speechalyzer.featureextract.IExtractor;
import com.tlabs.speechalyzer.featureextract.OpenEarExtractor;
import com.tlabs.speechalyzer.featureextract.PraatExtractor;
import com.tlabs.speechalyzer.util.EmlUtils;

import dk.dren.hunspell.Hunspell;
import dk.dren.hunspell.Hunspell.Dictionary;
import psk.cmdline.ApplicationSettings;
import psk.cmdline.BooleanToken;
import psk.cmdline.StringToken;
import psk.cmdline.TokenOptions;

/**
 * The main class for the recording server. The server is implemented as a
 * thread that waits for tcp-connections from the client. The
 * communication-protocol between client & server is defined by the modus: the
 * first integer that is send by the client.
 * 
 * @version 1.0
 * @author Felix Burkhardt
 */
public class Speechalyzer extends Thread {
	/**
	 * Mode selects the communication between server and client. <br>
	 * 0: recording. <br>
	 * 1: playing. <br>
	 * 2: judge _file emotionally. <br>
	 * 3: delete a _file. <br>
	 * 4: send the list of recordings to client. 5: reset sympalog module 6:
	 * reset list of ratings 7: send general message 8: receive emotion
	 * judgement 9: _transcript a _file
	 * 
	 * <br>
	 */
	private int mode;
	public final static int MODE_RECORD = 0;
	public final static int MODE_PLAY = 1;
	public final static int MODE_JUDGE = 2;
	public final static int MODE_DELETE = 3;
	public final static int MODE_SEND_LIST = 4;
	public final static int MODE_STOP = 5;
	public final static int MODE_EVALUATE = 6;
	public final static int MODE_MESSAGE = 7;
	public final static int MODE_SET_LABEL = 8;
	public final static int MODE_SET_TRANS = 9;
	public final static int MODE_RECOGNIZE = 10;

	// private RecordThread recordThread;
	// private PlayThread playThread;
	// private JudgeAllThread judgeAllThread;
	// private SendFileListThread sendFileListThread;
	// private TrainThread trainThread;
	private static boolean _garrulous = true;
	private DataInputStream _in;
	private DataOutputStream _out;
	private String _filePath;
	private String _fileList = "";
	private String _fileEmotionML = "";
	private ApplicationSettings _aps = null;
	private File _file;
	private AudioFileManager _afm;
	private File _recordingDir;
	public int _port = 0;
	public String _host = "127.0.0.1";
	private String _charEnc;
	int neutralThreshold = 100;
	public static KeyValues _config;
	public LoggerInterface _logger = null;
	private IClassifier _classifier;
	private IExtractor _featExtractor;
	private Hunspell hunspell;
	private boolean _emotionmlMode = false;
	private ServerSocket _serversocket;
	private Socket _socket;
	private boolean _withClassifier = false;
	private static Speechalyzer globalRef = null; // singleton static reference
													// to the instance

	/**
	 * 
	 * @param sympaConfigName
	 *            Name der Konfiguration
	 * @param recDir
	 *            Pfad zu den Dialogen.
	 */
	public Speechalyzer(String[] args) {
		_aps = new ApplicationSettings();
		BooleanToken showUsage = new BooleanToken("h", "print usage", "", TokenOptions.optSwitch, false);
		_aps.addToken(showUsage);
		StringToken configFile = new StringToken("cf", "configuration file", "", TokenOptions.optDefault,
				Constants.CONFIG_FILE_NAME);
		_aps.addToken(configFile);
		StringToken recordingDir = new StringToken("rd", "directory with recordings", "", TokenOptions.optDefault,
				"recordings");
		_aps.addToken(recordingDir);
		StringToken fileList = new StringToken("fl",
				"<textlist with audiofiles>.\n\t\tFormat: filePath label_1 label_2 ... label_i", "",
				TokenOptions.optDefault, "");
		_aps.addToken(fileList);
		StringToken fileEmotionML = new StringToken("fe", "<EmotionML document>.\n\t\tFormat: XML", "",
				TokenOptions.optDefault, "");
		_aps.addToken(fileEmotionML);
		StringToken audioFormat = new StringToken("aft", "Set audio file type, e.g. wav or pcm", "",
				TokenOptions.optDefault, "");
		_aps.addToken(audioFormat);
		StringToken sampleRate = new StringToken("srt", "Set audio sample rate, e.g. 8000 or 16000", "",
				TokenOptions.optDefault, "");
		_aps.addToken(sampleRate);
		StringToken port = new StringToken("port", "Set port number, e.g. 6666", "", TokenOptions.optDefault, "");
		_aps.addToken(port);
		StringToken host = new StringToken("host", "Set host ip, e.g. 127.0.0.1", "", TokenOptions.optDefault, "");
		_aps.addToken(host);
		BooleanToken pe = new BooleanToken("pe",
				"Print evaluation format to stdout.\n\t\tFormat: filepath <string label> <prediction category>.\n", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(pe);
		BooleanToken pa = new BooleanToken("pa",
				"Print \"angry\" files to stdout (according to angerBorder in config file).\n\t\tFormat: filepath <string label>.\n",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(pa);
		BooleanToken pm = new BooleanToken("pm", "Print EmotionML. All recordings with labels get printed out.\n", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(pm);
		BooleanToken pp = new BooleanToken("pp",
				"Print prediction to stdout.\n\t\tFormat: filepath <prediction category>.\n", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(pp);
		BooleanToken pf = new BooleanToken("pf", "Prints file info to stdout.\n\t\tFormat: filepath size", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(pf);
		BooleanToken pl = new BooleanToken("pl",
				"Prints labels to stdout.\n\t\tFormat: filepath label_1 label_2...label_i", "", TokenOptions.optSwitch,
				false);
		_aps.addToken(pl);
		BooleanToken pi = new BooleanToken("pi",
				"Prints labels as integers to stdout.\n\t\tFormat: filepath label_1 label_2...label_i", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(pi);
		BooleanToken pt = new BooleanToken("pt", "Prints transcriptions to stdout.\n\t\tFormat: filepath _transcript",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(pt);
		BooleanToken pnt = new BooleanToken("pnt",
				"Prints files without transcriptions to stdout.\n\t\tFormat: filepath", "", TokenOptions.optSwitch,
				false);
		_aps.addToken(pnt);
		BooleanToken pc = new BooleanToken("pc",
				"Prints categories to stdout.\n\t\tFormat: filepath filesize C_all C_l1 C_l2...C_li\n\t\t(C=category).",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(pc);
		BooleanToken prtl = new BooleanToken("prtl",
				"Prints transcriptions and labels to stdout (if BOTH exist).\n\t\tFormat: <filepath> <transcript> <label>\n\t\texample: recs/rec.wav \"bla bla\" -3",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(prtl);
		BooleanToken al = new BooleanToken("al", "Adds labels from textlist.\n\t\tFormat: filepath label_1...label_n",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(al);
		BooleanToken at = new BooleanToken("at",
				"Adds transcriptions from textlist.\n\t\tFormat: filepath transcript_1...transcript_n", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(at);
		BooleanToken ar = new BooleanToken("ar",
				"Adds recognition results from textlist.\n\t\tFormat: filepath recognized word_1...recognized word_n",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(ar);
		BooleanToken gw = new BooleanToken("gw",
				"Generate (syntheseize) wav-files in textlist according to transcriptions", "", TokenOptions.optSwitch,
				false);
		_aps.addToken(gw);
		BooleanToken rl = new BooleanToken("rl", "Replaces/adds given labels in textlist to all files", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(rl);
		BooleanToken wer = new BooleanToken("wer",
				"compute word error rate for loaded audio files (must be transcribed and recognized)", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(wer);
		BooleanToken scliteOption = new BooleanToken("sclite", "sclite wer computation option", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(scliteOption);
		BooleanToken mixAll = new BooleanToken("mixAll", "Mix sound to all files", "", TokenOptions.optSwitch, false);
		_aps.addToken(mixAll);
		BooleanToken removeAnnotationFiles = new BooleanToken("removeAnnotationFiles",
				"Removes all annotation files \n\t\t(containing transcription and labels)", "", TokenOptions.optSwitch,
				false);
		_aps.addToken(removeAnnotationFiles);
		BooleanToken classify = new BooleanToken("classify", "Classifiy all files.", "", TokenOptions.optSwitch, false);
		_aps.addToken(classify);
		BooleanToken train = new BooleanToken("train", "Train a model from all files.", "", TokenOptions.optSwitch,
				false);
		_aps.addToken(train);
		BooleanToken noExtract = new BooleanToken("noExtract",
				"If set, features will not be extracted before model training.", "", TokenOptions.optSwitch, false);
		_aps.addToken(noExtract);
		BooleanToken evaluate = new BooleanToken("eval",
				"Evaluate given list internally (samples MUST have associated annotation files with labels and predictions)",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(evaluate);
		BooleanToken removeLabels = new BooleanToken("removeLabels", "Removes all labels for files given in textlist",
				"", TokenOptions.optSwitch, false);
		_aps.addToken(removeLabels);
		BooleanToken removePredictions = new BooleanToken("removePreds",
				"Removes all predictions for files given in textlist", "", TokenOptions.optSwitch, false);
		_aps.addToken(removePredictions);
		BooleanToken stats = new BooleanToken("stats", "Print statistics on the data to stdout.\n", "",
				TokenOptions.optSwitch, false);
		_aps.addToken(stats);
		try {
			_aps.parseArgs(args);
			if (showUsage.getValue()) {
				System.out.println("Speechalyzer version " + Constants.version);
				_aps.printUsage();
				System.exit(0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		_fileList = fileList.getValue();
		_fileEmotionML = fileEmotionML.getValue();
		_config = new KeyValues(configFile.getValue(), "=");

		_garrulous = _config.getBool("garrulous");
		globalRef = this;

		_withClassifier = _config.getBool("withClassifier");
		_emotionmlMode = _config.getBool("emotionmlMode");
		Logger logger = Logger.getLogger("com.tlabs.speechalyzer.Speechalyzer");
		_logger = new Log4JLogger(logger);
		if (FileUtil.existFile(_config.getPathValue("logConfig"))) {
			DOMConfigurator.configure(_config.getPathValue("logConfig"));
		} else {
			System.err.println("log config file not found: " + _config.getPathValue("logConfig"));
		}
		String rd = recordingDir.getValue();
		if (rd.length() > 1) {
			_recordingDir = new File(rd);
		} else {
			_recordingDir = new File(_config.getString("recordingDir"));
		}
		if (!_recordingDir.exists())
			_recordingDir.mkdir();
		if (!new File("tmp").exists())
			new File("tmp").mkdir();
		_logger.info("Speechalyzer " + Constants.version);
		if (_withClassifier && _config.getBool("useWEKA")) {
			_classifier = new WEKAClassifier(_config);
		} else {
			_logger.error("no classifier");
		}
		if (_config.getBool("usePraat")) {
			_featExtractor = new PraatExtractor(_config);
		} else if (_config.getBool("useOpenEar")) {
			_featExtractor = new OpenEarExtractor(_config);
		} else {
			_logger.error("no feature extractor");
		}

		_afm = new AudioFileManager(_fileList, _config, _recordingDir);
		if (audioFormat.getValue().length() > 0) {
			_afm.setAudioExtension(audioFormat.getValue());
		}
		if (sampleRate.getValue().length() > 0) {
			_afm.setSampleRate(Integer.parseInt(sampleRate.getValue()));
		}
		if (port.getValue().length() > 0) {
			_port = Integer.parseInt(port.getValue());
		} else {
			_port = Integer.parseInt(_config.getString("port"));
		}
		if (host.getValue().length() > 0) {
			_host = host.getValue();
		} else {
			_host = _config.getString("host");
		}
		if (audioFormat.getValue().length() > 0 || sampleRate.getValue().length() > 0) {
			_afm.reload();
		}

		if (StringUtil.isFilled(_fileEmotionML)) {
			_afm.parseEmotionMLFromFile(_fileEmotionML);
			_emotionmlMode = true;
		}

		if (pe.getValue()) {
			_logger.info("printing out evaluation format");
			Vector<RecFile> recFiles = _afm.getAudioFiles();
			for (Iterator<RecFile> iterator = recFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = (RecFile) iterator.next();
				System.out.println(recFile._path + " " + recFile.getStringLabel() + " "
						+ recFile.getClassificationResult().getWinner().getCat());
			}
		} else if (pm.getValue()) {
			_logger.info("printing out EmotionML");
			_afm.printEmotionML(System.out);
		} else if (pp.getValue()) {
			_logger.info("printing out filename and predicted category");
			Vector<RecFile> recFiles = _afm.getAudioFiles();
			for (Iterator<RecFile> iterator = recFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = (RecFile) iterator.next();
				System.out.println(recFile._path + " " + recFile.getClassificationResult().getWinner().getCat());
			}
		} else if (evaluate.getValue()) {
			_logger.info("evaluating files and printing to system.out...");
			EvaluatorThread et = new EvaluatorThread(_afm, new Categories(_config.getString("categories")));
			et.start();
			while (et.isRunning())
				Util.sleep(1000);
			System.out.println(et.getSummary());

		} else if (pc.getValue()) {
			_logger.info("printing out filename and categories from labels");
			for (RecFile recFile : _afm.getAudioFiles()) {
				String output = recFile._path + " " + recFile.getStringLabels();
				if (output != null) {
					System.out.println(output);
				}
			}
		} else if (prtl.getValue()) {
			_logger.info("printing out thranscripts and labels");
			for (RecFile recFile : _afm.getAudioFiles()) {
				String output = recFile.getTranscritpionAndLabel();
				if (output != null) {
					System.out.println(output);
				}
			}
		} else if (pf.getValue()) {
			_logger.info("printing out File Info");
			for (RecFile recFile : _afm.getAudioFiles()) {
				System.out.println(recFile._path + " " + recFile._size);
			}
		} else if (pl.getValue()) {
			_logger.info("printing out labels");
			for (RecFile recFile : _afm.getAudioFiles()) {
				if (recFile._lab != null) {
					System.out.println(recFile._path + " " + recFile.labToString());
				}
			}
		} else if (pa.getValue()) {
			_logger.info("printing out labels for angry samples");
			for (RecFile recFile : _afm.getAudioFiles()) {
				if (recFile._lab != null && recFile.isAngry()) {
					System.out.println(recFile._path + " " + recFile.labToString());
				}
			}
		} else if (pi.getValue()) {
			_logger.info("printing out labels as integers");
			for (RecFile recFile : _afm.getAudioFiles()) {
				if (recFile._lab != null) {
					System.out.println(recFile._path + " " + recFile.labToIntString());
				}
			}
		} else if (pt.getValue()) {
			_logger.info("printing out transcripts");
			for (RecFile recFile : _afm.getAudioFiles()) {
				String trans = recFile.getTranscript();
				if (trans != null && trans.length() > 0) {
					System.out.println(recFile._path + " " + trans);
				}
			}
		} else if (pnt.getValue()) {
			_logger.info("printing out files without transcripts ...");
			for (RecFile recFile : _afm.getAudioFiles()) {
				String trans = recFile.getTranscript();
				if (trans != null && trans.length() > 0) {
				} else {
					System.out.println(recFile._path);
				}
			}
		} else if (at.getValue()) {
			_logger.info("adding/overwrite transcripts from file...");
			_afm.addTranscripts(_fileList);
		} else if (ar.getValue()) {
			_logger.info("adding/overwrite recognition results from file...");
			_afm.addRecognition(_fileList);
		} else if (gw.getValue()) {
			_logger.info("synthesizing from file...");
			_afm.addTranscriptsAndSynthesize(_fileList);
			System.exit(0);
		} else if (al.getValue()) {
			_logger.info("adding labels from file...");
			_afm.addLabels(_fileList);
		} else if (rl.getValue()) {
			_logger.info("replacing labels from file...");
			_afm.replaceLabels(_fileList);
		} else if (removeLabels.getValue()) {
			_logger.info("removing all labels ...");
			_afm.removeLabels();
		} else if (removePredictions.getValue()) {
			_logger.info("removing all predictions ...");
			_afm.removePredictions();
		} else if (removeAnnotationFiles.getValue()) {
			_logger.info("removing all annotation files ...");
			for (RecFile recFile : _afm.getAudioFiles()) {
				recFile.removeAnnotationFile();
			}
		} else if (classify.getValue()) {
			_logger.info("judging all files");
			JudgeAllThread judgeAllThread = new JudgeAllThread(_afm, _classifier, _featExtractor);
			judgeAllThread.start();
			while (judgeAllThread.isRunning()) {
				Util.sleep(1000);
				System.out.print(".");
			}
			judgeAllThread = null;
		} else if (train.getValue()) {
			_logger.info("Training a model from all files");
			boolean extract = !noExtract.getValue();
			TrainThread trainThread = new TrainThread(_featExtractor, _classifier, _afm, _config, extract);
			trainThread.start();
		} else if (stats.getValue()) {
			System.out.println(_afm.getStats());
		} else if (mixAll.getValue()) {
			_logger.info("mixing sound to all files");
			MixAllThread mixAllThread = new MixAllThread(_afm, _config);
			mixAllThread.start();
			while (mixAllThread.isRunning()) {
				Util.sleep(1000);
				System.out.print(".");
			}
			mixAllThread = null;
		} else if (wer.getValue()) {
			_logger.info("computing word error rate");
			computeWER();
		} else if (scliteOption.getValue()) {
			_logger.info("producing sclite output");
			_afm.printHypothesisFile();
			_afm.printReferenceFile();
			String cmd = _config.getString("scliteTool") + " -r " + _config.getString("referenceFile") + " -h "
					+ _config.getString("hypothesisFile") + " -i rm -f 0 -o pra";
			try {
				Util.execCmd(cmd, _logger);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			this.start();
		}
	}

	public LoggerInterface getLogger() {
		return _logger;
	}

	/**
	 * Starts the server.
	 */
	public void run() {
		_serversocket = null;
		BufferedReader stringReader;
		FileOutputStream _fileout;
		DataInputStream filein;

		/**
		 * Start a socket server listening on port 6666.
		 */
		try {
			_serversocket = new ServerSocket(_port, 0, InetAddress.getByName(_host));
			System.out.println("Speechalyzer " + Constants.version + " started: " + _serversocket);
			if (!_config.getBool("withSpellChecker")) {
				System.out.println("spellchecker disabled");
			}
			_charEnc = _config.getString("charEnc");
			System.out.println("audio type: " + _config.getString("audioFormat") + ", character encoding: " + _charEnc);
		} catch (IOException e) {
			e.printStackTrace();
			_logger.error("Problem to start server on Port " + _port + ": " + e);
			System.exit(-1);
		}
		if (_config.getBool("withSpellChecker")) {
			initHunspell();
			checkSpelling("k�hlschrank");
		}

		/**
		 * Main loop. Server never finishes.
		 */
		while (true) {
			/**
			 * Create a socket on top of server socket that listens for client.
			 * On Connection introduce input and output stream.
			 */
			try {
				_socket = _serversocket.accept();
				_logger.info("Connection with: " + _socket.getInetAddress());
				_in = new DataInputStream(_socket.getInputStream());
				_out = new DataOutputStream(_socket.getOutputStream());
				/**
				 * First thing: read in the mode.
				 */
				mode = _in.readInt();
				/**
				 * Recording mode. The server expects a fileName and a stream of
				 * bytes. The audio-format is defined by the client.
				 * 
				 */
				if (mode == MODE_RECORD) {
					_logger.info("Modus: recording");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					String fileName = stringReader.readLine();
					_logger.info("recording to _file _name: " + fileName);
					_file = new File(_recordingDir, fileName);
					_fileout = new FileOutputStream(_file);
					RecordThread recordThread = new RecordThread(_in, _fileout);
					recordThread.start();
				}
				/**
				 * recording has stopped.
				 */
				else if (mode == MODE_STOP) {
					_logger.info("Modus: stopping");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					String fileName = stringReader.readLine();
					if (_config.getString("audioFormat").compareTo("wav") == 0) {
						_file = new File(_recordingDir, fileName);
						_afm.addAudioFile(_file);
						byte[] data = FileUtil.getFileContentAsByteArray(_file.getAbsolutePath());
						FileUtil.delete(_file.getAbsolutePath());
						AudioUtil.writeAudioToWavFile(data, AudioUtil.FORMAT_PCM_16KHZ, _file.getAbsolutePath());
						_logger.info("converted " + data.length + " bytes and saved to file: " + fileName);
					}
				}
				/**
				 * play mode. server expects a filename and starts a new play
				 * thread.
				 */
				else if (mode == MODE_PLAY) {
					_logger.info("Modus: playing");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					_filePath = stringReader.readLine();
					int offset = Integer.parseInt(stringReader.readLine());
					_file = _afm.findAudioFile(_filePath)._file;
					_logger.info("playing from _file _name: " + _file.getName());
					filein = new DataInputStream(new FileInputStream(_file));
					PlayThread playThread = new PlayThread(_out, filein, offset);
					playThread.start();
				}
				/**
				 * judge _file emotionally
				 */
				else if (mode == MODE_JUDGE) {
					stringReader = new BufferedReader(new InputStreamReader(_in));
					_filePath = stringReader.readLine();
					_logger.info("Modus: judge file " + _filePath + " emotionally");
					_featExtractor.extractFeatures(new File(_filePath).getAbsolutePath());
					ClassificationResult cr = _classifier.classify();
					_out.writeBytes(String.valueOf(cr.toString() + '\n'));
					_out.writeBytes(String.valueOf(_featExtractor.getInfo() + ", " + _classifier.getInfo() + '\n'));
					_logger.info("Modus: judge file " + _filePath + " emotionally: " + cr.toString());
					RecFile recFile = _afm.findAudioFile(_filePath);
					recFile.storePred(cr);
					_afm.updateAudioFile(_filePath);

				}
				/**
				 * remove mode. read the fileName and remove it from directory.
				 */
				else if (mode == MODE_DELETE) {
					_logger.info("Modus: delete _file not supported any more");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					_filePath = stringReader.readLine();
				}
				/**
				 * update _file list mode. start a sendFileList thread.
				 */
				else if (mode == MODE_SEND_LIST) {
					// update list of recordings
					stringReader = new BufferedReader(new InputStreamReader(_in));
					boolean updateAFM = Boolean.parseBoolean(stringReader.readLine());
					_logger.info("Modus: send filelist");
					SendFileListThread sendFileListThread = new SendFileListThread(_out, _afm, updateAFM);
					sendFileListThread.start();

				}
				/**
				 * evaluate model
				 */

				else if (mode == MODE_EVALUATE) {
					_logger.info("Modus: evaluate model");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					String msg = stringReader.readLine();
					_logger.info("Modus: got Msg: " + msg);
					String evalResult = "";
					if (msg.compareTo("files") == 0) {
						EvaluatorThread et = new EvaluatorThread(_afm, new Categories(_config.getString("categories")));
						// don't do it as a thread, but synchronous
						et.run();
						evalResult = et.getSummary();
					} else {
						evalResult = _classifier.evaluate();
					}
					_logger.info(evalResult);
					evalResult = URLEncoder.encode(evalResult, _charEnc);
					_out.writeBytes(evalResult + '\n');
					_out.writeBytes("finished\n");
				}
				/**
				 * respond to a client's message
				 */
				else if (mode == MODE_MESSAGE) {
					// general send message
					stringReader = new BufferedReader(new InputStreamReader(_in));
					String msg = stringReader.readLine();
					_logger.info("Modus: got Msg: " + msg);
					/**
					 * Quit server
					 */
					if (msg.startsWith("quit;")) {
						System.exit(0);
					}
					/**
					 * Rename recording.
					 */
					else if (msg.startsWith("rename;")) {
						String tokens[] = msg.split(";");
						String fp = tokens[1];
						String newName = tokens[2];
						_logger.info("rename file: " + fp + " to " + newName);
						_afm.renameRecording(fp, newName);
					}
					/**
					 * Delete files.
					 */
					else if (msg.startsWith("delete;")) {
						String tokens[] = msg.split(";");
						_logger.info("deleting files: " + msg);
						for (int i = 1; i < tokens.length; i++) {
							deleteFile(tokens[i]);
						}
					}
					/**
					 * Change audio type.
					 */
					else if (msg.startsWith("audioFormat;")) {
						String tokens[] = msg.split(";");
						_logger.info("changing audio format to: " + tokens[1]);
						_afm.setAudioExtension(tokens[1]);
					}
					/**
					 * Word Spelling check.
					 */
					else if (msg.startsWith("check;")) {
						if (_config.getBool("withSpellChecker")) {
							String tokens[] = msg.split(";");
							_logger.info("checking word: " + tokens[1]);
							List<String> sl = checkSpelling(tokens[1]);
							if (sl == null || sl.size() == 0) {
								_out.writeBytes("ok\n");
							} else {
								_out.writeBytes(StringUtil.stringList2String(sl, " ") + '\n');
							}
						} else {
							_logger.info("no spellChecker activated");
							_out.writeBytes("no spellChecker activated\n");
						}
						_out.writeBytes("finished\n");
					}
					/**
					 * Dictionary Word add.
					 */
					else if (msg.startsWith("addWord;")) {
						if (_config.getBool("withSpellChecker")) {
							String tokens[] = msg.split(";");
							_logger.info("adding word to dictionary: " + tokens[1]);
							addWordToDictionary(tokens[1]);
						} else {
							_logger.info("no spellChecker activated");
						}
					}
					/**
					 * open
					 */
					else if (msg.startsWith("open;")) {
						String tokens[] = msg.split(";");
						_logger.info("open file: " + tokens[1]);
						_afm.reload(tokens[1].trim());
					} else if (msg.startsWith("openDir;")) {
						String tokens[] = msg.split(";");
						_logger.info("open directory: " + tokens[1]);
						_afm.reloadDir(tokens[1].trim());
					} else if (msg.startsWith("openModel;")) {
						String tokens[] = msg.split(";");
						_logger.info("open classifier model: " + tokens[1]);
						_classifier.loadModel(tokens[1].trim());
					} else if (msg.startsWith("exportTranscriptsToFile;")) {
						String tokens[] = msg.split(";");
						_logger.info("export transcriptps to file: " + tokens[1]);
						if (_emotionmlMode) {
							_afm.printEmotionML(new PrintStream(new FileOutputStream(new File(tokens[1].trim()))));
						} else {
							printLlistToFile(tokens[1].trim());
						}
					} else if (msg.startsWith("importTranscriptsFromFile;")) {
						String tokens[] = msg.split(";");
						_logger.info("import transcriptps from file: " + tokens[1]);
						_afm.importTranscriptions(tokens[1].trim());

					}
					/**
					 * synthesize all
					 */
					else if (msg.startsWith("synthesizeAll")) {
						String tokens[] = msg.split(";");
						String female = tokens[1];
						String lang = tokens[2];
						_logger.info("synthesizing all files with female " + female + " and language " + lang);
						_afm.synthesizeAll(Boolean.valueOf(female), lang, _config.getString("ttsVoice"));
					}
					/**
					 * synthesize several
					 */
					else if (msg.startsWith("synthesize")) {
						String tokens[] = msg.split(";");
						String female = tokens[1];
						String lang = tokens[2];
						_logger.info("synthesizing files with female " + female + " and language " + lang);
						for (int i = 3; i < tokens.length; i++) {
							String fn = tokens[i].trim();
							_logger.info("Modus: synthesizing file: " + fn);
							RecFile rec = _afm.findAudioFile(fn);
							if (rec != null) {
								rec.generateAudioFile(Boolean.valueOf(female), lang, _config.getString("ttsVoice"));
							} else {
								_logger.error("can'T synthesize: " + fn + " because recording not stored");
							}
						}
					}
					/**
					 * execute a command on the audio file
					 */
					else if (msg.startsWith("exec")) {
						String tokens[] = msg.split(";");
						String fn = tokens[1].trim();
						_logger.info("Modus: executing command on file: " + fn);
						// RecFile rec = _afm.findAudioFile(fn);
						String cmd = _config.getString("execCmd") + " " + fn;
						Util.execCmd(cmd, _logger);
					}
					/**
					 * compute WER
					 */
					else if (msg.startsWith("wer")) {
						String result = computeWER();
						_out.writeBytes(result + '\n');
						_out.writeBytes("finished\n");
					}
					/**
					 * normalize all
					 */
					else if (msg.compareTo("normalizeAll") == 0) {
						_logger.info("Modus: normalizing all files");
						_afm.normalizeAll();
					}
					/**
					 * normalize one
					 */
					else if (msg.startsWith("normalize")) {
						String tokens[] = msg.split(";");
						RecFile rec = _afm.findAudioFile(tokens[1].trim());
						_logger.info("Modus: normalizing file");
						rec.normalizeTranscription();
					}
					/**
					 * judge all
					 */
					else if (msg.compareTo("judgeAll") == 0) {
						_logger.info("Modus: judging all files");
						JudgeAllThread judgeAllThread = new JudgeAllThread(_afm, _classifier, _featExtractor);
						judgeAllThread.start();
					}
					/**
					 * recognize all
					 */
					else if (msg.compareTo("recognizeAll") == 0) {
						_logger.info("Modus: recognizing all files");
						Runnable runnable = new Runnable() {
							public void run() {
								_afm.recognizeAll();
							}
						};
						new Thread(runnable).start();
					}
					/**
					 * remove last label
					 */
					else if (msg.startsWith("removeLastLabel")) {
						String tokens[] = msg.split(";");
						_afm.removeLastLabel(tokens[1].trim());
					}
					/**
					 * remove the predictions
					 */
					else if (msg.startsWith("removeAllPredictions")) {
						_afm.removePredictions();
					}
					/**
					 * remove last predicate (judgement)
					 */
					else if (msg.startsWith("removePred")) {
						String tokens[] = msg.split(";");
						_afm.removeLastPred(tokens[1].trim());
					}
					/**
					 * delete all untagged (without a label)
					 */
					else if (msg.startsWith("removeUntagged")) {
						_afm.removeUntagged();
					}
					/**
					 * retrain the model
					 */
					else if (msg.startsWith("train")) {
						boolean extract = true;
						if (msg.endsWith("false")) {
							extract = false;
						}
						_logger.info("Modus: train classification model");
						TrainThread trainThread = new TrainThread(_featExtractor, _classifier, _afm, _config, extract);
						trainThread.start();
					}
					/**
					 * set classifier type
					 */
					else if (msg.startsWith("classifierType")) {
						String tokens[] = msg.split(";");
						String newClassifierType = tokens[1].trim();
						_logger.info("Modus: change classifier type to: " + newClassifierType);
						_classifier.setClassifierType(newClassifierType);
					} else {
						_logger.info("Message: " + msg);
					}
				}
				/**
				 * set a new label.
				 */
				else if (mode == MODE_SET_LABEL) {
					_logger.info("Modus: set _file emo");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					_filePath = stringReader.readLine();
					String category = stringReader.readLine();
					String label = stringReader.readLine();
					RecFile recFile = _afm.findAudioFile(_filePath);
					recFile.addLabel(category, label);
					_afm.updateAudioFile(_filePath);
				}
				/**
				 * set a transcription
				 */
				else if (mode == MODE_SET_TRANS) {
					// set transcription
					stringReader = new BufferedReader(new InputStreamReader(_in));
					_filePath = stringReader.readLine();
					String transcript = URLDecoder.decode(stringReader.readLine(), _charEnc);
					_logger.info("Modus: set _transcript for _file " + _filePath + ": " + transcript);
					RecFile recFile = _afm.findAudioFile(_filePath);
					recFile.storeTranscript(transcript);
					_afm.updateAudioFile(_filePath);
				}
				/**
				 * recognize the words
				 */
				else if (mode == MODE_RECOGNIZE) {
					// start recognition request
					_logger.info("Modus: start recognition process");
					stringReader = new BufferedReader(new InputStreamReader(_in));
					_filePath = stringReader.readLine();
					RecFile rf = _afm.findAudioFile(_filePath);
					String result = EmlUtils.recognizeFile(_filePath);
					rf.storeRecongnition(result);
					_out.writeBytes(result + '\n');
					_out.writeBytes("finished\n");
				}
				/**
				 * shouldn't happen.
				 */
				else {
					_logger.info("unsupported mode; " + mode);
				}
			} catch (Exception e) {
				_logger.error("error while communicating with client: " + e);
				e.printStackTrace();
			}
		}
	}

	private void deleteFile(String path) {
		File deleteFile = _afm.findAudioFileAndRemove(path);
		_logger.info("delete _file _name: " + deleteFile.getAbsolutePath() + ", _size: " + deleteFile.length());
		try {
			deleteFile.delete();
		} catch (Exception e) {
			_logger.info(e.getMessage());
			e.printStackTrace();
		}

	}

	private void printTranscrptionsToFile(String fileName) {
		_logger.info("printing out transcripts");
		Vector<RecFile> recFiles = _afm.getAudioFiles();
		Vector<String> newVec = new Vector<String>();
		for (Iterator<RecFile> iterator = recFiles.iterator(); iterator.hasNext();) {
			RecFile recFile = (RecFile) iterator.next();
			String trans = recFile.getTranscript();
			if (trans != null && trans.length() > 0) {
				newVec.add(recFile._path + " " + trans);
			}
		}
		try {
			FileUtil.writeFileContent(fileName, newVec, _charEnc);
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void printLlistToFile(String fileName) {
		_logger.info("printing out list ro file: " + fileName);
		Vector<RecFile> recFiles = _afm.getAudioFiles();
		Vector<String> newVec = new Vector<String>();
		for (Iterator<RecFile> iterator = recFiles.iterator(); iterator.hasNext();) {
			RecFile recFile = (RecFile) iterator.next();
			String trans = recFile.getTranscript();
			newVec.add(recFile._path + " " + trans);
		}
		try {
			FileUtil.writeFileContent(fileName, newVec, _charEnc);
		} catch (Exception e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private String computeWER() {
		String ret = "";
		_afm.printHypothesisFile();
		_afm.printReferenceFile();
		String cmd = _config.getString("scliteTool") + " -r " + _config.getString("referenceFile") + " -h "
				+ _config.getString("hypothesisFile") + " -i rm -f 0 -o rsum";
		try {
			Util.execCmd(cmd, _logger);
			String resFileName = _config.getString("hypothesisFile") + ".raw";
			FileUtil.waitForFile(resFileName, false);
			Vector<String> lines = FileUtil.getFileLines(new File(resFileName));
			for (Iterator iterator = lines.iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				if (string.trim().startsWith("| Sum")) {
					System.out.println(string);
					String[] parts = StringUtil.stringToArray(string);
					int total = Integer.parseInt(parts[4]);
					int substitutions = Integer.parseInt(parts[7]);
					int deletions = Integer.parseInt(parts[8]);
					int insertions = Integer.parseInt(parts[9]);
					double wer = NLPUtil.computeWER(total, substitutions, deletions, insertions);
					System.out.println("word error rate= " + wer);
					ret = "WER: " + wer + " " + string;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	private void initHunspell() {
		try {
			hunspell = null;
			hunspell = Hunspell.getInstance();
			_logger.debug("HunSpell initialized --- loading dics");

			String dics = "de_DE;en_US";
			if (dics != null) {
				String[] dic = dics.split(";");
				for (String d : dic) {
					Dictionary dd = hunspell.getDictionary("res/dict/" + d + "/" + d);
					if (dd != null) {
						_logger.debug("Dictionary " + d + " loaded");
					} else {
						_logger.debug("Could not load Dictionary " + d);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Could not initialize HunSpell");
			System.out.println(e.getMessage());
		}
	}

	private List<String> checkSpelling(String word) {
		List<String> ret = new ArrayList<String>();
		String lang = "res/dict/de_DE/de_DE";
		try {
			if (hunspell.getDictionary(lang).misspelled(word)) {
				ret = hunspell.getDictionary(lang).suggest(word);
			} else {
				return null;
			}
		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
		for (String s : ret) {
			_logger.debug(s);
		}
		return ret;
	}

	private void addWordToDictionary(String word) {
		String dicName = "res/dict/de_DE/de_DE.dic";
		try {
			Vector fileLines = FileUtil.getFileLines(dicName);
			fileLines.add(word);
			FileUtil.writeFileContent(dicName, fileLines);
			initHunspell();
		} catch (Exception e) {
			Util.errorOut(e, _logger);
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
		try {
			if (_in != null)
				_in.close();
			if (_out != null)
				_out.close();
			if (_socket != null)
				_socket.close();
			if (_serversocket != null) {
				_serversocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * call the server from commandline
	 * 
	 * @param optional
	 *            arguments
	 */
	public static void main(String args[]) {
		new Speechalyzer(args);
	}
}