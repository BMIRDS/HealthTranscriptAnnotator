package edu.dartmouth.bmds.util.file;

import java.io.File;
import java.io.FilenameFilter;

public class FilenameExtensionFilter implements FilenameFilter {

	private String extension = null;
	
	public FilenameExtensionFilter(String extension) {
		this.extension = extension;
	}
	
	@Override
	public boolean accept(File dir, String name) {
		return name.toLowerCase().endsWith("." + extension.toLowerCase());
	}

}
