package edu.dartmouth.bmds.casxmi2knowtator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import edu.dartmouth.bmds.medex.Drug;
import edu.dartmouth.bmds.util.annotate.*;
import edu.dartmouth.bmds.util.file.*;

import org.apache.commons.cli.*;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentPath;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

public class MedEx2Knowtator {
	
	private static boolean debugMode = false;

	private static HashMap<String, SortedMap<String, Integer>> termSummaryMap = new HashMap<String, SortedMap<String, Integer>>();
	
	private static Map<IdentifiedAnnotation, Set<IdentifiedAnnotation>> overlappingAnnotationsMap = new HashMap<IdentifiedAnnotation, Set<IdentifiedAnnotation>>();
	
	public static void addAnnotationsToSummary(Iterable<AnnotatedText> annotatedTexts) {
		Iterator<AnnotatedText> annotatedTextIterator = annotatedTexts.iterator();
			
		while (annotatedTextIterator.hasNext()) {
			AnnotatedText annotatedText = annotatedTextIterator.next();
			
			SortedMap<String, Integer> termCountMap = termSummaryMap.get(annotatedText.getAnnotation().getAnnotationClass());

			if (termCountMap == null) {
				termCountMap = new TreeMap<String, Integer>();
				
				termSummaryMap.put(annotatedText.getAnnotation().getAnnotationClass(), termCountMap);
			}
			
			Integer termCount = termCountMap.get(annotatedText.getSpannedText());
			
			if (termCount == null) {
				termCountMap.put(annotatedText.getSpannedText(), 1);
			}
			else {
				termCountMap.put(annotatedText.getSpannedText(), termCount + 1);
			}
		
		}
	}
	
	public static void writeTermSummary(File outputDirectory) {
		
		Iterator<String> termSummaryIterator = termSummaryMap.keySet().iterator();
		
		while (termSummaryIterator.hasNext()) {
			
			String annotationClass = termSummaryIterator.next();
			
			File outputFile = new File(outputDirectory, annotationClass + ".csv");
			
			try {
				PrintStream ps = new PrintStream(outputFile);
				
				writeAnnotationClassSummary(ps, annotationClass);
				
				if (ps.checkError()) {
					System.err.println("Error: IOException thrown while writing " + outputFile.getAbsolutePath());
				}
				ps.close();
				
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}
	
	public static void writeAnnotationClassSummary(PrintStream outputStream, String annotationClass) {
		
		SortedMap<String, Integer> termCountMap = termSummaryMap.get(annotationClass);
		
		Set<Entry<String, Integer>> termCountEntrySet = termCountMap.entrySet();
		
		Iterator<Entry<String, Integer>> termCountEntryIterator = termCountEntrySet.iterator();

		while (termCountEntryIterator.hasNext()) {
			Entry<String, Integer> termEntry = termCountEntryIterator.next();
			outputStream.println(termEntry.getKey() + ", " + termEntry.getValue());
		}
	}
	
	public static AnnotatedText annotateVHS(Set<AnnotatedText> annotations, String vhsString, int begin, int end) {
		AnnotatedText at = new AnnotatedText(begin, end, vhsString);
		
        HashMap<String, Annotation> annotationMap = new HashMap<String, Annotation>();
		
		
		String cs = "VitaminHerbSupplement";
			
		at.setAnnotator("cTAKES");
		
		Annotation a = annotationMap.get(cs);
		if (a == null) {
			a = at.addAnnotation("Discussion_of_VitaminHerbSupplement");
			a.addAttribute("codingScheme", cs);
			annotationMap.put(cs, a);
		}

		//a.addAttribute("preferredText", ???);
		
		return at;
	}
	
	public static AnnotatedText annotateUMLSConcept(Set<AnnotatedText> annotations, IdentifiedAnnotation em) {
		
		//AnnotatedText at = new AnnotatedText(em.getBegin(), em.getEnd(), em.getCAS().getDocumentText().substring(em.getBegin(), em.getEnd()));
		AnnotatedText at = new AnnotatedText(em.getBegin(), em.getEnd(), em.getCoveredText());
		
		HashMap<String, Annotation> annotationMap = new HashMap<String, Annotation>();
		
		at.setAnnotator("cTAKES");
		
		FSArray fsA = em.getOntologyConceptArr();
		for (int j = 0; j < fsA.size(); j++) {
			OntologyConcept ontologyConcept = em.getOntologyConceptArr(j);	
			
			String cs = ontologyConcept.getCodingScheme();
			
			Annotation a = annotationMap.get(cs);
			if (a == null) {
				a = at.addAnnotation(em.getClass().getSimpleName());
				//a = at.addAnnotation("MedicationMention");
				a.addAttribute("codingScheme", cs);
				annotationMap.put(cs, a);
			}
			
			if (ontologyConcept instanceof UmlsConcept) {
				UmlsConcept umlsConcept = (UmlsConcept)ontologyConcept;
				
				a.addAttribute("CUI", umlsConcept.getCui());
				a.addAttribute("TUI", umlsConcept.getTui());
				a.addAttribute("preferredText", umlsConcept.getPreferredText());

			}
			
			System.out.println(ontologyConcept.toString());
		}
		
		return at;
	}
/*	
	public static AnnotatedText annotateAnatomicalSiteMention(Set<AnnotatedText> annotations, AnatomicalSiteMention am) {
		
		AnnotatedText at = new AnnotatedText(am.getBegin(), am.getEnd(), am.getCAS().getDocumentText().substring(am.getBegin(), am.getEnd()));
				
		HashMap<String, Annotation> annotationMap = new HashMap<String, Annotation>();
		
		FSArray fsA = am.getOntologyConceptArr();
		for (int j = 0; j < fsA.size(); j++) {
			OntologyConcept ontologyConcept = am.getOntologyConceptArr(j);	
			
			String cs = ontologyConcept.getCodingScheme();
			
			Annotation a = annotationMap.get(cs);
			if (a == null) {
				a = at.addAnnotation(am.getClass().getSimpleName());
				a.addAttribute("codingScheme", cs);
				annotationMap.put(cs, a);
			}
			
			if (ontologyConcept instanceof UmlsConcept) {
				UmlsConcept umlsConcept = (UmlsConcept)ontologyConcept;
				
				a.addAttribute("CUI", umlsConcept.getCui());
				a.addAttribute("TUI", umlsConcept.getTui());
				a.addAttribute("preferredText", umlsConcept.getPreferredText());

			}
			
			System.out.println(ontologyConcept.toString());
		}
		
		return at;
	}
*/	
	
/*	
	public static AnnotatedText annotateMedicationMention(Set<AnnotatedText> annotations, MedicationMention mm) {
		
		AnnotatedText at = new AnnotatedText(mm.getBegin(), mm.getEnd(), mm.getCAS().getDocumentText().substring(mm.getBegin(), mm.getEnd()));
		
		annotations.add(at);
		
		HashMap<String, Annotation> annotationMap = new HashMap<String, Annotation>();
		
		FSArray fsA = mm.getOntologyConceptArr();
		for (int j = 0; j < fsA.size(); j++) {
			OntologyConcept ontologyConcept = mm.getOntologyConceptArr(j);	
			
			String cs = ontologyConcept.getCodingScheme();
			
			Annotation a = annotationMap.get(cs);
			if (a == null) {
				a = at.addAnnotation("MedicationMention");
				a.addAttribute("codingScheme", cs);
				annotationMap.put(cs, a);
			}
			
			if (ontologyConcept instanceof UmlsConcept) {
				UmlsConcept umlsConcept = (UmlsConcept)ontologyConcept;
				
				a.addAttribute("CUI", umlsConcept.getCui());
				a.addAttribute("TUI", umlsConcept.getTui());
				a.addAttribute("preferredText", umlsConcept.getPreferredText());

			}
			
			System.out.println(ontologyConcept.toString());
		}
		
		return at;
	}
*/
	
	public static void writeSet(File outputFile, Set<String> stringSet) {
		
		
			PrintStream ps;
			try {
				ps = new PrintStream(outputFile);
				
				Iterator<String> si = stringSet.iterator();
				
				while (si.hasNext()) {
					ps.println(si.next());
				}
				
				if (ps.checkError()) {
					System.err.println("Error: IOException thrown while writing " + outputFile.getAbsolutePath());
				}
				ps.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		
	}
	
	public static void main(String[] args) {
		
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		// create the Options
		Options options = new Options();
		options.addOption("h", "help", false, "Print this message");
		options.addOption("d", "debug", false, "Include as much as possible from filtered output");
		options.addRequiredOption("c", "configDirectory", true, "Directory containing configuration files, including words to include/exclude");
		options.addRequiredOption("i", "inputFile", true, "File containing the MedXN formated annotations to be converted");
		options.addRequiredOption("o", "outputDirectory", true, "Directory where converted annotations will be stored in the Knowtator format");
		
		try {
	        // parse the command line arguments
	        CommandLine cmdLine = parser.parse( options, args );
	        
	        if (cmdLine.hasOption("h")) {
	        		HelpFormatter helpFormatter = new HelpFormatter();
	        		helpFormatter.printHelp( "medxn2knowtator", options );
	        }
	        
	        if (cmdLine.hasOption("d")) { 
	        	debugMode = true;
	        }
	        
	        File configDirectory = null;
	        File inputDirectory = null;
	        File outputDirectory = null;
	        
	        try {
	        	configDirectory = new File(cmdLine.getOptionValue("c"));
	        	System.out.println("config directory = " + configDirectory.getCanonicalPath());
	        	
	        	inputDirectory = new File(cmdLine.getOptionValue("i"));
				System.out.println("input directory = " + inputDirectory.getCanonicalPath());

	        
				outputDirectory = new File(cmdLine.getOptionValue("o"));
				System.out.println("output directory = " + outputDirectory.getCanonicalPath());
				if (!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}
				
				FilenameFilter filter = new FilenameExtensionFilter("txt");
				
				File[] inputFiles = inputDirectory.listFiles(filter);
				
				//HashMap<String, HashSet<AnnotatedText>> annotatedFileMap= new HashMap<String, HashSet<AnnotatedText>>();
				
				for (int i = 0; i < inputFiles.length; i++) {
					System.out.println("input file[" + i + "] = " + inputFiles[i].getCanonicalPath());

					BufferedReader br = new BufferedReader(new FileReader(inputFiles[i]));

					HashSet<AnnotatedText> annotations = new HashSet<AnnotatedText>();

					try {
						String line;
						while ((line = br.readLine()) != null) {
							Drug drug = Drug.parseDrug(line);

							AnnotatedText at = new AnnotatedText(drug.getMedicationNameBegin(),
									drug.getMedicationNameEnd(), drug.getMedicationName());
							at.setAnnotator("MedExDrool");
							at.addAnnotation("Discussion_of_Medications");
							annotations.add(at);
						}
					} finally {
						br.close();
					}

					// HashSet<AnnotatedText> annotations = new HashSet<AnnotatedText>();

					XMLOutputFactory xof = XMLOutputFactory.newInstance();

					File outputFile = new File(outputDirectory, inputFiles[i].getName() + ".knowtator.xml");

					FileWriter fw = new FileWriter(outputFile);
					XMLStreamWriter xsw = xof.createXMLStreamWriter(fw);

					KnowtatorUtil.writeAnnotatedTexts(xsw, annotations, inputFiles[i].getName());

					xsw.flush();
					fw.flush();
					// xsw.close();
					// fw.close();

					addAnnotationsToSummary(annotations);
				}

				writeTermSummary(outputDirectory);
				
	        }
	        catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong

	    		System.err.println( "Invalid command line options.  Reason: " + exp.getMessage() );
	    		HelpFormatter formatter = new HelpFormatter();
        		formatter.printHelp( "medex2knowtator", options );
	    }
		
		System.out.println("about to exit from main");
	}

}
