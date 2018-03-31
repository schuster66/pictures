package com.schuster.pictures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class combineDisks {
	
	private static Logger log = LogManager.getLogger();
	
	// checks to see if files on Source are on target, but not the other way around
	// If file a is on the source, but not ont he target then it will be listed, but
	// if file a is on target, but not the source then it won't be listed.
	
	public String sourceDrive = "F:/homepc/20130924/Downloads";
	public String targetDrive = "E:";
	
	public String targetBaseDir = "fromBlack";
	public String targetDir;
	
	//public String sourceExcludeFile="G:/tomsbackups.exclude";
	//public HashSet<Path> excludedDirs = new HashSet<>();
	
	//public String outputFileName="F:/tomsbackups.output";
	//public boolean writeToFile=true;
	//public BufferedWriter writer;
	
	//public int excludedCount = 0;
	
	public int sourceFileCount = 0;
	public int targetFileCount = 0;
	public int skipCount = 0;
	public int copyCount = 0;
	
	public float requiredSize = 0;
	public double requiredSizeGB = 0;
	
	private Hashtable<String,Long> dirHash = new Hashtable<>();
	private Hashtable<String,HashSet<Long>> fileHash = new Hashtable<>();
	
	public combineDisks() {
		if (!(createTargetBaseDir())) {
			log.error("Couldn't create target base directory.....exiting");
		}
	}
	
	public boolean  createTargetBaseDir() {
		log.trace("Entering createTargetBaseDir");
		boolean created = false;
		String targetDir = this.targetDrive + System.getProperty("file.separator") + this.targetBaseDir;
		this.targetDir = targetDir;
		log.info("Make directory " + targetDir);
		return createDirectory(targetDir);
	}
	
	public boolean createDirectory(String filename) {
		log.trace("Entering createDirectory for fileName " + filename);
		
		File test = new File(filename);
		if (test.isDirectory()) {
			return true;
		}
		
		boolean result = false;
		try {
			test.mkdir();
			result = true;
		} catch (SecurityException se) {
			log.error(se.toString());
		}
		
		return result;
	}
	
	/*
	public void buildExcludedDirsList() {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File(this.sourceExcludeFile)));
			String line = null;
			while ((line = br.readLine()) != null) {
				line.trim();
				this.excludedDirs.add(new File(line).toPath());
			}
		 
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
	
	// If check then check if the file exists.  If not check then put filename in hash
	// 
	public void traverseDirectory(File node, boolean copy) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		if (node.isDirectory()) {
			String[] subNote = node.list();
			if (subNote == null) return;
			myLabel: for (String filename : subNote) {
				File checkFile = new File(node, filename);
				if (checkFile.isDirectory()) {
					Path newPath = checkFile.toPath();
					traverseDirectory(checkFile, copy);
				} else {
					//not a directory so it's a file
					if (copy) {
						// check to see if it's in the hash and the size matches 
						// if not then create it on the target starting with creating all of the directories if
						// they don't already exists
						sourceFileCount++;
						if (fileHash.containsKey(checkFile.getName())) {
							if (fileHash.get(checkFile.getName()).contains(checkFile.length())) {
								skipCount++;
							} else {
								//System.out.println(checkFile.getName()  + " exists but is a different size");
								log.trace("Copy " + checkFile.getAbsolutePath());
								copyFile(checkFile);
								//copyCount++;
								requiredSize = requiredSize + checkFile.length();
								requiredSizeGB = requiredSizeGB + (checkFile.length() / (1024 * 1024 * 1024));
							}
						} else {
							log.trace("Copy " + checkFile.getAbsolutePath());
							copyFile(checkFile);
							//copyCount++;
							requiredSize = requiredSize + checkFile.length();
							requiredSizeGB = requiredSizeGB + (checkFile.length() / (1024 * 1024 * 1024));
						}
					} else {
						// just build a hash with the file as the key and the size as the value
						if (!(fileHash.containsKey(checkFile.getName()))) {
							fileHash.put(checkFile.getName(),new HashSet<Long>());
						}
						HashSet<Long> myHash = fileHash.get(checkFile.getName());
						myHash.add(checkFile.length());
						//log.info("size 1 " + myHash.size() + " - size 2 = " + fileHash.get(checkFile.getName()).size());
						//fileHash.get(checkFile.getName()).add(checkFile.length());
						//fileHash.put(checkFile.getName(),checkFile.length());
						targetFileCount++;
					}
					
				}
			}
		}
	}
	
	public void copyFile(File file) {
		if (file == null) return;
		log.info("Entering copyFile for file = " + file.getAbsolutePath());
		// Lets get the full path of the file and start creating the path on the target
		String fileFullPath = file.getAbsolutePath();
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] parts = fileFullPath.split(pattern);
		String newDirectory = this.targetDir;
		if (parts.length > 2) {
			for (int index = 1; index < parts.length - 1; index++) {
				newDirectory = newDirectory + System.getProperty("file.separator") + parts[index];
				if (!(createDirectory(newDirectory))) {
					log.error("Couldn't create directory " + newDirectory + " so cancelling file copy for file " + file.getAbsolutePath());
					return;
				}
				log.info("created directory" + newDirectory);
			}
		}
		File targetFile = new File(newDirectory + System.getProperty("file.separator") + file.getName());
		
		if (!targetFile.exists()) {
			try {
				Files.copy(file.toPath(), targetFile.toPath());
				copyCount++;
			} catch (IOException e) {
				log.error(e.toString());
			}
		} 
		
	}
	
	/*
	public void readExcludeFile() {
		
	}
	
	public boolean isPictureFile(String filename) {
		Matcher matcher = this.picPattern.matcher(filename);
		return matcher.matches();
	}
	
	public boolean isMovieFile(String filename) {
		Matcher matcher = this.moviePattern.matcher(filename);
		return matcher.matches();
	}
	
	public boolean isDocumentFile(String filename) {
		Matcher matcher = this.docPattern.matcher(filename);
		return matcher.matches();
	}
	*/
	
	
	public static void main(String[] args) {
		
		DecimalFormat df2 = new DecimalFormat("#######.##");
		
		long startTime = System.currentTimeMillis();
		
		combineDisks cd = new combineDisks();
		
		cd.traverseDirectory(new File(cd.targetDrive), false);
		cd.traverseDirectory(new File(cd.sourceDrive), true);
		
		long runTime = (System.currentTimeMillis() - startTime) / 1000;
		
		System.out.println("Target file count for hash = " + cd.targetFileCount + 
						   " - source file count = " + cd.sourceFileCount +
						   " - skipped count = " + cd.skipCount +
						   " - copied count = "  + cd.copyCount +
						   " - required size = " + df2.format(cd.requiredSize/1000000000) + " gig " + 
						   " - required Size GB = " + df2.format(cd.requiredSizeGB) + 
				           " - run time = " + runTime + " seconds");
		
	}

}
