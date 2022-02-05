package tool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Read {
	public static int[][] readFromShortPathFile(String filePath,int node_num) {
		//String path = "D:\\eclipse\\eclipse workspace\\test1\\src\\test1\\ES_4648.txt";
		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(filePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

		    String line = null;
		    
		    int[][] shortPath=new int[node_num][node_num];
		    int index=0;

		    while ((line = bufferedReader.readLine()) != null) {
		       String[] strings=line.split(" ");
		       for(int i=0;i<node_num;i++) {
		    	   shortPath[index][i]=Integer.parseInt(strings[i]);
		       }
		       index++;
		    }

		    fileInputStream.close();
		    return shortPath;
			
		} catch (IOException e) {
			System.out.println("读文件的时候出现问题...");
			e.printStackTrace();
		}
		return null;

	    
	}
	
	public static String[] readFromObvsFile(String filePath) {
		//String path = "D:\\eclipse\\eclipse workspace\\test1\\src\\test1\\ES_4648.txt";
		FileInputStream fileInputStream;
		String[] strings=null;
		try {
			fileInputStream = new FileInputStream(filePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

		    String line = null;

		    int line_index=1;
		    while ((line = bufferedReader.readLine()) != null) {
		       //System.out.println("第 "+line_index+" 行");
		       strings=line.split(", ");
		       //System.out.println(strings.length);
		    }

		    fileInputStream.close();
			
		} catch (IOException e) {
			System.out.println("读文件的时候出现问题...");
			e.printStackTrace();
		}
		
//		for (String string : strings) {
//			System.out.println(string);
//		}
		return strings;
		
	}
}
