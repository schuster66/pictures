package com.schuster.pictures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class testSymLink {
	
	
	
	// this is how you hard link files.  Class name is wrong
	
	public static void main(String[] args ) {
		long savedSpace = 0;
		
		Path file1 = Paths.get("E:\\tomsbackups\\test1.txt");
		Path file2 = Paths.get("E:\\tomsbackups\\test2.txt");
		
		// if the files have the same size then delete one and link them together
		if (file1.toFile().length() == file2.toFile().length()) {
			try {
				if (!(Files.isSameFile(file1, file2))) {
					savedSpace += file2.toFile().length();
					Files.delete(file2);
					Files.createLink(file2, file1);
					System.out.println("linked");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("Saved space = " + savedSpace);
		
	}

}
