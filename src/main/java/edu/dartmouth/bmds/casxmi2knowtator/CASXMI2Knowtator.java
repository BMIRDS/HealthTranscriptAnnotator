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
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
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
	
	public static AnnotatedText annotateUMLSConcept(Set<AnnotatedText> annotations, EventMention em) {
		
		//AnnotatedText at = new AnnotatedText(em.getBegin(), em.getEnd(), em.getCAS().getDocumentText().substring(em.getBegin(), em.getEnd()));
		AnnotatedText at = new AnnotatedText(em.getBegin(), em.getEnd(), em.getCoveredText());
		
		annotations.add(at);
		
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
	
	public static AnnotatedText annotateAnatomicalSiteMention(Set<AnnotatedText> annotations, AnatomicalSiteMention am) {
		
		AnnotatedText at = new AnnotatedText(am.getBegin(), am.getEnd(), am.getCAS().getDocumentText().substring(am.getBegin(), am.getEnd()));
		
		annotations.add(at);
		
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
		options.addRequiredOption("i", "inputDirectory", true, "Directory containing the CAS XPI files to be converted");
		options.addRequiredOption("o", "outputDirectory", true, "Directory where converted CAS XPI files will be stored in the Knowtator format");
		
		try {
	        // parse the command line arguments
	        CommandLine line = parser.parse( options, args );
	        
	        if (line.hasOption("h")) {
	        		HelpFormatter helpFormatter = new HelpFormatter();
	        		helpFormatter.printHelp( "casxmi2knowtator", options );
	        }
	        
	        File inputDirectory = null;
	        File outputDirectory = null;
	        
	        try {
	        		inputDirectory = new File(line.getOptionValue("i"));
				System.out.println("input directory = " + inputDirectory.getCanonicalPath());
			
	        
				outputDirectory = new File(line.getOptionValue("o"));
				System.out.println("output directory = " + outputDirectory.getCanonicalPath());
				
				System.out.println("trying new stuff");

				TreeSet<String> excludeWords = new TreeSet<String>();
				TreeSet<String> wordsExcluded = new TreeSet<String>();
				
				excludeWords.add("hmm");
				excludeWords.add("tum");
				
				File excludeWordsFile = new File(inputDirectory, "ExcludeWords.txt");
				
				if (excludeWordsFile.exists()) {
					List<String> words = Files.readAllLines(excludeWordsFile.toPath(), StandardCharsets.UTF_8);

					for (String word : words) {
						excludeWords.add(word.trim().toLowerCase());
					}
					excludeWords.remove("addiction");
					excludeWords.remove("aids");
					excludeWords.remove("allergy");
					excludeWords.remove("ambien");
					excludeWords.remove("anxiety");
					excludeWords.remove("appendix");
					excludeWords.remove("arm");
					excludeWords.remove("arms");
					excludeWords.remove("arthritis");
					excludeWords.remove("bacteria");
					excludeWords.remove("belly");
					excludeWords.remove("bleeding");
					excludeWords.remove("blood");
					excludeWords.remove("bone");
					excludeWords.remove("bones");
					excludeWords.remove("brain");
					excludeWords.remove("breast");
					excludeWords.remove("breasts");
					excludeWords.remove("breathing");
					excludeWords.remove("burn");
					excludeWords.remove("burning");
					excludeWords.remove("burns");
					excludeWords.remove("cancer");
					excludeWords.remove("cardiac");
					excludeWords.remove("cholesterol");
					excludeWords.remove("cialis");
					excludeWords.remove("colon");
					excludeWords.remove("cut");
					excludeWords.remove("cuts");
					excludeWords.remove("cvs");
					excludeWords.remove("dead");
					excludeWords.remove("death");
					excludeWords.remove("depression");
					excludeWords.remove("diabetes");
					excludeWords.remove("diagnosis");
					excludeWords.remove("die");
					excludeWords.remove("disability");
					excludeWords.remove("disease");
					excludeWords.remove("drop");
					excludeWords.remove("drops");
					excludeWords.remove("drug");
					excludeWords.remove("drugs");
					excludeWords.remove("ear");
					excludeWords.remove("ears");
					excludeWords.remove("ejaculation");
					excludeWords.remove("enzyme");
					excludeWords.remove("exercise");
					excludeWords.remove("exercises");
					excludeWords.remove("eye");
					excludeWords.remove("eyes");
					excludeWords.remove("face");
					excludeWords.remove("facial");
					excludeWords.remove("feel");
					excludeWords.remove("finding");
					excludeWords.remove("finger");
					excludeWords.remove("fingers");
					excludeWords.remove("fit");
					excludeWords.remove("foot");
					excludeWords.remove("genetic");
					excludeWords.remove("glucose");
					excludeWords.remove("hair");
					excludeWords.remove("hand");
					excludeWords.remove("hands");
					excludeWords.remove("head");
					excludeWords.remove("healing");
					excludeWords.remove("hearing");
					excludeWords.remove("heart");
					excludeWords.remove("hearts");
					excludeWords.remove("hydrocodone");
					excludeWords.remove("ill");
					excludeWords.remove("infection");
					excludeWords.remove("infections");
					excludeWords.remove("injury");
					// need to review ones after above
					excludeWords.remove("medication");
					excludeWords.remove("medications");
					excludeWords.remove("medicine");
					excludeWords.remove("medicines");
					excludeWords.remove("paxil");
					excludeWords.remove("pill");
					excludeWords.remove("pills");
					excludeWords.remove("prozac");
					excludeWords.remove("tablet");
					excludeWords.remove("tablets");
					excludeWords.remove("tramadol");
					excludeWords.remove("vaccine");
					excludeWords.remove("valium");
					excludeWords.remove("zoloft");
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
				
				for (int i = 0; i < inputFiles.length; i++) {
					System.out.println("input file[" + i + "] = " + inputFiles[i].getCanonicalPath());
					
					FileInputStream fis = new FileInputStream(inputFiles[i]);
					
					JCas jCas;
					try {
						jCas = JCasFactory.createJCas(tsd);
						
				        XmiCasDeserializer.deserialize(fis, jCas.getCas());

				        HashSet<AnnotatedText> annotations = new HashSet<AnnotatedText>();
 				        
				        String docText = jCas.getDocumentText();
				        
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
				        		
				        		
				        		if (!excludeWords.contains(em.getCoveredText().trim().toLowerCase())) {
				        			annotateUMLSConcept(annotations, em);
				        		}
				        		else {
				        			wordsExcluded.add(em.getCoveredText().trim().toLowerCase());
				        		}
				        }
				        
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
				        		
				        		if (!excludeWords.contains(am.getCoveredText().trim().toLowerCase())) {
				        			annotateAnatomicalSiteMention(annotations, am);
				        		}
				        		else {
				        			wordsExcluded.add(am.getCoveredText().trim().toLowerCase());
				        		}
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
						xsw.close();
						fw.close();
						
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
				
				writeTermSummary(outputDirectory);
				writeSet(new File(outputDirectory, "WordsExcluded.txt"), wordsExcluded);
	        }
	        catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ResourceInitializationException e1) {
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
