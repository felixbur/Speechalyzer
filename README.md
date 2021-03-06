﻿Speechalyzer: a tool for the daily work of a 'speech worker'.
 
 For a first glance you [might look at this presentation](https://github.com/felixbur/Speechalyzer/blob/master/docs/Labeltool_Usage.pdf)
 
 I wrote a [blog post on how to install Speechalyzer and Labeltool](http://blog.syntheticspeech.de/2021/05/05/how-to-install-speechalyzer-labeltool/)

NOTE: if you want to label data, you probably will need to use the 'Labeltool' project as well.
it can be installed/downloaded here https://github.com/felixbur/Labeltool


It is optimized to process large speech data sets with respect to transcription, labeling and annotation. 
It is implemented as a client server based framework in Java and interfaces software for speech recognition,
synthesis, speech classification and quality evaluation. 
The application is mainly the processing of training data for speech recognition and classification models and performing benchmarking tests on speech-to-text, text-to-speech and speech classification software systems.
We also used it for listening experiments (labels can be hidden).

There's a paper describing Speechalyzer:
F. Burkhardt: Fast Labeling and Transcription with the Speechalyzer Toolkit, Proc. LREC, 2012

Speechalyzer is divided into two projects.
1. Server: Speechalyzer.jar is the server program and maintains the audio files and does batch processing.
2. Client: Labeltool.jar is a client program to use a GUI for labeling and annotation.

USAGE:
Here's what you would to to label/annotate some speech files.
1.) adapt the res/speechalyzer.properties file at least to the audio format
2.) put the audio files in the 'recordings' directory (or provide a file list with pathes)
3.) start the Speechalyzer: java -jar speechalyzer.jar
4.) start the Labeltool: java -jar labeltool.jar
5.) label/annotate the speech files
6.) the things you did will immediately be stored into text files having the same name and path as the audio files


Not included, but with provided interfaces are third party Speech softwares:
Acoustic Feature Extraction
	Praat, openEar, openSmile
Classification:
	WEKA
Text-to-Speech
	Nuance, Svox, Ivona, Mbrola, Mary
Speech-to-Text
	Nuance Speech server
Evaluation
	NIST WER calculation
	
If you want to compile, you will also need the 'FelixUtil' project (beneath other open-source libraries).

Program options:
Speechalyzer version 2.0
Usage: program [-h ] [-cf <String>] [-rd <String>] [-fl <String>] [-fe <String>] [-aft <String>] [-srt <String>] [-port <String>] [-pe ] [-pm ] [-pp ] [-pf ] [-pl ] [-pi ] [-pt ] [-pnt ] [-pc ] [-prtl ] [-al ] [-at ] [-ar ] [-gw ] [-rl ] [-wer ] [-sclite ] [-mixAll ] [-removeAnnotationFiles ] [-classify ] [-train ] [-noExtract ] [-eval ] [-removeLabels ] [-removePreds ] 

	-h 'print usage'  Default: false
	-cf 'configuration file'  Default: res/speechalyzer.properties
	-rd 'directory with recordings'  Default: 
	-fl '<textlist with audiofiles>.
		Format: filePath label_1 label_2 ... label_i'  Default: 
	-fe '<EmotionML document>.
		Format: XML'  Default: 
	-aft 'Set audio file type, e.g. wav or pcm'  Default: 
	-srt 'Set audio sample rate, e.g. 8000 or 16000'  Default: 
	-port 'Set port number, e.g. 6666'  Default: 
	-pe 'Print evaluation format to stdout.
		Format: filepath <string label> <prediction category>.
'  Default: false
	-pm 'Print EmotionML. All recordings with labels get printed out.
'  Default: false
	-pp 'Print prediction to stdout.
		Format: filepath <prediction category>.
'  Default: false
	-pf 'Prints file info to stdout.
		Format: filepath size'  Default: false
	-pl 'Prints labels to stdout.
		Format: filepath label_1 label_2...label_i'  Default: false
	-pi 'Prints labels as integers to stdout.
		Format: filepath label_1 label_2...label_i'  Default: false
	-pt 'Prints transcriptions to stdout.
		Format: filepath _transcript'  Default: false
	-pnt 'Prints files without transcriptions to stdout.
		Format: filepath'  Default: false
	-pc 'Prints categories to stdout.
		Format: filepath filesize C_all C_l1 C_l2...C_li
		(C=category).'  Default: false
	-prtl 'Prints transcriptions and labels to stdout (if BOTH exist).
		Format: <filepath> <transcript> <label>
		example: recs/rec.wav "bla bla" -3'  Default: false
	-al 'Adds labels from textlist.
		Format: filepath label_1...label_n'  Default: false
	-at 'Adds transcriptions from textlist.
		Format: filepath transcript_1...transcript_n'  Default: false
	-ar 'Adds recognition results from textlist.
		Format: filepath recognized word_1...recognized word_n'  Default: false
	-gw 'Generate (syntheseize) wav-files in textlist according to transcriptions'  Default: false
	-rl 'Replaces/adds given labels in textlist to all files'  Default: false
	-wer 'compute word error rate for loaded audio files (must be transcribed and recognized)'  Default: false
	-sclite 'sclite wer comuptation option'  Default: false
	-mixAll 'Mix sound to all files'  Default: false
	-removeAnnotationFiles 'Removes all annotation files 
		(containing transcription and labels)'  Default: false
	-classify 'Classifiy all files.'  Default: false
	-train 'Train a model from all files.'  Default: false
	-noExtract 'If set, features will not be extracted before model training.'  Default: false
	-eval 'Evaluate given list internally (samples MUST have associated annotation files with labels and predictions)'  Default: false
	-removeLabels 'Removes all labels for files given in textlist'  Default: false
	-removePreds 'Removes all predictions for files given in textlist'  Default: false
