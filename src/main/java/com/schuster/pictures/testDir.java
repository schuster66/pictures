package com.schuster.pictures;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class testDir {
	
	private static Logger log = LogManager.getLogger();
	
	
	public void traverseDirectory(File node) { 
		if (node == null) return;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		if (node.isDirectory()) {
			String[] subNote = node.list();
			log.info("node = " + node.getName());
			if (subNote == null) {
				log.error("Access probably denied to " + node.getAbsolutePath() + " skipping....");
				return;
			}
			myLabel: for (String filename : subNote) {
				
				log.info("filename = " + filename);
				
				File checkFile = new File(node, filename);
				if (checkFile == null) {
					log.info("checkfile is null");
					continue myLabel;
				}
				if (checkFile.getName().startsWith("\\$")) {
					log.info("Here we are");
				}
				if (checkFile.isDirectory()) {
					log.info("directory = " + checkFile.toPath());
					Path newPath = checkFile.toPath();
					try {
						traverseDirectory(checkFile);
					} catch(NullPointerException e) {
			            System.out.print("NullPointerException Caught");
			            System.out.println(node.getAbsolutePath());
			            e.printStackTrace();
			            System.exit(0);
			        }
				} else {
					//log.info("file = " + checkFile.getAbsolutePath());
				}
			}
		}
	}
	
	public static void main(String[] args) {
		testDir td = new testDir();
		td.traverseDirectory(new File("E:"));
	}

}
