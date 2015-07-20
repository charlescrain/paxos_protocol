//package proj4;

public class Message {
	
	private String message;
	private int id;
	
	public Message(String message, int id) {
		this.message = message;
		this.id = id;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getId() {
		return id;
	}
}