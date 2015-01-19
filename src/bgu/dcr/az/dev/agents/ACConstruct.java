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
import java.util.List;
import java.util.Vector;

import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.ImmutableProblem;
import bgu.dcr.az.api.tools.DFSPsaudoTree;

public class ACConstruct {
	
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
    Integer[] ACRecordsProjectFromMe;  // AC_records   
    Integer[] ACRecordsProjectToMe; // ACCsrc->self  In the implementation, is AC_accept_count
    Double[][][] P_records; // Stack P


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
    		Vector<Nogood> newNogoods = (Vector<Nogood>) nogoods.clone();
//    		Vector<Nogood> newNogoods = new Vector();
//    		for(Nogood nogood : this.nogoods){
//    			newNogoods.add(nogood.duplicate());
//    		}
//    		binaryConstraint.nogoods = newNogoods;
    		return binaryConstraint;
    	}
    };
    
    public Vector<BinaryConstraint> constraints;
    
    // global top and cPhi
    public double global_top;
    public double global_cPhi;
    
    public ACConstruct(AC_BnBAdoptAgent agent){

        depth = agent.tree.getDepth();
        agentID = agent.getId();
        
    	File file = new File("costs.txt");
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file, true);
			fileWriter.write("agentID: "+ agentID+" depth: "+depth + "\n");
			fileWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
        int domainSize = agent.getDomain().size();
        domain = new Integer[domainSize];
        for(int i = 0; i < domainSize; i++){
        	domain[i] = i;  // The value is the index of the value in the domain
        }
        
//        domain = (Integer[]) agent.getDomain().toArray();
        
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
    
    public ACConstruct() {
        
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
    
//    public ACConstruct duplicate() {
//        ACConstruct ac = new ACConstruct();
//        
//        ac.agentID = this.agentID;
//        ac.myContribution = this.myContribution;
//        ac.global_cPhi = this.global_cPhi;
//        ac.global_top = this.global_top;
//        
//        for(int i = 0; i <this.domain.length; i++){
//        	ac.domain[i] = this.domain[i];
//        }
//        
//        ac.unary_costs = new Vector();
//        for (int i = 0; i < this.unary_costs.size(); i++) {
//            double uc = (Double) this.unary_costs.elementAt(i);
//            ac.unary_costs.add(uc);
//        }        
//        
//        ac.neighbors = new Vector();
//        for (int neighbor : this.neighbors) {
//            ac.neighbors.add(neighbor);
//        }
//        
////        ac.constraints = (Vector<BinaryConstraint>) this.constraints.clone();
//        ac.constraints = new Vector();
//        for (BinaryConstraint cons : constraints) {
//            ac.constraints.add(cons.duplicate()); 
//        }
//        
//        return ac;
//    }
    
    public int domainSizeAfterPruning() {
        int count = 0;
        for (int i = 0; i < domain.length; i++) {
            if (pruned[i] == false)
                count++;
        }
        return count;
    }
    
//    public String toString() {
//        String str = "";
//        
//        str += "var ID: "+myvar.varID+"\ndomain: ";
//        for(int i = 0; i < myvar.domainSize; i++) 
//            str += myvar.domain[i] + "("+myvar.pruned[i]+")";
//        str += "\n";
//
//                
//        str += "unary costs: ";
//        for(int i = 0; i < myvar.domainSize; i++) 
//            str += (Double) unary_costs.elementAt(i) + " ";
//        
//        
//        str += "\nconstraint vars: ";
//        for(int i = 0; i < cons_vars.size(); i++) 
//            str += ((Variable) cons_vars.elementAt(i)).varID + " ";
//        str += "\n";
//        
//        for(int j = 0; j < cons_vars.size(); j++) {
//        
//            Variable cv = (Variable)cons_vars.elementAt(j);
//            MaxCOPConstraint mcc = getConstraint(myvar, cv);
//            if (mcc == null) continue;
//            if (cv.equalVar(myvar))  cv = mcc.var1;
//            
//            for(int i = 0; i < myvar.domainSize; i++) {
//                Value myvar_d = myvar.domain[i];
//
//                for (int k = 0; k < cv.domainSize; k++) {
//                    Value cv_d = cv.domain[k];
//                    
//                    str += "var"+myvar.varID+" var"+cv.varID+" "+myvar_d.toString()+" "+cv_d.toString()+" --> "+mcc.evaluate(myvar, myvar_d, cv, cv_d)+"\n";
//                }
//            }
//        }
//        
//        str += "My Contribution (CPhi): "+myContribution+"\n";
//
//        return str;
//    }
//    
//    public void toFile() {
//        BufferedWriter data;
//        try {
//            data = new BufferedWriter(new FileWriter("var"+myvar.varID+".txt", true));
//            
//            data.write("VARIABLE "+myvar.varID+" 1 "+myvar.domainSize()+" ");
//            for(int i = 0; i < myvar.domainSize; i++) 
//                data.write((Double) unary_costs.elementAt(i) + " ");
//            data.newLine();
//            
//            for(int j = 0; j < cons_vars.size(); j++) {
//        
//                Variable cv = (Variable)cons_vars.elementAt(j);
//                MaxCOPConstraint mcc = getConstraint(myvar, cv);
//                if (mcc == null) continue;
//                if (cv.equalVar(myvar))  cv = mcc.var1;
//                
//                data.write("CONSTRAINT "+myvar.varID+" "+cv.varID+"\n");
//
//                for(int i = 0; i < myvar.domainSize; i++) {
//                    Value myvar_d = myvar.domain[i];
//
//                    for (int k = 0; k < cv.domainSize; k++) {
//                        Value cv_d = cv.domain[k];
//
//                        data.write("F "+myvar_d.toString()+" "+cv_d.toString()+" "+mcc.evaluate(myvar, myvar_d, cv, cv_d)+"\n");
//                    }
//                }
//            }
//            
//            
//            data.close();
//        } catch (IOException ex) {
//
//        }
//        
//        
//    }
//    
    
}
