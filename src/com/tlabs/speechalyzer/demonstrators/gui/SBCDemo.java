package com.tlabs.speechalyzer.demonstrators.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.felix.util.AudioUtil;
import com.felix.util.DateTimeUtil;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.PlayWave;
import com.felix.util.SwingUtil;
import com.felix.util.SwingUtil.ImagePanel;
import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.RecFile;
import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.classifier.IClassifier;
import com.tlabs.speechalyzer.classifier.WEKAClassifier;
import com.tlabs.speechalyzer.featureextract.IExtractor;
import com.tlabs.speechalyzer.featureextract.OpenEarExtractor;

public class SBCDemo extends JFrame implements DemonstratorInterface {

	private static final long serialVersionUID = 1L;
	public static final int IMG_START = 0, IMG_ACTIVE = 1, IMG_RED = 2,
			IMG_GREEN = 3, IMG_C = 4, IMG_YF = 5, IMG_YM = 6, IMG_AF = 7,
			IMG_AM = 8, IMG_SF = 9, IMG_SM = 10, AREA_MIC = 0, AREA_GREEN = 1,
			AREA_RED = 2, AREA_SWITCH = 3, AUDIOMODE_PUSH_TO_ACTIVATE = 0,
			AUDIOMODE_PUSH_TO_TALK = 1, AUDIOMODE_PERMANENT_RECORDING = 2;
	private KeyValues _config, _agenderConfig;
	private Logger _logger;
	private boolean _recording;
	private final AudioFormat _format = AudioUtil.FORMAT_PCM_16KHZ;
	private PlayWave _player;
	private IClassifier _agenderClassifier, _angerClassifier;
	private IExtractor _agenderExtractor, _angerExtractor;
	private String _lastPred = "";
	private AudioFileManager _afm;
	private int _silenceThreshold = 0, _speechTimeout = 0, _initialTimeout = 0,
			_sampleRate = 0, _maxRecordingTime = 0;
	private DemonstratorConfigurator _demonstratorConfigurator;
	private Area[] _areas;
	private Point _switchModeTopLeft = null, _logoTopLeft = null,
			_okTopLeft = null;
	private ImagePanel _mainPanel;
	private boolean _angerRecognition = true, _agenderRecognition = false,
			_audioLogging, _feedback = false, _showGreenLight = false;
	private String _resolution, _redClass="", _greenClass="";
	private String _lastResult = " ";
	private boolean _firststart = true;
	private int _globalCounter = 0;
	private byte[] _lastRecording;

	public SBCDemo() {
		super("Speech Based Classification Demo");
		loadConfig();
		FileUtil.createDir("tmp");
		_logger = Logger
				.getLogger("com.tlabs.speechalyzer.callcenter.MainFrame");
		DOMConfigurator.configure(_config.getPathValue("logConfig"));
		_player = new PlayWave();
		_sampleRate = _config.getInt("sampleRate");
		_silenceThreshold = _config.getInt("silenceThreshold");
		_speechTimeout = _config.getInt("speechTimeout");
		_initialTimeout = (int) (_config.getDouble("initialTimeout") * _sampleRate);
		_audioLogging = _config.getBool("audioLogging");
		_angerRecognition = _config.getBool("angerRecognition");
		_showGreenLight = _config.getBool("showNonAnger");
		_redClass = _config.getString("redClass");
		_greenClass = _config.getString("greenClass");
		_maxRecordingTime = _config.getInt("maxRecordingTime");
		_feedback = _config.getBool("feedback");
		_angerClassifier = new WEKAClassifier(_config);
		_angerExtractor = new OpenEarExtractor(_config);
		_agenderClassifier = new WEKAClassifier(_agenderConfig);
		_agenderExtractor = new OpenEarExtractor(_agenderConfig);
		_afm = new AudioFileManager("", _config);
		_resolution = _config.getString("resolution");
		_demonstratorConfigurator = new DemonstratorConfigurator(_config, this,
				_angerClassifier, _angerExtractor, _afm);
		initGui();
	}

	private void initGui() {
		try {
			getContentPane().removeAll();
			Point redTopLeft = null;
			Point greenTopLeft = null;
			Point micTopLeft = null;
			Dimension bulbDim = null;
			Dimension micDim = null;
			Dimension switchLabelDim = new Dimension(100, 100);
			redTopLeft = SwingUtil.getPoint(_config, "gui." + _resolution
					+ ".redTopLeft");
			greenTopLeft = SwingUtil.getPoint(_config, "gui." + _resolution
					+ ".greenTopLeft");
			micTopLeft = SwingUtil.getPoint(_config, "gui." + _resolution
					+ ".micTopLeft");
			bulbDim = SwingUtil.getDimension(_config, "gui." + _resolution
					+ ".bulbDim");
			micDim = SwingUtil.getDimension(_config, "gui." + _resolution
					+ ".micDim");
			_switchModeTopLeft = SwingUtil.getPoint(_config, "gui."
					+ _resolution + ".switchModeTopLeft");
			_logoTopLeft = SwingUtil.getPoint(_config, "gui." + _resolution
					+ ".logoTopLeft");
			_okTopLeft = SwingUtil.getPoint(_config, "gui." + _resolution
					+ ".okTopLeft");

			Area red = new Area(new Rectangle(redTopLeft, bulbDim));
			Area green = new Area(new Rectangle(greenTopLeft, bulbDim));
			Area mic = new Area(new Rectangle(micTopLeft, micDim));
			Area switchMode = new Area(new Rectangle(_switchModeTopLeft,
					switchLabelDim));
			_areas = new Area[4];
			_areas[AREA_MIC] = mic;
			_areas[AREA_RED] = red;
			_areas[AREA_GREEN] = green;
			_areas[AREA_SWITCH] = switchMode;
			_mainPanel = makeMainPanel();
			getContentPane().add(_mainPanel);
			showMainframe();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setAudioLogging(boolean audioLogging) {
		_audioLogging = audioLogging;
	}

	public void setShowNonAnger(boolean showNonAnger) {
		_showGreenLight = showNonAnger;
	}

	public void setAudioMode(int mode) {
		_logger.debug("not implemented");
	}

	public AudioFormat getAudioFormat() {
		return _format;
	}

	public JFrame getFrame() {
		return this;
	}

	public void exit() {
		System.exit(0);
	}

	public void setInitialTimeout(double timeout) {
		_logger.debug("new initial timeout: " + timeout);
		_initialTimeout = (int) (timeout * _sampleRate);
	}

	public void setNoiseLevel(int level) {
		_logger.debug("new noiselevel: " + level);
		_silenceThreshold = level;
	}

	public String getLastResult() {
		return _lastResult;
	}

	public void switchMode() {
		if (_angerRecognition) {
			setAgenderMode();
		} else if (_agenderRecognition) {
			setAngerMode();
		}
	};

	public void setResolution(String resolution) {
		_logger.debug("setting resolution to: " + resolution);
		_resolution = resolution;
		initGui();
	}

	private ImagePanel makeMainPanel() {
		Image[] images = new Image[11];
		images[IMG_START] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("startImage"))
				.getImage();
		images[IMG_ACTIVE] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("activeImage"))
				.getImage();
		images[IMG_RED] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("redImage"))
				.getImage();
		images[IMG_GREEN] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("greenImage"))
				.getImage();
		images[IMG_C] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("cImage"))
				.getImage();
		images[IMG_YF] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("yfImage"))
				.getImage();
		images[IMG_YM] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("ymImage"))
				.getImage();
		images[IMG_AF] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("afImage"))
				.getImage();
		images[IMG_AM] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("amImage"))
				.getImage();
		images[IMG_SF] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("sfImage"))
				.getImage();
		images[IMG_SM] = new ImageIcon(_config.getString("imageDir")
				+ _resolution + "/" + _config.getString("smImage"))
				.getImage();
		ImagePanel pane = new SwingUtil.ImagePanel(images);
		pane.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				int modifiers = mouseEvent.getModifiers();
				Point clickPoint = mouseEvent.getPoint();
				for (int i = 0; i < _areas.length; i++) {
					Area aArea = _areas[i];
					if (aArea.contains(clickPoint)) {
						if (i == AREA_MIC) {
							if ((modifiers & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
								// left click
								if (_recording) {
									_logger.debug("stop recording.");
									_recording = false;
									_globalCounter = 0;
									_mainPanel.switchImage(IMG_START);
								} else {
									captureAudio();
								}
							} else if ((modifiers & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
								// right click
								playAudio();
							}
						} else if (i == AREA_RED) {
							if (_feedback
									&& _angerRecognition
									&& (modifiers & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
								// left click
								storeLastRecording("A", _lastRecording);
								showOk();
							} else if ((modifiers & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
								// right click
								_demonstratorConfigurator.showMainframe();
							}
						} else if (i == AREA_GREEN) {
							if (_feedback
									&& _angerRecognition
									&& (modifiers & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
								// left click
								storeLastRecording("N", _lastRecording);
								showOk();
							} else if ((modifiers & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
								// right click
								_demonstratorConfigurator.showMainframe();
							}
						} else if (i == AREA_SWITCH) {
							switchMode();
						}
					}
				}
			}
		});
		pane.setAdditionalImage(new ImageIcon(_config.getString("gui."
				+ _resolution + ".logo")).getImage(), _logoTopLeft);
		return pane;
	}

	private void showOk() {
		Runnable runner = new Runnable() {
			public void run() {
				_mainPanel.setAdditionalImage(new ImageIcon(_config
						.getPathValue("okImage")).getImage(), _okTopLeft);
				_mainPanel.repaint();
				try {
					Thread.sleep(_config.getInt("waitTime"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				showLogo();
			}
		};
		Thread runThread = new Thread(runner);
		runThread.start();
	}

	private void showMode() {
		Runnable runner = new Runnable() {
			public void run() {
				if (_angerRecognition) {
					_mainPanel.setAdditionalImage(new ImageIcon(_config
							.getPathValue("angerImage")).getImage(),
							_switchModeTopLeft);
				} else {
					_mainPanel.setAdditionalImage(new ImageIcon(_config
							.getPathValue("agenderImage")).getImage(),
							_switchModeTopLeft);
				}
				_mainPanel.repaint();
				try {
					Thread.sleep(_config.getInt("waitTime"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				showLogo();
			}
		};
		Thread runThread = new Thread(runner);
		runThread.start();
	}

	private void showLogo() {
		_mainPanel.setAdditionalImage(new ImageIcon(_config.getString("gui."
				+ _resolution + ".logo")).getImage(), _logoTopLeft);
		_mainPanel.repaint();

	}

	private void setAgenderMode() {
		_logger.debug("switch to agender mode");
		_agenderRecognition = true;
		_angerRecognition = false;
		showMode();
	}

	private void setAngerMode() {
		_logger.debug("switch to anger mode");
		_agenderRecognition = false;
		_angerRecognition = true;
		showMode();
	}

	private void processRecording(byte byteArray[]) {
		_logger.info("recording stopped");

		try {
			if (byteArray.length > 100) {
				_lastRecording = byteArray;
				String filename = _config.getString("testAudioFile");
				AudioUtil.writeAudioToWavFile(byteArray, _format, filename);
				if (_audioLogging && _config.getBool("logAll")) {
					storeLastRecording("", byteArray);
				}
				if (_angerRecognition) {
					_angerExtractor.extractFeatures(new File(filename)
							.getAbsolutePath());
					ClassificationResult cr = _angerClassifier.classify();
					_lastResult = cr.toString();
					_demonstratorConfigurator.setStatusLabel(_lastResult);
					_logger.info("classified file " + filename + ": "
							+ _lastResult);
					if (cr.getWinner().getCat().compareTo(_redClass) == 0) {
						_lastPred = _redClass;
						if (_audioLogging && !_config.getBool("logAll")) {
							storeLastRecording("", byteArray);
						}
						showResult(IMG_RED);
					} else if (cr.getWinner().getCat().compareTo(_greenClass) == 0) {
						_lastPred = _greenClass;
						if (_audioLogging && !_config.getBool("logAll")) {
							storeLastRecording("", byteArray);
						}
						if (_showGreenLight) {
							showResult(IMG_GREEN);
						}
					} else {
						// something else recognized, e.g. garbage
					}
				} else if (_agenderRecognition) {
					_agenderExtractor.extractFeatures(new File(filename)
							.getAbsolutePath());
					ClassificationResult cr = _agenderClassifier.classify();
					_lastResult = cr.toString();
					_demonstratorConfigurator.setStatusLabel(_lastResult);
					_logger.info("classified file " + filename + ": "
							+ _lastResult);
					int winner = Integer.parseInt(cr.getWinner().getCat());
					switch (winner) {
					case 1:
						showResult(IMG_C);
						break;
					case 2:
						showResult(IMG_YF);
						break;
					case 3:
						showResult(IMG_YM);
						break;
					case 4:
						showResult(IMG_AF);
						break;
					case 5:
						showResult(IMG_AM);
						break;
					case 6:
						showResult(IMG_SF);
						break;
					case 7:
						showResult(IMG_SM);
						break;
					}
				}
			} else {
				_logger.debug("no valid audio to process.");
			}
		} catch (Exception e) {
			error(e);
		}
	}

	private void showResult(int imgIndex) {
		_mainPanel.switchImage(imgIndex);
		try {
			Thread.sleep(_config.getInt("waitTime"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (_recording)
			_mainPanel.switchImage(IMG_ACTIVE);
		else
			_mainPanel.switchImage(IMG_START);

	}

	private String storeLastRecording(String annotation, byte byteArray[]) {
		FileUtil.createDir(_config.getString("recordingDir"));
		String filename = _config.getString("recordingDir")
				+ DateTimeUtil.getDateName() + ".wav";
		try {
			AudioUtil.writeAudioToWavFile(byteArray, AudioUtil.FORMAT_PCM_16KHZ,
					filename);
			RecFile rec = _afm.addAudioFile(new File(filename));
			String label = "";
			if (annotation.compareTo("A") == 0) {
				label = "5";
				rec.addLabel(_config.getString("defaultEmotionName"),label);
				_logger.info("noted anger and stored to : " + filename);
			} else if (annotation.compareTo("N") == 0) {
				label = "1";
				rec.addLabel(_config.getString("defaultEmotionName"), label);
				_logger.info("noted neutral and stored to : " + filename);
			} else {
				_logger.info("Stored without annotation to : " + filename);
			}
		} catch (Exception e) {
			error(e);
		}
		return filename;
	}

	private void trainModel() {
		if (_angerRecognition) {
			_angerExtractor.extractAllFeatures(_afm, true);
			_angerClassifier.trainModel();
		}
	}

	private void error(Exception e) {
		_logger.error(e.getMessage());
		e.printStackTrace();
	}

	private void captureAudio() {
		try {
			_mainPanel.switchImage(IMG_ACTIVE);
			_logger.debug("recording...");
			DataLine.Info info = new DataLine.Info(TargetDataLine.class,
					_format);
			final TargetDataLine line = (TargetDataLine) AudioSystem
					.getLine(info);
			line.open(_format);
			line.start();
			Runnable runner = new Runnable() {
				public void run() {
					int bufferSize, recordedSize=0;
					int maxSize = _maxRecordingTime * _sampleRate;
					boolean storing = false, silence = true;
					bufferSize = _speechTimeout;
					_recording = true;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte buffer[] = new byte[bufferSize];
					try {
						while (_recording) {
							int count = line.read(buffer, 0, bufferSize);
							short[] values = AudioUtil
									.byteToShort(buffer, true);
							silence = true;
							for (int i = 0; i < values.length; i++) {
								if (Math.abs(values[i]) > _silenceThreshold) {
									silence = false;
									if (storing == false) {
										storing = true;
										_logger.debug("start storing");
									}
									break;
								}
							}
							if (count > 0 && storing) {
//								_logger.debug("read "+count+" bytes");
								out.write(buffer, 0, count);
								recordedSize+=count;
							}
							if ((silence && storing) || recordedSize > maxSize) {
								_logger.debug("stop storing");
								recordedSize=0;
								storing = false;
								line.flush();
//								line.drain();
								out.flush();
								processRecording(out.toByteArray());
								out.close();
								out = new ByteArrayOutputStream();
							}

						}
					} catch (Exception e) {
						error(e);
					}
				}
			};
			Thread captureThread = new Thread(runner);
			captureThread.start();
		} catch (LineUnavailableException e) {
			_logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void playAudio() {
		try {
			if (_lastRecording != null && _lastRecording.length > 1000) {
				_player.playAudioFromByteArray(_lastRecording, _format);
				_logger.info("playing audio...");
			} else {
				_logger.info("Can't play audio, last recording not valid.");
			}
		} catch (Exception e) {
			error(e);
		}
	}

	private void showMainframe() {
		try {
			if (_config.getBool("noWindowDecoration") && _firststart) {
				_firststart = false;
				setUndecorated(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void loadConfig() {
		try {
			_config = new KeyValues("res/sbcDemo.properties", "=");
			_agenderConfig = new KeyValues("res/aGenderDemo.properties", "=");
		} catch (Exception e) {
			error(e);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new SBCDemo();
	}
}
