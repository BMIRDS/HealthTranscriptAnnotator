package edu.dartmouth.bmds.striptranscript;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class StripTrancriptTest {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		StripTranscript st = new StripTranscript();
		
		File transcriptInputFolder = new File("transcriptInputFolder");
		File transcriptOutputFolder = new File("strippedTranscriptOutputFolder");
		st.createStrippedTranscript(transcriptInputFolder, transcriptOutputFolder);
		
	}
		
}
