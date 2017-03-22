package com.tlabs.speechalyzer.demonstrators.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.felix.util.AudioUtil;
import com.felix.util.DateTimeUtil;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.SwingUtil;
import com.felix.util.Util;
import com.felix.util.logging.Log4JLogger;
import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.Constants;
import com.tlabs.speechalyzer.RecFile;
import com.tlabs.speechalyzer.classifier.ClassificationResult;
import com.tlabs.speechalyzer.classifier.IClassifier;
import com.tlabs.speechalyzer.featureextract.IExtractor;

public class DemonstratorConfigurator extends JFrame implements ActionListener,
		ItemListener {
	private KeyValues _config;
	private DemonstratorInterface _demonstrator;
	private Log4JLogger _logger;
	private AudioFormat _format;
	private int _calibrationTime = 0, _maxVal = 0, _sampleRate;
	private double _initialTimeout = 0;
	private JTextField _calibrationResult, _initialTimeoutLabel;
	private JComboBox _noiseLevelCB, _initialTimeoutCB, _resolutionCB,
			_audioModeCB;
	private JCheckBox _noWindowDecorationCB, _audioLoggingCB, _showNonAngerCB;
	private IClassifier _classifier;
	private IExtractor _featExtractor;
	private String _modelName;
	private String _resolution;
	private JLabel _modelNameLab, _statusLabel;
	private AudioFileManager _afm;
	private String _guiNoiseLevelLabel = "", _guiNoiseLevelButton = "",
			_guiAudioTestButton = "", _guiTimeoutLabel = "",
			_guiTimeoutButton = "", _guiSecreenResolutionLabel = "",
			_guiSwitchModeButton = "", _guiDoneButton = "",
			_guiTrainModelButton = "", _guiLoadModelButton = "",
			_guiResetModelButton = "", _guiMakeBackupButton = "",
			_guiDeleteRecordingsButton = "", _guiZipRecordingsButton = "",
			_guiTitleStart = "", _guiUndoLastStorageButton = "",
			_guiStoreConfigurationButton = "", _guiChangeAudioModeLabel = "",
			_guiAudioChangeModePushToTalk = "",
			_guiAudioChangeModePushToActivate = "",
			_guiAudioChangeModePermanentRecording = "", _selectAudioMode = "",
			_guiExitButton = "", _guiToggleHideButton="";
	private boolean _noWindowDecorated,  _audioLogging, _showNonAnger, _demoHidden=false;

	public DemonstratorConfigurator(KeyValues config,
			DemonstratorInterface demonstrator, IClassifier classifier,
			IExtractor featureExtractor, AudioFileManager afm) {
		super("Configurator");
		_logger = new Log4JLogger(Logger
				.getLogger("com.tlabs.speechalyzer.demonstrators.gui.DemonstratorConfiguration"));
		_config = config;
		_demonstrator = demonstrator;
		_resolution = _config.getString("resolution");
		_sampleRate = _config.getInt("sampleRate");
		_noWindowDecorated = _config.getBool("noWindowDecoration");
		_format = _demonstrator.getAudioFormat();
		_calibrationTime = _config.getInt("calibrationTime");
		_classifier = classifier;
		_modelName = _config.getString("modelFile");
		_featExtractor = featureExtractor;
		_afm = afm;
		_maxVal = _config.getInt("silenceThreshold");
		_initialTimeout = _config.getDouble("initialTimeout");
		_audioLogging = _config.getBool("audioLogging");
		_showNonAnger = _config.getBool("showNonAnger");
		_guiExitButton = _config.getString("gui.exit.button");
		_guiNoiseLevelLabel = _config.getString("gui.noiseLevel.label");
		_guiNoiseLevelButton = _config.getString("gui.noiseLevel.button");
		_guiAudioTestButton = _config.getString("gui .audioTest.button");
		_guiTimeoutLabel = _config.getString("gui.timeout.label");
		_guiTimeoutButton = _config.getString("gui.timeout.button");
		_guiSecreenResolutionLabel = _config
				.getString("gui.secreenResolution.label");
		_guiSwitchModeButton = _config.getString("gui.switchMode.button");
		_guiToggleHideButton = _config.getString("gui.toggleHide.button");
		_guiDoneButton = _config.getString("gui.done.button");
		_guiTrainModelButton = _config.getString("gui.trainModel.button");
		_guiLoadModelButton = _config.getString("gui.loadModel.button");
		_guiResetModelButton = _config.getString("gui.resetModel.button");
		_guiMakeBackupButton = _config.getString("gui.makeBackup.button");
		_guiDeleteRecordingsButton = _config
				.getString("gui.deleteRecordings.button");
		_guiZipRecordingsButton = _config.getString("gui.zipRecordings.button");
		_guiTitleStart = _config.getString("gui.title.start");
		_guiUndoLastStorageButton = _config
				.getString("gui.undoLastStorage.button");
		_guiStoreConfigurationButton = _config
				.getString("gui.storeConfiguration.button");

		_guiChangeAudioModeLabel = _config
				.getString("gui.changeAudioMode.label");
		_guiAudioChangeModePushToTalk = _config
				.getString("gui.changeAudioMode.pushToTalk");
		_guiAudioChangeModePushToActivate = _config
				.getString("gui.changeAudioMode.pushToActivate");
		_guiAudioChangeModePermanentRecording = _config
				.getString("gui.changeAudioMode.permanentRecording");
		if (_config.getBool("pushToTalk")) {
			_selectAudioMode = _guiAudioChangeModePushToTalk;
		} else if (_config.getBool("permanentRecording")) {
			_selectAudioMode = _guiAudioChangeModePermanentRecording;
		} else {
			_selectAudioMode = _guiAudioChangeModePushToActivate;
		}

		getContentPane().add(makeMainPanel());
	}

	JPanel makeMainPanel() {
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.add(makeTitlePane());
		pane.add(makeCalibrationPane());
		if (_config.getBool("switchAudioMode"))
			pane.add(makeInitialTimeoutPane());
		pane.add(makeClassifierPane());
		pane.add(makeRecordingsPane());
		pane.add(makeResolutionPane());
		if (_config.getBool("switchAudioMode"))
			pane.add(makeAudioModePane());
		pane.add(makeUndoPanel());
		pane.add(makeStatusPanel());
		pane.add(makeBottomPanel());
		return pane;
	}

	JPanel makeTitlePane() {
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		JLabel label = new JLabel(_guiTitleStart + Constants.version
				+ _config.getString("titleString"));
		label.setForeground(Color.white);
		label.setBackground(Color.black);
		pane.add(label);
		return pane;
	}

	JPanel makeBottomPanel() {
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		pane.add(SwingUtil.makeButton(_guiExitButton, this));
		pane.add(SwingUtil.makeButton(_guiSwitchModeButton, this));
		JButton exitButton = SwingUtil.makeButton(_guiDoneButton, this);
		pane.add(exitButton);
		return pane;
	}

	JPanel makeStatusPanel() {
		JPanel pane = new JPanel();
		_statusLabel = new JLabel("  ");
		_statusLabel.setForeground(Color.white);
		_statusLabel.setBackground(Color.black);
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		pane.add(_statusLabel);
		return pane;
	}

	JPanel makeUndoPanel() {
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		pane.add(SwingUtil.makeButton(_guiUndoLastStorageButton, this));
		pane.add(SwingUtil.makeButton(_guiStoreConfigurationButton, this));
		_noWindowDecorationCB = new JCheckBox(_config
				.getString("gui.noWindowDecoration.label"));
		_noWindowDecorationCB.setForeground(Color.white);
		_noWindowDecorationCB.setBackground(Color.black);
		_noWindowDecorationCB.setSelected(_noWindowDecorated);
		_noWindowDecorationCB.addItemListener(this);
		pane.add(_noWindowDecorationCB);
		return pane;
	}

	JPanel makeCalibrationPane() {
		JButton startCalibrationButton = SwingUtil.makeButton("audio test",
				this);
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		String[] noiseLevels = _config.getStringArray("gui.noiseLevels", " ");
		_noiseLevelCB = new JComboBox(noiseLevels);
		_noiseLevelCB.setSelectedItem(String.valueOf(_maxVal));
		_noiseLevelCB.addActionListener(this);
		JLabel label = new JLabel(_guiNoiseLevelLabel);
		label.setForeground(Color.white);
		label.setBackground(Color.black);
		_calibrationResult = new JTextField(6);
		_calibrationResult.setText(String.valueOf(_maxVal));
		JButton setCalibrationButton = SwingUtil.makeButton(
				_guiNoiseLevelButton, this);
		pane.add(label);
		pane.add(_calibrationResult);
		pane.add(setCalibrationButton);
		pane.add(_noiseLevelCB);
		pane.add(startCalibrationButton);
		return pane;
	}

	JPanel makeInitialTimeoutPane() {
		JLabel label = new JLabel(_guiTimeoutLabel);
		label.setForeground(Color.white);
		label.setBackground(Color.black);
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		String[] timeOuts = _config.getStringArray("gui.timeOuts", " ");
		_initialTimeoutCB = new JComboBox(timeOuts);
		_initialTimeoutCB.setSelectedIndex(1);
		_initialTimeoutCB.addActionListener(this);
		_initialTimeoutLabel = new JTextField(6);
		_initialTimeoutLabel.setText(String.valueOf(_initialTimeout));
		JButton ib = SwingUtil.makeButton(_guiTimeoutButton, this);
		pane.add(label);
		pane.add(_initialTimeoutLabel);
		pane.add(ib);
		pane.add(_initialTimeoutCB);
		return pane;
	}

	JPanel makeAudioModePane() {
		JLabel label = new JLabel(_guiChangeAudioModeLabel);
		label.setForeground(Color.white);
		label.setBackground(Color.black);
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		String[] vals = new String[] { _guiAudioChangeModePushToTalk,
				_guiAudioChangeModePushToActivate,
				_guiAudioChangeModePermanentRecording };
		_audioModeCB = new JComboBox(vals);
		_audioModeCB.setSelectedItem(_selectAudioMode);
		_audioModeCB.addActionListener(this);
		pane.add(label);
		pane.add(_audioModeCB);
		return pane;
	}

	JPanel makeClassifierPane() {
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		JPanel upperPane = new JPanel();
		upperPane.setForeground(Color.white);
		upperPane.setBackground(Color.black);

		JPanel lowerPane = new JPanel();
		lowerPane.setForeground(Color.white);
		lowerPane.setBackground(Color.black);
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		upperPane.add(SwingUtil.makeButton(_guiTrainModelButton, this));
		upperPane.add(SwingUtil.makeButton(_guiLoadModelButton, this));
		upperPane.add(SwingUtil.makeButton(_guiResetModelButton, this));
		upperPane.add(SwingUtil.makeButton(_guiMakeBackupButton, this));
		JLabel label = new JLabel("model: ");
		label.setForeground(Color.white);
		label.setBackground(Color.black);
		_modelNameLab = new JLabel(" " + _classifier.getModelFileName());
		_modelNameLab.setForeground(Color.white);
		_modelNameLab.setBackground(Color.black);
		lowerPane.add(label);
		lowerPane.add(_modelNameLab);
		pane.add(upperPane);
		pane.add(lowerPane);
		return pane;
	}

	JPanel makeRecordingsPane() {
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		pane.add(SwingUtil.makeButton(_guiZipRecordingsButton, this));
		pane.add(SwingUtil.makeButton(_guiDeleteRecordingsButton, this));
		pane.add(SwingUtil.makeButton(_guiToggleHideButton, this));
		return pane;
	}

	private JPanel makeResolutionPane() {
		JLabel label = new JLabel(_guiSecreenResolutionLabel);
		label.setForeground(Color.white);
		label.setBackground(Color.black);
		JPanel pane = new JPanel();
		pane.setForeground(Color.white);
		pane.setBackground(Color.black);
		String[] choice = _config.getStringArray("gui.screenDimensions", " ");
		_resolutionCB = new JComboBox(choice);
		_resolutionCB.setSelectedItem(_resolution);
		_resolutionCB.addActionListener(this);
		pane.add(label);
		pane.add(_resolutionCB);
		_showNonAngerCB = new JCheckBox(_config
				.getString("gui.showNonAnger.label"));
		_showNonAngerCB.setForeground(Color.white);
		_showNonAngerCB.setBackground(Color.black);
		_showNonAngerCB.setSelected(_showNonAnger);
		_showNonAngerCB.addItemListener(this);
		pane.add(_showNonAngerCB);
		_audioLoggingCB = new JCheckBox(_config
				.getString("gui.audioLogging.label"));
		_audioLoggingCB.setForeground(Color.white);
		_audioLoggingCB.setBackground(Color.black);
		_audioLoggingCB.setSelected(_audioLogging);
		_audioLoggingCB.addItemListener(this);
		pane.add(_audioLoggingCB);
		return pane;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().compareTo(_guiAudioTestButton) == 0) {
			calibrate();
		}
		if (e.getActionCommand().compareTo(_guiNoiseLevelButton) == 0) {
			setCalibrate();
		}
		if (e.getActionCommand().compareTo(_guiTimeoutButton) == 0) {
			setTimeout();
		} else if (e.getActionCommand().compareTo(_guiDoneButton) == 0) {
			dispose();
		} else if (e.getActionCommand().compareTo(_guiTrainModelButton) == 0) {
			trainModel();
		} else if (e.getActionCommand().compareTo(_guiLoadModelButton) == 0) {
			loadModel();
		} else if (e.getActionCommand().compareTo(_guiResetModelButton) == 0) {
			loadBackupModel();
		} else if (e.getActionCommand().compareTo(_guiMakeBackupButton) == 0) {
			makeBackup();
		} else if (e.getActionCommand().compareTo(_guiDeleteRecordingsButton) == 0) {
			File recDir = _config.getFileHandler("recordingDir");
			try {
				FileUtil.deleteDir(recDir);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (e.getActionCommand().compareTo(_guiZipRecordingsButton) == 0) {
			zipRecordings();
		} else if (e.getActionCommand().compareTo(_guiSwitchModeButton) == 0) {
			_demonstrator.switchMode();
		} else if (e.getActionCommand().compareTo(_guiUndoLastStorageButton) == 0) {
			undoLastStorage();
		} else if (e.getActionCommand().compareTo(_guiStoreConfigurationButton) == 0) {
			_config.fileStoreWithComments();
			_logger.info("saved configuration to file");
		} else if (e.getActionCommand().compareTo(_guiExitButton) == 0) {
			_demonstrator.exit();
		} else if (e.getActionCommand().compareTo(_guiToggleHideButton) == 0) {
			if (_demoHidden) {
			_demonstrator.getFrame().show();	
			_demoHidden = false;
			}else {
			_demonstrator.getFrame().hide();
			_demoHidden = true;
			}
		}
		if (e.getSource() == _noiseLevelCB) {
			setNoiseLevelFromCB();
		} else if (e.getSource() == _initialTimeoutCB) {
			setTimeoutFromCB();
		} else if (e.getSource() == _resolutionCB) {
			_demonstrator.setResolution(((String) _resolutionCB
					.getSelectedItem()).trim());
			_resolution = ((String) _resolutionCB.getSelectedItem()).trim();
			_config.setValue("resolution", _resolution);
		} else if (e.getSource() == _audioModeCB) {
			switchAudioMode();
		}
	}

	private void zipRecordings() {
		Runnable doIt = new Runnable() {
			public void run() {
				String recDir = _config.getString("recordingDir");
				String fileName = DateTimeUtil.getDateSortableName()
						+ "_recordings.zip";
				String cmd = _config.getString("zipCommand") + " " + fileName
						+ " " + recDir;
				try {
					Util.execCmd(cmd, _logger);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		new Thread(doIt).start();
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if (source == _noWindowDecorationCB) {
			setWindowDecoration();
		} else if (source == _audioLoggingCB) {
			setAudioLogging();
		} else if (source == _showNonAngerCB) {
			setShowNonAnger();
		}
	}

	private void setWindowDecoration() {
		if (_noWindowDecorationCB.isSelected()) {
			_config.setValue("noWindowDecoration", "true");
		} else {
			_config.setValue("noWindowDecoration", "false");
		}
	}

	private void setAudioLogging() {
		_audioLogging = _audioLoggingCB.isSelected();
		if (_audioLogging) {
			_config.setValue("audioLogging", "true");
		} else {
			_config.setValue("audioLogging", "false");
		}
		_demonstrator.setAudioLogging(_audioLogging);
	}

	private void setShowNonAnger() {
		_showNonAnger = _showNonAngerCB.isSelected();
		if (_showNonAnger) {
			_config.setValue("showNonAnger", "true");
		} else {
			_config.setValue("showNonAnger", "false");
		}
		_demonstrator.setShowNonAnger(_showNonAnger);
	}

	private void switchAudioMode() {
		String am = (String) _audioModeCB.getSelectedItem();
		if (_selectAudioMode.compareTo(am) != 0) {
			_selectAudioMode = am;
			if (am.compareTo(_guiAudioChangeModePushToTalk) == 0) {
				_demonstrator.setAudioMode(SBCDemo_Old.AUDIOMODE_PUSH_TO_TALK);
				_config.setValue("pushToTalk", "true");
				_config.setValue("permanentRecording", "false");
			} else if (am.compareTo(_guiAudioChangeModePushToActivate) == 0) {
				_demonstrator
						.setAudioMode(SBCDemo_Old.AUDIOMODE_PUSH_TO_ACTIVATE);
				_config.setValue("pushToTalk", "false");
				_config.setValue("permanentRecording", "false");
			} else if (am.compareTo(_guiAudioChangeModePermanentRecording) == 0) {
				_demonstrator
						.setAudioMode(SBCDemo_Old.AUDIOMODE_PERMANENT_RECORDING);
				_config.setValue("pushToTalk", "false");
				_config.setValue("permanentRecording", "true");
			}
		}
	}

	private void undoLastStorage() {
		_afm.removeLastRecording();
		_logger.info("removed last recording");
	}

	private void trainModel() {
		_statusLabel.setText("training...");
		Runnable runner = new Runnable() {
			public void run() {
				_afm.reload();
				_featExtractor.extractAllFeatures(_afm, _config
						.getBool("additiveTraining"));
				_classifier.trainModel();
				if (_config.getBool("judgeAfterTrain")) {
					// judge files so they won't get extracted again
					for (Iterator<RecFile> iter = _afm
							.getAudioFilesWithoutPredictions().iterator(); iter
							.hasNext();) {
						RecFile recFile = (RecFile) iter.next();
						_featExtractor.extractFeatures(recFile._file
								.getAbsolutePath());
						ClassificationResult cr = _classifier.classify();
						recFile.storePred(cr);
						_afm.updateAudioFile(recFile._path);
					}
				}
				trainingFinished();
			}
		};
		Thread captureThread = new Thread(runner);
		captureThread.start();
	}

	public void trainingFinished() {
		_statusLabel.setText("done");
	}

	private void loadModel() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File(_config
				.getPathValue("ressourceDir")));
		chooser.setDialogTitle("choose model");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			_modelName = chooser.getSelectedFile().getAbsolutePath();
		} else {
			return;
		}
		_modelNameLab.setText(new File(_modelName).getName());
		_classifier.loadModel(_modelName);

	}

	private void loadBackupModel() {
		if (JOptionPane.showConfirmDialog(this, _config
				.getString("gui.resetModel.question"), "sure",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			File modelBackup = _config.getFileHandler("modelBackupFile");
			File arffBackup = _config.getFileHandler("arffBackupFile");
			File arffTrain = _config.getFileHandler("trainFile");
			File recDir = _config.getFileHandler("recordingDir");
			String modelFile = FileUtil.addNamePart(_config
					.getAbsPath("modelFile"), "_"
					+ _config.getString("classifier"));
			try {
				FileUtil.copyFile(modelBackup, new File(modelFile));
				FileUtil.copyFile(arffBackup, arffTrain);
				FileUtil.deleteDir(recDir);
			} catch (Exception e) {
				e.printStackTrace();
			}
			_modelNameLab.setText(modelFile);
			_classifier.loadModel(modelFile);
			_statusLabel.setText("done");
		}
	}

	private void makeBackup() {
		if (JOptionPane.showConfirmDialog(this, _config
				.getString("gui.makeBackup.question"), "sure",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			File modelBackup = _config.getFileHandler("modelBackupFile");
			File arffBackup = _config.getFileHandler("arffBackupFile");
			File arffTrain = _config.getFileHandler("trainFile");
			File recDir = _config.getFileHandler("recordingDir");
			String modelFile = FileUtil.addNamePart(_config
					.getAbsPath("modelFile"), "_"
					+ _config.getString("classifier"));
			try {
				FileUtil.copyFile(new File(modelFile), modelBackup);
				FileUtil.copyFile(arffTrain, arffBackup);
				FileUtil.deleteDir(recDir);
			} catch (Exception e) {
				e.printStackTrace();
			}
			_statusLabel.setText("done");
		}
	}

	private void setCalibrate() {
		setMaxVal(Integer.parseInt(_calibrationResult.getText()));
	}

	private void setMaxVal(int newVal) {
		_maxVal = newVal;
		_config.setValue("silenceThreshold", String.valueOf(_maxVal));

	}

	private void setNoiseLevelFromCB() {
		_calibrationResult.setText((String) _noiseLevelCB.getSelectedItem());
		setMaxVal(Integer.parseInt(_calibrationResult.getText()));
		_demonstrator.setNoiseLevel(_maxVal);
	}

	private void setTimeout() {
		_initialTimeout = Double.parseDouble(_initialTimeoutLabel.getText());
		_config.setValue("initialTimeout", String.valueOf(_initialTimeout));
		_demonstrator.setInitialTimeout((int) (_initialTimeout * _sampleRate));
	}

	private void setTimeoutFromCB() {
		_initialTimeoutLabel.setText((String) _initialTimeoutCB
				.getSelectedItem());
		_initialTimeout = Double.parseDouble(_initialTimeoutLabel.getText());
		_config.setValue("initialTimeout", String.valueOf(_initialTimeout));
		_demonstrator.setInitialTimeout(_initialTimeout);
	}

	public void listeningFinished() {
		_statusLabel.setText("done");
	}

	private void calibrate() {
		try {
			_logger.info("recording...");
			_statusLabel.setText("listening...");
			DataLine.Info info = new DataLine.Info(TargetDataLine.class,
					_format);
			final TargetDataLine line = (TargetDataLine) AudioSystem
					.getLine(info);
			line.open(_format);
			line.start();
			Runnable runner = new Runnable() {
				public void run() {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int bufferSize = (int) _format.getSampleRate()
							* _format.getFrameSize();
					byte buffer[] = new byte[bufferSize];
					short[] values;
					_maxVal = 0;
					try {
						for (int i = 0; i < _calibrationTime; i++) {
							line.read(buffer, 0, bufferSize);
							values = AudioUtil.byteToShort(buffer, true);
							for (int j = 0; j < values.length; j++) {
								if (values[j] > _maxVal) {
									_maxVal = values[j];
								}
							}
							out.close();
						}
						_calibrationResult.setText(String.valueOf(_maxVal));
						_logger.info("maxval: " + _maxVal);
						_demonstrator.setNoiseLevel(_maxVal);
						setMaxVal(_maxVal);
						listeningFinished();
					} catch (IOException e) {
						e.printStackTrace();
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

	public void setStatusLabel(String text) {
		try {
			_statusLabel.setText(text);
		} catch (Exception e) {
			// don't care
		}

	}

	public void showMainframe() {
		try {
			// UIManager
			// .setLookAndFeel("com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		_statusLabel.setText(_demonstrator.getLastResult());
		setAlwaysOnTop(true);
		setVisible(true);
	}
}
