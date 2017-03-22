package com.tlabs.speechalyzer.synthesizer;

public interface ISynthesizer {
	public void synthesize(String intext, String outfile);
	public void synthesize(String intext, String outfile, String sex);
	public void synthesize(String intext, String outfile, String sex, String voice);
	public void synthesize(String intext, String outfile, boolean female, String langauge, String voice);
	public void synthesize(String intext, String outfile, String emotion, String sex, String voice);
}