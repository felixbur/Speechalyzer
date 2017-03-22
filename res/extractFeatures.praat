# Praat script "AngerExtraction.praat"
# Extraction of high information-gain features for acoustic anger classification
# scripted by: 
# Tim Polzehl 
# Deutsche Telekom  Laboratories / Tu-Berlin,
# Ernst Reuter Platz 7, D - 10587 Berlin, Germany
#
# initial script 15.Dez 2009
#
# The script reads a file and throws the features onto the command line
# the arguments can be cought from there
#
# acoustic processing: 
# 1. segmentation v/uv
# 2. extract mfcc and loudness from v parts only
# 4. apply statsistics and derivations 
#
# Version 0.9 15.Dez
# outputParseString
# time;loudness_max;loudness_min;loudness_mean;loudness_std;loudness_median;loudness_iqr;loudness_D_max;loudness_D_min;loudness_D_mean;loudness_D_std;loudness_D_median;loudness_D_iqr;loudness_DD_max;loudness_DD_min;loudness_DD_mean;loudness_DD_std;loudness_DD_median;loudness_DD_iqr;mfcc_0_max;mfcc_0_min;mfcc_0_mean;mfcc_0_std;mfcc_0_median;mfcc_0_iqr;mfcc_0_D_max;mfcc_0_D_min;mfcc_0_D_mean;mfcc_0_D_std;mfcc_0_D_median;mfcc_0_D_iqr;mfcc_1_max;mfcc_1_min;mfcc_1_mean;mfcc_1_std;mfcc_1_median;mfcc_1_iqr;mfcc_1_D_max;mfcc_1_D_min;mfcc_1_D_mean;mfcc_1_D_std;mfcc_1_D_median;mfcc_1_D_iqr;mfcc_5_max;mfcc_5_min;mfcc_5_mean;mfcc_5_std;mfcc_5_median;mfcc_5_iqr;mfcc_5_D_max;mfcc_5_D_min;mfcc_5_D_mean;mfcc_5_D_std;mfcc_5_D_median;mfcc_5_D_iqr;mfcc_8_max;mfcc_8_min;mfcc_8_mean;mfcc_8_std;mfcc_8_median;mfcc_8_iqr;mfcc_8_D_max;mfcc_8_D_min;mfcc_8_D_mean;mfcc_8_D_std;mfcc_8_D_median;mfcc_8_D_iqr;mfcc_9_max;mfcc_9_min;mfcc_9_mean;mfcc_9_std;mfcc_9_median;mfcc_9_iqr;mfcc_9_D_max;mfcc_9_D_min;mfcc_9_D_mean;mfcc_9_D_std;mfcc_9_D_median;mfcc_9_D_iqr;mfcc_10_max;mfcc_10_min;mfcc_10_mean;mfcc_10_std;mfcc_10_median;mfcc_10_iqr;mfcc_10_D_max;mfcc_10_D_min;mfcc_10_D_mean;mfcc_10_D_std;mfcc_10_D_median;mfcc_10_D_iqr;mfcc_11_max;mfcc_11_min;mfcc_11_mean;mfcc_11_std;mfcc_11_median;mfcc_11_iqr;mfcc_11_D_max;mfcc_11_D_min;mfcc_11_D_mean;mfcc_11_D_std;mfcc_11_D_median;mfcc_11_D_iqr;mfcc_12_max;mfcc_12_min;mfcc_12_mean;mfcc_12_std;mfcc_12_median;mfcc_12_iqr;mfcc_12_D_max;mfcc_12_D_min;mfcc_12_D_mean;mfcc_12_D_std;mfcc_12_D_median;mfcc_12_D_iqr;mfcc_13_max;mfcc_13_min;mfcc_13_mean;mfcc_13_std;mfcc_13_median;mfcc_13_iqr;mfcc_13_D_max;mfcc_13_D_min;mfcc_13_D_mean;mfcc_13_D_std;mfcc_13_D_median;mfcc_13_D_iqr;mfcc_14_max;mfcc_14_min;mfcc_14_mean;mfcc_14_std;mfcc_14_median;mfcc_14_iqr;mfcc_14_D_max;mfcc_14_D_min;mfcc_14_D_mean;mfcc_14_D_std;mfcc_14_D_median;mfcc_14_D_iqr;mfcc_15_max;mfcc_15_min;mfcc_15_mean;mfcc_15_std;mfcc_15_median;mfcc_15_iqr;mfcc_15_D_max;mfcc_15_D_min;mfcc_15_D_mean;mfcc_15_D_std;mfcc_15_D_median;mfcc_15_D_iqr;


# select all
# Remove

form 
	text source
	text label
	text dest
endform
# dest$ = "D:\Tools\SBC\TMO_Anger\SBC_Anger_Features_test_neu.txt"
# source$ = "d:\Projekte\Databases\AIBO\Train\Train_01_042_00.wav"

Read from file... 'source$'
soundID = selected("Sound",-1)
name$ = selected$("Sound")

timestep = 0.015
maxfreq = 3400
winlen = 0.03

# Cochleagram settings (excitation)
fresoBarc = 1.0
forwmask = 0.02

# MFCC settings
nCC = 16
# following values are [300Hz - 3.4kHz] in MEL:
firstFilter = 400
distFilter = 100
maxFilter = 2000



# --------------------------
# 1. Segmentation - vuv Grid
# --------------------------

   select soundID
   To Pitch... 'timestep' 75 600
   To PointProcess
   To TextGrid (vuv)... 'winlen'/2.0 0.01
   vuvGridID = selected ("TextGrid", -1)
   nSegments = Get number of intervals... 1

   count = 1	
   for col from 1 to nSegments
	select vuvGridID
	lab$ = Get label of interval... 1 col
	if lab$ == "V"
		# printline ...extracting 'count'. segment ('col'/'nSegments' ['lab$'])
		vStart'count' = Get starting point... 1 'col'
		# vStart'count' = vStart'count' - winlen/2.0
		vEnd'count' = Get end point... 1 'col'
		# vEnd'count' = vEnd'count' + winlen/2.0

		if count = 1
			select soundID
		        Extract part... vStart1 vEnd1 rectangular 1 no
			Rename... voiced

			# save cutting sites for exclusion from derivatives
			cuttingIndex1 = vEnd1 - vStart1		
		else
			select soundID
			Extract part... vStart'count' vEnd'count' rectangular 1 no
			plus Sound voiced
			Concatenate
			Rename... voiced
			
			# save cutting sites for exclusion from derivatives
			tmp = count-1 
			cuttingIndex'count' = cuttingIndex'tmp' + vEnd'count' - vStart'count'			
		endif 
		count +=1;

	endif	
   endfor


   # -----------------------------------------------------------	
   # overwrite soundID (whole utterance) with voiced sounds only
   select Sound voiced
   soundID = selected ("Sound", -1)
   t1 = Get start time
   t2 = Get end time
   ncols = (t2 - t1) / timestep + 1


# ----------------
# Extract MFCC
# ----------------

   select soundID
   To MFCC... 'nCC' 'winlen' 'timestep' 'firstFilter' 'distFilter' 'maxFilter' 
   To Matrix
   mfccMatrixID = selected ("Matrix", -1)


# ----------------
# Extract Loudness 
# ----------------

   select soundID
   To Cochleagram... 'timestep' 'fresoBarc' 'winlen' 'forwmask'
   cochID = selected ("Cochleagram", -1)
   
   nColsLoudness = floor( (t2-t1 - winlen)/timestep )
   loudnessMatrixID = Create simple Matrix... loudness 1 nColsLoudness 0
   t = winlen/2.0
	
   for col from 1 to nColsLoudness
      select 'cochID'
      To Excitation (slice)... 't'
      y = Get loudness
      select loudnessMatrixID
      Set value... 1 'col' 'y'
      t += timestep
      if col>980
		tmp = selected ("Excitation", -1)    
		Remove... tmp
      endif
   endfor


# ------------------
# Apply Statistics
# ------------------
	# stats are calculated on Intensity cast

# Loudness
   # means 6; max 3, min 0, std 8, reg. 2, median 4 , iqr 7
   # D 12; DD 5 --> (2nd Derivatives to be skipped if necessary)
   # DCT 31, coeff 1-3 predominant
	
   # Features on original contour
   select loudnessMatrixID
   To Intensity
   statsID = selected("Intensity", -1)
   
   # Features on original contour
   time_total = Get total duration
   time_total = time_total*timestep

   # throw on Command Window
   print 'time_total:3', 
 
   # save to file
   fileappend 'dest$' 'time_total:3', 

   call stats
   
   # Features on 1st derivation 
   select loudnessMatrixID   
   call derive
   loudnessDMatrix = selected("Matrix", -1)
   #call clearCuttingSites
   To Intensity
   call stats
   # Features on 2nd derivation 
   select loudnessDMatrix
   call derive
   To Intensity
   call stats
   
  
# MFCC
   # mfc coeffs predominant: 0,1,5,8:15 --> (12,13 to be excluded if necessary)
   # means 8; max 18, min 10, std 11 
   # D 5; DD 1 --> 2nd derivation not applied

   select mfccMatrixID 
   To TableOfReal
   mfccTable = selected ("TableOfReal", -1)

   doIncludeCoef = 10
   nIncludeCoef1 = 1 
   nIncludeCoef2 = 2
   nIncludeCoef3 = 6
   nIncludeCoef4 = 9
   nIncludeCoef5 = 10
   nIncludeCoef6 = 11
   nIncludeCoef7 = 12
   nIncludeCoef8 = 13
   nIncludeCoef9 = 14
   nIncludeCoef10 = 15
   
   for rows from 1 to doIncludeCoef
      select mfccTable
      curr = nIncludeCoef'rows'
	  
      Extract row ranges... 'curr'
      To Matrix
      tmp = selected("Matrix", -1)
      To Intensity
      call stats

      # 1st deriv.
      select tmp
      call derive
      To Intensity
      call stats
   endfor
 print 'label$' 'newline$'
 fileappend 'dest$'  'label$' 'newline$'
# printline script done.
# -------

# -------------------
# procedures
# -------------------

   # ----------------
   procedure derive
	orgID = selected("Matrix", -1)
	select orgID

	currCols = Get number of columns
	currRows = Get number of rows

	deltaID = Create simple Matrix... deltas 'currRows' 'currCols'-1 0

        for row from 1 to currRows
	   for col from 1 to currCols-1
	      select orgID
	      y1 = Get value in cell... row 'col'
	      y2 = Get value in cell... row 'col'+1
	   
	      select deltaID
	      Set value... 'row' 'col' 'y2'-'y1'
  	   endfor  
	endfor
   endproc
   # ----------------


   # ----------------
   procedure stats

	# exclude concatenation points from calculation
	# calculate stats
	
        x1 = Get maximum... 0 0 None
		x2 = Get minimum... 0 0 None
        x3 = Get mean... 0 0 energy
        x4 = Get standard deviation... 0 0 
        x5 = Get quantile... 0 0 0.5
		tmp1 = Get quantile... 0 0 0.75 
		tmp2 = Get quantile... 0 0 0.25
		x6 = tmp1-tmp2

	# throw on Command/Info window
	print 'x1:3', 'x2:3', 'x3:3', 'x4:3', 'x5:3', 'x6:3', 
  
	# write to file
	fileappend 'dest$' 'x1:3', 'x2:3', 'x3:3', 'x4:3', 'x5:3', 'x6:3', 
   
   endproc
   # ----------------


   # ----------------
#   procedure clearCuttingSites
#	if count > 1 
#		printline 'count'
#	   	To TableOfReal
#		for cuts from 1 to count-1
#			n = round( cuttingIndex'cuts' / timestep )
#			printline 'n'
#			#Remove column (index)... 'n'
#
#		endfor
#	endif
#   endproc
# ----------------