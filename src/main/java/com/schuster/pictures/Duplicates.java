package com.schuster.pictures;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Duplicates {
	
	private static Logger log = LogManager.getLogger();
	private Pattern picPattern = Pattern.compile("(.*(\\.(?i)(jpg|gif|jpeg))$)");
	
	private Hashtable<String,File> fileHash = new Hashtable<>();
	private int numDeleted = 0;
	private int numKept = 0;
	
	public void traverseDirectory(File node) { 
		if (node == null) return;
		log.info(node.getAbsolutePath());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		if (node.isDirectory()) {
			String[] subNote = node.list();
			if (subNote == null) {
				log.error("Access probably denied to " + node.getAbsolutePath() + " skipping....");
				return;
			}
			myLabel: for (String filename : subNote) {
				//log.info("filename = " + filename);
				File checkFile = new File(node, filename);
				if (checkFile.isDirectory()) {
					Path newPath = checkFile.toPath();
					try {
						traverseDirectory(checkFile);
					} catch(NullPointerException e) {
			            System.out.print("NullPointerException Caught");
			            System.out.println(node.getAbsolutePath());
			            e.printStackTrace();
			        }
				} else {
					if (isPictureFile(checkFile.getName())) {
						
						String md5 = "";
						try (InputStream is = Files.newInputStream(checkFile.toPath())) {
						    md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
						    if (fileHash.containsKey(md5)) {
						    	System.out.println("Duplicate deleting " + checkFile.toPath().toString() + " - keeping --> " + fileHash.get(md5).toPath().toString());
						    	checkFile.delete();
						    	numDeleted++;
						    } else {
						    	fileHash.put(md5, checkFile);
						    	numKept++;
						    }
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//System.out.println(checkFile.getName() + " - " + md5);
						
						
						
					}

				}
			}
		}
	}
	
	public boolean isPictureFile(String filename) {
		Matcher matcher = this.picPattern.matcher(filename);
		return matcher.matches();
	}
	
	public static void main(String[] args) {
		
		Duplicates d = new Duplicates();
		d.traverseDirectory(new File("/I:/picsByDate/2002/"));
		System.out.println("Deleted " + d.numDeleted);
		System.out.println("Kept: " + d.numKept);
		
	}

}
