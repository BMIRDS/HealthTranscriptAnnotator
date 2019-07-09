package edu.dartmouth.bmds.medex;

import java.util.Scanner;

public class Drug {
	
	private String filename;
	private String medicationNameText;
	private int    medicationNameBegin;
	private int    medicationNameEnd;
	private String medicationRxCUI;
	private String strengthText;
	private String doseText;
	private String formText;
	private String routeText;
	private String frequencyText;
	private String durationText;
	private String medicationRxNormName;
	private String specificRxCUI;
	private String sentenceText;
	private int    sentenceTextBegin;
	
	static class TextSpan {
		
		TextSpan(String text, int begin, int end) {
			this.text = text;
			this.spanBegin = begin;
			this.spanEnd = end;
		}
		
		private String text;
		private int spanBegin;
		private int spanEnd;
	}
	
	public static Drug parseDrug(String text) {
		
		System.out.println("Drug.parseDrug(" + text + ")");
		
		Drug drug = new Drug();
		
		Scanner sc = new Scanner(text);
		sc.useDelimiter("\t");
		drug.sentenceTextBegin = sc.nextInt();
		sc.skip("\t");
		
		sc.useDelimiter("\\|");
		drug.sentenceText = sc.next();
		
		String medication = sc.next();
		System.out.println(medication);
		
		TextSpan medicationTextSpan = parseTextSpan(medication);
		drug.medicationNameText = medicationTextSpan.text;
		drug.medicationNameBegin = medicationTextSpan.spanBegin;
		drug.medicationNameEnd = medicationTextSpan.spanEnd;
		
		sc.close();
		
		return drug;
	}
	
	private static TextSpan parseTextSpan(String text) {
		
		System.out.println("TextSpan.parseTextSpan(" + text + ")");
		
		String spanText;
		int spanBegin;
		int spanEnd;
		
		Scanner sc = new Scanner(text);
		sc.useDelimiter("\\[");
		spanText = sc.next();
		sc.skip("\\[");
		
		sc.useDelimiter(",");
		//String str1 = sc.next();
		spanBegin = sc.nextInt();
		sc.skip(",");
		
		sc.useDelimiter("\\]");
		//String str2 = sc.next();
		spanEnd = sc.nextInt();
		
		TextSpan textSpan = new TextSpan(spanText, spanBegin, spanEnd);
		
		sc.close();
		
		return textSpan;
	}
	
	
	public String getFilename() {
		return filename;
	}
	
	public String getMedicationName() {
		return medicationNameText;
	}
	
	public int getMedicationNameBegin() {
		return medicationNameBegin;
	}
	
	public int getMedicationNameEnd() {
		return medicationNameEnd;
	}
   
}
