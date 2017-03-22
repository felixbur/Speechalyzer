package com.tlabs.speechalyzer.featureextract;

import java.io.File;

import com.tlabs.speechalyzer.AudioFileManager;
import com.tlabs.speechalyzer.RecFile;


public interface IExtractor {
	public abstract String getInfo();
	public abstract void extractAllFeatures(AudioFileManager afm, boolean append);
	public abstract void extractFeatures(String testFileName);
}
