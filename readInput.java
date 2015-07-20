//package proj4;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class readInput {
	
	private String inputName;
	
	public readInput(String name) {
		inputName = name;
	}
	
	public String[] openInput () throws IOException {
		FileReader fr = new FileReader(inputName);
		BufferedReader br = new BufferedReader(fr);
		
		int length = findLength();
		String[] inputs = new String[length];
		
		int i;
		
		for (i=0; i<length; i++) {
			inputs[i] = br.readLine();
		}
		
		br.close();
		
		return inputs;
	}
	
	int findLength() throws IOException {
		FileReader fr = new FileReader(inputName);
		BufferedReader br = new BufferedReader(fr);
		
		@SuppressWarnings("unused")
		String line;
		int length = 0;
		
		while ((line = br.readLine()) != null) {
			length++;
		}
		br.close();
		return length;
	}
	
}
