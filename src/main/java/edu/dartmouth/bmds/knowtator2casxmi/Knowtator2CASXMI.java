package edu.dartmouth.bmds.knowtator2casxmi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
//import org.apache.ctakes.core.knowtator.KnowtatorAnnotation;
//import org.apache.ctakes.core.knowtator.KnowtatorXMLParser;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.FileUtils;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.Tag;
import edu.dartmouth.bmds.util.ehost.EHostKnowtatorAnnotation;
import edu.dartmouth.bmds.util.ehost.EHostKnowtatorXMLParser;
import edu.dartmouth.bmds.util.file.FilenameExtensionFilter;

public class Knowtator2CASXMI {

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
		options.addRequiredOption("o", "outputDirectory", true, "Directory where converted Knowtator files will be stored in the CAS XPI format");
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
				
				TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
				
				FilenameFilter filter = new FilenameExtensionFilter(KNOWTATOR_EXTENTION);
				
				File[] inputFiles = inputDirectory.listFiles(filter);
				
				for (int i = 0; i < inputFiles.length; i++) {
					System.out.println("input file[" + i + "] = " + inputFiles[i].getCanonicalPath());
					
					//FileInputStream fis = new FileInputStream(inputFiles[i]);
					
					EHostKnowtatorXMLParser knowtatorParser = new EHostKnowtatorXMLParser("Extensible_Human_Oracle_Suite_of_Tools");
					
					try {
						System.out.println("input file[" + i + "] = " + inputFiles[i].toURI().toString());
						Collection<EHostKnowtatorAnnotation> annotations = knowtatorParser.parse(inputFiles[i].toURI());
						
						File textFile = new File(textDirectory, inputFiles[i].getName().substring(0, inputFiles[i].getName().length() - KNOWTATOR_EXTENTION.length() - 1));
						
						String text = FileUtils.file2String(textFile);
						
						JCas jCas;
						
						jCas = JCasFactory.createJCas(tsd);
						
						SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jCas);
					    srcDocInfo.setUri(textFile.toURI().toString());
					    srcDocInfo.setOffsetInSource(0);
					    srcDocInfo.setDocumentSize((int) textFile.length());
					    //srcDocInfo.setLastSegment(mCurrentIndex == mFiles.size());
					    srcDocInfo.setLastSegment(true);
					    srcDocInfo.addToIndexes();
						
						Iterator<EHostKnowtatorAnnotation> k = annotations.iterator();
						
						while (k.hasNext()) {
							EHostKnowtatorAnnotation annotation = k.next();
							
							System.out.println(annotation.type + ", " + annotation.spannedText);
							
							//Annotation newAnnotation = new Annotation(jCas, annotation.getCoveringSpan().begin, annotation.getCoveringSpan().end);
							//newAnnotation.addToIndexes(jCas);
							
							
							// need to set type on newAnnotation
							
							Tag tag = new Tag(jCas, annotation.getCoveringSpan().begin, annotation.getCoveringSpan().end);
							tag.setValue(annotation.type);
							tag.addToIndexes(jCas);
						}
						
						File outputFile = new File(outputDirectory,  inputFiles[i].getName().substring(0, inputFiles[i].getName().length() - KNOWTATOR_EXTENTION.length() - 1) + ".xmi");
				        				        
				        FileOutputStream fos = new FileOutputStream(outputFile);
				        
				        try {
							XmiCasSerializer.serialize(jCas.getCas(), fos);
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				        
					} catch (JDOMException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UIMAException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
	        }
	        catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
		    catch (ResourceInitializationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
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
