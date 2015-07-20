//package proj4;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;

public class CommThread extends Thread {
	
	private ServerSocket commSocket;
	private Socket socket;
	private int id, ballotNum, ballotID, acceptNum, acceptID, logPos;
	private String[] publicIPs;
	private String privateIP;
	//private int mode;
	private String port;
	private Vector<Message> log;

	private final int recovered = 0;
	private final int recovering = 1;
	private final int failed = 2; 

	public volatile int mode;
	
	public CommThread(int id, String[] publicIPs, String privateIP, String port) {
		try {	
			synchronized(this){
				mode = recovered;
			}
			ballotID = 0;
			this.id = id;
			this.publicIPs = publicIPs;
			this.privateIP = privateIP;
			this.port = port;
			this.log = new Vector<Message>(0,1);

			ballotNum = acceptNum = logPos = 0;
			commSocket = new ServerSocket();
			commSocket.bind(new InetSocketAddress(privateIP, Integer.parseInt(port)));
			commSocket.setSoTimeout(1000);
		}
		catch (Exception e) {
		}
	}
	
	public void read(String resIP, String portNum) {
		String output = "L-";
		for(int i=0; i<log.size(); i++) {
			Message mess = log.get(i);
			String tmp = "Message " + mess.getId() + ": " + mess.getMessage();
			output += tmp + "-";
		}
		try {
			Socket readSocket = new Socket();
			readSocket.connect(new InetSocketAddress(resIP, Integer.parseInt(portNum)), 1000);
			if(!readSocket.isConnected()) {
				System.out.println("Failed to connect");
			}
			DataOutputStream outToClient = new DataOutputStream(readSocket.getOutputStream());
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(readSocket.getInputStream()));
			
			outToClient.writeBytes(output + "\n");
			
			readSocket.close();
		}
		catch( Exception e ) {
		}
		
	}
	
	public void post(String message, String resIP, String portNum) {
		//Boolean status = ISPaxos(message);
		Boolean status = leadPaxos(message);
		try {
			Socket postSocket = new Socket();
			postSocket.connect(new InetSocketAddress(resIP, Integer.parseInt(portNum)), 1000);
			if(!postSocket.isConnected()) {
				System.out.println("Failed to connect");
			}
			DataOutputStream outToClient = new DataOutputStream(postSocket.getOutputStream());
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(postSocket.getInputStream()));
			

			if(status == true) {
				outToClient.writeBytes("Success\n");
			}
			else if (status = false) {
				System.out.println("Sent failed to RxThread");
				outToClient.writeBytes("Fail\n");
			}
			
			postSocket.close();
		}
		catch( Exception e ) {
		}
		
	}

	public boolean leadPaxos(String msg){
		Message message;
		int i, timeout, ackSites;
		int balNum, bal_id, acpNum, acpID, acpVal;
		int maxNum, maxID, maxVal;
		String maxMsg, acpMsg, command;
		String[] parser;
		boolean majority, elect_failed;
		balNum = bal_id = acpNum = acpID = acpVal =0;

		//---Prepare Phase---//
		ballotNum ++;
		ballotID = id;
		maxMsg = msg;
		Socket paxosSocket = null;
		for(i=0;i<5;i++){
			try{
				if(i!=id){
					paxosSocket = new Socket();
					paxosSocket.connect(new InetSocketAddress(publicIPs[i], 6778), 1000);
					if(!paxosSocket.isConnected()) {
						System.out.println("Failed to connect to site " + i);
						continue;
					}
				
					DataOutputStream outToSite = new DataOutputStream(paxosSocket.getOutputStream());
					//BufferedReader inFromClient = new BufferedReader(new InputStreamReader(postSocket.getInputStream()));
					outToSite.writeBytes("prepare," + ballotNum + "," + id + "\n");
					System.out.println("Sending prepare with ballotNum " + ballotNum +  " to : " + i);
					paxosSocket.close();
				}
			}catch(Exception e){
				//e.printStackTrace();
			}
		}
		//---Now loop until we get a majority---//
		//maxNum = maxID = maxVal = 0;
		majority = false;
		elect_failed = false;
		timeout = 0;
		ackSites  = 1;
		//---set maxNum to my ballotNum and change when higher acpNum occurs--//
		maxNum = ballotNum;
		maxID = ballotID;
		maxVal = logPos + 1; //might need to change to this site believes is next position
		while(ackSites < 3 && timeout < 20){
			try{
				paxosSocket = commSocket.accept();
				if (!paxosSocket.isConnected()) {
					paxosSocket.close();
					continue;
				}
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(paxosSocket.getInputStream()));
				
				if ((command = inFromClient.readLine()) == null) {
					paxosSocket.close();
					continue;
				}
				paxosSocket.close();
				//---Need to send acks with form ("ack", ballotNum, ballotID, acceptNum, acceptID, logPos)---//
				
				
				parser = command.split(",");
				System.out.println(parser[0]);
				if(!parser[0].equals("ack")){
					timeout++;
					continue;
				}

				balNum = Integer.parseInt(parser[1]);
				bal_id = Integer.parseInt(parser[2]);
				acpNum = Integer.parseInt(parser[3]);
				acpID = Integer.parseInt(parser[4]);
				acpVal = Integer.parseInt(parser[5]);
				acpMsg = parser[6];
				System.out.println("Received ack with balNum:" + balNum + " bal_id: " +  bal_id + " acpNum " + acpNum + " acpID " +
				acpID +  " acpVal  " + acpVal + " acpMsg " + acpMsg);

				

				if(acpNum > maxNum){
					elect_failed = true;
					maxNum = acpNum;
					maxID = acpID;
					maxVal = acpVal;
					maxMsg = acpMsg;
					// Set to 2 because we know I and at least one other site agree
					ackSites = 2;
				}else if (acpNum == maxNum && acpID > maxID){
					elect_failed = true;
					maxNum = acpNum;
					maxID = acpID;
					maxVal = acpVal;
					maxMsg = acpMsg;
					ackSites = 2;
				}else{
					ackSites++;
				}
				
			}catch (Exception e){

			}
			timeout++;
		}
		if(timeout >= 20 && ackSites < 3){
			System.out.println("timed out in prep and didnt get a majority");
			return false;
		}


		for(i=0;i<5;i++){
			try{
				if(i!=id){
					paxosSocket = new Socket();
					paxosSocket.connect(new InetSocketAddress(publicIPs[i], 6778), 1000);
					if(!paxosSocket.isConnected()) {
						System.out.println("Failed to connect to site " + i);
						continue;
					}
				
					DataOutputStream outToSite = new DataOutputStream(paxosSocket.getOutputStream());

					outToSite.writeBytes("accept," + ballotNum + "," + ballotID + "," + maxVal+","+ maxMsg  + "\n");
					System.out.println("Sent accept to " + i+ " with ballotNum: " + ballotNum + " ballotID: " +  ballotID +  " maxVal  " + maxVal +
						" maxMsg "+maxMsg);
					paxosSocket.close();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		timeout = 0;
		ackSites  = 1;
		maxNum = ballotNum;
		maxID = ballotID;

		while(ackSites < 3 && timeout < 20){
			try{
				paxosSocket = commSocket.accept();
				if (!paxosSocket.isConnected()) {
						paxosSocket.close();
						continue;
				}
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(paxosSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(paxosSocket.getOutputStream());
				if ((command = inFromClient.readLine()) == null) {
					paxosSocket.close();
					continue;
				}
				paxosSocket.close();
				//---Need to send acks with form ("accept", ballotNum, ballotID, logPos, 'msg)---//
				
				parser = command.split(",");
				if(!parser[0].equals("accept")){
					timeout++;
					continue;
				}

				balNum = Integer.parseInt(parser[1]);
				bal_id = Integer.parseInt(parser[2]);
				acpVal = Integer.parseInt(parser[3]);
				acpMsg = parser[4];
				System.out.println("Received accept with balNum:" + balNum + " bal_id: " +  bal_id +  " acpVal  " + acpVal + " acpMsg " + acpMsg);
				//System.out.println("maxNum " + maxNum + " maxID " + maxID + " maxVal " + maxVal + " maxMsg " + maxMsg);

				if(maxNum == balNum && maxID == bal_id && maxVal == acpVal ){
					//System.out.println("Received accept so +1 to majority");
					ackSites++;
				} else if(balNum > maxNum){
					elect_failed = true;
					maxNum = balNum;
					maxID = bal_id;
					maxVal = acpVal;
					maxMsg = acpMsg;
					//System.out.println("Found better value");
					// Set to 2 because we know I and at least one other site agree
					ackSites = 2;
				}else if (balNum == maxNum && bal_id > maxID){
					elect_failed = true;
					maxNum = balNum;
					maxID = bal_id;
					maxVal = acpVal;
					maxMsg = acpMsg;
					//System.out.println("Found better value");
					ackSites = 2;
				}



			}catch (Exception e){

			}
			timeout++;
		}

		if(ackSites < 3 && timeout >= 20){
			System.out.println("timed out in prep and didnt get a majority");
			return false;
		}
		acceptNum = maxNum;
		acceptID = maxID;
		logPos = maxVal;
		message = new Message(maxMsg,maxVal);
		log.insertElementAt(message, maxVal-1);
		if(elect_failed){
			System.out.println("Elect Failed!");
		}else{
			System.out.println("Elect Succeeded!");
		}	
		return !elect_failed;

	}
	
	public boolean ISPaxos(Message message) {
		log.addElement(message);
		return true;
	}
	
	public void run() {
		updateLog update;
		int balNum, bal_id, acpNum, acpID, acpVal;
		int maxNum, maxID, maxVal;
		int i, ackSites, timeout;
		String command, msg, resIP, portNum, acpMsg, maxMsg;
		Message message;
		String[] commandParse, parser;
		acpMsg = maxMsg = "";
		while(true) {
			synchronized(this){
				while(mode == recovered){
					try {	
						socket = commSocket.accept();
						if (!socket.isConnected()) {
							socket.close();
							continue;
						}
						BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
						if ((command = inFromClient.readLine()) == null) {
							socket.close();
							continue;
						}
						socket.close();
						
					}
					catch (Exception e) {
						command = "";
					}
					//commandParse = command.split(",");
					if(command.startsWith("prepare")){
						try{
							commandParse = command.split(",");
							balNum = Integer.parseInt(commandParse[1]);
							bal_id = Integer.parseInt(commandParse[2]);
							// Check if ballot is 
							if(balNum > ballotNum){
								ballotNum = balNum;
								ballotID = bal_id;
							}else if(balNum == ballotNum && bal_id >= ballotID){
								ballotNum = balNum;
								ballotID = bal_id;
							}else{
								continue;
							}
							socket = new Socket();
							socket.connect(new InetSocketAddress(publicIPs[bal_id], 6778), 1000);
							DataOutputStream outToSite = new DataOutputStream(socket.getOutputStream());
							//BufferedReader inFromClient = new BufferedReader(new InputStreamReader(postSocket.getInputStream()));
							if(log.isEmpty() ){
								outToSite.writeBytes("ack," + ballotNum + "," + ballotID + "," + acceptNum + "," + acceptID + "," +  logPos + "," +
									"null" + "\n");
							}else{
								outToSite.writeBytes("ack," + ballotNum + "," + ballotID + "," + acceptNum + "," + acceptID + "," +  logPos + "," +
									log.get(logPos-1)+ "\n");
							}
							//System.out.println("Got here!");
							socket.close();
						}catch (Exception e) {
							//e.printStackTrace();
						}

					}else if (command.startsWith("accept")){
						commandParse = command.split(",");
						balNum = Integer.parseInt(commandParse[1]);
						bal_id = Integer.parseInt(commandParse[2]);
						acpVal = Integer.parseInt(commandParse[3]);
						acpMsg = commandParse[4];
						maxNum = ballotNum;
						maxID = ballotID;
						maxVal = logPos;
						if(log.isEmpty()){
							maxMsg = "null";
						}else{
							maxMsg = log.get(logPos-1).getMessage();
						}
						System.out.println("Received accept 1st with balNum:" + balNum + " bal_id: " +  bal_id +  " acpVal  " + acpVal + " acpMsg " + acpMsg);
						System.out.println(" maxNum " + maxNum + " maxID " + maxID + " maxVal " + maxVal + " maxMsg " + maxMsg);
						if(balNum > maxNum){
							//elect_failed = true;
							maxNum = balNum;
							maxID = bal_id;
							maxVal = acpVal;
							maxMsg = acpMsg;
							// Set to 2 because we know I and at least one other site agree
							
						}else if (balNum == maxNum && bal_id > maxID){
							//elect_failed = true;
							maxNum = balNum;
							maxID = bal_id;
							maxVal = acpVal;
							maxMsg = acpMsg;
							
						}else if(balNum == maxNum && bal_id ==  maxID && maxVal != acpVal ){
							maxVal = acpVal;
							maxMsg = acpMsg;
						}else{
							continue;
						}
						//System.out.println("Here!");
						for(i=0;i<5;i++){
							try{
								//System.out.println("Here2!");
								if(i!=id){
									System.out.println("Sent accept to " + i+ " with maxNum: " + maxNum + " maxID: " +  maxID +  " maxVal  " + acpVal +
										" maxMsg " + maxMsg);
									socket = new Socket();
									socket.connect(new InetSocketAddress(publicIPs[i], 6778), 1000);
									if(!socket.isConnected()) {
										System.out.println("Failed to connect to site " + i);
										continue;
									}
								
									DataOutputStream outToSite = new DataOutputStream(socket.getOutputStream());
									//BufferedReader inFromClient = new BufferedReader(new InputStreamReader(postSocket.getInputStream()));
									// if(log.isEmpty() ){
									// 	outToSite.writeBytes("accept," + maxNum + "," + maxID + "," + maxVal + "," +
									// 	"null"+ "\n");
									// }else{
									// 	outToSite.writeBytes("accept," + maxNum + "," + maxID + "," + maxVal + "," +
									// 	log.get(logPos-1)+ "\n");
									// }
									outToSite.writeBytes("accept," + maxNum + "," + maxID + "," + maxVal + "," + maxMsg + "\n");
									socket.close();
								}
							}catch(Exception e){
								//e.printStackTrace();
							}
						}

						timeout = 0;
						ackSites  = 1;
						while(ackSites < 3 && timeout < 20){
							try{
								socket = commSocket.accept();
								if (!socket.isConnected()) {
									socket.close();
									continue;
								}
								BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
								DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
								if ((command = inFromClient.readLine()) == null) {
									socket.close();
									continue;
								}
								socket.close();
								//---Need to send acks with form ("accept", ballotNum, ballotID, logPos)---//
								
								parser = command.split(",");
								if(!parser[0].equals("accept")){
									timeout++;
									continue;
								}

								balNum = Integer.parseInt(parser[1]);
								bal_id = Integer.parseInt(parser[2]);
								acpVal = Integer.parseInt(parser[3]);
								acpMsg = parser[4];


								System.out.println("Received accept  with balNum:" + balNum + " bal_id: " +  bal_id +  " acpVal  " + acpVal + " acpMsg " + acpMsg);

								if(maxNum == balNum && maxID == bal_id && maxVal == acpVal ){
									ackSites++;
								} else if(balNum > maxNum){
									//elect_failed = true;
									maxNum = balNum;
									maxID = bal_id;
									maxVal = acpVal;
									maxMsg = acpMsg;
									// Set to 2 because we know I and at least one other site agree
									ackSites = 2;
								}else if (balNum == maxNum && bal_id > maxID){
									//elect_failed = true;
									maxNum = balNum;
									maxID = bal_id;
									maxVal = acpVal;
									maxMsg = acpMsg;
									ackSites = 2;
								}



							}catch (Exception e){

							}
							
						}
						timeout++;
						acceptNum = maxNum;
						acceptID = maxID;
						logPos = maxVal;
						message = new Message(maxMsg,maxVal);
						for(i=0;i<log.size();i++){
							System.out.println(log.get(i).getId() + ": " + log.get(i).getMessage());
						}
						log.insertElementAt(message, maxVal-1);

					}else if (command.startsWith("R")) { // read
						commandParse = command.split(",");
						resIP = commandParse[1];
						portNum = commandParse[2];
						read(resIP, portNum);
					
					}else if (command.startsWith("P")) { // post
						//leadPaxos();
						commandParse = command.split("\"");
						msg = commandParse[1];
						//message = new Message(msg, log.size());
						String[] tmp = commandParse[0].split(",");
						tmp = commandParse[2].split(",");
						resIP = tmp[1];
						System.out.println("1: " + resIP);
						portNum = tmp[2];
						System.out.println("2: " + portNum);
						post(msg, resIP, portNum);

					}else if(command.startsWith("restore")){
						commandParse = command.split(",");
						i = Integer.parseInt(commandParse[1]);
						read(publicIPs[i],"6778");
					}
				}
			}
			synchronized(this){
				while(mode == recovering){
					for(i=0;i<5;i++){
						try{
							if(i!=id){
								//System.out.println("restore log from " + i);
								socket = new Socket();
								socket.connect(new InetSocketAddress(publicIPs[i], 6778), 1000);
								if(!socket.isConnected()) {
									System.out.println("Failed to connect to site " + i);
									continue;
								}
							
								DataOutputStream outToSite = new DataOutputStream(socket.getOutputStream());
								//BufferedReader inFromClient = new BufferedReader(new InputStreamReader(postSocket.getInputStream()));
								// if(log.isEmpty() ){
								// 	outToSite.writeBytes("accept," + maxNum + "," + maxID + "," + maxVal + "," +
								// 	"null"+ "\n");
								// }else{
								// 	outToSite.writeBytes("accept," + maxNum + "," + maxID + "," + maxVal + "," +
								// 	log.get(logPos-1)+ "\n");
								// }
								outToSite.writeBytes("restore," + id+ "\n");
								socket.close();
							}
						}catch(Exception e){
							//e.printStackTrace();
						}
					}
					timeout = 0;
					while(timeout < 20){
						try{
							socket = commSocket.accept();
							if (!socket.isConnected()) {
								socket.close();
								continue;
							}
							BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
							if ((command = inFromClient.readLine()) == null) {
								socket.close();
								continue;
							}
							socket.close();
							//---Need to send acks with form ("accept", ballotNum, ballotID, logPos)---//
							
							
							if(!command.startsWith("L")){
								System.out.println(command);
								timeout++;
								continue;
							}

							update = new updateLog(log, command);
							log = update.updatingLog();

							for(i=0;i<log.size();i++){
								System.out.println(log.get(i).getId() + ": " + log.get(i).getMessage());
							}
							timeout++;



							
						}catch (Exception e){
							timeout++;
						}

						
					}

					synchronized(this){
						mode = recovered;
					}

				}
			}
			synchronized(this){
				while(mode == failed){}
			}

		}

		
	}
}