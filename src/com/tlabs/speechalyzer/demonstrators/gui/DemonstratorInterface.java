package com.tlabs.speechalyzer.demonstrators.gui;

import javax.sound.sampled.AudioFormat;
import javax.swing.JFrame;

public interface DemonstratorInterface {
	abstract public void setNoiseLevel(int level);

	abstract public void setInitialTimeout(double timeout);

	abstract public AudioFormat getAudioFormat();

	abstract public void switchMode();
	
	abstract public void exit();
	
	abstract public void setAudioMode(int mode);
	abstract public void setShowNonAnger(boolean showNonAnger);
	abstract public void setResolution(String resolution);
	abstract public void setAudioLogging(boolean audioLogging);
	public abstract String getLastResult();
	public abstract JFrame getFrame();	
}
