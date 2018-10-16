package edu.dartmouth.bmds.util.annotate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

public class Annotation {
	
	//private String id;
	private String annotatonClassName;
	private MultiValuedMap<String, String> attributes;
	
	//private long idValue = 0;
	
	public Annotation(String className) {
	//	this.id = newId();
		this.annotatonClassName = className;
		attributes = new HashSetValuedHashMap<String, String>();
	}
	
	public String getAnnotationClass() {
		return annotatonClassName;
	}
	
	public void setAnnotationClass(String className) {
		annotatonClassName = className;
		
	}
 	
	public boolean addAttribute(String name, String value) {
		
		return attributes.put(name, value);
	}
	
	public void writeKnowtator(XMLStreamWriter xsw, AnnotatedText annotatedText) throws XMLStreamException {
		Iterator<String> attributeNamesIterator = attributes.keySet().iterator();
		
		ArrayList<String> ids = new ArrayList<String>(attributes.keys().size());
		
		while (attributeNamesIterator.hasNext()) {
			String name = attributeNamesIterator.next();
			
			Collection<String> values = attributes.get(name);
			
			Iterator<String> valuesIterator = values.iterator();
			
			while (valuesIterator.hasNext()) {
				String value = valuesIterator.next();
				String id = KnowtatorUtil.getNewId();
				ids.add(id);
				
				xsw.writeStartElement("stringSlotMention");
				xsw.writeAttribute("id", id);
				
				xsw.writeStartElement("mentionSlot");
				xsw.writeAttribute("id", name);
				xsw.writeEndElement();
				
				xsw.writeStartElement("stringSlotMentionValue");
				xsw.writeAttribute("value", value);
				xsw.writeEndElement();
				
				xsw.writeEndElement();
				
			}
		}
		
		xsw.writeStartElement("classMention");
		xsw.writeAttribute("id", annotatedText.getId());
		
		Iterator<String> slotMentionIdsIterator = ids.iterator();
		
		while (slotMentionIdsIterator.hasNext()) {
			String slotMentionId = slotMentionIdsIterator.next();
			
			xsw.writeStartElement("hasSlotMention");
			xsw.writeAttribute("id", slotMentionId);
			xsw.writeEndElement();
		}
		
		xsw.writeStartElement("mentionClass");
		xsw.writeAttribute("id", annotatonClassName);
		xsw.writeCharacters(annotatedText.getSpannedText());
		xsw.writeEndElement();
		
		xsw.writeEndElement();

	}

	public boolean containsAttributeValue(String attributeName, String[] valuesToFilter) {
		
		Collection<String> values = attributes.get(attributeName);
			
		for (int i = 0; i < valuesToFilter.length; i++) {
			if (values.contains(valuesToFilter[i])) {
				return true;
			}
		}
		
		return false;
	}

	public boolean containsAttributeValueSubstring(String attributeName, String[] valuesToFilter) {
		
		Collection<String> values = attributes.get(attributeName);
		
		for (int i = 0; i < valuesToFilter.length; i++) {
			Iterator<String> j = values.iterator();
			
			while (j.hasNext()) {
				if (j.next().contains(valuesToFilter[i])) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	//private String newId() {
	//	return "CASXMI2Knowtator_Instance_" + idValue++;
	//}
}
