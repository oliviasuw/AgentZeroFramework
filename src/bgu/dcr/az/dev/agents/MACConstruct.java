/**
 * ACConstruct.java
   Created by Su Wen
   Date: Dec 21, 2014
   Time: 9:09:22 PM 
 */
package bgu.dcr.az.dev.agents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.ImmutableProblem;
import bgu.dcr.az.api.tools.DFSPsaudoTree;

public class MACConstruct {
	
	public final static int MAX_PROJECTION_NUM_RECORDED = 1000;
	
    public int myDepth;
    
    public int agentID;
    
    // The indexes of domain, unary_costs and pruned corresponds to each other
    // Here, the ith domain value is i.
    public Integer[] domain;   //e.g. i's domain value is domain[i]
    
    public Double[] unaryCosts;  // e.g. unary cost when take domain[i] is unaryCosts[i]
      
    public boolean[] pruned; // e.g. pruned[i]=true, value i is pruned from this agent
    
    public double myContribution;
    
    public List<Integer> neighbors;
    public Vector<Integer[]> neighborsDomains;  // The index of the domains of neighbors is the same with this agent itself
    public Vector<boolean[]> neighborsPruned;
    
    /* Stack records the projections that may needs to be undone*/
    Integer[] ACRecordsProjectFromMe;  // AC_records   
    Integer[] ACRecordsProjectToMe; // ACCsrc->self  In the implementation, is AC_accept_count
    Double[][][] P_records; // Stack P
    
    /* MAC */
    public int depth;  // the depth of this copy

    class Nogood{
    	int valueA;
    	int valueB;
    	double cost;
//    	Nogood duplicate(){
//    		Nogood newNogood = new Nogood();
//    		newNogood.valueA = this.valueA;
//    		newNogood.valueB = this.valueB;
//    		newNogood.cost = this.cost;
//    		return newNogood;
//    	}
    };
    class BinaryConstraint{
    	int varA;
    	int varB;
    	Vector<Nogood> nogoods;
    	BinaryConstraint duplicate(){
    		BinaryConstraint binaryConstraint = new BinaryConstraint();
    		binaryConstraint.varA = this.varA;
    		binaryConstraint.varB = this.varB;
    		binaryConstraint.nogoods = new Vector();
    		for(Nogood aNogood : nogoods){
    			Nogood newNogood = new Nogood();
    			newNogood.valueA = aNogood.valueA;
    			newNogood.valueB = aNogood.valueB;
    			newNogood.cost = aNogood.cost;
    			binaryConstraint.nogoods.add(newNogood);
    		}
    		return binaryConstraint;
    	}
    };
    
    public Vector<BinaryConstraint> constraints;
    
    // global top and cPhi
    public double global_top;
    public double global_cPhi;
    
    public MACConstruct(MAC_BnBAdoptAgent agent, int copyIndex){

        myDepth = agent.tree.getDepth();
        agentID = agent.getId();
        
    	File file = new File("costs.txt");
      FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file, true);
			fileWriter.write("agentID: "+ agentID+" depth: "+myDepth + "\tcopyIndex: "+copyIndex+"\n");
			fileWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        depth = copyIndex;
		
        int domainSize = agent.getDomain().size();
        domain = new Integer[domainSize];
        for(int i = 0; i < domainSize; i++){
        	domain[i] = i;  // The value is the index of the value in the domain
        }
        
        myContribution = 0;
        neighbors = agent.tree.getNeighbors();
        neighborsDomains = new Vector();
        neighborsPruned = new Vector();
        
        constraints = new Vector();
        global_top = Double.MAX_VALUE;

        ACRecordsProjectFromMe = new Integer[neighbors.size()];
        ACRecordsProjectToMe = new Integer[neighbors.size()];
        P_records = new Double[neighbors.size()][][];
        for(int i = 0; i < neighbors.size(); i++){
        	ACRecordsProjectFromMe[i] = 0;
        	ACRecordsProjectToMe[i] = 0;
        	P_records[i] = new Double[MAX_PROJECTION_NUM_RECORDED][];
        	P_records[i][ACRecordsProjectFromMe[i]] = new Double[domain.length];
        	
        	int currentNeighbor = neighbors.get(i);
        	int neighborDomainSize = agent.getDomainOf(currentNeighbor).size();

        	Integer[] neighborDomain = new Integer[neighborDomainSize];
            for(int j = 0; j < neighborDomainSize; j++){
            	neighborDomain[j] = j;  // The value is the index of the value in the domain
            }
        	neighborsDomains.add(neighborDomain);
        	
        	boolean[] neighborPruned = new boolean[neighborDomain.length];
        	for(int j = 0; j < neighborDomain.length; j++){
        		neighborPruned[j] = false;
        	}
        	neighborsPruned.add(neighborPruned);
        }
        
        unaryCosts = new Double[domain.length];
        pruned = new boolean[domain.length];
        for(int i = 0; i < domain.length; i++) {
        	// The unary cost when agentID choose 0 as its value
        	int value = domain[i];
        	unaryCosts[i] = (double)agent.getConstraintCost(agentID, value);
        	pruned[i] = false;
        }
        
        ImmutableProblem prob = agent.getProblem();
        for(int neighbor : neighbors) {
        	BinaryConstraint binaryCon = new BinaryConstraint();
        	binaryCon.varA = agentID;
        	binaryCon.varB = neighbor;
        	binaryCon.nogoods = new Vector();
        	for(int val1 : domain){
        		for(int val2 : domain){
        			int cost = agent.getConstraintCost(agentID, val1, neighbor, val2);
        			Nogood nogood = new Nogood();
        			nogood.valueA = val1;
        			nogood.valueB = val2;
        			nogood.cost = cost;
        			binaryCon.nogoods.add(nogood);
        		}
        	}
        	constraints.add(binaryCon);
        }
    }
    
    public MACConstruct() {
        
    }   

	public BinaryConstraint getConstraint(int neighbor){
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varB == neighbor){
    			return binaryCon;
    		}
    	}
    	return null;
    }
    
    public double getConstraint(int self, int myVal, int neighbor, int hisVal){
    	
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varA == self && binaryCon.varB == neighbor){
    			
    			for(Nogood nogood : binaryCon.nogoods){    				
    				if(nogood.valueA == myVal && nogood.valueB == hisVal){
    					return nogood.cost;
    				}
    			}
    		}
    	}
    	return 0;
    }
    
//    public int getAncestorDepth(int ancestor){
//    	for(Map.Entry<Integer, Integer> entry: ancestor_depth_map.entrySet()){
//    		if(entry.getKey() == ancestor){
//    			return entry.getValue();
//    		}
//    	}
//    	return -1;
//    }
    
    /**
     * 
     * @param self
     * @param myVal
     * @param neighbor
     * @return the minimum alpha cost could be projected to me 
     */
    public double checkAlphaProjectToMe(int self, int myVal, int neighbor){
    	// This value has been pruned
    	if(pruned[myVal]) {
    		return 0;
    	}
    	
    	int neighborIndex = getNeighborIndex(neighbor);
    	double alpha = Double.MAX_VALUE;
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varA == self && binaryCon.varB == neighbor){
    			for(Nogood nogood : binaryCon.nogoods){
    				if(nogood.valueA == myVal){
    					int hisVal = nogood.valueB;
    					if(this.neighborsPruned.get(neighborIndex)[hisVal])
    						continue;
    					if(nogood.cost < alpha) {
    						alpha = nogood.cost;
    					}
    				}
    			}
    			break;
    		}
    	}
    	return alpha;
    }
    
    /**
     * 
     * @param self
     * @param neighbor
     * @param hisVal
     * @return the minimum alpha cost could be projected to neighbor
     */
    public double checkAlphaProjectToNeighbor(int self, int neighbor, int hisVal){
    	int neighborIndex = getNeighborIndex(neighbor);
    	// This value has been pruned
    	if(this.neighborsPruned.get(neighborIndex)[hisVal])
			return 0;
    	
    	double alpha = Double.MAX_VALUE;
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varA == self && binaryCon.varB == neighbor){
    			for(Nogood nogood : binaryCon.nogoods){
    				if(nogood.valueB == hisVal){
    					int myVal = nogood.valueA;
    					if(pruned[myVal])
    						continue;
    					if(nogood.cost < alpha) {
    						alpha = nogood.cost;
    					}
    				}
    			}
    			break;
    		}
    	}
    	return alpha;
    }
    
    /**
     * 
     * @param self
     * @param myVal
     * @param neighbor
     * @return 0 success
     */
    public int updateCostsWhenProjectToMe(int self, int myVal, int neighbor, double alpha){
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varA == self && binaryCon.varB == neighbor){
    			for(Nogood nogood : binaryCon.nogoods){
    				if(nogood.valueA == myVal){
    					nogood.cost -= alpha;
    				}
    			}
    		}
    	}
    	
    	unaryCosts[myVal] += alpha;
    	return 0;
    }
    
    /**
     * 
     * @param self
     * @param neighbor
     * @param hisVal
     * @param alpha
     * @return 0 success
     */
    public int updateCostsWhenProjectToNeighbor(int self, int neighbor, int hisVal, double alpha){
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varA == self && binaryCon.varB == neighbor){
    			for(Nogood nogood : binaryCon.nogoods){
    				if(nogood.valueB == hisVal){
    					nogood.cost -= alpha;
    				}
    			}
    		}
    	}
  
    	return 0;
    }
    
    /**
     * Reverse operation of updateCostsWhenProjectToNeighbor()
     * @param self
     * @param neighbor
     * @param hisVal
     * @param alpha
     * @return 0 success
     */
    public int updateCostsWhenRollBack(int self, int neighbor, int hisVal, double alpha){
    	int neighborIndex = this.getNeighborIndex(neighbor);
    	for(BinaryConstraint binaryCon : constraints){
    		if(binaryCon.varA == self && binaryCon.varB == neighbor){
    			for(Nogood nogood : binaryCon.nogoods){
    				if(nogood.valueB == hisVal){
    					nogood.cost += alpha;
    				}
    			}
    		}
    	}
    	return 0;
    }
    /**
     * 
     * @param neighbor
     * @return The index of 'neighbor' in my neighbor vector; if not exsit, return -1
     */
    public int getNeighborIndex(int neighbor){
    	for(int i = 0; i < neighbors.size(); i++){
    		if(neighbor == neighbors.get(i)){
    			return i;
    		}
    	}
    	
    	System.out.println("I have no such neighbor!!");
    	return -1;
    }
    
    public MACConstruct duplicate() {
    	if(agentID == 9){
    		System.out.println("debug");
    	}
    	
    	MACConstruct ac = new MACConstruct();
        ac.myDepth = myDepth;
        ac.agentID = agentID;
        
        ac.depth = depth;
	
        ac.domain = new Integer[domain.length];
        for(int i = 0; i < domain.length; i++){
        	ac.domain[i] = domain[i];  // The value is the index of the value in the domain
        }
        
        ac.myContribution = myContribution;
        ac.neighbors = neighbors;
        ac.neighborsDomains = neighborsDomains;

        ac.constraints = new Vector();
        for (BinaryConstraint cons : constraints) {
        	ac.constraints.add(cons.duplicate()); 
        }
        
        ac.global_top = global_top;
        ac.global_cPhi = global_cPhi;

        ac.ACRecordsProjectFromMe = new Integer[neighbors.size()];
        ac.ACRecordsProjectToMe = new Integer[neighbors.size()];
        ac.P_records = new Double[neighbors.size()][][];
        ac.neighborsPruned = new Vector<boolean[]>();
        for(int i = 0; i < neighbors.size(); i++){
        	int neighbor = neighbors.get(i);
        	int neighborIndex = this.getNeighborIndex(neighbor);
        	ac.ACRecordsProjectFromMe[i] = 0;
        	ac.ACRecordsProjectToMe[i] = 0;
        	ac.P_records[i] = new Double[MAX_PROJECTION_NUM_RECORDED][];
        	ac.P_records[i][ACRecordsProjectFromMe[i]] = new Double[domain.length];

        	boolean[] newNeighborPruned = new boolean[domain.length];
    
    		boolean[] ori_pruned = neighborsPruned.get(neighborIndex);
    		
        	for(int j = 0; j < ori_pruned.length; j++){
        		newNeighborPruned[j] = ori_pruned[j];
        	}
        	ac.neighborsPruned.add(newNeighborPruned);
        }
        
        ac.unaryCosts = new Double[domain.length];
        ac.pruned = new boolean[domain.length];
        for(int i = 0; i < domain.length; i++) {
        	// The unary cost when agentID choose 0 as its value
        	int value = domain[i];
        	ac.unaryCosts[i] = unaryCosts[value];
        	ac.pruned[i] = pruned[i];
        }
        
        return ac;
    }
    
    public int domainSizeAfterPruning() {
        int count = 0;
        for (int i = 0; i < domain.length; i++) {
            if (pruned[i] == false)
                count++;
        }
        return count;
    }
    
    public int neighborDomainSizeAfterPruning(int neighborIndex) {
    	boolean [] neighborPruned = this.neighborsPruned.get(neighborIndex);
        int count = 0;
        for (int i = 0; i < domain.length; i++) {
            if (neighborPruned[i] == false)
                count++;
        }
        return count;
    }
    
    
    
}
