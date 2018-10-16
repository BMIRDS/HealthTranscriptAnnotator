package edu.dartmouth.bmds.knowtator.stripdosages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
//import org.apache.ctakes.core.knowtator.KnowtatorAnnotation;
//import org.apache.ctakes.core.knowtator.KnowtatorXMLParser;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.Tag;
import edu.dartmouth.bmds.util.annotate.AnnotatedText;
import edu.dartmouth.bmds.util.annotate.Annotation;
import edu.dartmouth.bmds.util.annotate.KnowtatorUtil;
import edu.dartmouth.bmds.util.ehost.EHostKnowtatorAnnotation;
import edu.dartmouth.bmds.util.ehost.EHostKnowtatorXMLParser;
import edu.dartmouth.bmds.util.file.FilenameExtensionFilter;

public class KnowtatorStripDosages {

	private static boolean debugMode = false;
	
	private static String KNOWTATOR_EXTENTION = "knowtator.xml";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CommandLineParser parser = new DefaultParser();

		// create the Options
		Options options = new Options();
		options.addOption("h", "help", false, "Print this message");
		options.addOption("d", "debug", false, "Include as much as possible from filtered output");
		//options.addOption("e", "exclude", false, "Exclude words in ExcludeWords.txt");
		options.addRequiredOption("c", "configDirectory", true, "Directory containing configuration files, including words to include/exclude");
		options.addRequiredOption("i", "inputDirectory", true, "Directory containing the Knowtator files to be converted");
		options.addRequiredOption("o", "outputDirectory", true, "Directory where converted Knowtator files will be stored");
		options.addRequiredOption("t", "textDirectory", true, "Directory where source text files are stored as .txt");
		
		try {
	        // parse the command line arguments
	        CommandLine line = parser.parse( options, args );
	        
	        if (line.hasOption("h")) {
	        		HelpFormatter helpFormatter = new HelpFormatter();
	        		helpFormatter.printHelp( "knowtator2casxmi", options );
	        }
	        
	        if (line.hasOption("d")) { 
	        	debugMode = true;
	        }
	        
	        File configDirectory = null;
	        File inputDirectory = null;
	        File outputDirectory = null;
	        File textDirectory = null;

	        try {
	        	configDirectory = new File(line.getOptionValue("c"));
	        	System.out.println("config directory = " + configDirectory.getCanonicalPath());
	        	
	        	inputDirectory = new File(line.getOptionValue("i"));
				System.out.println("input directory = " + inputDirectory.getCanonicalPath());
			
	        
				outputDirectory = new File(line.getOptionValue("o"));
				System.out.println("output directory = " + outputDirectory.getCanonicalPath());
				if (!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}
				
				textDirectory = new File(line.getOptionValue("t"));
				System.out.println("text directory = " + textDirectory.getCanonicalPath());
						
				XMLOutputFactory xof = XMLOutputFactory.newInstance();
				
				FilenameFilter filter = new FilenameExtensionFilter(KNOWTATOR_EXTENTION);
				
				File[] inputFiles = inputDirectory.listFiles(filter);
				
				for (int i = 0; i < inputFiles.length; i++) {
					System.out.println("input file[" + i + "] = " + inputFiles[i].getCanonicalPath());
					
					//FileInputStream fis = new FileInputStream(inputFiles[i]);
					
					EHostKnowtatorXMLParser knowtatorParser = new EHostKnowtatorXMLParser("ADJUDICATION");
					
					try {
						System.out.println("input file[" + i + "] = " + inputFiles[i].toURI().toString());
						Collection<EHostKnowtatorAnnotation> annotations = knowtatorParser.parse(inputFiles[i].toURI());
						
						File textFile = new File(textDirectory, inputFiles[i].getName().substring(0, inputFiles[i].getName().length() - KNOWTATOR_EXTENTION.length() - 1));
						
						String text = FileUtils.file2String(textFile);
						
				        HashSet<AnnotatedText> filteredAnnotations = new HashSet<AnnotatedText>();
						
						Iterator<EHostKnowtatorAnnotation> k = annotations.iterator();
						
						while (k.hasNext()) {
							EHostKnowtatorAnnotation annotation = k.next();
														
							System.out.println(annotation.type + ", " + annotation.spannedText);
							String spannedText = annotation.spannedText.trim().toLowerCase();
							
							if (spannedText.endsWith(" mg") || spannedText.endsWith(" mg.")
									|| spannedText.endsWith("-mg") || spannedText.endsWith("-mg.")) {
								System.out.println("Filtered: " + spannedText);
							}
							else {
								// need to set type on newAnnotation
								AnnotatedText at = new AnnotatedText(annotation.spans.get(0).begin, annotation.spans.get(0).end, annotation.spannedText);

								Annotation a = at.addAnnotation(annotation.type);
								at.setAnnotator("AdjudicatedDosageFiltered");

								filteredAnnotations.add(at);
							}
						}
						
						File outputFile = new File(outputDirectory,  inputFiles[i].getName());
				        				        
				        
				        FileWriter fw = new FileWriter(outputFile);
				        XMLStreamWriter xsw = xof.createXMLStreamWriter(fw);
				        
				        KnowtatorUtil.writeAnnotatedTexts(xsw, filteredAnnotations, textFile.getName()); 				        
				        
				        xsw.flush();
						fw.flush();
					} catch (JDOMException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (XMLStreamException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
	        }
	        catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
		}
		catch( ParseException exp ) {
	        // oops, something went wrong

	    		System.err.println( "Invalid command line options.  Reason: " + exp.getMessage() );
	    		HelpFormatter formatter = new HelpFormatter();
        		formatter.printHelp( "casxmi2knowtator", options );
	    }
	}

}
