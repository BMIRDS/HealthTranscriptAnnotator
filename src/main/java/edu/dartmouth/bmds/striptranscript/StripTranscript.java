package edu.dartmouth.bmds.striptranscript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class StripTranscript {
	public void createStrippedTranscript(File transcriptInputFolder, File transcriptOutputFolder) throws FileNotFoundException, IOException {
		BufferedReader br = null;
		FileWriter fw = null;
		String outputFolderPath = transcriptOutputFolder.getAbsolutePath();


		try {
			File[] transcriptInputFolderDirectoryListing = transcriptInputFolder.listFiles();
			if (transcriptInputFolderDirectoryListing != null) {
				
				// For each transcript file in transcript input folder
				for (File transcriptFile : transcriptInputFolderDirectoryListing) {
					br = new BufferedReader(new FileReader(transcriptFile));
					String fileNameSplit = transcriptFile.getName().split("\\.")[0];
					String strippedFileName = outputFolderPath + "/" + fileNameSplit + "_stripped.txt";
					
					fw = new FileWriter(strippedFileName);

					System.out.println(strippedFileName);
					
				    for(String line; (line = br.readLine()) != null; ) {
				    		String lineCopy = line + "\n";
				    		System.out.println(line);
				    		if (line.length() > 1) {
				    			if (Character.isDigit(line.charAt(0))) {
				    				String[] lineSplit = line.split("N:");
				    				if (lineSplit.length == 1) {
				    					lineSplit = line.split("T:");
				    				}
				    				
				    				String lineStart = lineSplit[0];
				    				String lineEnd = "";
				    				if (lineSplit.length > 1) {
					    				lineEnd = lineSplit[1]; 
				    				}
				    				
			    					String lineStartCopy = "";
				    				for (int i = 0 ; i < lineStart.length() + 3;) {
				    					lineStartCopy = lineStartCopy + " ";
				    					i += 1;
				    				}
				    				lineCopy = lineStartCopy + lineEnd;
				    
				    			}

				    		}
				    		
				    		fw.write(lineCopy);
				    	
				    }
					
				    br.close();
				    fw.close();
				}
			}
		}
		finally {
			if (br != null) {
				br.close();
			}
			
			if (fw != null) {
				fw.close();
			}
			
		}
	}

}
