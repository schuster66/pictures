package com.schuster.pictures;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Hello world!
 *
 */
public class App 
{
	private static Logger log = LogManager.getLogger();
	
	private File sourceConfig;
	private File targetConfig;
	private String backupType;
	
	private String sourceName;
	private String targetDirectory;
	private String sourceExcludeFile;
	private String sourceBaseDirectory = "";
	private String linkSourceDirectory="";
	
	private String sourceDrive = "";
	private String targetDrive = "";
	
	private boolean quiet = false;
	
	private boolean copyPhotos = false;
	private boolean copyMovies = false;
	private boolean copyDocuments = false;
	
	public void parseSourceConfig(File configFile) {
		log.trace("Entering parseConfig config = " + configFile);
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(configFile);
			doc.getDocumentElement().normalize();
			// should only be one connectionInfo section, but possibly more
			// in the future. Doubtfully
			
			NodeList globals = doc.getElementsByTagName("global");
			Node gNode = globals.item(0);
			if (gNode.getNodeType() == Node.ELEMENT_NODE) {
				log.info("here we are");
				Element gElement= (Element) gNode;
				this.sourceName = getStringValueFromTag(gElement,"sourceName",false);
				this.targetDirectory = getStringValueFromTag(gElement, "targetDirectory", false);
				log.info("Target directory = " + this.targetDirectory);
				this.sourceExcludeFile = getStringValueFromTag(gElement, "excludeFile", false);
				this.sourceBaseDirectory = getStringValueFromTag(gElement, "sourceBaseDirectory", false);
			} else {
				log.error("Disasterous no global section of source config");
				System.exit(0);
			}
			
			
			
			
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void parseTargetConfig() {
		log.trace("Entering parseTargetConfig config = " + this.targetConfig);
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(this.targetConfig);
			doc.getDocumentElement().normalize();
			// should only be one connectionInfo section, but possibly more
			// in the future. Doubtfully
			
			NodeList globals = doc.getElementsByTagName("global");
			Node gNode = globals.item(0);
			if (gNode.getNodeType() == Node.ELEMENT_NODE) {
				log.info("here we are");
				Element gElement= (Element) gNode;
				if (gElement.getElementsByTagName("photos").getLength() > 0) {
					this.copyPhotos = true;
				}
				if (gElement.getElementsByTagName("movies").getLength() > 0) {
					this.copyMovies = true;
				}
				if (gElement.getElementsByTagName("documents").getLength() > 0) {
					this.copyDocuments = true;
				}
			} else {
				log.error("Disasterous no global section of source config");
				System.exit(0);
			}
			
			
			
			
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getStringValueFromTag(Element element, String tagName, boolean required) {
		NodeList nl = element.getElementsByTagName(tagName);
		if (nl.getLength() > 0) {
			String value = nl.item(0).getTextContent();
			log.trace("Tag " + tagName + " found...returning value = " + value);
			return value;
		} else {
			if (required) {
				log.error("Config missing required tag tag = " + tagName + "  exitting");
				System.exit(-1);
			}
		}
		return null;
	}
	
	private void handleCommandLine(String[] args) {
		Options options = new Options();
		options.addOption("h", "help", false, "show help.");
		options.addOption("s", "sourceconfig", true, "Source Config File required");
		options.addOption("t", "targetconfig", true, "Target Config File");
		options.addOption("b", "backuptype", true, "backup type photo|system");
		options.addOption("r", "redacted", false, "Redacted target T|F");
		options.addOption("q", "quiet", false, "Quiet mode don't actually copy the files");
		options.addOption("f", "sourceDrive", true, "Source Drive if windows machine");
		options.addOption("g", "destinationDrive", true, "Destination Drive if windows machine");
		options.addOption("l", "link", true, "Link files after copying");
		CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h"))
			    help(options);
			
			if (cmd.hasOption("f")) {
				log.info("Option f =>" + cmd.getOptionValue("f"));
				this.sourceDrive = cmd.getOptionValue("f");
				if (!new File(this.sourceDrive).canRead()) {
					log.error("Invalid source Drive try again..");
					help(options);
					System.exit(0);
				}
			}
			if (cmd.hasOption("g")) {
				this.targetDrive = cmd.getOptionValue("g");
				if (!new File(this.targetDrive).canRead()) {
					log.error("Invalid target drive try again....");
					help(options);
					System.exit(0);
				}
			}
			if (cmd.hasOption("s")) {
				   File myFile = new File(cmd.getOptionValue("s"));
				   if (myFile.canRead()) {
					   log.trace("Found file and can read");
					   this.sourceConfig = myFile;
					   parseSourceConfig(myFile);
				   } else {
					   log.error("Can't read config file file = " + myFile.getAbsolutePath());
					   help(options);
				   }
			} 
			
			if (cmd.hasOption("l")) {
				this.linkSourceDirectory = cmd.getOptionValue("l");
				if (!new File(this.linkSourceDirectory).canRead()) {
					log.error("Can't read link Source try again ...");
					help(options);
					System.exit(0);
				}
			}
			
			if (cmd.hasOption("q")) {
				this.quiet = true;
			}	
			
			if (cmd.hasOption("t")) {
				
				File tFile = new File(this.targetDrive + System.getProperty("file.separator") + cmd.getOptionValue("t"));
				if (tFile.canRead()) {
					log.trace("Found target file and can read");
					this.targetConfig = tFile;
				} else {
					log.error("Can't read target file " + tFile.getAbsolutePath());
					help(options);
				}
			}
			
			
			if (cmd.hasOption("b")) {
				String backupType = cmd.getOptionValue("b");
				if (backupType.matches("photo")) {
					log.info("Photo backup starting");
					this.backupType = "photo";
					BackupMain bm = new BackupMain();
					if (this.quiet) {
						log.info("Setting quiet to true");
						bm.setPhoto();
						bm.setMovie();
						bm.setDoc();
						bm.setSilent();
					} else {
						bm.setNoisy();
						if (sourceExcludeFile != null) {
							File sourceExclude = new File(this.sourceExcludeFile);
							if (sourceExclude.canRead()) {
								bm.setExcludedFile(sourceExcludeFile);
								bm.buildExcludeHash();
							}
						}
						bm.createBaseDirs();
						if (this.targetConfig != null) {
							parseTargetConfig();
						}
						if(this.copyPhotos) bm.setPhoto();
						if (this.copyMovies) bm.setMovie();
						if (this.copyDocuments) bm.setDoc();
					}
					String sourceDirectory;
					if (this.sourceBaseDirectory == null ) {
						sourceDirectory = this.sourceDrive + System.getProperty("file.separator");
					} else {
						sourceDirectory = this.sourceDrive + System.getProperty("file.separator") + this.sourceBaseDirectory;
					}
					log.info("Setting source to " + sourceDirectory);
					bm.setSource(sourceDirectory);
					
					String td;
					if (this.targetDirectory == null) {
						td = this.targetDrive + System.getProperty("file.separator");
					} else {
						td = this.targetDrive + System.getProperty("file.separator") + this.targetDirectory;
					}
					log.info("Setting targert to " + td);
					bm.setTarget(td);

					bm.startBackup();
					log.info("Pictures copied = " + bm.picsCopiedCount + " - pictures skipped = " + bm.picsSkippedCount +
    						" - movies copied = " + bm.moviesCopiedCount + " - movies skipped = " + bm.moviesSkippedCount +
    						" - documents copied = " + bm.docsCopiedCount + " - documents skipped = " + bm.docsSkippedCount +
    						" - other files skipped = " + bm.otherSkippedCount + 
    						" - size required = " + bm.sizeRequired / (1024 * 1024) + "MB");
					System.exit(0);
				} else if (backupType.matches("system")) {
					this.backupType = "system";
					if (this.sourceDrive.isEmpty() || this.targetDrive.isEmpty()) {
						log.error("Valid source and target directory required");
						System.exit(0);
					}
					// Start setting up backup 
					CombineDisks2 cd = new CombineDisks2();
					// set quiet if the command line calls for it otherwise noisy
					if (this.quiet) cd.setQuiet(); else cd.setNoisy();
					// Set the source and target directories
					cd.setSourceDrive(this.sourceDrive);
					cd.setTargetDrive(this.targetDrive);
					// Start the backup
					cd.startBackup();
					// print report
					cd.printReport();
					// Now if there is a -l start linking the drive to save space 
					// never link in quiet mode, doesn't make sense
					log.info("here you are ");
					if (!this.quiet && !linkSourceDirectory.isEmpty()) {
						log.info("Linking start with " + this.linkSourceDirectory);
						LinkToSave lts = new LinkToSave();
						lts.setStartDir(this.linkSourceDirectory);
						lts.startLinking();
						lts.printStats();
					}

				} else {
					log.error("Illegal backup type ");
					help(options);
				}
			} else {
				log.error("-b option required");
				help(options);
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			log.error(e.toString());
		}
	}
	
	private void help(Options options) {
		  // This prints out some help
		  HelpFormatter formater = new HelpFormatter();

		  formater.printHelp("Main", options);
		  System.exit(0);
	}
	
    public static void main( String[] args )
    {
    	long startTime = System.currentTimeMillis();
    	
    	App myApp = new App();
    	myApp.handleCommandLine(args);
    	
   /*	
    	BackupMain bm = new BackupMain();
    	bm.traverseDirectory(new File(bm.sourceDirectory));
    	long runTime = (System.currentTimeMillis() - startTime) / 1000;
    	System.out.println("copied = " + bm.copied + " - skipped = " + bm.skipped + 
    						" - excluded dirs = " + bm.excludedCount + " - runtime = " + runTime);
    	System.out.println("Pictures copied = " + bm.picsCopiedCount + " - pictures skipped = " + bm.picsSkippedCount +
    						" - movies copied = " + bm.moviesCopiedCount + " - movies skipped = " + bm.moviesSkippedCount +
    						" - documents copied = " + bm.docsCopiedCount + " - documents skipped = " + bm.docsSkippedCount +
    						" - other files skipped = " + bm.otherSkippedCount);
    	System.out.println("Size required = " + bm.sizeRequired / (1024 * 1024 * 1024) + " GB");
    */
    }
}
