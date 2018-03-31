package com.schuster.pictures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CombineDisks2 {
	
	private static Logger log = LogManager.getLogger();
	
	// checks to see if files on Source are on target, but not the other way around
	// If file a is on the source, but not ont he target then it will be listed, but
	// if file a is on target, but not the source then it won't be listed.
	
	public String sourceDrive = "F:/dadsphotos";
	public String targetDrive = "G:";
	
	public String targetBaseDir = "";
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
	
	private long startTime;
	private long endTime;
	
	private boolean silent = true;
	
	public CombineDisks2() {
		
	}
	
	public void setQuiet() {
		this.silent = true;
	}
	public void setNoisy() {
		this.silent = false;
	}
	public void setSourceDrive(String drive) {
		this.sourceDrive = drive;
	}
	public void setTargetDrive(String drive) {
		this.targetDrive = drive;
	}
	public void startBackup() {
		
		this.startTime = System.currentTimeMillis();
		if (!this.silent) {
			if (!(createTargetBaseDir())) {
				log.error("Couldn't create target base directory.....exiting");
			}
		}
		
		this.traverseDirectory(new File(this.sourceDrive));
		
		this.endTime = System.currentTimeMillis();
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
	public void traverseDirectory(File node) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		if (node.isDirectory()) {
			String[] subNote = node.list();
			if (subNote == null) return;
			myLabel: for (String filename : subNote) {
				File checkFile = new File(node, filename);
				if (checkFile.isDirectory()) {
					Path newPath = checkFile.toPath();
					traverseDirectory(checkFile);
				} else {
					//not a directory so it's a file
					// check to see if it's in the hash and the size matches 
					// if not then create it on the target starting with creating all of the directories if
					// they don't already exists
					sourceFileCount++;
					copyFile(checkFile);
					//copyCount++;
					requiredSize = requiredSize + checkFile.length();
					requiredSizeGB = requiredSizeGB + (checkFile.length() / (1024 * 1024 * 1024));	
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
			if (silent) {
				copyCount++;
				log.info("Copy " + file.getAbsolutePath() + " - to - " + targetFile.getAbsolutePath());
			} else {
				try {
					Files.copy(file.toPath(), targetFile.toPath());
					copyCount++;
				} catch (IOException e) {
					log.error(e.toString());
				} catch (InvalidPathException e) {
					log.error("File " + file.getAbsolutePath() + " not copied. something wrong in name" + e.toString() );
				}
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
	
	public void printReport() {
		DecimalFormat df2 = new DecimalFormat("#######.##");
		
		long runTime = (this.endTime - this.startTime) / 1000;
		
		System.out.println("Target file count for hash = " + this.targetFileCount + 
				   " - source file count = " + this.sourceFileCount +
				   " - skipped count = " + this.skipCount +
				   " - copied count = "  + this.copyCount +
				   " - required size = " + df2.format(this.requiredSize/1000000000) + " gig " + 
				   " - required Size GB = " + df2.format(this.requiredSizeGB) + 
		           " - run time = " + runTime + " seconds");
		
	}
	
	
	public static void main(String[] args) {
		
		DecimalFormat df2 = new DecimalFormat("#######.##");
		
		long startTime = System.currentTimeMillis();
		
		CombineDisks2 cd = new CombineDisks2();
		
		cd.startBackup();
		
		//cd.traverseDirectory(new File(cd.sourceDrive));
		
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
