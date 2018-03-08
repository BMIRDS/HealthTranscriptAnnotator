package knowtator;

import java.io.RandomAccessFile;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

class Knowtator {
	
	public void createKnowtator(String inputFileName, String transcriptFileName) throws FileNotFoundException, IOException, XMLStreamException, ParseException {
		Scanner scan = null;
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		XMLOutputFactory testOF = XMLOutputFactory.newInstance();
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLStreamWriter xsw = null;
		XMLStreamWriter testW = null;
		XMLStreamReader reader = null; 
		RandomAccessFile transcript = null;
		String outputFileName = inputFileName + ".knowtator.xml";
		String testFileName = "test.xml";
		String transcriptString = "";
		
		
		try {
//			Path path = Paths.get(transcriptFileName);
//			Charset charset = StandardCharsets.UTF_8;
//			
//			String content = new String(Files.readAllBytes(path), charset);
//			content = content.replaceAll("\u00AD", "-");
//			Files.write(path, content.getBytes(charset));
			
			File transcriptFile = new File(transcriptFileName);
			Scanner transcriptScanner = new Scanner(transcriptFile);
			transcriptString = transcriptScanner.nextLine();
			while (transcriptScanner.hasNextLine()) {
				transcriptString = transcriptString + "\n" +transcriptScanner.nextLine();
			}
			char[] charbuff = transcriptString.toCharArray();
			
//			int size = transcriptReader.read(charbuff);
//			transcriptReader.skip(1511);
//			int size = transcriptReader.read(charbuff, 0, 7);
//			
//			
//			
//			System.out.println(String.valueOf(charbuff, 0, size));
//			System.out.println(charbuff);
//			
			transcript = new RandomAccessFile(transcriptFileName, "r");
			File inputFile = new File(inputFileName);
			
			String creationDate = formatCreationDate(inputFile);
			
			scan = new Scanner(inputFile, "UTF-8");
			scan.useDelimiter("<textsem:");
			
			testW = testOF.createXMLStreamWriter(new FileWriter(testFileName));
			xsw = xof.createXMLStreamWriter(new FileWriter(outputFileName));
			xsw.writeStartDocument("utf-8", "1.0");
			xsw.writeCharacters("\n");
	        xsw.writeStartElement("annotations");
	        xsw.writeAttribute("textsource", "Chris.txt");
	        
	        
    			String line = scan.next();
    			
	        while (scan.hasNext()) {
	        		line = scan.next().toString();
	        		
	        		if (line.contains("Mention xmi:")) {
	        			int index = line.indexOf(">");
	        			
	        			String resultLine = line.substring(0, index);
	        			
	        			testW.writeCharacters(resultLine);
		        		testW.writeCharacters("\n\n");
		        		ArrayList <String> textArray = getTexts(resultLine, charbuff, creationDate);
		        		
		        		
		        		for (int i = 0; i < textArray.size(); i++)
		        			testW.writeCharacters(textArray.get(i) + " ");
		        		
		        		testW.writeCharacters("\n\n\n");
		        		annotation(xsw, textArray);
		        		classMention(xsw, textArray);
	        		}
	        		
			}
		xsw.writeCharacters("\n");
    		xsw.writeEndElement();
//    		formatXML(outputFileName);
    		
		} finally {
			if (scan != null) {
				scan.close();
			}
			
			if (xsw != null) {
				xsw.flush();
				xsw.close();
			}
			
			if (testW != null) {
				testW.flush();
				testW.close();
			}
			
			if (transcript != null) {
				transcript.close();
			}
		}
	}
	
	
	public void annotation(XMLStreamWriter xsw, ArrayList <String> textArray) throws XMLStreamException {
		xsw.writeCharacters("\n\t");
		xsw.writeStartElement("annotation");
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "mention", new String[] {"id"}, new String[] {textArray.get(0)}, null);
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "annotator", new String[] {"id"}, new String[] {"eHOST_2010"}, "Extensible_Human_Oracle_Suite_of_Tools");
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "span", new String[] {"start", "end"}, new String[] {textArray.get(1), textArray.get(2)}, null);
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "spannedText", null, null, textArray.get(3));
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "creationDate", null, null, textArray.get(4));
		xsw.writeCharacters("\n\t");
		xsw.writeEndElement();
		
	}
	
	public void classMention(XMLStreamWriter xsw, ArrayList <String> textArray) throws XMLStreamException {
		xsw.writeCharacters("\n\t");
		xsw.writeStartElement("classMention");
		xsw.writeAttribute("id", textArray.get(0));
		xsw.writeCharacters("\n\t\t");
		xmlWrite(xsw, "mentionClass", new String[] {"id"}, new String[] {textArray.get(5)}, textArray.get(3));
		xsw.writeCharacters("\n\t");
		xsw.writeEndElement();
	}
	
	public String getMentionClass(String text) {
//		if (text.equals("MedicationMention")) {
//			return "Discussion_of_Medications";
//		}
//		
//		if (text.equals("SignSymptomMention")) {
//			return "Signs_Symptoms_and_Problems";
//			
//		}
//		
//		else {
		return text;
//		}
		
	}
	public ArrayList<String> getTexts(String line, char[] charbuff, String creationDate) throws NumberFormatException, IOException {
		ArrayList <String> wordArray = new ArrayList <String> (15);
		
		Pattern p = Pattern.compile("\"([^\"]*)\"");
		Matcher m = p.matcher(line);
		
		wordArray.add(line.split(" ")[0]);
	
		while (m.find()) {
			wordArray.add(m.group(1));
		}
		
		String spannedText = getSpannedText(charbuff, Integer.parseInt(wordArray.get(3)), Integer.parseInt(wordArray.get(4)));
		String mentionClass = getMentionClass(wordArray.get(0));
		
		ArrayList <String> returnArray = new ArrayList <String> (Arrays.asList("EHOST_Instance_" + wordArray.get(1), wordArray.get(3), wordArray.get(4), spannedText, creationDate, mentionClass));
		
		return returnArray;
	}
	
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
	
	
	public String getSpannedText(char[] charbuff, int start, int end) throws IOException {
		
		
		
//		System.out.println(String.valueOf(charbuff, 0, size));
//		byte[] document = new byte [end - start];
//        transcript.seek(start);
//        transcript.read(document);
		return String.valueOf(charbuff, start, end-start);
	}
	
	
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
	
}


