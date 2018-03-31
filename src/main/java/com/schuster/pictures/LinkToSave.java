package com.schuster.pictures;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkToSave {
	
	private static Logger log = LogManager.getLogger();
	
	private Hashtable<String,HashSet<File>> fileHash = new Hashtable<>();
	private String startDir = "F:";
	private Long savedSpace = 0L; 
	
	private Long startTime;
	private Long endTime;
	
	private boolean silent = false;
	
	public void setStartDir(String startDir) {
		this.startDir = startDir;
	}
	

	
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
					if (fileHash.containsKey(checkFile.getName())) {
						HashSet<File> files = fileHash.get(checkFile.getName());
						boolean found = false;
						for (File fileFromHashset : files) {
							if (fileFromHashset.length() == checkFile.length()) {
								log.info("file name and size match newfile = " + checkFile.getAbsolutePath() +
										" - hashset file = " + fileFromHashset.getAbsolutePath());
								link(fileFromHashset, checkFile);
								found = true;
								break;
							}
						}
						if (!found) {
							files.add(checkFile);
							log.info("Found file with name " + checkFile.getName() + " and " + fileHash.get(checkFile.getName()).size() +
										" different sizes.");
						}
					} else {
						fileHash.put(checkFile.getName(),new HashSet<File>());
						fileHash.get(checkFile.getName()).add(checkFile);
					}
				}
			}
		}
	}
	
	private void link(File fileInHash, File newFileToDeleteAndLink) {
		
		
		try {
			Path pathFromFileInHash = fileInHash.toPath();
			Path pathFromNewFileToDeleteAndLink = newFileToDeleteAndLink.toPath();
			if (!(Files.isSameFile(pathFromFileInHash, pathFromNewFileToDeleteAndLink))) {
				// only link files > than 1K in length.  Not enough bang for the buck linking little files
				if (fileInHash.length() > 1000) {
					savedSpace += fileInHash.length();
					if (!silent) {
						log.info("Changing source and target file to read/write");
						fileInHash.setWritable(true);
						newFileToDeleteAndLink.setWritable(true);
						log.info("Deleting " + pathFromNewFileToDeleteAndLink.toFile().getAbsolutePath());
						Files.delete(pathFromNewFileToDeleteAndLink);
						log.info("Linking to new file " + pathFromFileInHash.toFile().getAbsolutePath());
						Files.createLink(pathFromNewFileToDeleteAndLink, pathFromFileInHash);
					}
					log.info("Linked " + newFileToDeleteAndLink.getAbsolutePath() + " to " + fileInHash.getAbsolutePath());
				}
			}
		} catch (FileSystemException fs) {
			log.error("FS " + fs.getFile() + " - " + fs.getOtherFile() + " - " + fs.getMessage() + " - " + fs.getReason() + " - ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidPathException e) {
			log.error("Something wrong with the path don't worry about linking " + e.toString());
		} 
		
	}
	
	public void startLinking() {
		this.startTime = System.currentTimeMillis();
		this.traverseDirectory(new File(this.startDir));
		this.endTime = System.currentTimeMillis();
	}
	
	public void printStats() {
		long runTime = (this.startTime - this.endTime) / 1000;
		System.out.println("Saved space = " + this.savedSpace / (1024 * 1024 * 1024) + "gb" + " - runtime = " + runTime + " secs");
	}
	
	public static void main(String[] args) {
		LinkToSave lts = new LinkToSave();
		lts.traverseDirectory(new File(lts.startDir));
		System.out.println("Saved space = " + lts.savedSpace / (1024 * 1024 * 1024) + "gb");
	}
}
