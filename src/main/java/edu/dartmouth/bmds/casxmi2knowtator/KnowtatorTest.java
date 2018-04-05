package edu.dartmouth.bmds.casxmi2knowtator;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

public class KnowtatorTest {
	public static void main(String[] args) {
		
		Knowtator kt = new Knowtator();
		
		try {
			File transcriptInputFolder = new File("/Volumes/ORALS_AnnotationData/input");
			File casXmiInputFolder = new File("/Volumes/ORALS_AnnotationData/output");
			File knowtatorXmlOutputFolder = new File("/Volumes/ORALS_AnnotationData/knowtatorOutput");
			
//			File transcriptInputFolder = new File(args[0]);
//			File casXmiInputFolder = new File(args[1]);
//			File knowtatorXmlOutputFolder = new File(args[2]);
			
			kt.createKnowtator(transcriptInputFolder, casXmiInputFolder, knowtatorXmlOutputFolder);
		} 
		
		
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		catch (XMLStreamException e) {
			e.printStackTrace();
		} 
		
		catch (ParseException e) {
			e.printStackTrace();
		}
	}
}

