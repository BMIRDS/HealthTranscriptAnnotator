package edu.dartmouth.bmds.util.annotate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class KnowtatorUtil {
	
	public static String DEFAULT_ID_PREFIX = "KnowtatorUtil_Instance_";
	private static long idNumber = 1000;
	
	private static DateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
	
	public static String getNewId() {
		return DEFAULT_ID_PREFIX + idNumber++;
	}
	
	public static String formatDate(Date date) {
		return dateFormatter.format(date);
	}

	public static void writeAnnotatedTexts(XMLStreamWriter xsw, List<AnnotatedText> annotatedTexts, String annotatedTextFileName) throws XMLStreamException {
		xsw.writeStartElement("annotations");
		xsw.writeAttribute("textsource", annotatedTextFileName);
		
		Iterator<AnnotatedText> annotatedTextIterator = annotatedTexts.iterator();
		
		while (annotatedTextIterator.hasNext()) {
			AnnotatedText annotatedText = annotatedTextIterator.next();
			annotatedText.writeKnowtator(xsw);
		}
		
		xsw.writeEndElement();
		
	}
}
