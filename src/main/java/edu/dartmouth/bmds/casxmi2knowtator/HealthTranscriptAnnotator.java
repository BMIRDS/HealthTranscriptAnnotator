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

public class HealthTranscriptAnnotator {
	
	private static boolean debugMode = false;

	private static HashMap<String, SortedMap<String, Integer>> termSummaryMap = new HashMap<String, SortedMap<String, Integer>>();
	
	private static Map<IdentifiedAnnotation, Set<IdentifiedAnnotation>> overlappingAnnotationsMap = new HashMap<IdentifiedAnnotation, Set<IdentifiedAnnotation>>();
	
	// "T116", "T125", "T131"?
	// "T129", "T195" May 7,2018
	// "T125" Hormone, what to do with it related to medications???
	protected static String[] tuiToFilterForMedications = {"T114", "T122", "T123", "T125", "T130", "T197"};
	protected static String[] tuiToFilterForDiagnoses = {};
	protected static String[] tuiToFilterForSignsSymptoms = {};
	
	protected static String[] tuiToFilterForMeciationsOverlappingDiagnoses = { "T129" };
	protected static String[] tuiToFilterForMeciationsOverlappingProcedures = { "T109", "T116"};
	
	protected static String[] tuiForTestAndImaging = { "T059", "T060" };
	protected static String[] tuiForTreatmentAndProcedure = { "T061"};
	
	protected static String[] substringToFilterForAllergens = { "allergenic" };
	
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
	
	public static Sentence sentenceForSpan(Iterator<Sentence> sentences, int spanStart, int spanEnd) {
		
		Sentence foundSentence = null;
		
		while (sentences.hasNext() && (foundSentence == null)) {
        	Sentence s = sentences.next();

        	//System.out.print("Sentence[" + s.getSentenceNumber() + "](" + s.getBegin() + ", " + s.getEnd() + "):");        	
        	
        	if ((spanStart >= s.getBegin()) && (spanEnd <= s.getEnd())) {
        		foundSentence = s;
        	}
		}
			
		
		return foundSentence;
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		Calendar.getInstance().getTime();
		System.out.println(dateFormatter.format(Calendar.getInstance().getTime()));
		
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		// create the Options
		Options options = new Options();
		options.addOption("h", "help", false, "Print this message");
		options.addOption("d", "debug", false, "Include as much as possible from filtered output");
		options.addOption("e", "exclude", false, "Exclude words in ExcludeWords.txt");
		options.addRequiredOption("c", "configDirectory", true, "Directory containing configuration files, including words to include/exclude");
		options.addRequiredOption("i", "inputDirectory", true, "Directory containing the CAS XPI files to be converted");
		options.addRequiredOption("o", "outputDirectory", true, "Directory where converted CAS XPI files will be stored in the Knowtator format");
		
		try {
	        // parse the command line arguments
	        CommandLine line = parser.parse( options, args );
	        
	        if (line.hasOption("h")) {
	        		HelpFormatter helpFormatter = new HelpFormatter();
	        		helpFormatter.printHelp( "casxmi2knowtator", options );
	        }
	        
	        if (line.hasOption("d")) { 
	        	debugMode = true;
	        }
	        
	        File configDirectory = null;
	        File inputDirectory = null;
	        File outputDirectory = null;
	        
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
				
				TreeSet<String> medicationExcludeWords = new TreeSet<String>();
				TreeSet<String> medicationWordsExcluded = new TreeSet<String>();
				
				TreeSet<String> sspExcludeWords = new TreeSet<String>();
				TreeSet<String> sspWordsExcluded = new TreeSet<String>();
				
				TreeSet<String> diagnosisExcludeWords = new TreeSet<String>();
				TreeSet<String> diagnosisWordsExcluded = new TreeSet<String>();
				
				TreeSet<String> testProcedureExcludeWords = new TreeSet<String>();
				TreeSet<String> testProcedureWordsExcluded = new TreeSet<String>();
				
				TreeSet<String> treatmentProcedureExcludeWords = new TreeSet<String>();
				TreeSet<String> treatmentProcedureWordsExcluded = new TreeSet<String>();
				
				TreeSet<String> vitaminSupplementIncludeWords = new TreeSet<String>();
				
				List<String[]> vitaminSupplementIncludeWordsList;
								
				if (line.hasOption("e")) {

					File commonWordsFile = new File(configDirectory, "CommonWords.txt");

					if (commonWordsFile.exists()) {
						List<String> words = Files.readAllLines(commonWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							
							String trimmedWord = word.trim().toLowerCase();
							
							medicationExcludeWords.add(trimmedWord);
							//sspExcludeWords.add(trimmedWord);
							//diagnosisExcludeWords.add(trimmedWord);
							testProcedureExcludeWords.add(trimmedWord);
							treatmentProcedureExcludeWords.add(trimmedWord);				
							
						}
					}
					
					File medicationExcludeWordsFile = new File(configDirectory, "MedicationExcludeWords.txt");

					if (medicationExcludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(medicationExcludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							medicationExcludeWords.add(word.trim().toLowerCase());
						}
					}
					
					File medicationIncludeWordsFile = new File(configDirectory, "MedicationIncludeWords.txt");

					if (medicationIncludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(medicationIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							medicationExcludeWords.remove(word.trim().toLowerCase());
						}
					}
					
					File sspExcludeWordsFile = new File(configDirectory, "SignsSymptomsExcludeWords.txt");

					if (sspExcludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(sspExcludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							sspExcludeWords.add(word.trim().toLowerCase());
						}
					}
					
					File sspIncludeWordsFile = new File(configDirectory, "SignsSymptomsIncludeWords.txt");

					if (sspIncludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(sspIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							sspExcludeWords.remove(word.trim().toLowerCase());
						}
					}
					
					File diagnosisExcludeWordsFile = new File(configDirectory, "DiagnosisExcludeWords.txt");

					if (diagnosisExcludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(diagnosisExcludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							diagnosisExcludeWords.add(word.trim().toLowerCase());
						}
					}
					
					File diagnosisIncludeWordsFile = new File(configDirectory, "DiagnosisIncludeWords.txt");

					if (diagnosisIncludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(diagnosisIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							diagnosisExcludeWords.remove(word.trim().toLowerCase());
						}
					}
					
					File testProcedureExcludeWordsFile = new File(configDirectory, "TestProcedureExcludeWords.txt");

					if (testProcedureExcludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(testProcedureExcludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							testProcedureExcludeWords.add(word.trim().toLowerCase());
						}
					}
					
					File testProcedureIncludeWordsFile = new File(configDirectory, "TestProcedureIncludeWords.txt");

					if (testProcedureIncludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(testProcedureIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							testProcedureExcludeWords.remove(word.trim().toLowerCase());
						}
					}
					
					File treatmentProcedureExcludeWordsFile = new File(configDirectory, "TreatmentProcedureExcludeWords.txt");

					if (treatmentProcedureExcludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(treatmentProcedureExcludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							treatmentProcedureExcludeWords.add(word.trim().toLowerCase());
						}
					}
					
					File treatmentProcedureIncludeWordsFile = new File(configDirectory, "TreatmentProcedureIncludeWords.txt");

					if (treatmentProcedureIncludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(treatmentProcedureIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							treatmentProcedureExcludeWords.remove(word.trim().toLowerCase());
						}
					}

				}
				
				File vitaminSupplementIncludeWordsFile = new File(configDirectory, "VitaminSupplementWords.txt");
				
				if (vitaminSupplementIncludeWordsFile.exists()) {
					List<String> terms = Files.readAllLines(vitaminSupplementIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

					vitaminSupplementIncludeWordsList = new ArrayList<String[]>(terms.size());
					
					for (String term : terms) {
						
						String tterm = term.trim().toLowerCase();
						if (!tterm.isEmpty()) {
							vitaminSupplementIncludeWords.add(tterm);
							
							String[] words = tterm.split("\\s+");
							vitaminSupplementIncludeWordsList.add(words);
						}
					}
				}
				else {
					vitaminSupplementIncludeWordsList = new ArrayList<String[]>(0);
				}
				
				FilenameFilter filter = new FilenameExtensionFilter("xmi");
				
				File[] inputFiles = inputDirectory.listFiles(filter);
				
				XMLOutputFactory xof = XMLOutputFactory.newInstance();
				
				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
				InputStream is = classloader.getResourceAsStream("org/apache/ctakes/typesystem/types/TypeSystem.xml");
				
				if (is == null) {
					System.out.println("Resource Stream is null");
				}
				else {
					System.out.println("Resource Stream is not null");
				}
				
				TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
				
				/*
				XMLInputSource in = new XMLInputSource(is, null);
				//XMLInputSource in = new XMLInputSource("MyDescriptor.xml");
				try {
					ResourceSpecifier specifier = 
					    UIMAFramework.getXMLParser().parseResourceSpecifier(in);
					
					
					  //create AE here
					AnalysisEngine ae = 
					    UIMAFramework.produceAnalysisEngine(specifier);
					CasConsumer cc = UIMAFramework.produceCasConsumer(specifier);
					
					
				} catch (InvalidXMLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ResourceInitializationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/

			/*
				
				ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
				resMgr.setDataPath(yourPathString);
				AnalysisEngine ae = UIMAFramework.produceAE(desc, resMgr, null);
				*/
				
				File sentencesFile = new File(outputDirectory, "Sentences.txt");
				PrintStream sps = new PrintStream(sentencesFile);
				
				for (int i = 0; i < inputFiles.length; i++) {
					System.out.println("input file[" + i + "] = " + inputFiles[i].getCanonicalPath());
					
					FileInputStream fis = new FileInputStream(inputFiles[i]);
					
					Map<Integer, Sentence> sentencesMap = new HashMap<Integer, Sentence>();
					
					JCas jCas;
					try {
						jCas = JCasFactory.createJCas(tsd);
						
				        XmiCasDeserializer.deserialize(fis, jCas.getCas());

				        HashSet<AnnotatedText> annotations = new HashSet<AnnotatedText>();
 				        
				        String docText = jCas.getDocumentText();
				        
				        Iterator<IdentifiedAnnotation> iai = JCasUtil.iterator(jCas, IdentifiedAnnotation.class);
				        				        
				        while (iai.hasNext()) {
				        	IdentifiedAnnotation ia = iai.next();
							
							Set<IdentifiedAnnotation> overlappingAnnotations = overlappingAnnotationsMap.get(ia);
							
							Iterator<IdentifiedAnnotation> iaj = JCasUtil.iterator(jCas, IdentifiedAnnotation.class);
					        	
							while (iaj.hasNext()) {
								IdentifiedAnnotation ia2 = iaj.next();
								
								if (ia != ia2) {
									
									if ((ia.getBegin() >= ia2.getBegin()) && (ia.getEnd() <= ia2.getEnd())
											|| (ia2.getBegin() >= ia.getBegin()) && (ia2.getEnd() <= ia.getEnd())) {
										if (overlappingAnnotations == null) {
											overlappingAnnotations = new HashSet<IdentifiedAnnotation>();
											overlappingAnnotationsMap.put(ia, overlappingAnnotations);
										}
										overlappingAnnotations.add(ia2);
								  	}
								}
							}
				        }
				        
				        AnnotatedText at;

				        
				        
				        
				        Iterator<EventMention> ei = JCasUtil.iterator(jCas, EventMention.class);
				        
				        
				        while (ei.hasNext()) {
							EventMention em = ei.next();

							System.out.println(docText.substring(em.getBegin(), em.getEnd()));

							System.out.println(em.toString());

							FSArray fsA = em.getOntologyConceptArr();
							for (int j = 0; j < fsA.size(); j++) {
								OntologyConcept ontologyConcept = em.getOntologyConceptArr(j);
								System.out.println(ontologyConcept.toString());
							}

							at = null;
							
							Sentence s = null;
							String sText = null;
							
							try {
								s = sentenceForSpan(JCasUtil.iterator(jCas, Sentence.class), em.getBegin(), em.getEnd());
								//s = sentencesMap.get(Integer.parseInt(em.getSentenceID()));
								sText = s.getCoveredText();
							}
							catch (NumberFormatException e) {
								s = null;
							}
							
															
							if (em instanceof MedicationMention) {
								
								at = annotateUMLSConcept(annotations, em);
								
								if (em.getCoveredText().trim().toLowerCase().endsWith(" mg")) {
									at.setAnnotator(at.getAnnotator() + "_medicationExcludedDoseageMG");
									medicationWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
									if (debugMode) {
										annotations.add(at);
									}
								}
								if (medicationExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
									if (!(em.getCoveredText().trim().toLowerCase().endsWith(" medication")
											|| em.getCoveredText().trim().toLowerCase().endsWith(" medicine")
											|| em.getCoveredText().trim().toLowerCase().endsWith(" pill")
											|| em.getCoveredText().trim().toLowerCase().endsWith(" shot")
											|| em.getCoveredText().trim().toLowerCase().endsWith(" vaccine"))) {
										at.setAnnotator(at.getAnnotator() + "_medicationExcludedWords");
										medicationWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
										if (debugMode) {
											annotations.add(at);
										}
									}
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForMedications)) {
									at.setAnnotator(at.getAnnotator() + "_medicationFilteredTUI");
									if (debugMode) {
										annotations.add(at);
									}
								}
								else if (at.getAnnotation().containsAttributeValueSubstring("preferredText", substringToFilterForAllergens)) {
									at.setAnnotator(at.getAnnotator() + "_medicationFilteredAllergenic");
									if (debugMode) {
										annotations.add(at);
									}
								}
								else {
									Set<IdentifiedAnnotation> overlappingAnnotations = overlappingAnnotationsMap.get(em);
									if (overlappingAnnotations != null) {
										Iterator<IdentifiedAnnotation> oai = overlappingAnnotations.iterator();
										boolean overlapFound = false;
										
										while (oai.hasNext()) {
											IdentifiedAnnotation oa = oai.next();
											if ((oa instanceof DiseaseDisorderMention) && at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForMeciationsOverlappingDiagnoses)) {
												if (!(em.getCoveredText().trim().toLowerCase().endsWith(" shot") || em.getCoveredText().trim().toLowerCase().endsWith(" vaccine"))) {
													at.setAnnotator(at.getAnnotator() + "_medicationFilteredDiagnosisTUI");
													overlapFound = true;
												}
											}
											if ((oa instanceof ProcedureMention) && at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForMeciationsOverlappingProcedures)) {
												if (!(em.getCoveredText().trim().toLowerCase().endsWith(" medication")
														|| em.getCoveredText().trim().toLowerCase().endsWith(" medicine")
														|| em.getCoveredText().trim().toLowerCase().endsWith(" pill"))) {
													at.setAnnotator(at.getAnnotator() + "_medicationFilteredProcedureTUI");
													overlapFound = true;
												}
											}
										}
										
										if (overlapFound) {
											if (debugMode) {
												annotations.add(at);
											}
										}
										else {
											at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
										    annotations.add(at);
										}
									}
									else {
									    at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
									    annotations.add(at);
									}
								}
							}
							else if (em instanceof DiseaseDisorderMention) {
								at = annotateUMLSConcept(annotations, em);
																
								if (diagnosisExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
									at.setAnnotator(at.getAnnotator() + "_diagnosisExcludedWords");
									diagnosisWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
									if (debugMode) {
										annotations.add(at);
									}
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForDiagnoses)) {
									at.setAnnotator(at.getAnnotator() + "_diagnosisFilteredTUI");
									if (debugMode) {
										annotations.add(at);
									}
								}
								else if (sText.contains(" vaccine") || sText.contains(" shot") || sText.contains(" booster") || sText.contains(" pill")) {
									System.out.println("************************************** DiseaseDisorderMention with vaccine, shot, booster, pill found");
									System.out.println(sText);
									System.out.println(em.getCoveredText());
									at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
								//	//at.setAnnotator(at.getAnnotator() + "_unimplemented");
									annotations.add(at);
								}
								else {
									at.getAnnotation().setAnnotationClass("Medical Condition");
									//at.setAnnotator(at.getAnnotator() + "_unimplemented");
									annotations.add(at);
								}
				        	}
							else if (em instanceof SignSymptomMention) {
								at = annotateUMLSConcept(annotations, em);
								
								if (sspExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
									at.setAnnotator(at.getAnnotator() + "_sspExcludedWords");
									sspWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
									if (debugMode) {
										annotations.add(at);
									}
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForSignsSymptoms)) {
									at.setAnnotator(at.getAnnotator() + "_sspFilteredTUI");
									if (debugMode) {
										annotations.add(at);
									}
								}
								else {
									at.getAnnotation().setAnnotationClass("Signs_Symptoms_and_Problems");
									at.setAnnotator(at.getAnnotator() + "_unimplemented");
									annotations.add(at);
								}
				        	}
							else if (em instanceof ProcedureMention) {
								at = annotateUMLSConcept(annotations, em);
								
								
								if (at.getAnnotation().containsAttributeValue("TUI", tuiForTestAndImaging)) {
									
									if (testProcedureExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
										at.setAnnotator(at.getAnnotator() + "_testProcedureExcludedWords");
										testProcedureWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
										if (debugMode) {
											annotations.add(at);
										}
									}
									else {
										at.getAnnotation().setAnnotationClass("Test & Imaging");
										annotations.add(at);
									}
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiForTreatmentAndProcedure)) {
									if (treatmentProcedureExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
										at.setAnnotator(at.getAnnotator() + "_treatmentProcedureExcludedWords");
										treatmentProcedureWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
										if (debugMode) {
											annotations.add(at);
										}
									}
									else {			
									    at.getAnnotation().setAnnotationClass("Treatment & Procedure");
									    annotations.add(at);
									}
								}
								else {
									at.getAnnotation().setAnnotationClass("cTAKES Procedures");
									at.setAnnotator(at.getAnnotator() + "_unimplemented");
									if (debugMode) {
										annotations.add(at);
									}
								}
				        	}							
							
				        	
				        }
				        
				        // Handle AnatomicalSiteMention
				        Iterator<AnatomicalSiteMention> ami = JCasUtil.iterator(jCas, AnatomicalSiteMention.class);
				        
				        while (ami.hasNext()) {
							AnatomicalSiteMention am = ami.next();

							System.out.println(docText.substring(am.getBegin(), am.getEnd()));

							System.out.println(am.toString());

							FSArray fsA = am.getOntologyConceptArr();
							for (int j = 0; j < fsA.size(); j++) {
								OntologyConcept ontologyConcept = am.getOntologyConceptArr(j);
								System.out.println(ontologyConcept.toString());
							}

							//at = annotateAnatomicalSiteMention(annotations, am);
							at = annotateUMLSConcept(annotations, am);

							at.setAnnotator(at.getAnnotator() + "_unimplemented");
							//annotations.add(at);
				        }
				        				        
				        SortedMap<Integer, WordToken> wtSortedMap = new TreeMap<Integer, WordToken>();
				        
				        Iterator<WordToken> wti = JCasUtil.iterator(jCas, WordToken.class);

				        while (wti.hasNext()) {
				        	WordToken wt = wti.next();
				        	
				        	wtSortedMap.put(wt.getTokenNumber(), wt);
				        }
				        
				        SortedMap<Integer, NumToken> ntSortedMap = new TreeMap<Integer, NumToken>();
				        
				        Iterator<NumToken> nti = JCasUtil.iterator(jCas, NumToken.class);

				        while (nti.hasNext()) {
				        	NumToken nt = nti.next();
				        	
				        	ntSortedMap.put(nt.getTokenNumber(), nt);
				        }
				        
				        Iterator<PunctuationToken> pti = JCasUtil.iterator(jCas, PunctuationToken.class);

				        while (pti.hasNext()) {
				        	PunctuationToken pt = pti.next();
				        	
				        	if (pt.getPartOfSpeech().equals("HYPH")) {
				        		int tokenNumber = pt.getTokenNumber();
				        		
				        		WordToken wtBefore = wtSortedMap.get(tokenNumber - 1);
				        		WordToken wtAfter = wtSortedMap.get(tokenNumber + 1);
				        		NumToken ntBefore = ntSortedMap.get(tokenNumber - 1);
				        		NumToken ntAfter = ntSortedMap.get(tokenNumber + 1);
				        		
				        		if ((wtBefore != null) && (wtAfter != null)) {
				        			String hyphenatedWord = wtBefore.getCoveredText() + "-" + wtAfter.getCoveredText();
				        			
				        			Iterator<String> vhsTermIterator = vitaminSupplementIncludeWords.iterator();
						        	
						        	while (vhsTermIterator.hasNext()) {
						        		String vhsTerm = vhsTermIterator.next();
						        		
						        		if (hyphenatedWord.toLowerCase().trim().equals(vhsTerm)) {
							        		System.out.println("******* Matched w-w comapre: " + hyphenatedWord + "==" + vhsTerm);

						        			at = annotateVHS(annotations, hyphenatedWord, wtBefore.getBegin(), wtAfter.getEnd());

				        					if (at.firstSpanContaining(annotations) == null) {
				        						at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
				        						annotations.add(at);
				        					}
						        		}
						        	}
				        		}
				        		else if ((wtBefore != null) && (ntAfter != null)) {
				        			String hyphenatedWord = wtBefore.getCoveredText() + "-" + ntAfter.getCoveredText();
				        			
				        			Iterator<String> vhsTermIterator = vitaminSupplementIncludeWords.iterator();
						        	
						        	while (vhsTermIterator.hasNext()) {
						        		String vhsTerm = vhsTermIterator.next();
						        		
						        		if (hyphenatedWord.toLowerCase().trim().equals(vhsTerm)) {
							        		System.out.println("******* Matched w-n comapre: " + hyphenatedWord + "==" + vhsTerm);

						        			at = annotateVHS(annotations, hyphenatedWord, wtBefore.getBegin(), ntAfter.getEnd());

				        					if (at.firstSpanContaining(annotations) == null) {
				        						at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
				        						annotations.add(at);
				        					}
						        		}
						        	}
				        		}
				        		else if ((ntBefore != null) && (wtAfter != null)) {
				        			String hyphenatedWord = ntBefore.getCoveredText() + "-" + wtAfter.getCoveredText();
				        			
				        			Iterator<String> vhsTermIterator = vitaminSupplementIncludeWords.iterator();
						        	
						        	while (vhsTermIterator.hasNext()) {
						        		String vhsTerm = vhsTermIterator.next();
						        		
						        		if (hyphenatedWord.toLowerCase().trim().equals(vhsTerm)) {
							        		System.out.println("******* Matched n-w comapre: " + hyphenatedWord + "==" + vhsTerm);

						        			at = annotateVHS(annotations, hyphenatedWord, ntBefore.getBegin(), wtAfter.getEnd());

				        					if (at.firstSpanContaining(annotations) == null) {
				        						at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
				        						annotations.add(at);
				        					}
						        		}
						        	}
				        		}
				        	}
				        }
				        
				        Iterator<Integer> wtmi = wtSortedMap.keySet().iterator();
				        
				        while (wtmi.hasNext()) {
				        	Integer wtKey = wtmi.next();
				        	WordToken wt = wtSortedMap.get(wtKey);
				        	
				        	String wtText = wt.getCoveredText().trim().toLowerCase();
				        	
				        	Iterator<String[]> vhsTermIterator = vitaminSupplementIncludeWordsList.iterator();
				        	
				        	while (vhsTermIterator.hasNext()) {
				        		String[] vhsTerm = vhsTermIterator.next();
				        		
				        		if (wtText.equals(vhsTerm[0])) {
				        			
				        			if ((vhsTerm.length) > 1 && wtmi.hasNext()) { // compare the rest of the words in the term
				        				
				        				boolean matchingSoFar = true;
				        				
				        				SortedMap<Integer, WordToken> remainingWords = wtSortedMap.tailMap(wtKey);
				        				Iterator<Integer> rwmj = remainingWords.keySet().iterator();
				        				
				        				int j = 1;
				        				WordToken mostRecentWordToken = remainingWords.get(rwmj.next());;

				        				
				        				while ((j < vhsTerm.length) &&  rwmj.hasNext() && matchingSoFar) {
				        					
				        					mostRecentWordToken = remainingWords.get(rwmj.next());
				        					
				        					if (!vhsTerm[j].equals(mostRecentWordToken.getCoveredText().trim().toLowerCase())) {
				        						matchingSoFar = false;
				        					}
				        					
				        					j++;
				        				}
				        				
				        				if (matchingSoFar) {
				        					String matchedText = vhsTerm[0];
				        					
				        					for (int k = 1; k < vhsTerm.length; k++) {
				        						matchedText = matchedText + " " + vhsTerm[k];
				        					}
				        					
				        					at = annotateVHS(annotations, matchedText, wt.getBegin(), mostRecentWordToken.getEnd());

				        					if (at.firstSpanContaining(annotations) == null) {
				        						at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
				        						annotations.add(at);
				        					}
				        				}

				        			}
				        			else {
				        				at = annotateVHS(annotations, wtText, wt.getBegin(), wt.getEnd());

			        					if (at.firstSpanContaining(annotations) == null) {
			        						at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
			        						annotations.add(at);
			        					}
				        			}
				        		}
				        		
				        	}
				        			
				        	
				        	
				        }
			/*	        
				        // old sentence based matching code for VHS dictionary
				        Iterator<Sentence> si = JCasUtil.iterator(jCas, Sentence.class);
				        
				        while (si.hasNext()) {
				        	Sentence s = si.next();

				        	sps.print("Sentence[" + s.getSentenceNumber() + "](" + s.getBegin() + ", " + s.getEnd() + "):");
							sps.println(docText.substring(s.getBegin(), s.getEnd()));
							
							Iterator<String> vwi = vitaminSupplementIncludeWords.iterator();
							while (vwi.hasNext()) {
								
								String vhs = vwi.next();
								
								int index = s.getCoveredText().toLowerCase().trim().indexOf(vhs.toLowerCase().trim());
								
								if (index >= 0) {
									System.out.println("Sentence[" + s.getSentenceNumber() + "] contains: " +vhs);
									
									at = annotateVHS(annotations, vhs, s.getBegin() + index, s.getBegin() + index + vhs.length()); // this isn't perfect as the text has been trimmmed.
									
									if (at.firstSpanContaining(annotations) == null) {
									    at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
								        annotations.add(at);
									}
								    
								    while ((index >= 0) && (index + 1 + vhs.toLowerCase().trim().length() < s.getCoveredText().length())) {
								    	
								    	index = s.getCoveredText().toLowerCase().trim().indexOf(vhs.toLowerCase().trim(), index + 1);
								    	
								    	if (index >= 0) {
											System.out.println("Sentence[" + s.getSentenceNumber() + "] contains: " +vhs);
											
											at = annotateVHS(annotations, vhs, s.getBegin() + index, s.getBegin() + index + vhs.length()); // this isn't perfect as the text has been trimmmed.
											
											if (at.firstSpanContaining(annotations) == null) {
											    at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
										        annotations.add(at);
											}
								    	}
								    }
								}
							}

							sentencesMap.put(s.getSentenceNumber(), s);
							//System.out.println(s);
				        }
				        /*
				        
				        /*
				        Iterator<MedicationMention> ei = JCasUtil.iterator(jCas, MedicationMention.class);
				        
				        while (ei.hasNext()) {
				        		MedicationMention mm = ei.next();
		        		
						    System.out.println(docText.substring(mm.getBegin(), mm.getEnd()));
				        		
				        		System.out.println(mm.toString());
				        		
				        		FSArray fsA = mm.getOntologyConceptArr();
				        		for (int j = 0; j < fsA.size(); j++) {
				        			OntologyConcept ontologyConcept = mm.getOntologyConceptArr(j);
				        			System.out.println(ontologyConcept.toString());
				        		}
				        		
				        		annotateUMLSConcept(annotations, mm);
				        }
				        */
				        
				        DocumentPath docPath = JCasUtil.selectSingle(jCas, DocumentPath.class);
				        
				        File docFile = new File(docPath.getDocumentPath());
				        
				        String docFileName = docFile.getName();
				        
				        File outputFile = new File(outputDirectory,  docFileName + ".knowtator.xml");
				        
				        FileWriter fw = new FileWriter(outputFile);
				        XMLStreamWriter xsw = xof.createXMLStreamWriter(fw);
				        
				        KnowtatorUtil.writeAnnotatedTexts(xsw, annotations, docFileName);
				        				        
				        
				        xsw.flush();
						fw.flush();
						//xsw.close();
						//fw.close();
						
						addAnnotationsToSummary(annotations);
				        
					} catch (UIMAException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (XMLStreamException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						
					
					
					//CAS aCas = UIMAFramework.
					
					//CasIOUtil.readXmi(aCas, inputFiles[i]);

					//cc.
				}
				
		        sps.flush();
		        sps.close();
				
				writeTermSummary(outputDirectory);
				writeSet(new File(outputDirectory, "DiagnosisWordsExcluded.txt"), diagnosisWordsExcluded);
				writeSet(new File(outputDirectory, "MedicationWordsExcluded.txt"), medicationWordsExcluded);
				writeSet(new File(outputDirectory, "SignsSymptomsWordsExcluded.txt"), sspWordsExcluded);
				writeSet(new File(outputDirectory, "TestProcedureWordsExcluded.txt"), testProcedureWordsExcluded);
				writeSet(new File(outputDirectory, "TreatmentProcedureWordsExcluded.txt"), treatmentProcedureWordsExcluded);

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
		
		System.out.println("about to exit from main");
	}

}
