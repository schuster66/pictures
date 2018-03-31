package com.schuster.pictures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class BackupMain {
	
	private static Logger log = LogManager.getLogger();
	
	private String sourceDirectory = "D:/";
	private String destinationDirectory = "I:";
	
	public String sourceExcludeFile="D:/tomsbackups/tomsbackups.exclude"; 
	public HashSet<Path> excludedDirs = new HashSet<>();
	
	public boolean copyPics = false;
	public boolean copyDocs = false;
	public boolean copyMovies = true;
	
	public long sizeRequired = 0L;
	
	//private Pattern picPattern = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|jpeg))$)");
	//private Pattern moviePattern = Pattern.compile("([^\\s]+(\\.(?i)(mp4|avi))$)");
	//private Pattern docPattern = Pattern.compile("([^\\s]+(\\.(?i)(doc|pdf|txt|xls|ppt))$)");
	private Pattern docPattern = Pattern.compile("(.*(\\.(?i)(doc|pdf|txt|xls|ppt))$)");
	private Pattern picPattern = Pattern.compile("(.*(\\.(?i)(jpg|gif|jpeg))$)");
	private Pattern moviePattern = Pattern.compile("(.*(\\.(?i)(mp4|avi))$)");
	
	public int copied = 0;
	public int skipped = 0;
	public int excludedCount = 0;
	
	public int picsCopiedCount = 0;
	public int moviesCopiedCount = 0;
	public int docsCopiedCount = 0;
	
	public int picsSkippedCount = 0;
	public int moviesSkippedCount = 0;
	public int docsSkippedCount = 0;
	public int otherSkippedCount = 0;
	
	public String pictureBaseDirectory;
	private String movieBaseDirectory;
	private String docBaseDirectory;
	
	private Hashtable<String,HashSet<Long>> fileExistsHash = new Hashtable<>();	
	
	private Hashtable<String,Long> copiedHash = new Hashtable<>();
	
	// Set this to true and then look at log to see what would be copied.  Good for building exclude list
	private boolean silent = true;
	
	//Constructor
	public BackupMain() {
		//this.createBaseDirs();
		//this.buildExcludeHash();
	}
	
	public void setSilent() {
		this.silent = true;
	}
	public void setNoisy() {
		this.silent = false;
	}
	
	public void setSource(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}
	
	public void setTarget(String targetDirectory) {
		this.destinationDirectory = targetDirectory;
	}
	
	public void setExcludedFile(String excludedFileName) {
		this.sourceExcludeFile = excludedFileName;
	}
	
	public void setPhoto() {
		this.copyPics = true;
	}
	public void setMovie() {
		this.copyMovies = true;
	}
	public void setDoc() {
		this.copyDocs = true;
	}
	
	public void buildExcludeHash() {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File(this.sourceExcludeFile)));
			String line = null;
			while ((line = br.readLine()) != null) {
				line.trim();
				if (line.matches(".*\\w+.*")) {
					String fileName = sourceDirectory + System.getProperty("file.separator") + line;
					this.excludedDirs.add(new File(fileName).toPath());
					log.info("Excluding directory " + fileName);
				}
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
	
	public void startBackup() {
		log.info("Starting backup from source directory " + this.sourceDirectory);
		this.createBaseDirs();
		traverseDirectory(new File(this.sourceDirectory));
	}
	
	
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
					for (Path p : this.excludedDirs ) {
						if (newPath.startsWith(p)) {
							excludedCount++;
							continue myLabel;
						}
					}
					try {
						traverseDirectory(checkFile);
					} catch(NullPointerException e) {
			            System.out.print("NullPointerException Caught");
			            System.out.println(node.getAbsolutePath());
			            e.printStackTrace();
			        }
				} else {
					if (isPictureFile(checkFile.getName())) {
						if (copyPics) {
							String[] yearMonth = getYearMonth(checkFile);
							String targetDirString = this.pictureBaseDirectory  + System.getProperty("file.separator") + yearMonth[0];
							createDirectory(targetDirString);
							targetDirString = targetDirString + System.getProperty("file.separator") + yearMonth[1];
							File targetDir = createDirectory(targetDirString);
							if (copyFile(checkFile, targetDir)) picsCopiedCount++; else picsSkippedCount++;	
						} else {
							picsSkippedCount++;
						}
					} else if (isMovieFile(checkFile.getName())){
						if (copyMovies) {
							String targetDirString = this.movieBaseDirectory + System.getProperty("file.separator") + this.getYearFromLastModified(checkFile);
							File targetDir = createDirectory(targetDirString);
							if (copyFile(checkFile, targetDir)) moviesCopiedCount++; else moviesSkippedCount++;
						} else {
							moviesSkippedCount++;
						}
					} else if (isDocumentFile(checkFile.getName())) {
						if (copyDocs) {
							String targetDirString = this.docBaseDirectory + System.getProperty("file.separator") + this.getYearFromLastModified(checkFile);
							File targetDir = createDirectory(targetDirString);
							if (copyFile(checkFile, targetDir)) docsCopiedCount++; else docsSkippedCount++ ;
						} else {
							docsSkippedCount++;
						}
					} else {
						otherSkippedCount++;
						//System.out.println("Not a known copy file type " + checkFile.getAbsolutePath());
					}
				}
			}
		}
	}
	
	
	public String[] getYearMonth(File file) {
		String[] valueArray = new String[2];
		Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(file);
			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			if (directory != null) {
				Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				if (date != null) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy");
					dateFormat1.setTimeZone(cal.getTimeZone());
					SimpleDateFormat dateFormat2 = new SimpleDateFormat("MM");
					dateFormat2.setTimeZone(cal.getTimeZone());
					
					valueArray[0] = dateFormat1.format(cal.getTime());
					valueArray[1] = dateFormat2.format(cal.getTime());
					
					return valueArray;
				}
			} else {
				// get the date from the lastmodified time of the file
				valueArray[0] = getYearFromLastModified(file);
				valueArray[1] = "NOEXIF";
				return valueArray;
			}
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("ImageProcessingException for " + file.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("IOException for " + file.getAbsolutePath());
		} catch (NegativeArraySizeException e) {
			System.out.println("NegativeArraySizeException for " + file.getAbsolutePath());
			//e.printStackTrace();
		}
		
		valueArray[0] = getYearFromLastModified(file);
		valueArray[1] = "NOEXIF";
		return valueArray;
	}
	
	public String getYearFromLastModified(File file) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
		return sdf.format(file.lastModified());
	}
	public String getMonthFromLastModified(File file) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM");
		return sdf.format(file.lastModified());
	}
	public String getYearMonthFromLastModified(File file) {
		return this.getYearFromLastModified(file) + this.getMonthFromLastModified(file);
	}
	
	public File createDirectory(String dir) {	
	    
	    if (this.silent) {
	    	return new File(dir);
	    }
	    
	    log.info("Creating directory " + dir);

		
		File test = new File(dir);
		if (test.isDirectory()) {
			return test;
		}
		
		boolean result = false;
		try {
			test.mkdir();
			result = true;
		} catch (SecurityException se) {
			System.out.println(se.toString());
		}
		
		if (result) {
			return test;
		}
		System.out.println("Returning null?????");
		return null;
		
	}
	
	public void createBaseDirs() {
		
		
		String picDirString = destinationDirectory + System.getProperty("file.separator") + "picsByDate";
		this.pictureBaseDirectory = picDirString;
		log.info("pics dirs = " + picDirString);
		
		String movieBaseDirString = destinationDirectory + System.getProperty("file.separator") + "moviesByDate";
		this.movieBaseDirectory = movieBaseDirString;
		log.info("Movie dir = " + movieBaseDirString);
		
		String docBaseDirString = destinationDirectory + System.getProperty("file.separator") + "docsByDate";
		this.docBaseDirectory = docBaseDirString;
		log.info("Docs dir = " + this.docBaseDirectory);
		
		if (!silent) {
			File destDir = createDirectory(this.destinationDirectory);
			createDirectory(picDirString);
			createDirectory(movieBaseDirString);
			createDirectory(docBaseDirString);
		}
	}
	
	// Returns true if the file was copied
	// returns false if it was skipped
	
	public boolean copyFile(File sourceFile, File destDirectory) {
		
		/*
		System.out.println("From file = " + sourceFile.toPath());
		System.out.println("To file = " + destDirectory.toPath());
		*/
		
		if (destDirectory == null) {
			this.skipped++;
			System.out.println("Null destDirectory not sure why");
			return false;
		}
		
		
		File targetFile = new File(destDirectory.toPath() + System.getProperty("file.separator") + sourceFile.getName());
		
		if (!targetFile.exists()) {
			return copyFile(sourceFile.toPath(), targetFile.toPath());
			/*
			try {
				Files.copy(sourceFile.toPath(), targetFile.toPath());
				this.copied++;
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		} else {
			String tfileName = targetFile.getAbsolutePath();
			//log.info("TfileName = " + tfileName);
			if (targetFile.length() == sourceFile.length()) {
				this.skipped++;
				return false;
			} else if (fileExistsHash.containsKey(tfileName)) {
				// Now we know this has happened before and there are two or more files with the same name
				// but different sizes.  If we find this size in the hashset then we know that this file 
				// has already been copied, so just skip it.  If we don't then add this size to the hashset
				// and then make new file name and copy it.
				if (fileExistsHash.get(tfileName).contains(sourceFile.length())) {
					log.trace("File exists with this name " + sourceFile.getAbsolutePath() + " and it's size.....skipping");
					this.skipped++;
					return false;
				} else {
					//log.info("Hash size before add = " + fileExistsHash.get(tfileName).size());
					fileExistsHash.get(tfileName).add(sourceFile.length());
					//log.info("Hash size after add = " + fileExistsHash.get(tfileName).size());
					int newCopyCount = fileExistsHash.get(tfileName).size();
					String newFileName = makeNewFileName(sourceFile.getName(), newCopyCount);
					targetFile = new File(destDirectory.toPath() + System.getProperty("file.separator") + newFileName);
					log.trace("Found multiple copies for fileName " + sourceFile.getName() +
							" - new target file is " + targetFile.getAbsolutePath());
					//System.exit(0);
					return copyFile(sourceFile.toPath(), targetFile.toPath());
				}
				
			} else {
				// Now we know at least 2 files with the same name with different sizes exist
				// Add the filename to the fileExistsHash with a HashSet of the file sizes as the value
				
				// this is the first time there as been 2 files with the same name and different sizes. 
				// create an entry in the fileExistHash
				
				log.trace("First time differnt sizes source = " + sourceFile.getAbsolutePath() + " target = " + tfileName);
				/*
				fileExistsHash.put(sourceFile.getName(),new HashSet<Long>());
				fileExistsHash.get(sourceFile.getName()).add(sourceFile.length());
				*/
				fileExistsHash.put(tfileName,new HashSet<Long>());
				//fileExistsHash.get(tfileName).add(sourceFile.length());
				
				// have to load sizes of the created newfiles in the hash also in case the order 
				// was different.  This won't happen on the first run, but could happen on future runs
				
				int i = 1;
				for (i = 1; i <= 100; i++) {
					String testFileName = makeNewFileName(sourceFile.getName(), i);
					File testFile = new File(destDirectory.toPath() + System.getProperty("file.separator") + testFileName);
					log.trace("Testfile = " + testFile.getAbsolutePath());
					if (testFile.exists()) {
						log.trace("Found " + testFile.getAbsolutePath() + " index = " + i);
						fileExistsHash.get(tfileName).add(testFile.length());
					} else {
						break;
					}
				}
				
				// now check to see if this file's length is in the fileExists hash.  This file could
				// be the same size as -3 for instance.  If it's in there then skip it.  If it's not then
				// copy it
				if (fileExistsHash.get(tfileName).contains(sourceFile.length())) {
					log.trace("File exists with this name " + sourceFile.getAbsolutePath() + " and it's size.....skipping");
					this.skipped++;
					return false;
				} else {
					fileExistsHash.get(tfileName).add(sourceFile.length());
					//int newCopyCount = fileExistsHash.get(tfileName).size();
					//String newFileName = makeNewFileName(sourceFile.getName(), newCopyCount);
					String newFileName = makeNewFileName(sourceFile.getName(), i);
					targetFile = new File(destDirectory.toPath() + System.getProperty("file.separator") + newFileName);
					log.trace("First attempt ---- Found multiple copies for fileName " + sourceFile.getName() +
							" - new target file is " + targetFile.getAbsolutePath());
					return copyFile(sourceFile.toPath(), targetFile.toPath());
				}

			}
		}
	}
	
	public boolean copyFile(Path source, Path target) {
		log.info("Copy file " + source.toFile().getAbsolutePath() + " - to - " + target.toFile().getAbsolutePath());
		if (this.silent) {
			if (copiedHash.containsKey(source.toFile().getName()) && copiedHash.get(source.toFile().getName()) == source.toFile().length()) {
				log.info("Skipping file " + source.toFile().getAbsolutePath());
				return false;
			} else {
				copiedHash.put(source.toFile().getName(), source.toFile().length());
				sizeRequired += source.toFile().length();
				this.copied++;
				return true;
			}
		} else {
			try {
				Files.copy(source, target);
				this.copied++;
				sizeRequired += source.toFile().length();
				return true;
			} catch (IOException e) {
				log.error(e.toString());
			}
		}
		return false;
	}
	
	public String makeNewFileName(String fileName, int count) {
		log.trace("Entering makeNewFileName  file = " + fileName + " count = " + count);
		String[] parts = fileName.split("\\.");
		String extension = parts[parts.length - 1];
		String base = "";
		for (int i = 0; i < parts.length - 1; i++) {
			base = base + parts[i];
		}
		String newFileName = base + "-" + count + "." + extension;
		return newFileName;
		
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

}
