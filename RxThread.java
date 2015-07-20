import java.io.*;
import java.net.*;
//import java.util.Vector;
//import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;

public class RxThread extends Thread{
	private ServerSocket serverSocket;
	private Socket rxSocket;
	private String  myIP;
	private int port;
	public volatile String reply;
	public volatile boolean received_reply, expect_msg;
	

	RxThread(String myIP, int port){
		this.port = port;
		this.myIP = myIP;
		synchronized(this){
			reply = null;
			received_reply = false;
			expect_msg = false;
		}
		try{
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(myIP,port));
			serverSocket.setSoTimeout(1000);
			

		}catch (Exception e){
			//e.printStackTrace();
		}
	}

	public void run(){
		int time_count;
		boolean msgReceived;
		while(true){
			time_count = 0;
			msgReceived = false;
			synchronized(this){
				if(expect_msg){
					//reply = "";
					received_reply = false;
					while(!msgReceived){
						if(time_count < 20){
							msgReceived = rxMessage();
							try{
								
								if(!msgReceived){
									sleep(10);
								}
							}catch (Exception e){}
							time_count++;
						}else{
							time_count = 0;
							
							System.out.println("got a failed reply");
							reply = "Failed";
							received_reply = true;
							
							break;
						}
					}
				}
				expect_msg = false;
			}
		}
	}

	public boolean rxMessage(){
		String replyMsg;
		String[] parser;
		try{
			rxSocket = serverSocket.accept();
			if(!rxSocket.isConnected()){
				rxSocket.close();
				return false;
			}
			BufferedReader inFromSite = new BufferedReader(new InputStreamReader(rxSocket.getInputStream()));
			if( (replyMsg = inFromSite.readLine()) == null){
				rxSocket.close();
				return false;
			}
			//System.out.println("RxThread, site sent: " + replyMsg);
			if(replyMsg.startsWith("L")){
				
				parser = replyMsg.split("-");
				for(int i=1; i< parser.length; i++){
					System.out.println(parser[i]);
				}
				synchronized(this){
					reply = "Success";
					received_reply = true;
				}
				return true;
			}else if(replyMsg.startsWith("S")){
				synchronized(this){
					reply = "Success";
					received_reply = true;
				}
				return true;
			}else if(replyMsg.startsWith("F")){
				synchronized(this){
					System.out.println("got a failed reply");
					reply = "Failed";
					received_reply = true;
				}
				return true;
			}else{
				System.out.println("Error: Should not enter here at rxMessage()");
				return false;
			}
		} catch (Exception e){
			//e.printStackTrace();
			return false;
		}

	}
}