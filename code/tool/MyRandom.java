package tool;

import java.util.Random;

public class MyRandom {
	public double randomBetween(double smallNumber, double bigNumber){
		Random rand=new Random();
		double diff = bigNumber - smallNumber;
	    return (((double) rand.nextInt(100000) / 100000l) * diff) + smallNumber;
	}
	
	public static void main(String[] args) {
		for(int i=1;i<=100;i++) {
			System.out.println(new MyRandom().randomBetween(0.15, 0.45));
		}
	}

}
