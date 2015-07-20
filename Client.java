import java.io.*;
import java.net.*;
//import java.util.Vector;
//import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;

public class Client extends Thread {
	
	private String[] siteIPs;
	private String myIP, sendIP;
	private int leader, port, rxPort;
	public RxThread rxThread;

	String output;
	String input;
	//public volatile boolean done;
	
	public Client(String[] siteIPs, int port){
		try{
			this.siteIPs =  siteIPs;
			this.port = 6778;
			this.rxPort = port;
			myIP = InetAddress.getLocalHost().getHostAddress();
			leader = 0;
			rxThread = new RxThread(myIP, rxPort);
			sendIP = "52.25.103.7";
			//rxThread.start();
			//done = false;
		}catch(Exception e){}

	}

	private boolean sendRequest(int leader){
		try{
			Socket requestSocket = new Socket(); // handled later with receiverId
			requestSocket.connect(new InetSocketAddress(siteIPs[leader], port), 1000);
			if(!requestSocket.isConnected()) {
				System.out.println("Failed to connect to log in Site: " + myIP+ "\n");
			}
			DataOutputStream outToSite = new DataOutputStream(requestSocket.getOutputStream());

			outToSite.writeBytes("R," + sendIP + "," + rxPort + "\n");
			requestSocket.close();
			synchronized(this){
				rxThread.expect_msg = true;
			}
			return true;
		}catch (Exception e){
			e.printStackTrace();
			System.out.println("Failed to send read request to " + leader);
			return false;
		}
		
	}
	private boolean sendRequest(int leader, String msg){
		try{
			Socket requestSocket = new Socket(); // handled later with receiverId
			requestSocket.connect(new InetSocketAddress(siteIPs[leader], port), 1000);
			if(!requestSocket.isConnected()) {
				System.out.println("Failed to connect to log in Site: " + myIP+ "\n");
			}
			//System.out.println("Sending a post request!");
			DataOutputStream outToSite = new DataOutputStream(requestSocket.getOutputStream());

			outToSite.writeBytes("P,"+ "\"" + msg + "\"" + "," +  sendIP + "," + rxPort + "\n");
			requestSocket.close();
			synchronized(this){
				rxThread.expect_msg = true;
			}
			return true;
		}catch(Exception e){
			System.out.println("Failed to send post request to " + leader);
			return false;
		}
	}

	public String readFromCommLine(Scanner scanner) {
		//---Expect "read"/"post message" from CLI---//
		System.out.print("Enter your instruction: ");		
		String instruction = scanner.nextLine();		
		return instruction;
	}

	public String checkRetry(Scanner scanner) {
		//---Expect to get "yes"/"no" from CLI---//
		System.out.print("Failed. Would you like to retry? (y/n): ");		
		String instruction = scanner.nextLine();		
		return instruction;
	}
	
	public void run() {
		Scanner scanner = new Scanner(System.in);
		int timeout;
		boolean timedout, leader_down;
		String tmp;

		String[] msgTok;
		String instruction = "";
		String message = "";
		
		leader_down = false;

		rxThread.start();	
		/******* handle instructions *******/
		while(true){
			//---Get user input---//
			instruction = readFromCommLine(scanner);
			//msgTok = instruction.split(" ");
			//---Handle User input and check for Read or Post
			if(instruction.startsWith("R") || instruction.startsWith("r")) {
				//---send read request to Leader---//
				if(!sendRequest(leader)){
					leader_down = true;
				}
				synchronized(this){
					while(!(leader_down || rxThread.received_reply)){}
					rxThread.received_reply = false;
				}
				while(true){
					synchronized(this){
						if(leader_down || rxThread.reply.startsWith("F")){
							//System.out.println("Failed because reply is " + rxThread.reply + " or becase leade_down is " + leader_down);
							leader_down = false;
							leader = (leader+1) % 5;
							instruction = checkRetry(scanner);
							if(instruction.startsWith("Y") || instruction.startsWith("y")){
								
								if(!sendRequest(leader)){
									leader_down = true;
								}
								
							}else{
								break;
							}
							while( !(leader_down || rxThread.received_reply)){}
							rxThread.received_reply = false;
						}else{
							break;
						}
					}
				}
			}if(instruction.startsWith("P") || instruction.startsWith("p")) {

				//if(msgTok[1] == null){
					//message = "";
				tmp = instruction.substring(5);
				if(tmp.length() > 140){
					message = tmp.substring(0,140);
				}else{
					message = tmp;
				}
				if(!sendRequest(leader,message)){
					//System.out.println("Leader down");
					leader_down = true;
				}
				synchronized(this){
					while(!(leader_down || rxThread.received_reply)){}
					rxThread.received_reply = false;
				}
				while(true){
					synchronized(this){
						if(leader_down || rxThread.reply.startsWith("F")){
							//System.out.println("Failed because reply is " + rxThread.reply + " or becase leade_down is " + leader_down);
							leader_down = false;	
							leader = (leader+1) % 5;
							instruction = checkRetry(scanner);
							if(instruction.startsWith("Y") || instruction.startsWith("y")){
								if(!sendRequest(leader,message)){
									leader_down = true;
								}
							}else{
								break;
							}
							while(!(leader_down || rxThread.received_reply)){}
							rxThread.received_reply = false;
						}else{
							break;
						}
					}
				}
				
			}
		}					
	}
		
	
	
}