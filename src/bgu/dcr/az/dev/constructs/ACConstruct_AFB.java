/**
 * ACConstruct_AFB.java
   Created by Su Wen
   Date: Mar 29, 2015
   Time: 4:19:06 PM 
 */
package bgu.dcr.az.dev.constructs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import bgu.dcr.az.api.Agent;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.ImmutableProblem;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.tools.DFSPsaudoTree;
import bgu.dcr.az.dev.agents.AC_BnBAdoptPlusAgent;

public class ACConstruct_AFB {
	
	public final static int MAX_PROJECTION_NUM_RECORDED = 1000;
	
    public int depth;
    
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
    public Integer[] ACRecordsProjectFromMe;  // AC_records   
    public Integer[] ACRecordsProjectToMe; // ACCsrc->self  In the implementation, is AC_accept_count
    public Double[][][] P_records; // Stack P


    class Nogood{
    	int valueA;
    	int valueB;
    	double cost;
    };
    class BinaryConstraint{
    	int varA;
    	int varB;
    	Vector<Nogood> nogoods;
    	BinaryConstraint duplicate(){
    		BinaryConstraint binaryConstraint = new BinaryConstraint();
    		binaryConstraint.varA = this.varA;
    		binaryConstraint.varB = this.varB;
    		Vector<Nogood> newNogoods = (Vector<Nogood>) nogoods.clone();
    		return binaryConstraint;
    	}
    };
    
    public Vector<BinaryConstraint> constraints;
    
    // global top and cPhi
//    public double global_top;
//    public double global_cPhi;
    
    public ACConstruct_AFB(Agent agent){
    	ImmutableProblem prob = agent.getProblem();
    	
        agentID = agent.getId();
        depth = agentID;  // Here, we only consider alphabetically ordered tree
        
//    	File file = new File("costs.txt");
//        FileWriter fileWriter = null;
//		try {
//			fileWriter = new FileWriter(file, true);
//			fileWriter.write("agentID: "+ agentID+" depth: "+depth + "\n");
//			fileWriter.close();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
        int domainSize = agent.getAgentDomainSize();
        domain = new Integer[domainSize];
        for(int i = 0; i < domainSize; i++){
        	domain[i] = i;  // The value is the index of the value in the domain
        }
        
        myContribution = 0;
        neighbors = new ArrayList<Integer>(prob.getAgentNeighbors(agentID));
        neighborsDomains = new Vector();
        neighborsPruned = new Vector();
        
        constraints = new Vector();
//        global_top = Double.MAX_VALUE;

        ACRecordsProjectFromMe = new Integer[neighbors.size()];
        ACRecordsProjectToMe = new Integer[neighbors.size()];
        P_records = new Double[neighbors.size()][][];
        for(int i = 0; i < neighbors.size(); i++){
        	ACRecordsProjectFromMe[i] = 0;
        	ACRecordsProjectToMe[i] = 0;

        	
        	int currentNeighbor = neighbors.get(i);
        	int neighborDomainSize = agent.getAgentDomainSize(currentNeighbor);

        	
        	P_records[i] = new Double[MAX_PROJECTION_NUM_RECORDED][];
        	P_records[i][ACRecordsProjectFromMe[i]] = new Double[neighborDomainSize];

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
        	unaryCosts[i] = (double)agent.getAgentConstraintCost(agentID, value);
        	pruned[i] = false;
        }
        
        for(int neighbor : neighbors) {
        	BinaryConstraint binaryCon = new BinaryConstraint();
        	binaryCon.varA = agentID;
        	binaryCon.varB = neighbor;
        	binaryCon.nogoods = new Vector();
        	for(int val1= 0; val1 < agent.getAgentDomainSize(agentID); val1++){
        		for(int val2 = 0; val2 < agent.getAgentDomainSize(neighbor); val2++){
        			int cost = agent.getAgentConstraintCost(agentID, val1, neighbor, val2);
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
    public ACConstruct_AFB() {
        
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
    
    
    public int domainSizeAfterPruning() {
        int count = 0;
        for (int i = 0; i < domain.length; i++) {
            if (pruned[i] == false)
                count++;
        }
        return count;
    }
     
    
}

