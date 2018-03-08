package knowtator;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

public class KnowtatorTest {
	public static void main(String[] args) {
		Knowtator kt = new Knowtator();
		try {
			kt.createKnowtator("Chris_Long.txt.xmi", "Chris_Long.txt");
			kt.createKnowtator("Linda_Long.txt.xmi", "Linda_Long.txt");
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

