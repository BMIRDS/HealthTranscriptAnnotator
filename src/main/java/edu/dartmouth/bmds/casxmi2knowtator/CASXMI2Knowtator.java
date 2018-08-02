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

public class CASXMI2Knowtator {

	private static HashMap<String, SortedMap<String, Integer>> termSummaryMap = new HashMap<String, SortedMap<String, Integer>>();
	
	// "T116", "T125", "T131"?
	// "T129", "T195" May 7,2018
	protected static String[] tuiToFilterForMedications = {"T122", "T123", "T197"};
	protected static String[] tuiToFilterForDiagnoses = {};
	protected static String[] tuiToFilterForSignsSymptoms = {};
	
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
								
				if (line.hasOption("e")) {

					File commonWordsFile = new File(configDirectory, "CommonWords.txt");

					if (commonWordsFile.exists()) {
						List<String> words = Files.readAllLines(commonWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							medicationExcludeWords.add(word.trim().toLowerCase());
							sspExcludeWords.add(word.trim().toLowerCase());
							diagnosisExcludeWords.add(word.trim().toLowerCase());
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
					
					File diagnosisExcludeWordsFile = new File(configDirectory, "DiagnosisSymptomsExcludeWords.txt");

					if (diagnosisExcludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(diagnosisExcludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							diagnosisExcludeWords.add(word.trim().toLowerCase());
						}
					}
					
					File diagnosisIncludeWordsFile = new File(configDirectory, "DiagnosisIncludeWords.txt");

					if (sspIncludeWordsFile.exists()) {
						List<String> words = Files.readAllLines(diagnosisIncludeWordsFile.toPath(), StandardCharsets.UTF_8);

						for (String word : words) {
							diagnosisExcludeWords.remove(word.trim().toLowerCase());
						}
					}

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
					
					Map<String, Sentence> sentencesMap = new HashMap<String, Sentence>();
					
					JCas jCas;
					try {
						jCas = JCasFactory.createJCas(tsd);
						
				        XmiCasDeserializer.deserialize(fis, jCas.getCas());

				        HashSet<AnnotatedText> annotations = new HashSet<AnnotatedText>();
 				        
				        String docText = jCas.getDocumentText();
				        
				        Iterator<Sentence> si = JCasUtil.iterator(jCas, Sentence.class);
				        
				        while (si.hasNext()) {
				        	Sentence s = si.next();

				        	sps.print("Sentence[" + s.getSentenceNumber() + "](" + s.getBegin() + ", " + s.getEnd() + "):");
							sps.println(docText.substring(s.getBegin(), s.getEnd()));

							sentencesMap.put(s.getSegmentId(), s);
							//System.out.println(s);
				        }
				        
				        Iterator<EventMention> ei = JCasUtil.iterator(jCas, EventMention.class);
				        
				        AnnotatedText at;
				        
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
							
							Sentence s = sentencesMap.get(em.getSentenceID());
															
							if (em instanceof MedicationMention) {
								
								at = annotateUMLSConcept(annotations, em);
								
								if (medicationExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
									at.setAnnotator(at.getAnnotator() + "_medicationExcludedWords");
									medicationWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForMedications)) {
									at.setAnnotator(at.getAnnotator() + "_medicationFilteredTUI");
								}
								else {
									at.getAnnotation().setAnnotationClass("Discussion_of_Medications");
									annotations.add(at);
								}
							}
							else if (em instanceof DiseaseDisorderMention) {
								at = annotateUMLSConcept(annotations, em);
								
								if (diagnosisExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
									at.setAnnotator(at.getAnnotator() + "_diagnosisExcludedWords");
									diagnosisWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForDiagnoses)) {
									at.setAnnotator(at.getAnnotator() + "_diagnosisFilteredTUI");
								}
								else if (s.getCoveredText().contains("I ") || s.getCoveredText().contains("you ")) {
									
								}
								else {
									at.getAnnotation().setAnnotationClass("Diagnosis");
									at.setAnnotator(at.getAnnotator() + "_unimplemented");
									annotations.add(at);
								}
				        	}
							else if (em instanceof SignSymptomMention) {
								at = annotateUMLSConcept(annotations, em);
								
								if (sspExcludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
									at.setAnnotator(at.getAnnotator() + "_sspExcludedWords");
									sspWordsExcluded.add(em.getCoveredText().trim().toLowerCase());
								}
								else if (at.getAnnotation().containsAttributeValue("TUI", tuiToFilterForSignsSymptoms)) {
									at.setAnnotator(at.getAnnotator() + "_sspFilteredTUI");
								}
								else {
									at.getAnnotation().setAnnotationClass("Signs_Symptoms_and_Problems");
									at.setAnnotator(at.getAnnotator() + "_unimplemented");
									annotations.add(at);
								}
				        	}
							else if (em instanceof ProcedureMention) {
								//at.setAnnotator(at.getAnnotator() + "_unimplemented");
								//annotations.add(at);
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
