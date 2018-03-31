package com.schuster.pictures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class comparator {
	
	// checks to see if files on Source are on target, but not the other way around
	// If file a is on the source, but not ont he target then it will be listed, but
	// if file a is on target, but not the source then it won't be listed.
	
	public String sourceDrive = "F:/";
	public String targetDrive = "E:/";
	
	public String sourceExcludeFile="G:/tomsbackups.exclude";
	public HashSet<Path> excludedDirs = new HashSet<>();
	
	public String outputFileName="F:/tomsbackups.output";
	public boolean writeToFile=true;
	public BufferedWriter writer;
	
	public int picNotFound = 0;
	public int movieNotFound = 0;
	public int docNotFound = 0;
	
	public int picFound = 0;
	public int movieFound = 0;
	public int docFound = 0;
	
	public int excludedCount = 0;
	
	
	private Pattern docPattern = Pattern.compile("(.*(\\.(?i)(doc|pdf|txt|xls|ppt))$)");
	private Pattern picPattern = Pattern.compile("(.*(\\.(?i)(jpg|gif|bmp|jpeg))$)");
	private Pattern moviePattern = Pattern.compile("(.*(\\.(?i)(mp4|avi))$)");
	
	private Hashtable<String,Long> picHash = new Hashtable<>();
	private Hashtable<String,Long> movieHash = new Hashtable<>();
	private Hashtable<String,Long> docHash = new Hashtable<>(); 
	
	public comparator() {
		buildExcludedDirsList();
		if (writeToFile) {
			try {
				writer = new BufferedWriter(new FileWriter(outputFileName));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
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
	
	// If check then check if the file exists.  If not check then put filename in hash
	// 
	public void traverseDirectory(File node, boolean check) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		if (node.isDirectory()) {
			String[] subNote = node.list();
			if (subNote == null) return;
			myLabel: for (String filename : subNote) {
				File checkFile = new File(node, filename);
				if (checkFile.isDirectory()) {
					//System.out.println("Dir = " + checkFile.getAbsolutePath());
					Path newPath = checkFile.toPath();
					for (Path p : this.excludedDirs ) {
						if (newPath.startsWith(p)) {
							excludedCount++;
							continue myLabel;
						}
					}
					
					traverseDirectory(checkFile, check);
				} else {
					if (isPictureFile(checkFile.getName())) {
						if (check) {
							if (!(picHash.containsKey(checkFile.getName()))) {
								picNotFound++;
								if (writeToFile) {
									try {
										this.writer.write(checkFile.getAbsolutePath() + "<!>NF");
										this.writer.newLine();		
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} else {
									System.out.println("File " + checkFile.getAbsolutePath() + " - notfound on target");
								}
							} else {
								picFound++;
							}
							
						} else {
							picHash.putIfAbsent(checkFile.getName(), checkFile.length());
						}
						
						//System.out.println(checkFile.getAbsolutePath() + " - last mode = " + 
										//	this.sdf.format(checkFile.lastModified()));
					} else if (isMovieFile(checkFile.getName())){
						if (check) {
							if (!(movieHash.containsKey(checkFile.getName()))) {
								movieNotFound++;
								if (writeToFile) {
									try {
										this.writer.write(checkFile.getAbsolutePath() + "<!>NF");
										this.writer.newLine();		
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} else {
									System.out.println("File " + checkFile.getAbsolutePath() + " - notfound on target");
								}
							} else {
								movieFound++;
							}
						} else {
							movieHash.put(checkFile.getName(), checkFile.length());
						}
						
					} else if (isDocumentFile(checkFile.getName())) {
						if (check) {
							if (!(docHash.containsKey(checkFile.getName()))) {
								docNotFound++;
								if (writeToFile) {
									try {
										this.writer.write(checkFile.getAbsolutePath() + "<!>NF");
										this.writer.newLine();		
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} else {
									System.out.println("File " + checkFile.getAbsolutePath() + " - notfound on target");
								}
							} else {
								docFound++;
							}
						} else {
							docHash.put(checkFile.getName(), checkFile.length());
						}
					} else {
						//System.out.println("Not a known copy file type " + checkFile.getAbsolutePath());
					}
				}
			}
		}
	}
	
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
	
	
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		
		comparator bm = new comparator();
		
		bm.traverseDirectory(new File(bm.targetDrive), false);
		bm.traverseDirectory(new File(bm.sourceDrive), true);
		
		long runTime = (System.currentTimeMillis() - startTime) / 1000;
		
		System.out.println("Pics not found = " + bm.picNotFound + " - found = " + bm.picFound);
		System.out.println("Movies not found = " + bm.movieNotFound + " - found = " + bm.movieFound);
		System.out.println("Documents not found = " + bm.docNotFound + " - found = " + bm.docFound);
		System.out.println("Excluded count = " + bm.excludedCount);
		System.out.println("run time = " + runTime + " seconds");
		
		if (bm.writeToFile) {
			try {
				bm.writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}

}
