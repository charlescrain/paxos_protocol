import java.io.*;
import java.net.*;
//import java.util.Vector;
//import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;

public class Main {
	
	public static void main(String[] args) throws InterruptedException {
		String tmp;
		int port;
		String[] ips = {"52.7.121.252", "52.8.171.137", "54.79.63.156", "52.74.193.161", "54.207.100.74"};
		System.out.print("Please enter a Rx port number client from: ");
		Scanner scanner = new Scanner(System.in);
		tmp = scanner.nextLine();
		port = Integer.parseInt(tmp);
		Client client = new Client(ips,port);

		client.start();
	}
}