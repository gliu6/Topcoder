import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;



public class PathDefense {

	static char[][] plane;
	static int planeSize;
	
	static int acount;
	static int cHealth;
	static int cMoney;
	static int numOfTowers = 0;
	static int towerR[];
	static int towerD[];
	static int towerC[];
	static int maxRange = 0;
	static int previousBaseH = 0;
	
	
	static int[] baseX = new int[10];
	static int[] baseY = new int[10];
	static int numOfBases = 0; //smaller than 10
	static ArrayList<base> baseList = new ArrayList<base>();
	static LinkedList<place> allPlaces = new LinkedList<place>();
	static ArrayList<road> roadList = new ArrayList<road>();
	static ArrayList<tower> towerList = new ArrayList<tower>();
	static ArrayList<path> pathList = new ArrayList<path>();
	static ArrayList<path> entranceList = new ArrayList<path>();
	
	static int simulationTime = 0;
	static int timeCost = 0;
	
	static int roadLimit = 5;
	static LinkedList<place> topPlaces = new LinkedList<place>();
	
	static int[] creepChoosePath = new int[2001];
	static double[] creepChoosePathPriority = new double[2001];
	
	static ArrayList<road> crossRoadList = new ArrayList<road>();
	
	static int totalCreep = 500;
	static int spawnedCreep = 0;
	
	static int[] creepBornX = new int[2001];
	static int[] creepBornY = new int[2001];
	
	static double[][] creepExistProb = new double[60][60];
	
	
	public static class tower{
		place p = null;    //the place where the tower is built
		int index = 0;    //indicate the tower type
		int range = 0;
		int damage = 0;
		int cost = 0;
		boolean fired = false;
		boolean[] firedArr = new boolean[200]; //mark whether it has fired in the time slot or not
		ArrayList<path>	pathList = new ArrayList<path>(); //the paths it covers
		public tower(place p, int range, int damage, int cost) {
			super();
			this.p = p;
			this.range = range;
			this.damage = damage;
			this.cost = cost;
			for(int i=0; i<firedArr.length; i++){
				firedArr[i] = false;
			}
		}
	}
	
	public static class monster implements Comparable<monster>{
		int x;
		int y;
		int health;
		int index;
		
		road r = null;
		double pathProb = 0; //the probability of choosing a path after adjusting
		int remainPath = 0;
		int startPath = 0;
		
		int tempHealth = 0;
		
		public monster(int index, int health, int x, int y) {
			super();
			this.index = index;
			this.health = health;
			this.x = x;
			this.y = y;
		}

		public int compareTo(monster o) {
			// TODO Auto-generated method stub
			return this.index - o.index;
		}
	}
	
	
	public static class place implements Comparable<place>{
		int x;
		int y;
		int selectedTower = 0;
		int[] spots = new int[5]; //how many path cells it can shoot from range 1 to 5
		int totalSpot = 0;
		int[] distance = new int[8]; //distance from all bases, maximum number of bases is 8, distance is in the pow format.
		int avgDistance = 0;
		
		//the attack ability/money spent
		int OverallHarm = 0; // total attack possible, which is estimated
		int EconomicTower = 0;
		
		//whether it is used for a tower or not
		boolean selected = false;
		
		public place(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}
		
		@Override
	    public boolean equals(Object obj) {   
	            if (obj instanceof path) {   
	            	path u = (path) obj;   
	                if(this.x==u.x&&this.y==u.y)
	                	return true;
	            }   
	            return super.equals(obj);  
	    }

		public int compareTo(place o) {
			// TODO Auto-generated method stub
			return o.OverallHarm - this.OverallHarm;
		}
	}
	
	
	public static class base{
		int x;
		int y;
		int health = 1000;
		public base(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}
		@Override
	    public boolean equals(Object obj) {   
	            if (obj instanceof base) {   
	            	base u = (base) obj;   
	                if(this.x==u.x&&this.y==u.y)
	                	return true;
	            }   
	            return super.equals(obj);  
	    }
		
	}
	
	public static class path{
		int x;
		int y;
		ArrayList<monster> creepList = new ArrayList<monster>();
		int totalCreepHealth = 0;  //the health of all creeps who pass this cell
		
		//limits on the roads to all bases, index 0 means to base 0, the number of roads from this entry
		int[] toBaseLimit = new int[10];
		
				
		public 	path(int x, int y) {
			super();
			this.x = x;
			this.y = y;
			for(int i=0; i<toBaseLimit.length; i++){
				toBaseLimit[i] = roadLimit;
			}
			toBaseLimit[1] = 20;
		}
		@Override
	    public boolean equals(Object obj) {   
	            if (obj instanceof path) {   
	            	path u = (path) obj;   
	                if(this.x==u.x&&this.y==u.y)
	                	return true;
	            }   
	            return super.equals(obj);  
	    }
	}
	
	public static class road implements Comparable<road>{
		boolean[] toBases = new boolean[10];
		int[] counterOfMoveAway = new int [10];
		ArrayList<path> pathList = new ArrayList<path>();
		
		double priority = 1;
		
		public road() {
			super();
			for(int i=0; i<10; i++){
				toBases[i] = true;
				counterOfMoveAway[i] = 1;
			}
		}

		public int compareTo(road r) {
			// TODO Auto-generated method stub
			//sort accordign to the start location and the length
			if(this.pathList.get(0).x !=r.pathList.get(0).x){
				return this.pathList.get(0).x - r.pathList.get(0).x;
			}else if(this.pathList.get(0).y !=r.pathList.get(0).y){
				return this.pathList.get(0).y - r.pathList.get(0).y;
			}else{
				return (int)((r.priority - this.priority)*1000000);
			}
			
		}
		
	}
	
	
	public static int init(String[] board, int money, int creepHealth, int creepMoney, int[] towerTypes) throws IOException{
		
		if(board.length<= 30){
			roadLimit = 10;
		}else {
			roadLimit = 5;
		}
		
		for(int i=0; i<creepChoosePath.length; i++){
			creepChoosePath[i] = -1;
			creepChoosePathPriority[i] = 0;
		}
		
		for(int i=0; i<creepBornX.length; i++){
			creepBornX[i] = 0;
			creepBornY[i] = 0;
		}
		
		for(int i=0; i<60; i++){
			for(int j=0; j<60; j++){
				creepExistProb[i][j] = 0;
			}
		}
		
		plane = new char[board.length][board.length];
		planeSize = board.length;
		for(int i=0; i<board.length; i++){
			if(board[i]!=null){
				plane[i]=board[i].toCharArray();
			}
		}
		
		//matrix transposition
		for(int i=0; i<board.length; i++){
			for(int j=0; j<i; j++){
				char temp = plane[i][j];
				plane[i][j] = plane[j][i];
				plane[j][i] = temp;
			}
		}
		
		//local output test
//		for(int j=0; j<board.length; j++){
//			for(int i=0; i<board.length; i++){
//				System.out.print(plane[i][j]);
//			}
//			System.out.println();
//		}
		
		//initialization of bases
		for(int i=0; i<10; i++){
			baseX[i] = 0;
			baseY[i] = 0;
		}
		for(int i=0; i<planeSize; i++){
			for(int j=0; j<planeSize; j++){
				if(plane[i][j]>='0'&&plane[i][j]<'9'){
					base b = new PathDefense.base(i,j);
					baseList.add(b);
					int index = plane[i][j] -'0';
					baseX[index] = i;
					baseY[index] = j;
					numOfBases++;
				}
				if(plane[i][j]=='.'){
					path p = new path(i,j);
					pathList.add(p);
					if(i==0||j==0||i==planeSize-1||j==planeSize-1){
						entranceList.add(p);
					}
				}
			}
		}

		acount = money;
		cHealth = creepHealth;
		cMoney = creepMoney;
		
		numOfTowers = towerTypes.length/3;
		towerR = new int[towerTypes.length/3];
		towerD = new int[towerTypes.length/3];
		towerC = new int[towerTypes.length/3];
		
		int num = 0;
		for(int i=0; i<towerTypes.length/3; i++){
			towerR[i] = towerTypes[num];
			if(towerR[i] > maxRange){
				maxRange = towerR[i];
			}
			num++;
			towerD[i] = towerTypes[num];
			num++;
			towerC[i] = towerTypes[num];
			num++;
		}
		
		//tower evaluation, disable some towers that totally are weaker than others
		for(int i=0; i<towerTypes.length/3; i++){
			for(int j=i+1; j<towerTypes.length/3; j++){
				if(towerR[i]<=towerR[j]&&towerD[i]<=towerD[j]&&towerC[i]>=towerC[j]){
					towerR[i] = 0;
					towerD[i] = 0;
					towerC[i] = 0;
				}
				if(towerR[j]<=towerR[i]&&towerD[j]<=towerD[i]&&towerC[j]>=towerC[i]){
					towerR[j] = 0;
					towerD[j] = 0;
					towerC[j] = 0;
				}
			}
		}
		
		
		double time1 = System.nanoTime();
		
		placeEvaluation();
		
		//place
		placeAnalysis();
		
		
		double time2 = System.nanoTime();
		timeCost += (time2 -time1)/1000000;
		
		int total = 0;
		for(place p: topPlaces){
			total += p.OverallHarm;
		}
		total = total/topPlaces.size();
		
		time1 = System.nanoTime();
		
		//find all paths
		findAllPaths();
		
//		writer.println("find cross roads complete, total paths: " + crossRoadList.size());
//		for(int i=0; i<crossRoadList.size(); i++){
//			writer.print("cross road: ");
//			for(path p: crossRoadList.get(i).pathList){
//				writer.print(p.x + " " + p.y+", ");
//			}
//			path tp = crossRoadList.get(i).pathList.get(crossRoadList.get(i).pathList.size()-1);
//			base bb = new base(tp.x, tp.y);
//			int index = baseList.indexOf(bb);
//			writer.print(" with baseLimit: " + crossRoadList.get(i).pathList.get(0).toBaseLimit[index]);
//			writer.println();
//		}
//		writer.flush();
		
		
		time2 = System.nanoTime();
		timeCost += (time2 -time1)/1000000;
		
		
		time1 = System.nanoTime();
		//assign path priority and sort the paths
		pathPriorityAnalysis();
		
		time2 = System.nanoTime();
		timeCost += (time2 -time1)/1000000;
		
		//calculate the probability that a path may have creep pass by
		creepAppearInPath();
		//output test
		
		
		
		
		return 0;
	}
	
	
	
	public static void attackAbility() {
		
	}
	
	public static boolean placeDecision(int[] creep, int[] baseHealth, int time){
		//by base health;
		int totalHealth = 0;
		for(int i=0; i<baseHealth.length; i++){
			totalHealth += baseHealth[i];
		}
		if(totalHealth < previousBaseH){
			previousBaseH = totalHealth;
			return true;
		}
		
		previousBaseH = totalHealth;
		
		//by time
		if(time==3||time==8||time==15||time==25||time==40){
			return true;
		}
		
		return false;
	}
	
	public static int[] placeTowers(int[] creep, int money, int[] baseHealth) throws Exception{
		
		
		
	
		simulationTime++;
		//control by simulation time
//		if(simulationTime%50!=0&&simulationTime !=3&&simulationTime !=10&&simulationTime !=20&&simulationTime !=35){
//			int[] temp = new int[0];
//			return temp;
//		}
		
		//or control by base health status, if get further attack, update information
//		if(!placeDecision(creep, baseHealth, simulationTime)){
//			int[] temp = new int[0];
//			writer.close();
//			return temp;
//		}
		
		
		ArrayList<monster> creepList = new ArrayList<monster>();
		creepList.clear();
		for(int i=0; i<creep.length; i=i+4){
			monster m = new monster(creep[i], creep[i+1], creep[i+2], creep[i+3]);
			//new born creep init;
			if(creep[i+2]==0||creep[i+2]==planeSize-1||creep[i+3]==0||creep[i+3]==planeSize-1){
				if(creepBornX[creep[i]] ==0){
					spawnedCreep++;
				}
				creepBornX[creep[i]] = creep[i+2];
				creepBornY[creep[i]] = creep[i+3];
			}
			if(creep[i]>totalCreep){
				totalCreep = creep[i];
			}
			//potential bug in the main procedure.
			creepList.add(m);
		}
		
		//sort creeps according to index
		Collections.sort(creepList);
		
		
		//output test
		
		if(creepList.size()==0){
			int[] nothing = {};
			return nothing;
		}
		
		
		//creep find paths (roads), currently, use random methods
		double time1 = System.nanoTime();
		ArrayList<monster> badCreepList = new ArrayList<monster>();
		for(monster m: creepList){
			//creep only choose path when it is spawned.
			//if the creep is not on entry, it already has a road
			path p = new path(m.x, m.y);
			
			//always select path again
			creepChoosePath[m.index]=-1;
			
//			if(creepChoosePath[m.index]!=-1){
//				if(roadList.get(creepChoosePath[m.index]).pathList.contains(p)){
//					m.r = roadList.get(creepChoosePath[m.index]);
//					m.startPath = roadList.get(creepChoosePath[m.index]).pathList.indexOf(p);
//					m.remainPath = roadList.get(creepChoosePath[m.index]).pathList.size() - m.startPath;
//					
//					//update the selectedRoad prob
//					double totalProb = 0;
//					for(road r: roadList){
//						if(r.pathList.get(0).x == creepBornX[m.index]&& r.pathList.get(0).y == creepBornY[m.index]){
//							if(r.pathList.contains(p)){
//								totalProb += r.priority;
//							}
//						}
//					}
//					if(totalProb == 0){
//						for(road r: roadList){
//							if(r.pathList.contains(p)){
//								totalProb += r.priority;
//							}
//						}
//					}
//					m.pathProb = creepChoosePathPriority[m.index] / totalProb;
//				}else{
//					creepChoosePath[m.index] = -1;
//				}
//				//all time calculation, cost more time in budget tower since the movePath are more and may cause unnecessary 
//				//tower placement which cost money. Good thing is maybe better defense.
////				creepChoosePath[m.index] = -1;
//			}
			
			
			//if it is just spawned. choose a road by priority
			if(creepChoosePath[m.index]==-1){
				double ran = Math.random();
				boolean founded = false;
				int fullRound = 0;
				double maxPriority = 0;
				road selectedRoad = null;
				double totalProb = 0;
				int remainStep = 0;
				while(!founded){
					for(road r: roadList){
						if(r.pathList.get(0).x == creepBornX[m.index]&& r.pathList.get(0).y == creepBornY[m.index]&&r.pathList.contains(p)){
							int length1 = 1000;
							if(selectedRoad!=null){
								length1 = selectedRoad.pathList.size() -2 - selectedRoad.pathList.indexOf(p);
							}
							
							int length2 = r.pathList.size() -2 - r.pathList.indexOf(p);
							//choose short than choose large probability
							if(length2< length1){
								selectedRoad = r;
								maxPriority = r.priority;
								remainStep = r.pathList.size() -2 - r.pathList.indexOf(p);
							}
							else if(length2==length1){
								if(r.priority>maxPriority){
									selectedRoad = r;
									maxPriority = r.priority;
									remainStep = length2;
								}
							}
							if(r.pathList.contains(p)){
								totalProb +=r.priority;
							}
							
//							if(r.priority>=ran){
//								if(r.pathList.contains(p)){
//									m.r = r;
//									m.startPath = r.pathList.indexOf(p);
//									founded = true;
//									creepChoosePath[m.index] = roadList.indexOf(r);
//									break;
//								}else{
//									ran -= r.priority;
//								}
//							}else{
//								ran -= r.priority;
//							}
						}
						else if(r.pathList.get(0).x != creepBornX[m.index]|| r.pathList.get(0).y != creepBornY[m.index]){
							break;
						}
					}
					//if we find it from its original path
					if(selectedRoad!=null){
						m.r = selectedRoad;
						m.startPath = selectedRoad.pathList.indexOf(p);
						founded = true;
						creepChoosePath[m.index] = roadList.indexOf(selectedRoad);
//						m.remainPath = remainStep;
						m.remainPath = selectedRoad.pathList.size() -2 - m.startPath;
						m.pathProb = selectedRoad.priority / totalProb;
						creepChoosePathPriority[m.index] = selectedRoad.priority;
						break;
					}
					//if not, try to find it from overall roads
					else{
						maxPriority = 0;
						totalProb = 0;
						remainStep = 0;
						for(road r: roadList){
							if(r.pathList.contains(p)){
								int length1 = 1000;
								if(selectedRoad!=null){
									length1 = selectedRoad.pathList.size()  -2- selectedRoad.pathList.indexOf(p);
								}
								int length2 = r.pathList.size()  -2 - r.pathList.indexOf(p);
								//choose short than choose large probability
								if(length2< length1){
									selectedRoad = r;
									maxPriority = r.priority;
									remainStep = r.pathList.size()  -2- r.pathList.indexOf(p);
								}
								else if(length2==length1){
									if(r.priority>maxPriority){
										selectedRoad = r;
										maxPriority = r.priority;
										remainStep = length2;
									}
								}
								if(r.pathList.contains(p)){
									totalProb +=r.priority;
								}
							}
						}
					}
					if(selectedRoad!=null){
						m.r = selectedRoad;
						m.startPath = selectedRoad.pathList.indexOf(p);
						founded = true;
						creepChoosePath[m.index] = roadList.indexOf(selectedRoad);
//						m.remainPath = remainStep;
						m.remainPath = selectedRoad.pathList.size() -2 - m.startPath;
						m.pathProb = selectedRoad.priority / totalProb;
						creepChoosePathPriority[m.index] = selectedRoad.priority;
						break;
					}
					if(!founded){
						badCreepList.add(m);
						break;
					}
				}
					
//					fullRound++;
//					//if cannot find from the birth paths, find from overall paths
//					if(fullRound==2){
//						int temp = (int)(Math.random()*roadList.size());
//						//try to find from index temp to the last
//						for(int i=temp; i<roadList.size(); i++){
//							road r = roadList.get(i);
//							if(r.pathList.contains(p)){
//								m.r = r;
//								m.startPath = r.pathList.indexOf(p);
//								founded = true;
//								creepChoosePath[m.index] = i;
//								m.remainPath = m.r.pathList.size() - m.startPath;
//								m.pathProb = r.priority / totalProb;
//								m.originalProb = selectedRoad.priority;
//								break;
//							}
//						}
//						//try to find from 0 to temp
//						if(!founded){
//							for(int i=0; i<temp; i++){
//								road r = roadList.get(i);
//								if(r.pathList.contains(p)){
//									m.r = r;
//									m.startPath = r.pathList.indexOf(p);
//									founded = true;
//									creepChoosePath[m.index] = i;
//									m.remainPath = m.r.pathList.size() - m.startPath;
//									m.pathProb = r.priority / totalProb;
//									break;
//								}
//							}
//						}
//						if(!founded){
//							badCreepList.add(m);
//							break;
//						}
//					}
			}
			
			
			//choose path, can be improved, currently, we always choose the first one
//			int temp = (int)(Math.random()*roadList.size());
//			path p = new path(m.x, m.y);
//			boolean found = false;
//			int fullRound = 0;//tells whether we have go through all paths, if yes, it means the current creep's cell is not in any path
//			while(!found){
//				if(roadList.get(temp).pathList.contains(p)){
//					found = true;
//					m.r = roadList.get(temp);
//					m.startPath = roadList.get(temp).pathList.indexOf(p);
//				}else {
//					temp++;
//					if(temp==roadList.size()){
//						temp=0;
//					}
//				}
//				fullRound++;
//				if(fullRound==roadList.size()){
//					badCreepList.add(m);
//					break;
//				}
//			}
			//output test
			
		}
		
		//remove some creeps if they choose very low probability path by a chance
		double probThreshold = 0.25;
		int stepThreshold = 6;
		for(monster m: creepList){
//			double rand = Math.random();
			double rand = 1.0;
			if(!badCreepList.contains(m)&&m.pathProb<probThreshold&&m.pathProb<rand&&m.remainPath>stepThreshold){
				badCreepList.add(m);
			}
		}
		
		
		creepList.removeAll(badCreepList);
		
		double time2 = System.nanoTime();
		timeCost += (time2 -time1)/1000000;
		
		
		//simulation, see how many creeps survive after the attack from current tower.
		time1 = System.nanoTime();
		surviveFromTowers(creepList, towerList);
		time2 = System.nanoTime();
		timeCost += (time2 -time1)/1000000;
		
		int totalCreepHealth = 0;
		for(monster m: creepList){
			if(m.health>0){
				totalCreepHealth += m.health;
			}
		}
		
		//output test
		
		//see where is the best place for next tower.
		//see the cumulative health in the paths
		time1 = System.nanoTime();
		ArrayList<path>	movePathList = movePathHealth(creepList);
		time2 = System.nanoTime();
		timeCost += (time2 -time1)/1000000;
		
		//output test
		
		//controller, control whether it is needed to calculate bestbudget
		//if no creep can survive the current towers, then simply we don't need to place more tower for this round
		if(movePathList.size()==0){
			int[] temp = new int[0];
			return temp;
		}
		
		
		double t1 = System.nanoTime();
		ArrayList<tower> towerInRnd = new ArrayList<tower>();
		//best budget method, every time select the best place to see whether we need more towers or not
		boolean needTower = false;
		do{
			needTower = false;
			
			time1 = System.nanoTime();
			tower to = bestBudget(movePathList, creepList, money, totalCreepHealth);
			time2 = System.nanoTime();
			
			if(to!=null){
				to.p.selected = true;
				towerInRnd.add(to);
				towerList.add(to);
				needTower = true;
				//update money
				money -= to.cost;
				//update creep health after the tower
				ArrayList<tower> tempList = new ArrayList<tower>();
				tempList.add(to);
				surviveFromTowers(creepList, tempList);
				
				//output test
				
				//update path health for new round calculation
				movePathList = movePathHealth(creepList);
				if(movePathList.size()==0){
					needTower = false;
				}
			}
		}while(needTower);
		double t2 = System.nanoTime();
		timeCost += (t2-t1)/1000000;
		
		
		//output test
		
		
		int[] temp = new int[towerInRnd.size()*3];
		
		int num = 0;
		for(tower t: towerInRnd){
			temp[num++] = t.p.x;
			temp[num++] = t.p.y;
			temp[num++] = t.index;
		}

		return temp;
	}
	public static void surviveFromTowers(ArrayList<monster> creepList, ArrayList<tower> towerList){
		
		//recover all tower fire condition
		for(tower t: towerList){
			for(int i=0; i<t.firedArr.length; i++){
				t.firedArr[i] = false;
			}
		}
		
		for(monster m: creepList){
			int step = 1;
			boolean moveMove = true;
			while(moveMove){
				moveMove = false;
				if(m.health>0&&m.startPath+step<m.r.pathList.size()-1){
					path p = m.r.pathList.get(m.startPath+step);
					moveMove = true;
					for(tower t: towerList){
						if(t.pathList.contains(p)&&!t.firedArr[step]){
							t.firedArr[step] = true;
							m.health -= t.damage;
						}
					}
				}
				step++;
			}
		}
	}
	
	public static ArrayList<path> movePathHealth(ArrayList<monster> creepList){
		ArrayList<path>	movePathList = new ArrayList<path>(); //if a creep passes a path, then add the path
		movePathList.clear();
		boolean moveMove = true;
		int step = 1;
		while(moveMove){
			moveMove = false;
			for(monster m: creepList){
				if(m.health>0&&m.startPath+step<m.r.pathList.size()-1){
					path p = m.r.pathList.get(m.startPath+step);
					moveMove = true;
					if(!movePathList.contains(p)){
						movePathList.add(p);
						p.totalCreepHealth += m.health;
						p.creepList.add(m);
					}else{
						int index = movePathList.indexOf(p);
						movePathList.get(index).totalCreepHealth += m.health;
						movePathList.get(index).creepList.add(m);
					}
				}
			}
			step++;
		}
		return movePathList;
	}
	
	public static tower bestBudget(ArrayList<path> movePathList, ArrayList<monster> creepList, int money, int totalCreepHealth) throws Exception{
		place selectedPlace = null;
		tower selectedTower = null;
		int max = -10000;
		int selectedReducedD = 0;
		double selectedFutureBonus = 0;
		
		//check whether the money is enough to buy any tower
		boolean affordable = false;
		for(int i=0; i<numOfTowers; i++){
			if(towerC[i] <= money&&towerC[i]!=0){
				affordable = true;
				break;
			}
		}
		
		if(!affordable){
			return null;
		}
		
		
//		pre analysis on topPlaces, add a threshold. This may hurt the performance, but save time
		int minHarm = 100000;
		
		
//		int threshold = topPlaces.size()/2;
//		int threshold = pathList.size()*2;
		
//		int threshold = 100;
//		if(numOfTowers<= 18){
//			threshold = 100;
//		}
//		if(numOfTowers<= 12){
//			threshold = 200;
//		}
//		if(numOfTowers<= 6){
//			threshold = 400;
//		}
//		
//		if(threshold>topPlaces.size()){
//			threshold = topPlaces.size();
//		}
		//we can also try to pre select places.
		//if smaller than 100, it is ok. otherwise, limit it to 50.
//		if(topPlaces.size()<100){
//			threshold = topPlaces.size();
//		}
//		if(topPlaces.size()<threshold){
//			threshold = topPlaces.size();
//		}
//		for(int i=0; i<threshold; i++){
//			if(topPlaces.get(i).OverallHarm < minHarm){
//				minHarm = topPlaces.get(i).OverallHarm;
//			}
//		}
		
		
		//another way to pre select places, it is according to the movePath Condition
		ArrayList<place> bestPlaces = new ArrayList<place>();
		for(place p: topPlaces){
			p.OverallHarm = 0;
			boolean covered = false;
			for(path p1: movePathList){
				if(Math.pow(p.x - p1.x, 2) + Math.pow(p.y - p1.y, 2) <=Math.pow(maxRange, 2)){
					covered = true;
					p.OverallHarm += p1.totalCreepHealth;
				}
			}
			if(covered){
				bestPlaces.add(p);
			}
		}
//		Collections.sort(bestPlaces);
		
		
		
		
		
		//only check the places before the threshold
		for(place p: bestPlaces){
			if(!p.selected){ 
				for(int i=0; i<numOfTowers; i++){
					//if cannot afford, continue
					if(towerC[i]>money||towerD[i]==0||towerR[i]==0){
						continue; 
					}
					tower t = new tower(p, towerR[i], towerD[i], towerC[i]);
					int balance = 0;
					int reducedDamage = 0;
					int kills = 0;
					double futureBonus = 0;
					
					//check the spots and the possible damage
					for(int k=0; k<towerR[i]; k++){
						//decide start position and boundaries
						int startX = p.x-(k+1);
						int moveX = (k+1)*2+1;
						int startY = p.y-(k+1);
						int moveY = (k+1)*2+1;
						if(startX<0){
							startX = 0;
							moveX = moveX - Math.abs(startX);
						}
						if(startY<0){
							startY = 0;
							moveY = moveY - Math.abs(startY);
						}
						if(startX+ moveX>planeSize-1){
							moveX = planeSize -1 - startX;
						}
						if(startY+ moveY>planeSize-1){
							moveY = planeSize -1 - startY;
						}
						for(int ii=0; ii<moveX; ii++){
							for(int jj=0; jj<moveY; jj++){
								boolean covered = false;
								if(Math.pow(startX+ii-p.x, 2) + Math.pow(startY+jj-p.y, 2)<= Math.pow(k+1, 2)){
									covered = true;
								}
								if(plane[startX+ii][startY+jj]=='.'&&covered){
									path temp = new path(startX+ii, startY+jj);
									t.pathList.add(temp);
//									int index = movePathList.indexOf(temp);
//									if(index!=-1){
//										int totalHealth = movePathList.get(index).totalCreepHealth;
//										int damage = towerD[i];
//										if(damage>totalHealth){
//											damage = totalHealth;
//											kills++;
//										}
//										
//										reducedDamage += damage;
//									}
								}
							}
						}
					}
					
					//current creep calculation. select tower by satisfying current need
					for(monster m: creepList){
						m.tempHealth = m.health;
						double roundDamage = 0; 
						boolean moveMove = true;
						int step = 1;
						while(moveMove){
							moveMove = false;
							if(m.tempHealth>0&&m.startPath+step<m.r.pathList.size()-1){
								path mp = m.r.pathList.get(m.startPath+step);
								moveMove = true;
								path last = m.r.pathList.get(m.r.pathList.size()-1);
								base b = new base(last.x, last.y);
								base targetBase = baseList.get(baseList.indexOf(b));
								if(t.pathList.contains(mp)&&!t.firedArr[step]){
									if(targetBase.health==0){
										t.firedArr[step] = true;
										if(t.damage > m.tempHealth){
											kills++;
										}
									}else{
										t.firedArr[step] = true;
										int damage = t.damage;
										if(t.damage > m.tempHealth){
											damage = m.tempHealth;
											kills++;
										}
										reducedDamage += damage;
									}
									m.tempHealth -= t.damage;
								}
							}
							step++;
						}
						
						
						if(m.health>=0){
							if(m.tempHealth<=0){
								roundDamage = m.health;
							}else{
								roundDamage = m.health - m.tempHealth;
							}
						}
						
						//provisional calculation on reduced damage for future
						double prob = m.r.priority/entranceList.size()*1.0;
						int remainingCreep = totalCreep - spawnedCreep;
						double futureCreep = remainingCreep* prob;
						double totalPossibleReduction = 0;
						
						if(simulationTime>=1500){
							totalPossibleReduction = futureCreep*8*roundDamage;
						}else if(simulationTime<1500&&simulationTime>=1000){
							totalPossibleReduction = futureCreep*(1500-simulationTime)/(2000-simulationTime)*4*roundDamage
									+futureCreep*(500)/(2000-simulationTime)*8*roundDamage;
						}else if(simulationTime<1000&&simulationTime>=500){
							totalPossibleReduction = futureCreep*(1000-simulationTime)/(2000-simulationTime)*2*roundDamage
									+futureCreep*(500)/(2000-simulationTime)*4*roundDamage
									+futureCreep*(500)/(2000-simulationTime)*8*roundDamage;
						}else if(simulationTime<500){
							totalPossibleReduction = futureCreep*(500-simulationTime)/(2000-simulationTime)*1*roundDamage
									+futureCreep*(500)/(2000-simulationTime)*2*roundDamage
									+futureCreep*(500)/(2000-simulationTime)*4*roundDamage
									+futureCreep*(500)/(2000-simulationTime)*8*roundDamage;
						}
						
						futureBonus += totalPossibleReduction;
						
					}
					
					//balance formula can be further improved.
					balance = reducedDamage - towerC[i] + kills*cMoney;
					if(balance > max){
						max = balance;
						selectedPlace = p;
						selectedTower = t;
						selectedTower.index = i;
						selectedReducedD = reducedDamage;
						selectedFutureBonus = futureBonus;
					}else if(balance == max){
						//decide by which place is nearer to bases.
//						double temp1 = 0;
//						double temp2 = 0;
//						for(int k=0; k<baseList.size(); k++){
//							temp1 += Math.pow(selectedPlace.x - baseX[k], 2) + Math.pow(selectedPlace.y - baseY[k], 2);
//							temp2 += Math.pow(p.x - baseX[k], 2) + Math.pow(p.y - baseY[k], 2);
//						}
//						//if new place is nearer tobases
//						if(temp1>temp2){
//							selectedPlace = p;
//							selectedTower = t;
//							selectedTower.index = i;
//							selectedReducedD = reducedDamage;
//							selectedFutureBonus = futureBonus;
//						}
						//decide by whose overall attack is larger
						int temp1 = 0;
						int temp2 = 0;
						//judge by the pathList size and the damage
//						temp1 = selectedTower.pathList.size() * selectedTower.damage;
//						temp2 = t.pathList.size() * t.damage;
						//judge by overall pathList value and the damage, this should be better
						for(path p1: selectedTower.pathList){
							temp1 += creepExistProb[p1.x][p1.y];
						}
						temp1 = temp1*selectedTower.damage;
						for(path p1: t.pathList){
							temp2 += creepExistProb[p1.x][p1.y];
						}
						temp1 = temp2*t.damage;
						
						if(temp2 > temp1){
							max = balance;
							selectedPlace = p;
							selectedTower = t;
							selectedTower.index = i;
							selectedReducedD = reducedDamage;
							selectedFutureBonus = futureBonus;
						}
					}
				}
			}
		}
		
		
		
		//calculate the future damage bonus this tower can achieve if without other tower's influence
//		double appearProb = 0;
//		for(path p: selectedTower.pathList){
//			appearProb += creepExistProb[p.x][p.y];
//			if(appearProb>=1){
//				appearProb = 1;
//			}
//		}
//		int remainingCreep = totalCreep - spawnedCreep;
//		double futureCreep = remainingCreep* appearProb;
//		double futureBonus = 0;
//		if(simulationTime>=1500){
//			futureBonus = futureCreep*8*cHealth;
//		}else if(simulationTime<1500&&simulationTime>=1000){
//			futureBonus = futureCreep*(1500-simulationTime)/(2000-simulationTime)*4*cHealth
//					+futureCreep*(500)/(2000-simulationTime)*8*cHealth;
//		}else if(simulationTime<1000&&simulationTime>=500){
//			futureBonus = futureCreep*(1000-simulationTime)/(2000-simulationTime)*2*cHealth
//					+futureCreep*(500)/(2000-simulationTime)*4*cHealth
//					+futureCreep*(500)/(2000-simulationTime)*8*cHealth;
//		}else if(simulationTime<500){
//			futureBonus = futureCreep*(500-simulationTime)/(2000-simulationTime)*1*cHealth
//					+futureCreep*(500)/(2000-simulationTime)*2*cHealth
//					+futureCreep*(500)/(2000-simulationTime)*4*cHealth
//					+futureCreep*(500)/(2000-simulationTime)*8*cHealth;
//		}
		
		
//		writer.println("simulation time: " + simulationTime + " budget tower: " + selectedPlace.x + " " + selectedPlace.y + " max: " + max + " tower: " + selectedTower.index + " reducedD: " + selectedReducedD 
//				+ " futureBonus: "+ futureBonus + " appearProb: " + appearProb + " remainCreep: " + remainingCreep);
//		writer.flush();
		
		
		
		//whether we should delay the tower, if delay, may cost a lot of time
//		boolean delayed = true;
//		int stepThreshold = 3;
//		double probThreshold = 0.25;
//		for(monster m: creepList){
//			//if the tower can kill a creep now.
//			if(m.health>0){
//				if(Math.pow(selectedPlace.x - m.x, 2) + Math.pow(selectedPlace.y - m.y, 2) <= Math.pow(selectedTower.range, 2)){
//					delayed = false;
//					writer.println("not delayed due to range");
//					writer.flush();
//					break;
//				}
//				//if a creep is going to get to the base
////				if(m.remainPath<=stepThreshold){
////					delayed = false;
////					writer.println("not delayed due to remain step");
////					writer.flush();
////					break;
////				}
//				//if the creep's path probability is high
////				if(m.pathProb > probThreshold){
////					delayed = false;
////					break;
////				}
//			}
//		}
//		if(delayed){
//			writer.println("the tower is delayed");
//			writer.flush();
//			return null;
//		}
		
		//if max >0, means the balance is positive, of course we need a tower
		if(max>0){
			selectedTower.p = selectedPlace;
			selectedTower.p.selected = true;
//			allPlaces.remove(selectedPlace);
			topPlaces.remove(selectedPlace);
		}
		//if max<0, the balance is negative.
		//compare the result between putting a tower of not
		//if put a tower, we lost abs(max) score cause we spend more money but reduce less hurt and gain less rewards, 
		//if not, we lost abs(damage) score cause we got hurt.
		//one benefit is that if we put a tower, it my have future uses.
		
		else{
//			double factor = simulationTime*1.0/2000;  //choosing a good factor is important
			double factor = 1; // through control of provision on each potential tower is more tower-specific and accurate than time
			double adjuster = 0.6;
			if(Math.abs(max)*factor < selectedReducedD + selectedFutureBonus*adjuster){
				selectedTower.p = selectedPlace;
				selectedTower.p.selected = true;
//				allPlaces.remove(selectedPlace);
				topPlaces.remove(selectedPlace);
			}else{
				return null;
			}
		}
		
		//update creepExistProb
//		if(selectedTower!= null){
//			for(path p: selectedTower.pathList){
//				for(road r: roadList){
//					if(r.pathList.contains(p)){
//						int index = r.pathList.indexOf(p);
//						creepExistProb[r.pathList.get(index).x][r.pathList.get(index).y] -= r.priority;
//						if(creepExistProb[r.pathList.get(index).x][r.pathList.get(index).y] <=0){
//							creepExistProb[r.pathList.get(index).x][r.pathList.get(index).y] = 0;
//						}
////						for(int i=0; i<index; i++){
////							creepExistProb[r.pathList.get(i).x][r.pathList.get(i).y] -= r.priority;
////							if(creepExistProb[r.pathList.get(i).x][r.pathList.get(i).y] <=0){
////								creepExistProb[r.pathList.get(i).x][r.pathList.get(i).y] = 0;
////							}
////						}
//					}
//				}
//			}
//		}
		
		
		return selectedTower;
	}
	
	
	//preparation for fining paths
	
	
	//find all paths
	public static void findAllPaths(){
		for(int i=0; i<planeSize; i++){
			if(plane[i][0]=='.'){
				path p = new path(i, 0);
				road r = new road();
				r.pathList.add(p);
				createRoad(r);
			}
			if(plane[i][planeSize-1]=='.'){
				path p = new path(i, planeSize-1);
				road r = new road();
				r.pathList.add(p);
				createRoad(r);
			}
		}
		for(int i=1; i<planeSize-1; i++){
			if(plane[0][i]=='.'){
				path p = new path(0, i);
				road r = new road();
				r.pathList.add(p);
				createRoad(r);
			}
			if(plane[planeSize-1][i]=='.'){
				path p = new path(planeSize-1, i);
				road r = new road();
				r.pathList.add(p);
				createRoad(r);
			}
		}
		
		crossRoadsAnalysis();
		Collections.sort(roadList);
		
		
	}
	
	
	public static void creepAppearInPath(){
		//calculate the probability that a creep may pass a path without any tower influence
		for(path p: pathList){
			double totalProb = 0;
			for(road r: roadList){
				if(r.pathList.contains(p)){
					totalProb +=r.priority;
				}
				//double check, cause there is a bug in the probability
//				if(totalProb>=entranceList.size()){
//					totalProb = entranceList.size();
//				}
			}
			creepExistProb[p.x][p.y] = totalProb/entranceList.size();
		}
		
		//matrix transposition
		for(int i=0; i<creepExistProb.length; i++){
			for(int j=0; j<i; j++){
				double temp = creepExistProb[i][j];
				creepExistProb[i][j] = creepExistProb[j][i];
				creepExistProb[j][i] = temp;
			}
		}
	}
	
	public static void crossRoadsAnalysis(){
		//according to road across, find more roads
		for(path p: pathList){
			int cross = 0;
			//first detect a strict cross to save calculation
			if(p.x-1>=0&&plane[p.x-1][p.y]=='.'){
				cross++;
			}
			if(p.y-1>=0&&plane[p.x][p.y-1]=='.'){
				cross++;
			}
			if(p.x+1<planeSize&&plane[p.x+1][p.y]=='.'){
				cross++;
			}
			if(p.y+1<planeSize&&plane[p.x][p.y+1]=='.'){
				cross++;
			}
			
			if(p.x-1>=0&&p.y-1>=0&&plane[p.x-1][p.y-1]=='.'){
				cross--;
			}
			if(p.x-1>=0&&p.y+1<planeSize&&plane[p.x-1][p.y+1]=='.'){
				cross--;
			}
			if(p.x+1<planeSize&&p.y-1>=0&&plane[p.x+1][p.y-1]=='.'){
				cross--;
			}
			if(p.x+1<planeSize&&p.y+1<planeSize&&plane[p.x+1][p.y+1]=='.'){
				cross--;
			}
			
			
			
			if(cross>2){
				boolean newCross = true;
				while(newCross){
					newCross = false;
					for(int i=0; i<roadList.size(); i++){
						road r = roadList.get(i);
						for(int j=i+1; j<roadList.size(); j++){
							road r1 = roadList.get(j);
							if(r!=r1&&!r.pathList.get(0).equals(r1.pathList.get(0))
									&&!r.pathList.get(r.pathList.size()-1).equals(r1.pathList.get(r1.pathList.size()-1))
									&&r.pathList.contains(p)
									&&r1.pathList.contains(p)){
								//when there is a cross of two roads, we can generate more.
//								newCross = true;
								addCrossRoads(p, r, r1);
								addCrossRoads(p, r1, r);
							}
						}
					}
				}
			}
		}
		roadList.addAll(crossRoadList);
//		crossRoadList.clear();
	}
	
	public static void addCrossRoads(path p, road r, road r1){
		path source = r.pathList.get(0);
		path end = r1.pathList.get(r1.pathList.size()-1);
		base b = new base(end.x, end.y);
		int baseIndex = baseList.indexOf(b);
		if(source.toBaseLimit[baseIndex]<=0){
			return;
		}
		
		road n = new road();
		for(int i=0; i<r.pathList.indexOf(p); i++){
			n.pathList.add(r.pathList.get(i));
		}
		for(int i=r1.pathList.indexOf(p); i<r1.pathList.size(); i++){
			if(n.pathList.contains(r1.pathList.get(i))){
				return;
			}
			n.pathList.add(r1.pathList.get(i));
		}
		boolean exist = false;
		for(road temp: crossRoadList){
			if(temp.pathList.size()==n.pathList.size()){
				int counter = 0; 
				for(int i=0; i<n.pathList.size(); i++){
					if(temp.pathList.get(i).equals(n.pathList.get(i))){
						counter++;
					}else{
						break;
					}
				}
				if(counter == n.pathList.size()){
					exist = true;
				}
			}
		}
		if(!exist){
			crossRoadList.add(n);
			source.toBaseLimit[baseIndex]--;
		}
	}
	
	
	public static void pathPriorityAnalysis(){
		for(path p: entranceList){
			ArrayList<road> targetList = new ArrayList<road>();
			for(road r: roadList){
				if(r.pathList.get(0).equals(p)){
					targetList.add(r);
				}
			}
			assignPriority(targetList, 0);
			
		}
		//sort the paths by the entry and the priority
		Collections.sort(roadList);
		
		
	}
	
	public static void assignPriority(ArrayList<road> targetList, int start){
		//if there is only one road, it reaches the base, 
		if(targetList.size()==1){
			return;
		}
		//or it has several roads, and any one of them does not has next path, it means they all reach base, assign priority and return
		if(targetList.get(0).pathList.size()<=start){
			for(road r: targetList){
				r.priority = r.priority / targetList.size();
			}
			return;
		}
		ArrayList<path> differences = new ArrayList<path>(); 
		for(road r: targetList){
			if(!differences.contains(r.pathList.get(start))){
				differences.add(r.pathList.get(start));
			}
		}
		if(differences.size()==0){
			assignPriority(targetList, start+1);
			return;
		}
		for(path p: differences){
			ArrayList<road> subList = new ArrayList<road>();
			for(road r: targetList){
				if(r.pathList.get(start).equals(p)){
					r.priority = r.priority / differences.size();
					subList.add(r);
				}
			}
			assignPriority(subList, start+1);
		}
	}
	
	//tell whether next position is more near to one of the bases.
	public static boolean towardsBases(road r, int currentX, int currentY, int nextX, int nextY){
		
		//update information to see whether this road can continue to some bases or not.
		for(int i=0; i<r.pathList.get(0).toBaseLimit.length; i++){
			if(r.pathList.get(0).toBaseLimit[i]<=0){
				r.toBases[i] = false;
			}
		}
		
		
		for(int i=0; i<numOfBases; i++){
			if(r.toBases[i]){
				if(Math.pow(nextX-baseX[i], 2)+Math.pow(nextY-baseY[i], 2)<Math.pow(currentX-baseX[i], 2)+Math.pow(currentY-baseY[i], 2)){
					return true;
				}
			}
		}
		return false;
	}
	
	//tell whether it moves away from a base, if so, it means it will not come back towards this base in future.
	public static void moveAwayFrom(road r, int currentX, int currentY, int nextX, int nextY){
		for(int i=0; i<numOfBases; i++){
			if(r.toBases[i]){
				if(Math.pow(nextX-baseX[i], 2)+Math.pow(nextY-baseY[i], 2)>Math.pow(currentX-baseX[i], 2)+Math.pow(currentY-baseY[i], 2)){
					r.counterOfMoveAway[i]--;
					if(r.counterOfMoveAway[i]<=0){
						r.toBases[i]=false;
					}
				}
			}
		}
	}
	
	public static void roadUpdate(road r, int index){
		
		r.pathList.get(0).toBaseLimit[index]--;
	}
	
	public static void createRoad(road r){
		path last = r.pathList.get(r.pathList.size()-1);
		boolean growing = true;
		int previousX = -1;
		int previousY = -1;
		if(r.pathList.size()>1){
			previousX = r.pathList.get(r.pathList.size()-2).x;
			previousY = r.pathList.get(r.pathList.size()-2).y;
		}
		int currentX = last.x;
		int currentY = last.y;
		while(growing){
			
			growing = false;
			//move left
			boolean multiple = false; //there is multiple choices or not

			final int[] DY = new int[]{1, 0, -1, 0};
		    final int[] DX = new int[]{0, -1, 0, 1};
		    
		    //first try to see whether it reaches base or boundary
	    	//if reaches base, add into rList
		    for(int i=0; i<4; i++){
		    	if(currentX+DX[i]<planeSize&&currentX+DX[i]>=0
						&&currentY+DY[i]<planeSize&&currentY+DY[i]>=0
						&&(currentY+DY[i]!=previousY||currentX+DX[i]!=previousX)
						&&plane[currentX+DX[i]][currentY+DY[i]]>='0'
						&&plane[currentX+DX[i]][currentY+DY[i]]<='9'
						&&r.pathList.get(0).toBaseLimit[plane[currentX+DX[i]][currentY+DY[i]]-'0']>0){
		    		road r1 = new road();
					for(path p: r.pathList){
//						path p2 = new path(p.x, p.y);
//						r1.pathList.add(p2);
						r1.pathList.add(p);
					}
					//copy tobase information
					for(int t=0; t<r.toBases.length;t++){
						r1.toBases[t] = r.toBases[t];
					}
		    		path p3 = new path(currentX+DX[i], currentY+DY[i]);
					r1.pathList.add(p3);
					roadList.add(r1);
					roadUpdate(r1, plane[currentX+DX[i]][currentY+DY[i]]-'0');
		    	}
		    }
		    
		    //first see how many choices we have
		    int choices = 0;
		    for(int i=0; i<4; i++){  	
				if(currentX+DX[i]<planeSize&&currentX+DX[i]>=0
						&&currentY+DY[i]<planeSize&&currentY+DY[i]>=0
						&&(currentY+DY[i]!=previousY||currentX+DX[i]!=previousX)
						&&plane[currentX+DX[i]][currentY+DY[i]]=='.'
						&&towardsBases(r, currentX, currentY, currentX+DX[i], currentY+DY[i])){
					choices++;
				}
		    }
		    if(choices>1){
		    	multiple=true;
		    }
		    
		    //try to move towards four directions	
		    for(int i=0; i<4; i++){  	
				if(currentX+DX[i]<planeSize&&currentX+DX[i]>=0
						&&currentY+DY[i]<planeSize&&currentY+DY[i]>=0
						&&(currentY+DY[i]!=previousY||currentX+DX[i]!=previousX)
						&&plane[currentX+DX[i]][currentY+DY[i]]=='.'
						&&towardsBases(r, currentX, currentY, currentX+DX[i], currentY+DY[i])){
					if(!multiple){
						path p1 = new path(currentX+DX[i], currentY+DY[i]);
						if(r.pathList.contains(p1)){
							continue;
						}
						r.pathList.add(p1);
						moveAwayFrom(r, currentX, currentY, currentX+DX[i], currentY+DY[i]);
						previousX = currentX;
						currentX = currentX+DX[i];
						previousY = currentY;
						currentY = currentY+DY[i];
						growing = true;
						
						break;
					}else{
						//copy all paths and create one more path.
						path p1 = new path(currentX+DX[i], currentY+DY[i]);
						if(r.pathList.contains(p1)){
							continue;
						}
						road r1 = new road();
						for(path p: r.pathList){
//							path p2 = new path(p.x, p.y);
//							r1.pathList.add(p2);
							r1.pathList.add(p);
						}
						//copy tobase information
						for(int t=0; t<r.toBases.length;t++){
							r1.toBases[t] = r.toBases[t];
						}
						
						path p3 = new path(currentX+DX[i], currentY+DY[i]);
						r1.pathList.add(p3);
						moveAwayFrom(r1, currentX, currentY, currentX+DX[i], currentY+DY[i]);
						createRoad(r1);
						choices--;
						if(choices==1){
							multiple=false;
						}
					}
				}
		    }
		}
		
		//stop growing means it has nowhere to go.
		return;
	}
	
	
	
	//evaluate and sort positions to find the best positions to put towers
	public static void placeEvaluation(){
		//find all avaliable places and evaluates available places to further rank them
		for(int i=0; i<planeSize; i++){
			for(int j=0; j<planeSize; j++){
				if(plane[i][j]=='#'){
					place p = new place(i, j);
					allPlaces.add(p);
					
					//check whether the place can be used to build a tower (with max range)
					//check the spots
//					boolean coverPath = false;
//					for(int k=0; k<maxRange; k++){
//						//decide start position and boundaries
//						int startX = p.x-(k+1);
//						int moveX = (k+1)*2+1;
//						int startY = p.y-(k+1);
//						int moveY = (k+1)*2+1;
//						if(startX<0){
//							startX = 0;
//							moveX = moveX - Math.abs(startX);
//						}
//						if(startY<0){
//							startY = 0;
//							moveY = moveY - Math.abs(startY);
//						}
//						if(startX+ moveX>planeSize-1){
//							moveX = planeSize -1 - startX;
//						}
//						if(startY+ moveY>planeSize-1){
//							moveY = planeSize -1 - startY;
//						}
//						for(int ii=0; ii<moveX; ii++){
//							for(int jj=0; jj<moveY; jj++){
//								if((plane[startX+ii][startY+jj]=='.')&& Math.pow(startX+ii-p.x, 2) + Math.pow(startY+jj-p.y, 2)< Math.pow(k+1, 2)){
//									coverPath = true;
//								}
//							}
//						}
//					}
//					
//					if(coverPath){
//						allPlaces.add(p);
//					}
					
				}
			}
		}
		
	}
	
	
	//pre-select current top place list
	public static void placeAnalysis(){
		
		for(place p: allPlaces){
			//check the distance from all bases
			int temp=0;
			for(base b: baseList){
				p.distance[temp] = (int) (Math.pow(p.x-b.x, 2)+Math.pow(p.y-b.y, 2));
				p.avgDistance +=p.distance[temp];
				temp++;
			}
			p.avgDistance = p.avgDistance/baseList.size();
			
			//check the spots
			for(int k=0; k<p.spots.length; k++){
				//decide start position and boundaries
				int startX = p.x-(k+1);
				int moveX = (k+1)*2+1;
				int startY = p.y-(k+1);
				int moveY = (k+1)*2+1;
				if(startX<0){
					startX = 0;
					moveX = moveX - Math.abs(startX);
				}
				if(startY<0){
					startY = 0;
					moveY = moveY - Math.abs(startY);
				}
				if(startX+ moveX>planeSize-1){
					moveX = planeSize -1 - startX;
				}
				if(startY+ moveY>planeSize-1){
					moveY = planeSize -1 - startY;
				}
				for(int ii=0; ii<moveX; ii++){
					for(int jj=0; jj<moveY; jj++){
						boolean covered = false;
						if(Math.pow(startX+ii-p.x, 2) + Math.pow(startY+jj-p.y, 2)< Math.pow(k+1, 2)){
							covered = true;
						}
						if(plane[startX+ii][startY+jj]=='.'&&covered){
							p.spots[k]++;
						}
					}
				}
				p.totalSpot+=p.spots[k];
			}
			
			//check the best damage per money spent on each cell
			for(int k=0; k< numOfTowers; k++){
				if(towerR[k]!=0&&towerD[k]!=0){
					int tempDpm = towerD[k]*p.spots[towerR[k]-1];
					tempDpm = tempDpm/towerC[k];
					if(tempDpm>p.OverallHarm){
						p.OverallHarm = tempDpm;
						p.EconomicTower = k;
					}
				}
			}
			
			if(p.OverallHarm >0){
				topPlaces.add(p);
			}
			
		}
		//sort the topPlaces
		Collections.sort(topPlaces);
		
	}
	
	
	public static void main(String[] args) throws Exception{
		
		

		//local test without Vis
		
//		Scanner sc = new Scanner(System.in);
//		int N = 31;
//	    int money = 71;
//		int creepHealth = 20;
//	    int creepMoney = 20;
//	    
//		BufferedReader br = null;
//		try {
//			 
//			String sCurrentLine;
// 
//			br = new BufferedReader(new FileReader("input1.txt"));
//			
//		    String[] board = new String[N];
//		    for (int i=0; i < N; i++){
//		        board[i] = br.readLine();
//	//	        writer.println(board[i]);
//		    }
//		    int NT = Integer.valueOf(br.readLine());
//		    int[] towerType = new int[NT];
//		    for (int i=0; i < NT; i++){
//		        towerType[i] = Integer.valueOf(br.readLine());
//	//	        writer.println(towerType[i]);
//		    }
//	//	    writer.flush();
//		    init(board, money, creepHealth, creepMoney, towerType);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
//----------------------------------------------------------------------
	    
	    
		Scanner sc = new Scanner(System.in);
		int N = Integer.valueOf(sc.nextLine());
	    int money = Integer.valueOf(sc.nextLine());
	    String[] board = new String[N];
	    for (int i=0; i < N; i++){
	        board[i] = sc.nextLine();
	    }
	    
	    int creepHealth = Integer.valueOf(sc.nextLine());
	    int creepMoney = Integer.valueOf(sc.nextLine());
	    int NT = Integer.valueOf(sc.nextLine());
	    int[] towerType = new int[NT];
	    for (int i=0; i < NT; i++){
	        towerType[i] = Integer.valueOf(sc.nextLine());
	    }
	    init(board, money, creepHealth, creepMoney, towerType);
	    
	    for (int t=0; t < 2000; t++)
	    {
	    	money = Integer.valueOf(sc.nextLine());
	        int NC = Integer.valueOf(sc.nextLine());
	        int[] creep = new int[NC];
	        for (int i=0; i < NC; i++){
	            creep[i] = Integer.valueOf(sc.nextLine());
	        }
	        int B = Integer.valueOf(sc.nextLine());
	        int[] baseHealth = new int[B];
	        for (int i=0; i < B; i++){
	            baseHealth[i] = Integer.valueOf(sc.nextLine());
	        }
	        int[] ret = placeTowers(creep, money, baseHealth);
	        
	        
	        System.out.println(ret.length);
	        for (int i=0; i < ret.length; i++){
	        	System.out.println(ret[i]);
	        }
	        System.out.flush();
	    }
	    sc.close();
	    
		
///--------------------------------------------------------------		
		//test case
//		for (int t=0; t < 2000; t++){
//			int[] ret;
//			if(t==0){
//				ret = new int[3];
//				ret[0]=12;
//				ret[1]=17;
//				ret[2]=1;
//			}
//			else if(t==1){
//		    	ret = new int[3];
//		    	ret[0]=12;
//				ret[1]=16;
//				ret[2]=2;
//		    }else{
//		    	ret = new int[0];
//		    }
//	        System.out.println(ret.length);
//	        for (int i=0; i < ret.length; i++){
//	        	System.out.println(ret[i]);
//	        }
//	        System.out.flush();
//		}

//-------------------------------------------------------
		//original code
//		Scanner sc = new Scanner(System.in);
//		int N = Integer.valueOf(sc.nextLine());
//	    int money = Integer.valueOf(sc.nextLine());
//	    String[] board = new String[N];
//	    for (int i=0; i < N; i++){
//	        board[i] = sc.nextLine();
//	    }
//	    
//	    int creepHealth = Integer.valueOf(sc.nextLine());
//	    int creepMoney = Integer.valueOf(sc.nextLine());
//	    int NT = Integer.valueOf(sc.nextLine());
//	    int[] towerType = new int[NT];
//	    for (int i=0; i < NT; i++){
//	        towerType[i] = Integer.valueOf(sc.nextLine());
//	    }
//	    init(board, money, creepHealth, creepMoney, towerType);
//	    
//	    int numOfSimulation  = 300;
//	    for (int t=0; t < numOfSimulation; t++)
//	    {
//	    	money = Integer.valueOf(sc.nextLine());
//	        int NC = Integer.valueOf(sc.nextLine());
//	        int[] creep = new int[NC];
//	        for (int i=0; i < NC; i++){
//	            creep[i] = Integer.valueOf(sc.nextLine());
//	        }
//	        int B = Integer.valueOf(sc.nextLine());
//	        int[] baseHealth = new int[B];
//	        for (int i=0; i < B; i++){
//	            baseHealth[i] = Integer.valueOf(sc.nextLine());
//	        }
//	        int[] ret = placeTowers(creep, money, baseHealth);
//	        
//	        System.out.println(ret.length);
//	        for (int i=0; i < ret.length; i++){
//	        	System.out.println(ret[i]);
//	        }
//	        System.out.flush();
//	    }
//	    sc.close();
		
	    
	}
}
