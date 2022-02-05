package tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Write {
	public static void writeTwoDimension(String writePath,int[][] a) {
		//path = "D:\\eclipse\\eclipse workspace\\test1\\src\\test1\\ES_4648_matrix.txt";
	    File file = new File(writePath);
	
	    BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(writePath));
			for (int[] ints : a) {
				for (int num : ints) {
					String aString=num+" ";
					writer.write(aString);
				}
				writer.write("\r\n");
			}
		    
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	}
}
