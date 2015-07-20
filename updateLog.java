//package proj4;

import java.util.Vector;

public class updateLog {
	private Vector<Message> myLog;
	private Vector<Message> sentLog;
	private Vector<Message> newLog;
	
	public updateLog(Vector<Message> myLog, String sentLogMsg) {
		this.myLog = myLog;
		this.sentLog = convertStringToLog(sentLogMsg);
		System.out.println("\nsentLog: ");
//		for(int i=0; i<sentLog.size(); i++) {
//			System.out.println(sentLog.get(i).getId() + ": " + sentLog.get(i).getMessage());
//		}
		this.newLog = new Vector<Message>(0,1);
	}
	
	public Vector<Message> convertStringToLog(String sentLogMsg) {
		Vector<Message> sentLog = new Vector<Message>(0,1);
		String[] msgTok = sentLogMsg.split("-");
		int logLen = msgTok.length;
		for(int i=1; i<logLen; i++) {
			String[] msgTok2 = msgTok[i].split(":");
			String[] msgTok3 = msgTok2[0].split(" ");
			int msgID = Integer.parseInt(msgTok3[1]);
			String msg = msgTok2[1].substring(1);
			Message message = new Message(msg, msgID);
			sentLog.add(message.getId()-1, message);
		}		
		return sentLog;
	}
	
	public Vector<Message> updatingLog() {
		int myLog_size = myLog.size();
		int sentLog_size = sentLog.size();
		int i, j;
		
		if (myLog_size > sentLog_size) {
			for(i=0; i<sentLog_size; i++) {
				newLog.add(i, sentLog.get(i));
			}
			for(j=i; j<myLog_size; j++) {
				newLog.add(j, myLog.get(j));
			}
			return newLog;
		}
		else if (myLog_size < sentLog_size) {
			for(i=0; i<myLog_size; i++) {
				newLog.add(i, myLog.get(i));
			}
			for(j=i; j<sentLog_size; j++) {
				newLog.add(j, sentLog.get(j));
			}
			return newLog;
		}
		else if (myLog_size == sentLog_size) {
			for(i=0; i<sentLog_size; i++) {
				newLog.add(i, sentLog.get(i));
			}
			for(j=i; j<myLog_size; j++) {
				newLog.add(j, myLog.get(j));
			}
			return newLog;
		}
		return newLog;
	}
}