//package proj4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Project4 {
	
	public static void main(String[] args) throws InterruptedException {
		int id = 0;
		int numSites = 1;
		int numLines = 0;
		Site site;
		readInput file;
		//String input = "/Users/sarahpilkington/Documents/UCSB/ECE_151/Project_4/Assignment4/src/proj4/config.txt";
		String input = "./config.txt";
		String[] config;
		String[] publicIPs = new String[5];
		String[] privateIPs = new String[5];
		String[] ports = new String[5];
		
		// open config file
		try {
	   		file = new readInput(input);
	   		String[] line = file.openInput();
	   		numLines = line.length;
	   		config = new String[numLines];
	   	
	   		int i;
	   		id = Integer.parseInt(line[0]);
	   		for (i=0; i<line.length; i++) {
		    	config[i] = line[i];
		    }
	   	} catch (IOException e) {
	   		System.out.println( e.getMessage() );
	   		config = new String[1];
		}
		
		// populate IP lists
		for(int i=1; i<numLines; i++) {
			String[] tmp = config[i].split("\\t");
			publicIPs[i-1] = tmp[0];
			privateIPs[i-1] = tmp[2];
		}
		
		for (int i=0; i<5; i++) {
			ports[i] = Integer.toString(6778);
		}
		
		site = new Site(id, publicIPs, privateIPs[id],ports);
		site.start();


		// for (int i=0; i<numSites; i++) {
		// 	site = new Site(i, publicIPs, privateIPs[i], ports);
		// 	site.start();
		// 	site.commThread.start();
		// }
	}
}