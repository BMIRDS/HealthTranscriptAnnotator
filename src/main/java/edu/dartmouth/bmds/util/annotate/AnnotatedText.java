package edu.dartmouth.bmds.util.annotate;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class AnnotatedText {

	private String id;
	private long spanStart;
	private long spanEnd;
	private String spannedText;
	private String annotatorId = "cTAKES_4.0.0";
	private String annotator = "ORALS";
	private Date creationDate = new Date();
	
	private Annotation annotation = null;
	
	public AnnotatedText(long spanStart, long spanEnd, String spannedText) {
		this.id = KnowtatorUtil.getNewId();
		this.spanStart = spanStart;
		this.spanEnd = spanEnd;
		this.spannedText = spannedText;
	}
	
	public AnnotatedText(long spanStart, long spanEnd, String spannedText, Date creationDate) {
		this.id = KnowtatorUtil.getNewId();
		this.spanStart = spanStart;
		this.spanEnd = spanEnd;
		this.spannedText = spannedText;
		this.creationDate = creationDate;
	}
	
	public void addAnnotation(Annotation annotation) {
		if (this.annotation == null) {
			this.annotation = annotation;
		}
		else {
			throw new RuntimeException("Runtime Exception: adding more than one annotation to this object is not implemented");
		}
	}
	
	public Annotation addAnnotation(String  annotatonClassName) {
		if (this.annotation == null) {
			this.annotation = new Annotation(annotatonClassName);
		}
		else {
			System.err.println("Error: adding more than one annotation to this object is not implemented");
			System.err.println("Fix this, if possible!!!");
			System.err.println("Fix this, if possible!!!");

			//throw new RuntimeException("Runtime Exception: adding more than one annotation to this object is not implemented");
		}
		
		return annotation;
	}
	
	public Annotation getAnnotation() {
		return annotation;
	}
	
	public String getId() {
		return id;
	}
	
	public String getSpannedText() {
		return spannedText;
	}
	
	public String getAnnotator() {
		return annotator;
	}
	
	public void setAnnotator(String annotator) {
		this.annotator = annotator;
	}
	
	public AnnotatedText firstSpanContaining(Set<AnnotatedText> annotations) {
		
		AnnotatedText atMatch = null;
		boolean found = false;
		
		Iterator<AnnotatedText> annotatedTextIterator = annotations.iterator();
		
		while (!found && annotatedTextIterator.hasNext()) {
			atMatch = annotatedTextIterator.next();
			
			found = (this.spanStart >= atMatch.spanStart) && (this.spanEnd <= atMatch.spanEnd);
		}
				
		if (!found) {
			atMatch = null;
		}
		
		return atMatch;
	}
	
	public void writeKnowtator(XMLStreamWriter xsw) throws XMLStreamException {
//		xsw.writeCharacters("\n\t");
		xsw.writeStartElement("annotation");
//		xsw.writeCharacters("\n\t\t");
		
		xsw.writeStartElement("mention");
		xsw.writeAttribute("id", id);
		xsw.writeEndElement();
		
		xsw.writeStartElement("annotator");
		xsw.writeAttribute("id", annotatorId);
		xsw.writeCharacters(annotator);
		xsw.writeEndElement();
		
		xsw.writeStartElement("span");
		xsw.writeAttribute("start", Long.toString(spanStart));
		xsw.writeAttribute("end", Long.toString(spanEnd));
		xsw.writeEndElement();

		xsw.writeStartElement("spannedText");
		xsw.writeCharacters(spannedText);
		xsw.writeEndElement();
		
		xsw.writeStartElement("creationDate");
		xsw.writeCharacters(KnowtatorUtil.formatDate(creationDate));
		xsw.writeEndElement();
		
		xsw.writeEndElement();
		
		if (annotation != null) {
			annotation.writeKnowtator(xsw, this);
		}
 	}
}
