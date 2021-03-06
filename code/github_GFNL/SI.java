package github_GFNL;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

//import Test.Test;

public class SI {
	public double infe_rate;
	public double recovery_rate=0;
	public int times;
	public int observer_num;

	public int source;

	public ArrayList<String> infected_people;
	public ArrayList<String> temp;
	
	
	public int[][] readInf_4_from_oneFile(Graph1 network,ArrayList<String> obv_set, String shortPath_fileName){
		ArrayList<String> Opre_set=new ArrayList<String>();
		for (String obv_name : obv_set) {
			Opre_set.add(network.vertexArray[Integer.parseInt(obv_name)-1].origin);
		}
		
		int[][] short_temp=new int[Opre_set.size()][network.verNum];
		String filePath=shortPath_fileName;
		try {
			FileInputStream fileInputStream = new FileInputStream(filePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
			String line = null;
			int index=1;
			while ((line = bufferedReader.readLine()) != null) {
				if(Opre_set.contains(index+"")) {
			       String[] dis_all=line.split(" ");
			       int index2=0;
			       int right_pos=Opre_set.indexOf(index+"");
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
	
	public int[][] readInf_4(Graph1 network,ArrayList<String> obv_set){
		int[][] short_temp=new int[obv_set.size()][network.verNum];
		int index=0;
		for (String working_name : obv_set) {
			Vertex1 obv=network.vertexArray[Integer.parseInt(working_name)-1];
			String filePath="G:\\Data Files\\dblp_large\\v"+obv.origin+".txt";
			try {
				FileInputStream fileInputStream;
				fileInputStream = new FileInputStream(filePath);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
			    String line = null;
			    while ((line = bufferedReader.readLine()) != null) {
			       String[] dis_all=line.split(" ");
			       int index2=0;
			       for (String dis : dis_all) {
			    	   short_temp[index][index2]=Integer.parseInt(dis);
			    	   index2++;
			       }
			    }
			    fileInputStream.close();
			} catch (IOException e) {
				System.out.println("读文件的时候出现问题...");
				e.printStackTrace();
			}
			index++;
			
		}
		
		return short_temp;
	}

	
	void initNet_SIR_Greedy_fromFile(Graph1 network, double infected_P, double recovery_p, int inf_source_num, String sensor_file_path) {
		times = 10000000;
		infected_people = new ArrayList<String>();

		String[] myFONC_obvs = null;
		myFONC_obvs = Test.Test.readFromObvsFile(sensor_file_path);
		
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

		int[][] shortP_temp = new int[network.verNum][network.verNum];
		for (int copy_i = 0; copy_i < network.verNum; copy_i++) {
			for (int copy_j = 0; copy_j < network.verNum; copy_j++) {
				shortP_temp[copy_i][copy_j] = ALG.shortPath[copy_i][copy_j];
			}
		}

		int[][] H=new int[network.verNum][30];
		for(int i=0;i<ALG.shortPath.length;i++) {
			for (int j = 0; j < ALG.shortPath.length; j++) {
				if (i != j)
					H[i][ALG.shortPath[i][j]] = H[i][ALG.shortPath[i][j]] + 1;
			}
		}

		ArrayList<String> obvsArrayList = new ArrayList<String>();
		Arraysort sort=new Arraysort(network);

		for (int i = 1; i <= shortP_temp.length; i++) {
			yitaArraysort.arr[i - 1].verName_typeINT = i;
			int yita = 0;
			
			for (int j = 1; j <= shortP_temp.length; j++) {
				if (shortP_temp[i - 1][j - 1] != 0) {// 有距离的
					int dis = shortP_temp[i - 1][j - 1];
					yita=yita+H[j-1][dis];
				}
			}
			yitaArraysort.arr[i - 1].Au = yita;
		}
		
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

		int[][] shortP_notChange=new int[shortP_temp.length][shortP_temp.length];
		for(int ii=0;ii<shortP_temp.length;ii++)
			for(int jj=0;jj<shortP_temp.length;jj++)
				shortP_notChange[ii][jj]=shortP_temp[ii][jj];

		//所有趟的DFS就是由此入口开始
		for(int max_num=1;max_num<=network.verNum;max_num++){
			if(obvsArrayList.size()>=need_deployed_num) {
				
				break;
			}

			
			if(obvsArrayList.contains(sort.arr[network.verNum - max_num].verName_typeINT+"")){
				continue;
			}
			obvsArrayList.add(sort.arr[network.verNum - max_num].verName_typeINT + "");
			
			for(int ii=0;ii<shortP_temp.length;ii++)
				for(int jj=0;jj<shortP_temp.length;jj++)
					shortP_temp[ii][jj]=shortP_notChange[ii][jj];
			
			
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
				
				for (int i = 1; i <= shortP_temp.length; i++) {
					yitaArraysort.arr[i - 1].verName_typeINT = i;
					int yita = 0;
					
					for (int j = 1; j <= shortP_temp.length; j++) {
						if (shortP_temp[i - 1][j - 1] != 0) {
							int dis = shortP_temp[i - 1][j - 1];
							yita=yita+H[j-1][dis];
						}
					}
					yitaArraysort.arr[i - 1].Au = yita;
				}
				
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

					obvsArrayList.add(sort.arr[network.verNum - max_num_inDFS].verName_typeINT + "");
					
					
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

		int[][] shortP_temp = ALG.shortPath;

		int[][] H=new int[network.verNum][30];
		for(int i=0;i<ALG.shortPath.length;i++) {
			for (int j = 0; j < ALG.shortPath.length; j++) {
				if (i != j)
					H[i][ALG.shortPath[i][j]] = H[i][ALG.shortPath[i][j]] + 1;
			}
		}

		
		ArrayList<String> obvsArrayList = new ArrayList<String>();
		Arraysort sort=new Arraysort(network);


		for (int i = 1; i <= shortP_temp.length; i++) {
			yitaArraysort.arr[i - 1].verName_typeINT = i;
			int yita = 0;
			
			for (int j = 1; j <= shortP_temp.length; j++) {
				if (shortP_temp[i - 1][j - 1] != 0) {// 有距离的
					int dis = shortP_temp[i - 1][j - 1];
					yita=yita+H[j-1][dis];
				}
			}
			yitaArraysort.arr[i - 1].Au = yita;
		}

		for (int index = 0; index < network.verNum; index++) {
			sort.arr[index].verName_typeINT = yitaArraysort.arr[index].verName_typeINT;
			sort.arr[index].Au = yitaArraysort.arr[index].Au;
		}
		Arrays.sort(sort.arr, new MyComprator());
		for (int i = 0; i < 5; i++) { 
			System.out.println("(" + sort.arr[sort.arr.length-1-i].verName_typeINT + "," + sort.arr[sort.arr.length-1-i].Au + ")");
		}
		
		
		for(int ii=0;ii<sort.arr.length;ii++){
			sort.arr[ii].Au=sort.arr[ii].Au;
			sort.arr[ii].verName_typeINT=sort.arr[ii].verName_typeINT;
		}

	
		
		//所有趟的DFS就是由此入口开始
		for(int max_num=1;max_num<=network.verNum;max_num++){
			if(obvsArrayList.size()>=need_deployed_num) {
				
				break;
			}

			
			if(obvsArrayList.contains(sort.arr[network.verNum - max_num].verName_typeINT+"")){
				continue;
			}
			obvsArrayList.add(sort.arr[network.verNum - max_num].verName_typeINT + "");
			
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
				
				for (int i = 1; i <= shortP_temp.length; i++) {
					yitaArraysort.arr[i - 1].verName_typeINT = i;
					int yita = 0;
					
					for (int j = 1; j <= shortP_temp.length; j++) {
						if (shortP_temp[i - 1][j - 1] != 0) {
							int dis = shortP_temp[i - 1][j - 1];
							yita=yita+H[j-1][dis];
						}
					}
					yitaArraysort.arr[i - 1].Au = yita;
				}
				
				sort = new Arraysort(network);
				for (int index = 0; index < network.verNum; index++) {
					sort.arr[index].verName_typeINT = yitaArraysort.arr[index].verName_typeINT;
					sort.arr[index].Au = yitaArraysort.arr[index].Au;
				}
				Arrays.sort(sort.arr, new MyComprator());
				System.out.println("------------DFS内---------------");
				for (int i = 0; i < 5; i++) { 
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

					obvsArrayList.add(sort.arr[network.verNum - max_num_inDFS].verName_typeINT + "");
					
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
		int end=0; 
		int infe_nums=infected_people.size();
		
		for (int i = 1; i <= times && infe_nums <=network.verNum; i++) {
			temp=(ArrayList<String>) infected_people.clone();
			for (int j = 1; j <= temp.size(); j++) {
				String infed_name = temp.get(j-1);
				Vertex1 infed_person = network.vertexArray[Integer.parseInt(infed_name)-1];
				if (infed_person == null) {
					System.out.println("不存在该姓名!");
					return;
				}
				Vertex1 maybe_person = infed_person.nextNode;
				while (maybe_person != null) {
					Vertex1 maybe_node=network.vertexArray[Integer.parseInt(maybe_person.verName)-1];					
					if (maybe_node.infe==false && maybe_node.recover==false) {
						int node1_index_inM=Integer.parseInt(infed_person.verName)-1;
						int node2_index_inM=Integer.parseInt(maybe_node.verName)-1;
						if(true) {							
							Random rand = new Random();
							int a;
							a = (rand.nextInt(10000) < 10000 * this.infe_rate) ? 1 : 0;
							if (a == 1) {
								infe_nums ++;
								infected_people.add(maybe_node.verName);
								maybe_node.infe=true;
								maybe_node.time=i;
								maybe_node.origin=infed_name;
								
								if(maybe_node.isObserver) {
									greedy_infected_obv_num++;
									if(greedy_infected_obv_num==4) {
										MyExecute.time_myStartEXE4=MyExecute.time_myStartEXE4+i;
									}
									if(greedy_infected_obv_num==(int)(observer_num*MyExecute.ppt_of_I_sensor)) {
										
										MyExecute.time_myStartEXE30=MyExecute.time_myStartEXE30+i;
										end++;
									}
								}
								
								if(maybe_node.isRandomObserver) {
									random_infected_obv_num++;
									if(random_infected_obv_num==(int)(observer_num*MyExecute.ppt_of_I_sensor)) {
										
										MyExecute.time_randEXE30=MyExecute.time_randEXE30+i;
										end++;
									}
								}
								if(greedy_infected_obv_num>=20||infe_nums>=network.verNum*ppt_of_I||end==2){
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


		for(int i=0;i<fetch_num;i++) {
			if(i<useful_obv_num)
				obvSet.add(sort.arr[i].verName_typeINT+"");
			else {
				break;
			}
		}
		
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
	
	public static double optimal_JordanValue_by_Dijstra(Graph1 n, String source, ArrayList<String> obv_set, boolean isBigScaleNet) {
		double JordanValue=0;
		int index=0;
		for (String string : obv_set) {
			if(isBigScaleNet) {
				JordanValue=JordanValue+ALG.shortPath_fourworkingsensor[index][Integer.parseInt(source)-1]+1;
			}else {
				JordanValue=JordanValue+ALG.shortPath[Integer.parseInt(n.vertexArray[Integer.parseInt(string)-1].origin)-1][Integer.parseInt(source)-1];
			}
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
	public HashSet<String> getJordonCenter_dividedWeight(Graph1 network,ArrayList<String>usedObvs_set, int n, int type, boolean isBigScaleNet) {
		HashSet<String> allID_set=new HashSet<String>();
		Arraysort mySort=new Arraysort(network.verNum-observer_num);
		int index=0;
		for(int i=0;i<network.verNum;i++) {
			if(!network.vertexArray[i].isObserver) {
				mySort.arr[index].verName_typeINT=Integer.parseInt(network.vertexArray[i].verName);
				mySort.arr[index].Au=optimal_JordanValue_by_Dijstra(network, network.vertexArray[i].verName, usedObvs_set, isBigScaleNet);
				index++;
			}
		}

		for(int i=0;i<mySort.arr.length;i++) {
			double total_div=0;
			int indexx=0;
			for (String obv_name : usedObvs_set) {
				Vertex1 obvNode=network.vertexArray[Integer.parseInt(obv_name)-1];

				double dis_real;
				if(isBigScaleNet) {
					dis_real=ALG.shortPath_fourworkingsensor[indexx][mySort.arr[i].verName_typeINT-1]+1;
				}else {
					dis_real=ALG.shortPath[mySort.arr[i].verName_typeINT-1][Integer.parseInt(obvNode.origin)-1]+1;
				}
				
				if( ((double)obvNode.time/dis_real) >= (dis_real/(double)obvNode.time) ){
					total_div+=(double)obvNode.time/dis_real;
				}else{
					total_div+=dis_real/(double)obvNode.time;
				}
				indexx++;

			}
			//System.out.println();

			if(type==-8) //前2/3 后3/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==-7) //前3/4 后2/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==-6) //前8/9，后3/4
				mySort.arr[i].Au=Math.pow(Math.pow(mySort.arr[i].Au, 0.111111), 8)*Math.pow(Math.pow(total_div, 0.25), 3);
			else if(type==-5) //前1，后5/6
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.166667), 5);
			else if(type==-4) //前1，后6/7
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.142857), 6);
			else if(type==-3) //前1，后7/8
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.125), 7);
			else if(type==-2) //前1，后8/9
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.111111), 8);
			else if(type==-1) //前1，后9/10
				mySort.arr[i].Au=mySort.arr[i].Au*Math.pow(Math.pow(total_div, 0.1), 9);

			/*if(type==-2)  //前8/9，后3/4
				mySort.arr[i].Au=Math.pow(Math.pow(mySort.arr[i].Au, 0.111111), 8)*Math.pow(Math.pow(total_div, 0.25), 3);
			*/

			else if(type==1) //前1/4 后1/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div));
			else if(type==2) //前1/4 后1/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div);
			else if(type==3) //前1/4 后1/2
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(total_div);
			else if(type==4) //前1/4 后2/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==5) //前1/4 后3/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==6) //前1/4 后1
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*total_div;

			else if(type==7) //前1/3 后1/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div));
			else if(type==8) //前1/3 后1/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div);
			else if(type==9) //前1/3 后1/2
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.sqrt(total_div);
			else if(type==10) //前1/3 后2/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==11) //前1/3 后3/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==12) //前1/3 后1
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*total_div;

			else if(type==13) //前1/2 后1/4
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div));
			else if(type==14) //前1/2 后1/3
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.cbrt(total_div);
			else if(type==15) //前1/2 后1/2
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.sqrt(total_div);
			else if(type==16) //前1/2 后2/3
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==17) //前1/2 后3/4
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==18) //前1/2 后1
				mySort.arr[i].Au=Math.sqrt(mySort.arr[i].Au)*total_div;

			else if(type==19) //前2/3 后1/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div));
			else if(type==20) //前2/3 后1/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div);
			else if(type==21) //前2/3 后1/2
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(total_div);
			else if(type==22) //前2/3 后2/3
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==23) //前2/3 后3/4
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==24) //前2/3 后1
				mySort.arr[i].Au=Math.cbrt(mySort.arr[i].Au)*Math.cbrt(mySort.arr[i].Au)*total_div;

			else if(type==25) //前3/4 后1/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div));
			else if(type==26) //前3/4 后1/3
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div);
			else if(type==27) //前3/4 后1/2
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(total_div);
			else if(type==28) //前3/4 后2/3 //best
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==29) //前3/4 后3/4
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==30) //前3/4 后1
				mySort.arr[i].Au=Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*Math.sqrt(Math.sqrt(mySort.arr[i].Au))*total_div;

			else if(type==31) //前1 后1/4
				mySort.arr[i].Au=mySort.arr[i].Au*Math.sqrt(Math.sqrt(total_div));
			else if(type==32) //前1 后1/3
				mySort.arr[i].Au=mySort.arr[i].Au*Math.cbrt(total_div);
			else if(type==33) //前1 后1/2
				mySort.arr[i].Au=mySort.arr[i].Au*Math.sqrt(total_div);
			else if(type==34) //前1 后2/3
				mySort.arr[i].Au=mySort.arr[i].Au*Math.cbrt(total_div)*Math.cbrt(total_div);
			else if(type==35) //前1 后3/4
				mySort.arr[i].Au=mySort.arr[i].Au*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div))*Math.sqrt(Math.sqrt(total_div));
			else if(type==36) //前1 后1
				mySort.arr[i].Au=mySort.arr[i].Au*total_div;

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
						if(Math.abs(MIN-mySort.arr[ii].Au)<0.000001) {
							allID_set.add(mySort.arr[ii].verName_typeINT+"");
						}else {//前x个相同的Au点没一个属于传播源
							
							break;
						}
					}
				}else {
					allID_set.add(mySort.arr[0].verName_typeINT+"");
				}
			}


		return allID_set;
	}

}

class Arraysort{
	Point[] arr;

	Arraysort(Graph1 network) {
		arr = new Point[network.verNum]; 
		for (int i = 0; i < network.verNum; i++) {
			arr[i] = new Point();
			
		}

	}

	Arraysort(int num) {
		arr = new Point[num]; 
		for (int i = 0; i < num; i++) {
			arr[i] = new Point();
			
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

