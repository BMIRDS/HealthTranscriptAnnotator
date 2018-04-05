package edu.dartmouth.bmds.casxmi2knowtator;

import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

class Knowtator {
	
	public void createKnowtator(File transcriptInputFolder, File casXmiInputFolder, File knowtatorXmlOutputFolder) throws FileNotFoundException, IOException, XMLStreamException, ParseException {
		Scanner transcriptScanner = null;
		Scanner casXmiScanner = null;
		
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		XMLStreamWriter xsw = null;
		FileWriter fw = null;
		
		try {			
			// Get list of files in transcript input folder
			File[] transcriptInputFolderDirectoryListing = transcriptInputFolder.listFiles();
			if (transcriptInputFolderDirectoryListing != null) {
				
				// For each transcript file in transcript input folder
				for (File transcriptFile : transcriptInputFolderDirectoryListing) {
					System.out.println(transcriptFile.getName());
					
					if (transcriptFile.exists()) {
						// Create transcript scanner and add text to char buffer
						transcriptScanner = new Scanner(transcriptFile);
						String transcriptString = transcriptScanner.nextLine();
						while (transcriptScanner.hasNextLine()) {
							transcriptString = transcriptString + "\n" + transcriptScanner.nextLine();
						}
						char[] charbuff = transcriptString.toCharArray();

						// Get path of casXMI folder and knowtator output folder
						String casXmiFolderPath = casXmiInputFolder.getAbsolutePath();
						String knowtatorXmlFolderPath = knowtatorXmlOutputFolder.getAbsolutePath();

						// Get name of transcript file and corresponding names for casXMI file and
						// knowtator file
						String transcriptFileName = transcriptFile.getName();
						System.out.println(transcriptFileName);
						String casXmiFileName = casXmiFolderPath + "/" + transcriptFileName + ".xmi";
						System.out.println(casXmiFileName);
						String knowtatorXmlFileName = knowtatorXmlFolderPath + "/" + transcriptFileName
								+ ".knowtator.xml";

						// Create casXMI file and get its creation date of
						File casXmiFile = new File(casXmiFileName);
						
						if (casXmiFile.exists()) {
							String creationDate = formatCreationDate(casXmiFile);

							// Create new knowtator file
							File knowtatorXmlFile = new File(knowtatorXmlFileName);
							knowtatorXmlFile.createNewFile();

							// Create XMLStreamWriter for knowtator file and start document
							fw = new FileWriter(knowtatorXmlFile);
							xsw = xof.createXMLStreamWriter(fw);
							xsw.writeStartDocument("utf-8", "1.0");
							xsw.writeCharacters("\n");
							xsw.writeStartElement("annotations");
							xsw.writeAttribute("textsource", transcriptFileName);

							// Create scanner for casXMI file and scan for Mentions
							casXmiScanner = new Scanner(casXmiFile, "UTF-8");
							casXmiScanner.useDelimiter("<textsem:");

							String line = casXmiScanner.next();

							while (casXmiScanner.hasNext()) {
								line = casXmiScanner.next().toString();

								if (line.contains("Mention xmi:")) {
									int index = line.indexOf(">");

									String resultLine = line.substring(0, index);

									// Get necessary text to create knowtator annotation from line
									ArrayList<String> textArray = getTexts(resultLine, charbuff, creationDate);

									// Make annotation from line
									annotate(xsw, textArray);
								}
							}

							// End document
							xsw.writeCharacters("\n");
							xsw.writeEndElement();
							//System.out.println("writeEndElement() called");
							
							xsw.flush();
							fw.flush();
							xsw.close();
							fw.close();
							xsw = null;
							fw = null;
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		finally {
			//System.out.println("finally block entered");
			if (casXmiScanner!= null) {
				casXmiScanner.close();
			}
					
			if (xsw != null) {
				xsw.flush();
				xsw.close();
			}
			
			if (fw != null) {
				fw.flush();
				fw.close();
			}
			
			if (transcriptScanner != null) {
				transcriptScanner.close();
			}
		}
		//System.out.println("end createKnowtator");

	}
	
	// Method to format creation date of file
	public String formatCreationDate(File inputFile) throws IOException, ParseException {
		Path inputPath = inputFile.toPath();
        BasicFileAttributes attribute = Files.readAttributes(inputPath, BasicFileAttributes.class);
        String creationDate = attribute.creationTime().toString();
        
        DateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        
        Date date = dateParser.parse(creationDate);
        String formattedDate = dateFormatter.format(date);
       
        return formattedDate;
	}
	
	// Method to extract necessary texts from string to create knowtator annotation
	public ArrayList<String> getTexts(String line, char[] charbuff, String creationDate) throws NumberFormatException, IOException {
		ArrayList <String> wordArray = new ArrayList <String> (15);
		
		Pattern p = Pattern.compile("\"([^\"]*)\"");
		Matcher m = p.matcher(line);
		
		wordArray.add(line.split(" ")[0]);
	
		while (m.find()) {
			wordArray.add(m.group(1));
		}
		
		String cTakesInstance = "cTAKES_Instance_" + wordArray.get(1);
		String start = wordArray.get(3);
		String end = wordArray.get(4);
		String spannedText = getSpannedText(charbuff, Integer.parseInt(start), Integer.parseInt(end));
		String mentionClass = wordArray.get(0);

		ArrayList <String> returnArray = new ArrayList <String> (Arrays.asList(cTakesInstance, start, end, spannedText, creationDate, mentionClass));

		return returnArray;
	}
	
	// Helper method for getTexts to get spanned text from corresponding transcript file
	public String getSpannedText(char[] charbuff, int start, int end) throws IOException {
		
		return String.valueOf(charbuff, start, end-start);
	}
	
	// Method to create a knowtator annotation
	public void annotate(XMLStreamWriter xsw, ArrayList<String> textArray) throws XMLStreamException{
		annotation(xsw, textArray);
		classMention(xsw, textArray);
	}
	
	// Helper method for annotate to create annotation portion
	public void annotation(XMLStreamWriter xsw, ArrayList <String> textArray) throws XMLStreamException {
		xsw.writeCharacters("\n\t");
		xsw.writeStartElement("annotation");
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "mention", new String[] {"id"}, new String[] {textArray.get(0)}, null);
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "annotator", new String[] {"id"}, new String[] {"cTAKES_4.0.0"}, "cTAKES");
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "span", new String[] {"start", "end"}, new String[] {textArray.get(1), textArray.get(2)}, null);
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "spannedText", null, null, textArray.get(3));
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "creationDate", null, null, textArray.get(4));
		xsw.writeCharacters("\n\t");
		xsw.writeEndElement();
		
	}
	
	// Helper method for annotate to create classMention portion
	public void classMention(XMLStreamWriter xsw, ArrayList <String> textArray) throws XMLStreamException {
		xsw.writeCharacters("\n\t");
		xsw.writeStartElement("classMention");
		xsw.writeAttribute("id", textArray.get(0));
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "mentionClass", new String[] {"id"}, new String[] {textArray.get(5)}, textArray.get(3));
		xsw.writeCharacters("\n\t");
		xsw.writeEndElement();
	}
	
	// Helper method for annotation and classMention to write annotation
	public void xmlWrite(XMLStreamWriter xsw, String startElement, String[] attributes, String values[], String characters) throws XMLStreamException {
		xsw.writeStartElement(startElement);
		
		if (attributes != null) {
			for (int i = 0; i < attributes.length; i++) {
				xsw.writeAttribute(attributes[i], values[i]);
			}
		}
	
		if (characters != null) {
			xsw.writeCharacters(characters);
		}
		xsw.writeEndElement();
	}

	
}


