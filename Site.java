//package proj4;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Site extends Thread {
	private int id;
	//private boolean mode;
	private String[] publicIPs;
	private String[] ports;
	private String privateIP;

	private final int recovered = 0;
	private final int recovering = 1;
	private final int failed = 2; 

	public CommThread commThread;
	
	public Site(int id, String[] publicIPs, String privateIP, String[] ports) {
		this.id = id;
		this.publicIPs = publicIPs;
		this.privateIP = privateIP;
		this.ports = ports;
		this.commThread = new CommThread(id, publicIPs, privateIP, ports[id]);
		//this.mode = true; // start in normal
		
	}
	
	public String readFromCommLine(Scanner scanner) {
		System.out.print("Enter your command: ");		
		String command = scanner.nextLine();		
		return command;
	}
	
	public void run() {
		commThread.start();
		while(true) {
			Scanner scanner = new Scanner(System.in);
			String command;
			command = readFromCommLine(scanner);
			if (command.equals("Fail")) {
				// switch to fail mode
				// if (mode == true) {
				// 	mode = false;
				// }
				// System.out.println("Mode: " + mode);
				synchronized(this){
					commThread.mode = failed;
				}
			}
			else if (command.equals("Restore")) {
				// switch back to normal mode
				// if (mode == false) {
				// 	mode = true;
				// }
				// System.out.println("Mode: " + mode);
				synchronized(this){
					commThread.mode = recovering;
				}
			}
		}
	}
}