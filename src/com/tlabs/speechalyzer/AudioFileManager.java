package com.tlabs.speechalyzer;

import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URI;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.StringUtil;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;
import com.felix.util.logging.LoggerInterface;
import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.emotions.Emotion;
import com.tlabs.speechalyzer.emotions.EmotionMLManager;
import com.tlabs.speechalyzer.emotions.VocabManager;

public class AudioFileManager {
	private Vector<RecFile> _audioFiles;
	private KeyValues _config;
	private LoggerInterface _logger;
	private String _fileList;
	private String _audioExtension = "";
	private int _sampleRate = 0;
	private RecFile _lastRec = null;
	private File _recordingDir;
	private VocabManager _vocabManager;
	private EmotionMLManager _emoMlManager;

	public AudioFileManager(String fileList, KeyValues config, File recordingDir) {
		_config = config;
		_fileList = fileList;
		_recordingDir = recordingDir;
		_logger = new Log4JLogger(Logger.getLogger("com.tlabs.speechalyzer.AudioFileManager"));
		_audioExtension = _config.getString("audioFormat");
		_sampleRate = _config.getInt("sampleRate");
		_emoMlManager = new EmotionMLManager(this, _config, _logger);
		reload();
	}

	public AudioFileManager(String fileList, KeyValues config) {
		_config = config;
		_fileList = fileList;
		_recordingDir = _config.getFileHandler("recordingDir");
		_logger = new Log4JLogger(Logger.getLogger("com.tlabs.speechalyzer.AudioFileManager"));
		_audioExtension = _config.getString("audioFormat");
		_sampleRate = _config.getInt("sampleRate");
		_emoMlManager = new EmotionMLManager(this, _config, _logger);
		reload();
	}

	public String get_audioExtension() {
		return _audioExtension;
	}

	public void setAudioExtension(String audioExtension) {
		_audioExtension = audioExtension;
	}

	public void setSampleRate(int sampleRate) {
		_sampleRate = sampleRate;
	}

	public KeyValues getConfig() {
		return _config;
	}

	public void reload(String file) {
		_recordingDir = null;
		_fileList = file;
		reload();
	}

	public LoggerInterface getLogger() {
		return _logger;
	}

	public void reloadDir(String file) {
		_recordingDir = new File(file);
		_fileList = null;
		reload();
	}

	public void initAudioFiles() {
		_audioFiles = new Vector<RecFile>();
	}

	public void addAudioFile(RecFile newFile) {
		_audioFiles.add(newFile);
	}

	public void printEmotionML(PrintStream out) {
		try {
			Document doc = new Document();
			Element root = Emotion.getRoot();
			root.setAttribute(new Attribute("version", "1.0"));
			root.setAttribute(Emotion.EMOTIONML_ATT_CATEGORYSET, _config.getString("emotionml-category-set-default"));
			root.setAttribute(Emotion.EMOTIONML_ATT_DIMENSIONSET, _config.getString("emotionml-dimension-set-default"));
			root.setAttribute(Emotion.EMOTIONML_ATT_APPRAISALSET, _config.getString("emotionml-appraisal-set-default"));
			root.setAttribute(Emotion.EMOTIONML_ATT_ACTIONTENDENCYSET,
					_config.getString("emotionml-action-tendency-set-default"));
			for (RecFile rec : _audioFiles) {
				if (rec.hasLabel()) {
					root.addContent(rec.getEmotionMLElement());
				}
			}
			doc.addContent(root);
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(doc, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void importTranscriptions(String file) {
		_recordingDir = null;
		_fileList = file;
		_audioFiles = new Vector<RecFile>();
		if (_fileList.length() > 0) {
			_logger.info("loading audio files with transcriptions from " + _fileList + "...");
			try {
				int index = 0;
				Vector<String> tmp = FileUtil.getFileLines(_fileList, _config.getString("charEnc"));
				for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
					String line = iterator.next();
					_logger.debug("loading line: " + line);
					if (!FileUtil.isCommentOrEmpty(line)) {
						StringTokenizer st = new StringTokenizer(line);
						String path = st.nextToken();
						if (path.endsWith("." + _audioExtension)) {
							if (!isAudioFileContained(path)) {
								_logger.warn("AFM: file, line " + line + ", doesnt exist, creating it.");
								String transcription = StringUtil.getRestOfLine(st);
								RecFile recFile = new RecFile(path, _config);
								recFile.storeTranscript(transcription);
								_logger.debug("adding file " + path + " with transcript: " + transcription);
								_audioFiles.add(recFile);
							}
						}
						// indicate progress while loading
						if (index % 10 == 0) {
							System.err.print(".");
						}
						index++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				_logger.error("invalid filelist: " + e.getMessage());
				System.exit(1);
			}
		}

	}

	public void parseEmotionMLFromFile(String filePath) {
		try {
			_recordingDir = null;
			_fileList = FileUtil.getFileText(filePath);
			_logger.info("loading audio files with transcriptions from EmotionML document " + _fileList + "...");
			_audioFiles = new Vector<RecFile>();
			SAXBuilder parser = new SAXBuilder();
			Document doc = parser.build(filePath);
			Element root = doc.getRootElement();
			_emoMlManager.loadEmotionMLElement(root);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseEmotionML(String contents) {
		try {
			_recordingDir = null;
			_fileList = contents;
			_logger.info("loading audio files with transcriptions from EmotionML document " + _fileList + "...");
			_audioFiles = new Vector<RecFile>();
			SAXBuilder parser = new SAXBuilder();
			Document doc = parser.build(new StringReader(contents));
			Element root = doc.getRootElement();
			_emoMlManager.loadEmotionMLElement(root);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean fileListIsEmotionML() {
		if (_fileList != null) {
			if (_fileList.startsWith("<"))
				return true;
		}
		return false;
	}

	public void reload() {
		if (fileListIsEmotionML()) {
			parseEmotionML(_fileList);
			return;
		}
		try {
			_audioFiles = new Vector<RecFile>();
			if (_fileList != null && _fileList.length() > 0) {
				_logger.info("loading audio files from " + _fileList + "...");
				try {
					int index = 0;
					Vector<String> tmp = FileUtil.getFileLines(_fileList, _config.getString("charEnc"));
					for (String line : tmp) {
						_logger.debug("loading line: " + line);
						if (!FileUtil.isCommentOrEmpty(line)) {
							StringTokenizer st = new StringTokenizer(line);
							String path = st.nextToken();
							if (path.endsWith("." + _audioExtension)) {
								if (!isAudioFileContained(path)) {
									if (!FileUtil.existFile(path)) {
										_logger.warn("AFM: file, line " + line + ", doesnt exist, creating it.");
										RecFile recFile = new RecFile(path, _config);
										// DON'T add transcriptions just yet,
										// that's what the "-at" option is for
										// String transcription = StringUtil
										// .getRestOfLine(st);
										// if (transcription.length() > 0) {
										// recFile.storeTranscript(transcription);
										// _logger.debug("adding file " + path
										// + " with transcript: "
										// + transcription);
										// } else {
										// _logger.debug("adding file " + path
										// + " without transcript.");
										// }
										_audioFiles.add(recFile);
									} else {
										RecFile recFile = new RecFile(path, _config);
										String txtS = path.substring(0, path.length() - 4) + ".txt";
										// if (FileUtil.existFile(txtS)) {
										// _logger.debug("adding already
										// existing file (ignoring transcript) "
										// + path);
										// } else {
										// String transcription = StringUtil
										// .getRestOfLine(st);
										// if (transcription.length() > 0) {
										// recFile.storeTranscript(transcription);
										// _logger.debug("adding file "
										// + path
										// + " with transcript: "
										// + transcription);
										// } else {
										// _logger.debug("adding file "
										// + path
										// + " without transcript.");
										// }
										// }
										_audioFiles.add(recFile);
									}
								}
							}
							// indicate progress while loading
							if (index % 10 == 0) {
								System.err.print(".");
							}
							index++;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					_logger.error("invalid filelist: " + e.getMessage());
					System.exit(1);
				}
			}
			if (_recordingDir != null && FileUtil.existPath(_recordingDir.getPath())) {
				_logger.info("loading audio files from " + _recordingDir + "...");
				File files[] = _recordingDir.listFiles();
				if (files != null) {
					for (int j = 0; j < files.length; j++) {
						File file = files[j];
						if (file.getName().endsWith("." + _audioExtension)) {
							String filepath = file.getPath().replace("\\", "/");
							_audioFiles.add(new RecFile(filepath, _config));
						} else if (file.isDirectory()) {
							File subdirfiles[] = file.listFiles();
							if (subdirfiles != null) {
								for (int k = 0; k < subdirfiles.length; k++) {
									File subfile = subdirfiles[k];
									if (subfile.getName().endsWith("." + _audioExtension)) {
										String filepath = subfile.getPath().replace("\\", "/");
										_audioFiles.add(new RecFile(filepath, _config));
									}
								}
							}

						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.info("AFM: added " + getStats());
	}

	public String getStats() {
		int fileNum = _audioFiles.size();
		long dataSize = 0;
		int angryNum = 0;
		long dataSizeAnger = 0;
		for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
			RecFile recFile = iterator.next();
			dataSize += recFile._size;
			if (recFile.isAngry()) {
				angryNum++;
			}
			if (_audioExtension.compareTo("wav") == 0) {
				dataSize -= 44;
			}
			if (recFile.isAngry()) {
				angryNum++;
				dataSizeAnger += recFile._size;
				if (_audioExtension.compareTo("wav") == 0) {
					dataSizeAnger -= 44;
				}
			}
		}
		if (_config.getDouble("angerBorder") > 0) {
			return "ALL: " + printSizeVals(dataSize, fileNum) + ", ANGER: " + printSizeVals(dataSizeAnger, angryNum)
					+ "\n";
		} else {
			return printSizeVals(dataSize, fileNum);
		}
	}

	private String printSizeVals(long dataSize, int fileNum) {
		int dataSec = (int) (dataSize / 2) / _sampleRate;
		double dataMin = Util.cutDouble(dataSec / 60.0);
		double dataHour = Util.cutDouble(dataSec / 3600.0);
		double meanDataSec = Util.cutDouble((double) dataSec / fileNum);
		return "#files: " + fileNum + ", " + dataSize + " bytes (" + dataSize / 1000 + " kb, " + dataSize / 1000000
				+ " mb), " + "secs: " + dataSec + ", mins: " + dataMin + ", hours: " + dataHour + ", avr sec: "
				+ meanDataSec;
	}

	public void addSympaResults(String fileList) {
		int index = 0;
		try {
			Vector<String> tmp = FileUtil.getFileLines(fileList);
			for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
				String line = iterator.next();
				StringTokenizer st = new StringTokenizer(line);
				String path = st.nextToken();
				st.nextToken();
				double na = Double.parseDouble(st.nextToken());
				double ha = Double.parseDouble(st.nextToken());
				RecFile recFile = findAudioFile(path);
				ClassificationResult cr = new ClassificationResult();
				cr.addResult("N", na);
				cr.addResult("A", ha);
				if (recFile != null) {
					index++;
					recFile.storePred(cr);
				} else {
					_logger.error("recfile not found: " + path);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.info("AFM: added sympalog Results to " + index + " audio files");
	}

	/**
	 * Add/Append labels to audiofiles.
	 * 
	 * @param fileList
	 */
	public void addLabels(String fileList) {
		int index = 0;
		try {
			Vector<String> tmp = FileUtil.getFileLines(fileList);
			for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
				String line = iterator.next();
				if (!FileUtil.isCommentOrEmpty(line)) {
					StringTokenizer st = new StringTokenizer(line);
					String path = st.nextToken();
					String labString = "";
					while (st.hasMoreTokens()) {
						labString += st.nextToken() + " ";
					}
					labString = labString.trim();
					if (labString.length() > 0) {
						RecFile recFile = findAudioFile(path);
						if (recFile != null) {
							Vector<String> labels = StringUtil.stringToVector(labString);
							for (Iterator iterator2 = labels.iterator(); iterator2.hasNext();) {
								String string = (String) iterator2.next();
								recFile.addLabel(_config.getString("defaultEmotionName"), string);

							}
							index++;
						} else {
							_logger.error("recfile not found: " + path);
						}
					}
					if (index % 10 == 0) {
						System.err.print(".");
					}
					if (index % 100 == 0) {
						System.err.println();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.info("AFM: added " + index + " labels to audio files");
	}

	public RecFile addAudioFile(File file) {
		RecFile newRec = new RecFile(file.getPath(), _config);
		try {
			_audioFiles.add(newRec);
			_lastRec = newRec;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newRec;
	}

	public boolean removeLastRecording() {
		try {
			_lastRec.removeAnnotationFile();
			_lastRec.removeAudioFile();
			_audioFiles.remove(_lastRec);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Replace/add labels to audiofiles
	 * 
	 * @param fileList
	 */
	public void replaceLabels(String fileList) {
		int index = 0;
		try {
			Vector<String> tmp = FileUtil.getFileLines(fileList);
			for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
				String line = iterator.next();
				StringTokenizer st = new StringTokenizer(line);
				String path = st.nextToken();
				String labString = "";
				while (st.hasMoreTokens()) {
					labString += st.nextToken() + " ";
				}
				labString = labString.trim();
				if (labString.length() > 0) {
					RecFile recFile = findAudioFile(path);
					if (recFile != null) {
						index++;
						recFile.replaceLabel(_config.getString("defaultEmotionName"), labString);
					} else {
						_logger.error("recfile not found: " + path);
					}
				}
				// indicate progress while loading
				if (index % 10 == 0) {
					System.err.print(".");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.info("AFM: replaced/added labels to " + index + " audio files");
	}

	public void addTranscripts(String fileList) {
		int index = 0;
		try {
			Vector<String> tmp = FileUtil.getFileLines(fileList, _config.getString("charEnc"));
			for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
				String line = iterator.next();
				if (!FileUtil.isCommentOrEmpty(line)) {
					StringTokenizer st = new StringTokenizer(line);
					String path = st.nextToken();
					String trans = "";
					while (st.hasMoreTokens()) {
						trans += st.nextToken() + " ";
					}
					trans = trans.trim();
					if (trans.length() > 0) {
						RecFile recFile = findAudioFile(path);
						if (recFile != null) {
							index++;
							recFile.storeTranscript(trans);
						} else {
							_logger.error("recfile not found: " + path);
						}
					}
					// indicate progress while loading
					if (index % 10 == 0) {
						System.err.print(".");
					}
				}
			}
			_logger.info("AFM: added " + index + " transcriptions to audio files");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addRecognition(String fileList) {
		int index = 0;
		try {
			Vector<String> tmp = FileUtil.getFileLines(fileList, _config.getString("charEnc"));
			for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
				String line = iterator.next();
				if (!FileUtil.isCommentOrEmpty(line)) {
					StringTokenizer st = new StringTokenizer(line);
					String path = st.nextToken();
					String trans = "";
					while (st.hasMoreTokens()) {
						trans += st.nextToken() + " ";
					}
					trans = trans.trim();
					if (trans.length() > 0) {
						RecFile recFile = findAudioFile(path);
						if (recFile != null) {
							index++;
							recFile.storeRecongnition(trans);
						} else {
							_logger.error("recfile not found: " + path);
						}
					}
					// indicate progress while loading
					if (index % 10 == 0) {
						System.err.print(".");
					}
				}
			}
			_logger.info("AFM: added " + index + " transcriptions to audio files");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printHypothesisFile() {
		int index = 0;
		String hypFileText = "";
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				hypFileText += recFile.getRecognition() + " (" + recFile._dialog + "_" + recFile._name + ")\n";
			}
			FileUtil.writeFileContent(_config.getFileHandler("hypothesisFile"), hypFileText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printReferenceFile() {
		int index = 0;
		String hypFileText = "";
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				hypFileText += recFile.getTranscript() + " (" + recFile._dialog + "_" + recFile._name + ")\n";
			}
			FileUtil.writeFileContent(_config.getFileHandler("referenceFile"), hypFileText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addTranscriptsAndSynthesize(String fileList) {
		int index = 0;
		try {
			Vector<String> tmp = FileUtil.getFileLines(fileList);
			for (Iterator<String> iterator = tmp.iterator(); iterator.hasNext();) {
				String line = iterator.next();
				if (!FileUtil.isCommentOrEmpty(line)) {
					StringTokenizer st = new StringTokenizer(line);
					String path = st.nextToken();
					String trans = "";
					while (st.hasMoreTokens()) {
						trans += st.nextToken() + " ";
					}
					trans = trans.trim();
					if (trans.length() > 0) {
						RecFile recFile = findAudioFile(path);
						if (recFile != null) {
							index++;
							recFile.storeTranscript(trans);
							try {
								recFile.generateAudioFile(_config.getBool("ttsSexFemale"), _config.getString("ttsLang"),
										_config.getString("ttsVoice"));
							} catch (Exception e) {
								_logger.error(e.getMessage());
								e.printStackTrace();
							}
						} else {
							_logger.error("recfile not found: " + path);
						}
					}
					// indicate progress while loading
					if (index % 10 == 0) {
						System.err.print(".");
					}
				}
			}
			_logger.info("AFM: synthesized " + index + " audio files");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void synthesizeAll(boolean female, String lang, String voice) {
		int index = 0;
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile != null) {
					index++;
					_logger.info("Synthesizing " + index + " of " + _audioFiles.size() + " audio files");
					recFile.generateAudioFile(female, lang, voice);
					// indicate progress while loading
					if (index % 10 == 0) {
						System.err.print(".");
					}
				}
				_logger.info("AFM: synthesized " + index + " audio files");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void normalizeAll() {
		int index = 0;
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile != null) {
					index++;
					recFile.normalizeTranscription();
					// indicate progress while loading
					if (index % 10 == 0) {
						System.err.print(".");
					}
				}
				_logger.info("AFM: normalized " + index + " audio files");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeLabels() {
		int index = 0;
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile != null) {
					if (FileUtil.existFile(recFile._path)) {
						index++;
						recFile.removeLabels();
					} else {
						_logger.error("AFM: ERROR: _file " + recFile._path + " doesn't exists");
					}
				}
				// indicate progress while loading
				if (index % 10 == 0) {
					System.err.print(".");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println("AFM: removed labels from " + index + " audio files");
	}

	/**
	 * Remove Predicions for all loaded audio files.
	 */
	public void removePredictions() {
		int index = 0;
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile != null) {
					if (FileUtil.existFile(recFile._path)) {
						index++;
						recFile.removePrediction();
					} else {
						_logger.error("AFM: ERROR: _file " + recFile._path + " doesn't exists");
					}
				}
				// indicate progress while loading
				if (index % 10 == 0) {
					System.err.print(".");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println("AFM: removed predictions from " + index + " audio files");
	}

	public RecFile findAudioFile(String path) {
		try {
			for (RecFile recFile : _audioFiles) {
				if (recFile._path.compareTo(path) == 0) {
					return recFile;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.error("AFM: did not find audio: " + path);
		return null;
	}

	public File findAudioFileAndRemove(String path) {
		try {
			for (RecFile recFile : _audioFiles) {
				if (recFile._path.compareTo(path) == 0) {
					recFile.removeAnnotationFile();
					_audioFiles.remove(recFile);
					String fn = recFile._file.getAbsolutePath();
					return new File(fn);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.error("AFM: didn't find audio; " + path);
		return null;
	}

	public void renameRecording(String path, String newName) {
		try {
			for (RecFile recFile : _audioFiles) {
				if (recFile._path.compareTo(path) == 0) {
					_audioFiles.remove(recFile);
					recFile.rename(newName);
					_audioFiles.add(recFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.error("AFM: didn't find audio; " + path);
	}

	public Vector<RecFile> getAudioFiles() {
		return _audioFiles;
	}

	public void recognizeAll() {
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				recFile.recognize();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Vector<RecFile> getAudioFilesWithLabels() {
		Vector<RecFile> returnVec = new Vector<RecFile>();
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile.hasLabel()) {
					returnVec.add(recFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnVec;
	}

	public Vector<RecFile> getAudioFilesWithPredictions() {
		Vector<RecFile> returnVec = new Vector<RecFile>();
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile.hasPrediction()) {
					returnVec.add(recFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnVec;
	}

	public Vector<RecFile> getAudioFilesWithoutPredictions() {
		Vector<RecFile> returnVec = new Vector<RecFile>();
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (!recFile.hasPrediction()) {
					returnVec.add(recFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnVec;
	}

	public void removeUntagged() {
		try {
			_logger.debug("deleting untagged...");
			int length = _audioFiles.size();
			for (int i = 0; i < length - 1; i++) {
				RecFile recFile = _audioFiles.elementAt(i);
				if (!recFile.hasLabel()) {
					recFile.removeFiles();
					_audioFiles.remove(recFile);
					i--;
				}
			}
			_logger.debug("done deleting untagged.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void set_vocabManager(VocabManager _vocabManager) {
		this._vocabManager = _vocabManager;
	}

	public Vector<RecFile> getAudioFilesWithTranscripts() {
		Vector<RecFile> returnVec = new Vector<RecFile>();
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile.hasTranscript()) {
					returnVec.add(recFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnVec;
	}

	public boolean isAudioFileContained(String path) {
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile._path.compareTo(path) == 0) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void printFilesWithoutTranscription() {
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile.getTranscript() == null) {
					System.out.println(recFile._path);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean removeLastLabel(String path) {
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile._path.compareTo(path) == 0) {
					recFile.removeLastLabel();
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.error("AFM: didn't find audio; " + path);
		return false;
	}

	public boolean removeLastPred(String path) {
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile._path.compareTo(path) == 0) {
					recFile.removePrediction();
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.error("AFM: didn't find audio; " + path);
		return false;
	}

	public boolean updateAudioFile(String path) {
		try {
			for (Iterator<RecFile> iterator = _audioFiles.iterator(); iterator.hasNext();) {
				RecFile recFile = iterator.next();
				if (recFile._path.compareTo(path) == 0) {
					recFile.loadAnnotations();
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_logger.error("AFM: didn't find audio; " + path);
		return false;
	}
}
