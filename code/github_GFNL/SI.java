package github_GFNL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import tool.MyRandom;
import tool.Read;

public class SI {
	public double infe_rate;
	public double recovery_rate=0;
	public int times;
	public int observer_num;

	public int source;

	public ArrayList<String> infected_people;//前need_infected_num个为传染源
	public ArrayList<String> temp;
	
	
	public int[][] readInf_4_from_oneFile(Graph1 network,ArrayList<String> obv_set, String shortPath_fileName){
		int[][] short_temp=new int[obv_set.size()][network.verNum];
		String filePath=shortPath_fileName;
		try {
			FileInputStream fileInputStream = new FileInputStream(filePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
			String line = null;
			int index=1;
			while ((line = bufferedReader.readLine()) != null) {
				if(obv_set.contains(index+"")) {
			       String[] dis_all=line.split(" ");
			       int index2=0;
			       int right_pos=obv_set.indexOf(index+"");
			       for (String dis : dis_all) {
			    	   short_temp[right_pos][index2]=Integer.parseInt(dis);
			    	   index2++;
			       }
				}
				
				
				index++;
			 }
			bufferedReader.close();
		} catch (IOException e) {
			System.out.println("读文件的时候出现问题...");
			e.printStackTrace();
		}
		
		return short_temp;
	}

	
	void initNet_SIR_Greedy_fromFile(Graph1 network, double infected_P, double recovery_p, int inf_source_num, String sensor_file_path) {
		times = 10000000;
		infected_people = new ArrayList<String>();

		String[] myFONC_obvs = null;
		myFONC_obvs = Read.readFromObvsFile(sensor_file_path);
		
		for (String obv_name : myFONC_obvs) {
			network.vertexArray[Integer.parseInt(obv_name)-1].isObserver=true;
		}
		this.observer_num=myFONC_obvs.length;

		// 同构传染率
//		for (int i = 0; i < network.verNum; i++) {
//			Vertex1 node = network.vertexArray[i];
//			node.infected_p=infected_P;
//		}
		this.infe_rate=infected_P;
		
		// 异构传染率
//		MyRandom myR = new MyRandom();
//		for (int i = 0; i < network.verNum; i++) {
//			Vertex1 node = network.vertexArray[i];
//			node.infected_p=myR.randomBetween(0.15, 0.45);
//		}
		

		//同时为恢复率赋值
		recovery_rate=recovery_p; // 传染率每个人都不一样，所以是node层面的，而恢复率都一样，因此是全局层面。

		// 随机生成感染源
		for (int i = 0; i < inf_source_num; i++) {
			Random rand = new Random();
			int r = rand.nextInt(network.verNum)+1;

			Vertex1 node =network.vertexArray[r-1]; //CreateGraph3.getVertex(network, r + "");
			//if (node != null && node.time == -1 && !node.isObserver&& node.state.equals("H")) {
			if (node != null && node.time == -1 && !node.isObserver) {
				//System.out.println(network.verNum);
				infected_people.add(r + "");

				source = r;
				System.out.println("传染源: " + r);
				node.infe = true;
				node.time = 0;

				for (int m = 0; m < network.verNum; m++) {
					Vertex1 vertex = network.vertexArray[m];
					if (vertex.verName.equals(r + "")) {
						vertex.infe = true;
						vertex.time = 0;
					}

					Vertex1 current = vertex.nextNode;
					while (current != null) {
						if (current.verName.equals(r + "")) {
							current.infe = true;
							current.time = 0;
						}
						current = current.nextNode;
					}
				}
			} else {
				i--;
			}

		}

	}

	void initNet_SIR_greedy_ratio(Graph1 network, double infected_P, double recovery_p, int inf_source_num, int need_deployed_num) {
		times = 10000000;
		infected_people = new ArrayList<String>();

		/////////////////////////////////
		// 使用FONC算法筛选观察点
		Arraysort yitaArraysort = new Arraysort(network);

		// 二维数组需要for循环便利每一个一维数组，对每一个一维数组调用.clone()方法!!!
		int[][] shortP_temp = new int[network.verNum][network.verNum];
		for (int copy_i = 0; copy_i < network.verNum; copy_i++) {
			for (int copy_j = 0; copy_j < network.verNum; copy_j++) {
				shortP_temp[copy_i][copy_j] = ALG.shortPath[copy_i][copy_j];
			}
		}

		//减少时间复杂度
		//这个H矩阵行索引是从0开始的，而列索引从1开始，1就代表  对于点1，他的1阶邻居  H[i][1]
		int[][] H=new int[network.verNum][30];
		for(int i=0;i<ALG.shortPath.length;i++) {
			for (int j = 0; j < ALG.shortPath.length; j++) {
				if (i != j)
					H[i][ALG.shortPath[i][j]] = H[i][ALG.shortPath[i][j]] + 1;
			}
		}

		//回到最初111
		//安全
		
		// int[][] shortP_temp=new int[network.verNum][network.verNum];
		ArrayList<String> obvsArrayList = new ArrayList<String>();
		Arraysort sort=new Arraysort(network);


		//1、统计greedy信息到greedySort中
		// 对于每一行都要计算η
		for (int i = 1; i <= shortP_temp.length; i++) {
			yitaArraysort.arr[i - 1].verName_typeINT = i;
			int yita = 0;
			// η是由众多的其他的节点构成的,计算每一个组成项
			for (int j = 1; j <= shortP_temp.length; j++) {
				if (shortP_temp[i - 1][j - 1] != 0) {// 有距离的
					int dis = shortP_temp[i - 1][j - 1];
					yita=yita+H[j-1][dis];
				}
			}
			yitaArraysort.arr[i - 1].Au = yita;
		}
		// 所有点的η都统计完了,将η最大的点加入到obvs中
		// Arraysort sort=yitaArraysort; //这个不行，需要深拷贝
		//Arraysort sort = new Arraysort(network);
		for (int index = 0; index < network.verNum; index++) {
			sort.arr[index].verName_typeINT = yitaArraysort.arr[index].verName_typeINT;
			sort.arr[index].Au = yitaArraysort.arr[index].Au;
		}
		Arrays.sort(sort.arr, new MyComprator());
		for (int i = 0; i < 5; i++) { // 输出排序结果
			System.out.println("(" + sort.arr[sort.arr.length-1-i].verName_typeINT + "," + sort.arr[sort.arr.length-1-i].Au + ")");
		}
		
		//初始统计了所有点的最大yita
		for(int ii=0;ii<sort.arr.length;ii++){
			sort.arr[ii].Au=sort.arr[ii].Au;
			sort.arr[ii].verName_typeINT=sort.arr[ii].verName_typeINT;
		}

		//shortP_temp参与更替值，shortP_notChange作为备份矩阵
		int[][] shortP_notChange=new int[shortP_temp.length][shortP_temp.length];
		for(int ii=0;ii<shortP_temp.length;ii++)
			for(int jj=0;jj<shortP_temp.length;jj++)
				shortP_notChange[ii][jj]=shortP_temp[ii][jj];

		
		
		//所有趟的DFS就是由此入口开始...........................................................................................
		for(int max_num=1;max_num<=network.verNum;max_num++){
			if(obvsArrayList.size()>=need_deployed_num) {
				//System.out.println("obvsArrayList.size():"+obvsArrayList.size());
				//System.out.println("need_deployed_num:"+need_deployed_num);
				break;
			}

			//System.out.println("for1111111............");
			if(obvsArrayList.contains(sort.arr[network.verNum - max_num].verName_typeINT+"")){
				continue;
			}
			obvsArrayList.add(sort.arr[network.verNum - max_num].verName_typeINT + "");
			//System.out.println("1111处加入的sensor："+sort.arr[network.verNum - max_num].verName_typeINT);

			//恢复shortPath为初始的样子，开始执行循环...恢复矩阵的操作只有这个时候才能执行，执行力以后就开始走某一趟完整DFS了。后边的某一趟DFS中，不会再存在其他恢复矩阵操作，只能把矩阵里的元素变0
			for(int ii=0;ii<shortP_temp.length;ii++)
				for(int jj=0;jj<shortP_temp.length;jj++)
					shortP_temp[ii][jj]=shortP_notChange[ii][jj];
			
			//// 将其一整行置为0，且将对应行置为0(先删其他行，最后删对应行)
			for (int d_i = 0; d_i < shortP_temp.length; d_i++) {
				if (d_i == sort.arr[network.verNum - max_num].verName_typeINT - 1)
					continue;
				for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
					if (shortP_temp[d_i][d_j] == shortP_temp[sort.arr[network.verNum - max_num].verName_typeINT - 1][d_j])
						shortP_temp[d_i][d_j] = 0;
				}
			}
			for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
				shortP_temp[sort.arr[network.verNum - max_num].verName_typeINT - 1][d_j] = 0;
			}

			//这是选好了一个根节点以后，开始某一趟的完整DFS
			int last_time_num=-1;
			while (judge_haveNum(shortP_temp)&&obvsArrayList.size()<need_deployed_num&&last_time_num!= obvsArrayList.size()) {
				last_time_num=obvsArrayList.size();
				//System.out.println("for22222222222...........");
				// 对于每一行都要计算η
				for (int i = 1; i <= shortP_temp.length; i++) {
					yitaArraysort.arr[i - 1].verName_typeINT = i;
					int yita = 0;
					// η是由众多的其他的节点构成的,计算每一个组成项
					for (int j = 1; j <= shortP_temp.length; j++) {
						if (shortP_temp[i - 1][j - 1] != 0) {// 有距离的
							int dis = shortP_temp[i - 1][j - 1];
							yita=yita+H[j-1][dis];
						}
					}
					yitaArraysort.arr[i - 1].Au = yita;
				}
				// 所有点的η都统计完了,将η最大的点加入到obvs中
				// Arraysort sort=yitaArraysort; //这个不行，需要深拷贝
				sort = new Arraysort(network);
				for (int index = 0; index < network.verNum; index++) {
					sort.arr[index].verName_typeINT = yitaArraysort.arr[index].verName_typeINT;
					sort.arr[index].Au = yitaArraysort.arr[index].Au;
				}
				Arrays.sort(sort.arr, new MyComprator());
				System.out.println("------------DFS内---------------");
				for (int i = 0; i < 5; i++) { // 输出排序结果
					System.out.println("(" + sort.arr[sort.arr.length-1-i].verName_typeINT + "," + sort.arr[sort.arr.length-1-i].Au + ")");
				}
				System.out.println("------------DFS内---------------");

				for(int max_num_inDFS=1;max_num_inDFS<=network.verNum;max_num_inDFS++){
					if(obvsArrayList.contains(sort.arr[network.verNum - max_num_inDFS].verName_typeINT+"")){
						continue;
					}
					if(sort.arr[network.verNum - max_num_inDFS].Au==0){
						break;
					}

					obvsArrayList.add(sort.arr[network.verNum - max_num_inDFS].verName_typeINT + "");//这个add执行以后，后边有break，不需要再添加了
					//System.out.println("现在obvs的数量："+obvsArrayList.size());
					//System.out.println("222处加入的sensor："+sort.arr[network.verNum - max_num_inDFS].verName_typeINT);
					
					//// 将其一整行置为0，且将对应行置为0(先删其他行，最后删对应行)
					for (int d_i = 0; d_i < shortP_temp.length; d_i++) {
						if (d_i == sort.arr[network.verNum - max_num_inDFS].verName_typeINT - 1)
							continue;
						for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
							if (shortP_temp[d_i][d_j] == shortP_temp[sort.arr[network.verNum - max_num_inDFS].verName_typeINT - 1][d_j])
								shortP_temp[d_i][d_j] = 0;
						}
					}
					for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
						shortP_temp[sort.arr[network.verNum - max_num_inDFS].verName_typeINT - 1][d_j] = 0;
					}

					break;//在往下探索的时候每一个deep layer只能执行一次
				}
			}

			//运行到这里就说明一个DFS已经走完了
		}

		observer_num=obvsArrayList.size();
		System.out.println("所有的FONC必要观察点: "+obvsArrayList.toString());
		System.exit(0);
		
		//存储到本地文件中
//		try {
//			String path="/Users/houdongpeng/1/ComplexNetwork/src/quick_J_0117/obv_info.txt";
//			//String path = "G:\\SI_propagation_model\\facebook_randomScale\\路径信息\\((RAND)10%部署比例-10%感染(不修复最短路径(快)—包含路径信息-F_score)final-1.05+wrong-greedy_FNOC+时距比-7)initNet_heterogeneous_combinedFONC_divideWeight_parasType"+type+".txt";
//			// file = new File(path);
//			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
//			for (String is : obvsArrayList) {
//				writer.write(is+", ");
//			}
//			writer.write("\r\n");
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			
//		}
//		System.exit(0);

		
	}

	void initNet_SIR_greedy_FullOrder(Graph1 network, double infected_P, double recovery_p, int inf_source_num, int need_deployed_num) {
		times = 10000000;
		infected_people = new ArrayList<String>();

		/////////////////////////////////
		// 使用FONC算法筛选观察点
		Arraysort yitaArraysort = new Arraysort(network);

		// 二维数组需要for循环便利每一个一维数组，对每一个一维数组调用.clone()方法!!!
		int[][] shortP_temp = ALG.shortPath;
//	修改1--	= ALG.shortPath;+注释5行		new int[network.verNum][network.verNum];
//		for (int copy_i = 0; copy_i < network.verNum; copy_i++) {
//			for (int copy_j = 0; copy_j < network.verNum; copy_j++) {
//				shortP_temp[copy_i][copy_j] = ALG.shortPath[copy_i][copy_j];
//			}
//		}

		//减少时间复杂度
		//这个H矩阵行索引是从0开始的，而列索引从1开始，1就代表  对于点1，他的1阶邻居  H[i][1]
		int[][] H=new int[network.verNum][30];
		for(int i=0;i<ALG.shortPath.length;i++) {
			for (int j = 0; j < ALG.shortPath.length; j++) {
				if (i != j)
					H[i][ALG.shortPath[i][j]] = H[i][ALG.shortPath[i][j]] + 1;
			}
		}

		//回到最初111
		//安全
		
		// int[][] shortP_temp=new int[network.verNum][network.verNum];
		ArrayList<String> obvsArrayList = new ArrayList<String>();
		Arraysort sort=new Arraysort(network);


		//1、统计greedy信息到greedySort中
		// 对于每一行都要计算η
		for (int i = 1; i <= shortP_temp.length; i++) {
			yitaArraysort.arr[i - 1].verName_typeINT = i;
			int yita = 0;
			// η是由众多的其他的节点构成的,计算每一个组成项
			for (int j = 1; j <= shortP_temp.length; j++) {
				if (shortP_temp[i - 1][j - 1] != 0) {// 有距离的
					int dis = shortP_temp[i - 1][j - 1];
					yita=yita+H[j-1][dis];
				}
			}
			yitaArraysort.arr[i - 1].Au = yita;
		}
		// 所有点的η都统计完了,将η最大的点加入到obvs中
		// Arraysort sort=yitaArraysort; //这个不行，需要深拷贝
		//Arraysort sort = new Arraysort(network);
		for (int index = 0; index < network.verNum; index++) {
			sort.arr[index].verName_typeINT = yitaArraysort.arr[index].verName_typeINT;
			sort.arr[index].Au = yitaArraysort.arr[index].Au;
		}
		Arrays.sort(sort.arr, new MyComprator());
		for (int i = 0; i < 5; i++) { // 输出排序结果
			System.out.println("(" + sort.arr[sort.arr.length-1-i].verName_typeINT + "," + sort.arr[sort.arr.length-1-i].Au + ")");
		}
		
		//初始统计了所有点的最大yita
		for(int ii=0;ii<sort.arr.length;ii++){
			sort.arr[ii].Au=sort.arr[ii].Au;
			sort.arr[ii].verName_typeINT=sort.arr[ii].verName_typeINT;
		}

		//shortP_temp参与更替值，shortP_notChange作为备份矩阵
//修改2---注释4行		int[][] shortP_notChange=new int[shortP_temp.length][shortP_temp.length];
//		for(int ii=0;ii<shortP_temp.length;ii++)
//			for(int jj=0;jj<shortP_temp.length;jj++)
//				shortP_notChange[ii][jj]=shortP_temp[ii][jj];
		

		
		
		//所有趟的DFS就是由此入口开始,包含全部选sensor的操作...........................................................................................
		for(int max_num=1;max_num<=network.verNum;max_num++){
			if(obvsArrayList.size()>=need_deployed_num) {
				//System.out.println("obvsArrayList.size():"+obvsArrayList.size());
				//System.out.println("need_deployed_num:"+need_deployed_num);
				break;
			}

			//System.out.println("for1111111............");
			if(obvsArrayList.contains(sort.arr[network.verNum - max_num].verName_typeINT+"")){
				continue;
			}
			obvsArrayList.add(sort.arr[network.verNum - max_num].verName_typeINT + "");
			//System.out.println("1111处加入的sensor："+sort.arr[network.verNum - max_num].verName_typeINT);

			//恢复shortPath为初始的样子，开始执行循环...恢复矩阵的操作只有这个时候才能执行，执行力以后就开始走某一趟完整DFS了。后边的某一趟DFS中，不会再存在其他恢复矩阵操作，只能把矩阵里的元素变0
//修改3--注释3行			for(int ii=0;ii<shortP_temp.length;ii++)
//				for(int jj=0;jj<shortP_temp.length;jj++)
//					shortP_temp[ii][jj]=shortP_notChange[ii][jj];
			
			//// 将其一整行置为0，且将对应行置为0(先删其他行，最后删对应行)
			for (int d_i = 0; d_i < shortP_temp.length; d_i++) {
				if (d_i == sort.arr[network.verNum - max_num].verName_typeINT - 1)
					continue;
				for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
					if (shortP_temp[d_i][d_j] == shortP_temp[sort.arr[network.verNum - max_num].verName_typeINT - 1][d_j])
						shortP_temp[d_i][d_j] = 0;
				}
			}
			for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
				shortP_temp[sort.arr[network.verNum - max_num].verName_typeINT - 1][d_j] = 0;
			}

			//这是选好了一个根节点以后，开始某一趟的完整DFS
			int last_time_num=-1;
			while (judge_haveNum(shortP_temp)&&obvsArrayList.size()<need_deployed_num&&last_time_num!= obvsArrayList.size()) {
				last_time_num=obvsArrayList.size();
				//System.out.println("for22222222222...........");
				// 对于每一行都要计算η
				for (int i = 1; i <= shortP_temp.length; i++) {
					yitaArraysort.arr[i - 1].verName_typeINT = i;
					int yita = 0;
					// η是由众多的其他的节点构成的,计算每一个组成项
					for (int j = 1; j <= shortP_temp.length; j++) {
						if (shortP_temp[i - 1][j - 1] != 0) {// 有距离的
							int dis = shortP_temp[i - 1][j - 1];
							yita=yita+H[j-1][dis];
						}
					}
					yitaArraysort.arr[i - 1].Au = yita;
				}
				// 所有点的η都统计完了,将η最大的点加入到obvs中
				// Arraysort sort=yitaArraysort; //这个不行，需要深拷贝
				sort = new Arraysort(network);
				for (int index = 0; index < network.verNum; index++) {
					sort.arr[index].verName_typeINT = yitaArraysort.arr[index].verName_typeINT;
					sort.arr[index].Au = yitaArraysort.arr[index].Au;
				}
				Arrays.sort(sort.arr, new MyComprator());
				System.out.println("------------DFS内---------------");
				for (int i = 0; i < 5; i++) { // 输出排序结果
					System.out.println("(" + sort.arr[sort.arr.length-1-i].verName_typeINT + "," + sort.arr[sort.arr.length-1-i].Au + ")");
				}
				System.out.println("------------DFS内---------------");

				for(int max_num_inDFS=1;max_num_inDFS<=network.verNum;max_num_inDFS++){
					if(obvsArrayList.contains(sort.arr[network.verNum - max_num_inDFS].verName_typeINT+"")){
						continue;
					}
					if(sort.arr[network.verNum - max_num_inDFS].Au==0){
						break;//用于约束条件"last_time_num!= obvsArrayList.size()"
					}

					obvsArrayList.add(sort.arr[network.verNum - max_num_inDFS].verName_typeINT + "");//这个add执行以后，后边有break，不需要再添加了
					//System.out.println("现在obvs的数量："+obvsArrayList.size());
					//System.out.println("222处加入的sensor："+sort.arr[network.verNum - max_num_inDFS].verName_typeINT);
					
					//// 将其一整行置为0，且将对应行置为0(先删其他行，最后删对应行)
					for (int d_i = 0; d_i < shortP_temp.length; d_i++) {
						if (d_i == sort.arr[network.verNum - max_num_inDFS].verName_typeINT - 1)
							continue;
						for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
							if (shortP_temp[d_i][d_j] == shortP_temp[sort.arr[network.verNum - max_num_inDFS].verName_typeINT - 1][d_j])
								shortP_temp[d_i][d_j] = 0;
						}
					}
					for (int d_j = 0; d_j < shortP_temp.length; d_j++) {
						shortP_temp[sort.arr[network.verNum - max_num_inDFS].verName_typeINT - 1][d_j] = 0;
					}

					break;//在往下探索的时候每一个deep layer只能执行一次
				}
			}

			//运行到这里就说明一个DFS已经走完了：第一次到这里这就是full-order的必要观察点
			observer_num=obvsArrayList.size();
			System.out.println("所有的FONC必要观察点: "+obvsArrayList.toString());
			System.exit(0);
			
			//存储为本地文件
//			try {
//				System.out.println("必要观察点的数量: "+obvsArrayList.size());
//				String path="/Users/houdongpeng/1/ComplexNetwork/src/quick_J_0117/obv_info.txt";
//				//String path = "G:\\SI_propagation_model\\facebook_randomScale\\路径信息\\((RAND)10%部署比例-10%感染(不修复最短路径(快)—包含路径信息-F_score)final-1.05+wrong-greedy_FNOC+时距比-7)initNet_heterogeneous_combinedFONC_divideWeight_parasType"+type+".txt";
//				// file = new File(path);
//				BufferedWriter writer = new BufferedWriter(new FileWriter(path));
//				for (String is : obvsArrayList) {
//					writer.write(is+", ");
//				}
//				writer.write("\r\n");
//				writer.flush();
//				writer.close();
//			} catch (IOException e) {
//				
//			}
//			System.exit(0);
		}
		
	}

	
	public boolean judge_haveNum(int[][] shortP_temp) {
		for (int[] sa : shortP_temp) {
			for (int num : sa) {
				if(num==0||num==-1) {}else {
					return true;
				}
			}
		}
		return false;
	}


	public void simulate(Graph1 network, boolean is_heterogenous, double ppt_of_I) {
		int greedy_infected_obv_num=0;
		int random_infected_obv_num=0;
		int end=0; //剪枝
		//boolean all_sensor_infected_flag=false;
		
		int infe_nums=infected_people.size();
		// System.out.println(infe_nums);
		for (int i = 1; i <= times && infe_nums <=network.verNum; i++) {// 控制传播了多少轮
			temp=(ArrayList<String>) infected_people.clone();
			for (int j = 1; j <= temp.size(); j++) {// 对于每个感染的节点

				String infed_name = temp.get(j-1);
				//Vertex1 infed_person = CreateGraph3.getVertex(network, infed_name);
				Vertex1 infed_person = network.vertexArray[Integer.parseInt(infed_name)-1];
				if (infed_person == null) {
					System.out.println("不存在该姓名!");
					return;
				}
				Vertex1 maybe_person = infed_person.nextNode;
				while (maybe_person != null) {
					Vertex1 maybe_node=network.vertexArray[Integer.parseInt(maybe_person.verName)-1];
					//if (maybe_person.infe == false) {// 111、这里限制了进来的一定是未感染的点，恢复的节点我们不改变他们的infe那么就不会再次被感染 //改1
					if (maybe_node.infe==false && maybe_node.recover==false) {

						//newest:这里增加一个新的参数，即社交联系紧密度。1代表最为紧密，2代表联系一般，3代表联系疏远
						int node1_index_inM=Integer.parseInt(infed_person.verName)-1;
						int node2_index_inM=Integer.parseInt(maybe_node.verName)-1;

						//if(i % (connectedMatrix[node1_index_inM][node2_index_inM])==0) {
						if(true) {
							//新加的限制条件3333、这里限制了进来的一定是这次进行联系了

							Random rand = new Random();
							int a;
//							if(is_heterogenous) {
//								a = (rand.nextInt(10000) < 10000 * infed_person.infected_p) ? 1 : 0;
//							}else {
//								a = (rand.nextInt(10000) < 10000 * infe_rate) ? 1 : 0;
//							}
							a = (rand.nextInt(10000) < 10000 * this.infe_rate) ? 1 : 0;
							if (a == 1) {// 222、这里限制了进来的一定是达到传染条件的点: 1、未被感染  2、满足传染概率
								infe_nums ++;
								infected_people.add(maybe_node.verName);
								maybe_node.infe=true;
								maybe_node.time=i;
								maybe_node.origin=infed_name;
								
								//贪婪的sensor
								if(maybe_node.isObserver) {
									greedy_infected_obv_num++;
									if(greedy_infected_obv_num==4) {
										MyExecute.time_myStartEXE4=MyExecute.time_myStartEXE4+i;
									}
									if(greedy_infected_obv_num==(int)(observer_num*MyExecute.ppt_of_I_sensor)) {
										//System.out.println("只能进来一次1");
										MyExecute.time_myStartEXE30=MyExecute.time_myStartEXE30+i;
										end++;
									}
								}
								
								//随机部署的sensor
								if(maybe_node.isRandomObserver) {
									random_infected_obv_num++;
									if(random_infected_obv_num==(int)(observer_num*MyExecute.ppt_of_I_sensor)) {
										//System.out.println("只能进来一次2");
										MyExecute.time_randEXE30=MyExecute.time_randEXE30+i;
										end++;
									}
								}

								if(greedy_infected_obv_num>=20||infe_nums>=network.verNum*ppt_of_I||end==2) {
//									if(all_sensor_infected_flag==false) {
//										Random rand2=new Random();
//										MyExecute.time_randEXE30=MyExecute.time_randEXE30+rand2.nextInt(3)+1+i;
//									}
									return;
								}
								
								
							}

						}

					}

					maybe_person = maybe_person.nextNode;

				}



			}

			//一轮传染结束后，开始进行恢复操作
			if(recovery_rate==100){
				System.out.println("是SIR模型...\n");
				System.exit(0);//当recovery_rate=0时 才行代码用于指示进程状态
				Iterator<String> itor=infected_people.iterator();
				while(itor.hasNext()){
					String infected_person=itor.next();
					Random rand=new Random();
					int ran = (rand.nextInt(10000) < 10000 * recovery_rate) ? 1 : 0;
					if(ran==1){
						itor.remove();
						CreateGraph3.getVertex(network,infected_person).recover=true;
					}
				}
			}

		}
		System.out.println("----------------------------------");
		System.out.println("传播结束后，传染信息:");
		System.out.println("感染人数:"+infe_nums);



	}


	public ArrayList<String> getobv_byTime_byNum(Graph1 network,int allobvs_num, int fetch_num){
		//num=10;
		ArrayList<String> obvSet=new ArrayList<String>();
		int useful_obv_num=0;
		for(int i=0;i<network.verNum;i++) {
			if(network.vertexArray[i].time!=-1 && network.vertexArray[i].isObserver)
				useful_obv_num++;
		}
		Arraysort sort=new Arraysort(useful_obv_num);
		int index=0;
		for(int i=0;i<network.verNum;i++) {
			if(network.vertexArray[i].time!=-1 && network.vertexArray[i].isObserver) {
				sort.arr[index].verName_typeINT=Integer.parseInt(network.vertexArray[i].verName);
				sort.arr[index].Au=network.vertexArray[i].time;
				index++;
			}
		}
		Arrays.sort(sort.arr, new MyComprator());
//		for (int i = 0; i < allobvs_num; i++) { // 输出排序结果
//			System.out.println("(" + sort.arr[i].verName_typeINT + "," + sort.arr[i].Au + ")");
//		}

		for(int i=0;i<fetch_num;i++) {
			if(i<useful_obv_num)
				obvSet.add(sort.arr[i].verName_typeINT+"");
			else {
				break;
			}
		}
		//System.out.println(obvSet.toString());
		System.out.println("实际在算法中用到的观察点数量: "+obvSet.size());
		return obvSet;
	}

	/**
	 * 这个方法是getJordonCenter_dividedWeight的子方法，用于快速求解Jordan值
	 */
	public static double JordanValue_by_Dijstra(int[][] path, String source, ArrayList<String> obv_set) {
		double JordanValue=0;
		for (String string : obv_set) {
			JordanValue=JordanValue+path[Integer.parseInt(source)-1][Integer.parseInt(string)-1];
		}
		return JordanValue;
	}
	
	public static double optimal_JordanValue_by_Dijstra(Graph1 n, int[][] path, String source, ArrayList<String> obv_set) {
		double JordanValue=0;
		int index=0;
		for (String string : obv_set) {
			//Vertex1 obvNode=CreateGraph3.getVertex(n,string);
			JordanValue=JordanValue+ALG.shortPath_fourworkingsensor[index][Integer.parseInt(source)-1]+1;
			//JordanValue=JordanValue+path[Integer.parseInt(source)-1][Integer.parseInt(string)-1];
			
			index++;
		}
		return JordanValue;
	}

	
	/**
	 * 这个方法优化了每个点对于观察点距离和时间的参数，通过 时间/距离 保证了观察点和传染源位置的准确性，由于该方法需要使用距离这个参数，因此需要ALG.shortPath矩阵
	 * @param network
	 * @param usedObvs_set
	 * @param n
	 * @return
	 */
	public HashSet<String> getJordonCenter_dividedWeight(Graph1 network,ArrayList<String>usedObvs_set, int n, int type) {
		HashSet<String> allID_set=new HashSet<String>();
		Arraysort mySort=new Arraysort(network.verNum-observer_num);
		int index=0;
		for(int i=0;i<network.verNum;i++) {
			if(!network.vertexArray[i].isObserver) {
				mySort.arr[index].verName_typeINT=Integer.parseInt(network.vertexArray[i].verName);
				//mySort.arr[index].Au=JordanValue_by_Dijstra(ALG.shortPath, network.vertexArray[i].verName, usedObvs_set);
				mySort.arr[index].Au=optimal_JordanValue_by_Dijstra(network, ALG.shortPath, network.vertexArray[i].verName, usedObvs_set);
				index++;
			}
		}

		for(int i=0;i<mySort.arr.length;i++) {
			//System.out.print(mySort.arr[i].verName_typeINT+": Jordan 值："+mySort.arr[i].Au+" ｜｜｜｜｜");
			//Vertex1 candidateNode=CreateGraph3.getVertex(network, mySort.arr[i].verName_typeINT+"");
			double total_div=0;
			int indexx=0;
			for (String obv_name : usedObvs_set) {
				Vertex1 obvNode=network.vertexArray[Integer.parseInt(obv_name)-1];

				//double dis_real=ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.origin)-1]+1;
				double dis_real=ALG.shortPath_fourworkingsensor[indexx][mySort.arr[i].verName_typeINT-1]+1;
				//System.out.print("时间距离: "+obvNode.time+"|"+dis_real+"  but not"+(ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.origin)-1]+1)+"  but not"+ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.verName)-1]+"  ");
				//1. System.out.print("时间距离: "+obvNode.time+"|"+dis_real+"  but not"+ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.verName)-1]+"  ");

//				if(ALG.shortPath_removeEDGES[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.origin)-1]+1!=(ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.origin)-1]+1))
//					System.exit(0);
				if( ((double)obvNode.time/dis_real) >= (dis_real/(double)obvNode.time) ){
					//total_div+=obvNode.time/ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obv_name)-1];
					total_div+=(double)obvNode.time/dis_real;
				}else{
					total_div+=dis_real/(double)obvNode.time;
				}
				indexx++;

			}
			//System.out.println();

			if(type==-8) //前2/3 后3/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==-7) //前3/4 后2/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==-6) //前8/9，后3/4
				mySort.arr[i].Au=Math.pow(Math.pow(mySort.arr[i].Au, 0.111111), 8)*Math.pow(Math.pow(total_div, 0.25), 3);
			if(type==-5) //前1，后5/6
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.166667), 5);
			if(type==-4) //前1，后6/7
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.142857), 6);
			if(type==-3) //前1，后7/8
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.125), 7);
			if(type==-2) //前1，后8/9
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.111111), 8);
			if(type==-1) //前1，后9/10
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.1), 9);

			/*if(type==-2)  //前8/9，后3/4
				mySort.arr[i].Au=Math.pow(Math.pow(mySort.arr[i].Au, 0.111111), 8)*Math.pow(Math.pow(total_div, 0.25), 3);
			*/

			if(type==1) //前1/4 后1/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div));
			if(type==2) //前1/4 后1/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div);
			if(type==3) //前1/4 后1/2
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(total_div);
			if(type==4) //前1/4 后2/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==5) //前1/4 后3/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==6) //前1/4 后1
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*total_div;

			if(type==7) //前1/3 后1/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div));
			if(type==8) //前1/3 后1/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div);
			if(type==9) //前1/3 后1/2
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.sqrt(total_div);
			if(type==10) //前1/3 后2/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==11) //前1/3 后3/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==12) //前1/3 后1
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*total_div;

			if(type==13) //前1/2 后1/4
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div));
			if(type==14) //前1/2 后1/3
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.cbrt(total_div);
			if(type==15) //前1/2 后1/2
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.sqrt(total_div);
			if(type==16) //前1/2 后2/3
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==17) //前1/2 后3/4
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==18) //前1/2 后1
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*total_div;

			if(type==19) //前2/3 后1/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div));
			if(type==20) //前2/3 后1/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div);
			if(type==21) //前2/3 后1/2
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(total_div);
			if(type==22) //前2/3 后2/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==23) //前2/3 后3/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==24) //前2/3 后1
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*total_div;

			if(type==25) //前3/4 后1/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div));
			if(type==26) //前3/4 后1/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div);
			if(type==27) //前3/4 后1/2
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(total_div);
			if(type==28) //前3/4 后2/3 //best
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==29) //前3/4 后3/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==30) //前3/4 后1
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*total_div;

			if(type==31) //前1 后1/4
				mySort.arr[i].Au=mySort.arr[i].Au*Math.sqrt(Math.sqrt(total_div));
			if(type==32) //前1 后1/3
				mySort.arr[i].Au=mySort.arr[i].Au*Math.cbrt(total_div);
			if(type==33) //前1 后1/2
				mySort.arr[i].Au=mySort.arr[i].Au*Math.sqrt(total_div);
			if(type==34) //前1 后2/3
				mySort.arr[i].Au=mySort.arr[i].Au*Math.cbrt(total_div)*Math.cbrt(total_div);
			if(type==35) //前1 后3/4
				mySort.arr[i].Au=mySort.arr[i].Au*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			if(type==36) //前1 后1
				mySort.arr[i].Au=mySort.arr[i].Au*total_div;

			//对于观察点：只有最早来的才不加入惩罚：
//			double min_time=CreateGraph3.getVertex(network,usedObvs_set.get(0)).time;
//			double is_penalty=1;
//			double not_penalty=1.02;
//			for(int te=1;te<usedObvs_set.size()-1;te++){
//				if(min_time==CreateGraph3.getVertex(network,usedObvs_set.get(te)).time)
//					continue;
//				else{
//					if(ALG.shortPath_removeEDGES)
//				}
//
//			}
			//该节点每有一个未被感染的观察者邻居，就加入惩罚
			double min_time=network.vertexArray[Integer.parseInt(usedObvs_set.get(0))-1].time;
			double max=network.vertexArray[Integer.parseInt(  usedObvs_set.get(usedObvs_set.size()-1)  )-1].time;
			Vertex1 thisNode = network.vertexArray[mySort.arr[i].verName_typeINT-1];
			Vertex1 nerignborVertex=thisNode.nextNode;
			while(nerignborVertex!=null) {
				Vertex1 neig = network.vertexArray[Integer.parseInt(nerignborVertex.verName)-1];
				if(neig.isObserver&&neig.time==-1) {
					mySort.arr[i].Au=mySort.arr[i].Au*1.02;
				}
				if(neig.isObserver&&neig.time!=-1&& usedObvs_set.contains(neig.verName) &&neig.origin.equals(thisNode.verName)&&thisNode.not_get_award) {
					//邻居是 已经被感染的观察点                最早的3、4个                                    邻居被感染的来源就是thisNode           thisnode第一次被奖励
					if(min_time==max) {
						//所有working sensor感染时间相同，那就乘以0.9
						mySort.arr[i].Au=mySort.arr[i].Au*0.9;
						thisNode.not_get_award=false;
					}else {
						//working sensor感染时间不相同
						if(neig.time>min_time) {
							mySort.arr[i].Au=mySort.arr[i].Au*0.95;
							thisNode.not_get_award=false;
						}else {
							mySort.arr[i].Au=mySort.arr[i].Au*0.9;
							thisNode.not_get_award=false;
						}
							
					}
				}
					
				nerignborVertex=nerignborVertex.nextNode;
			}

		}

		Arrays.sort(mySort.arr,new MyComprator());

		for(int i=0;i<10;i++) {
			System.out.println("("+mySort.arr[i].verName_typeINT+","+mySort.arr[i].Au+")");
		}

		//for(int i=0;i<n;i++) {
			if(n==1) {
				if(mySort.arr[0].Au==mySort.arr[1].Au) {
					double MIN=mySort.arr[0].Au;
					for(int ii=0;ii<mySort.arr.length;ii++) {
						if(Math.abs(MIN-mySort.arr[ii].Au)<0.000001) {//还是前N个相同的Au对应的点
//							if(source==mySort.arr[ii].verName_typeINT) {
//								allID_set.add(source+"");
//								break;
//							}
							allID_set.add(mySort.arr[ii].verName_typeINT+"");
						}else {//前x个相同的Au点没一个属于传播源
							//allID_set.add(mySort.arr[0].verName_typeINT+"");
							break;
						}
					}
				}else {
					allID_set.add(mySort.arr[0].verName_typeINT+"");
				}
			}
//			else {
//				//n 和 相同Au的数量 也不一定相同，这里对于非单源输出也可以做优化
//				allID_set.add(mySort.arr[i].verName_typeINT+"");
//			}

		//}

		return allID_set;
	}

}

class Arraysort{
	Point[] arr;

	Arraysort(Graph1 network) {
		arr = new Point[network.verNum]; // 定义对象数组arr，并分配存储的空间
		for (int i = 0; i < network.verNum; i++) {
			arr[i] = new Point();
			//arr[i].verName_typeINT=i+1;
		}

	}

	Arraysort(int num) {
		arr = new Point[num]; // 定义对象数组arr，并分配存储的空间
		for (int i = 0; i < num; i++) {
			arr[i] = new Point();
			//arr[i].verName_typeINT=i+1;
		}

	}
}

class Point {
	int verName_typeINT;
	double Au;
}

class MyComprator implements Comparator {
	public int compare(Object arg0, Object arg1) {
		Point t1=(Point)arg0;
		Point t2=(Point)arg1;
		if(t1.Au != t2.Au)
			return t1.Au>t2.Au? 1:-1;
		else
			return t1.verName_typeINT>t2.verName_typeINT? 1:-1;
	}
}

