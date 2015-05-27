/**
 * AC_AFBAgent1.java
   Created by Su Wen
   Date: Mar 31, 2015
   Time: 9:43:07 PM 
 */
package bgu.dcr.az.dev.agents.AFB;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.Vector;

import confs.Counter;
import bgu.dcr.az.api.*;
import bgu.dcr.az.api.agt.*;
import bgu.dcr.az.api.ano.*;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.tools.*;
import bgu.dcr.az.dev.constructs.ACConstruct_AFB;

/**
 * 1. Use the prune power in assignCPA
 * 2. Accumulate global cPhi from agents with lower priorities in handleFBESTIMATE()
 * @author Su Wen
 *
 */
@Algorithm(name = "AC_AFB1", useIdleDetector=false)
public class AC_AFBAgent1 extends SimpleAgent {
	
	boolean debug = false;

	private int bound;  //UB
 	private int cPhi;   //LB
	private Assignment cpa, bestCpa;
	private int[] estimates;
	private int[] h;
	private TimeStamp timeStamp;
	
	
    /** Structures for AC Enforcement**/
    boolean ACOn = true;
    int ACEnforceOrder = -1;  // -1 : first bottom to top;  1 : first top to bottom
    final static int ProjectToMe = 1;
    final static int ProjectFromMe = 2;
    public ACConstruct_AFB myACConstruct;
    boolean ACEnforcementNeed = false;
    boolean ACPreprocessFlag = true;
    
	
    @Override
    public void start() {
    	timeStamp = new TimeStamp(this);
    	bound = Integer.MAX_VALUE;
    	cPhi = 0;
    	estimates = new int[this.getProblem().getNumberOfAgents()];
    	h = new int[this.getProblem().getAgentDomainSize(this.getId())];
    	fillH();
    	
    	if(ACOn) {
    		initACConstruct();
    		ACPreprocess();
    		ACPreprocessFlag = false;
    	}
    	
        if (isFirstAgent()) {
        	cpa = new Assignment();    //generate CPA
        	assignCPA();
        }
       
    }
    
    public void initACConstruct(){
    	myACConstruct = new ACConstruct_AFB(this);
    }
    
    /**
     * AC Enforcement
     */
    void ACPreprocess(){
    	Set<Integer> myNeighbors = getProblem().getAgentNeighbors(getId());
    	
		if(-1 == ACEnforceOrder) {
			// First project to ancestors
			for (int neighbor : myNeighbors) {
				if (neighbor < getId()) {
					binaryProjection(ProjectFromMe, neighbor, false, false);
					checkDomainForDeletions();
					binaryProjection(ProjectToMe, neighbor, false, false);
					checkDomainForDeletions();
				}
				else {
					binaryProjection(ProjectToMe, neighbor, true, false);
					checkDomainForDeletions();
					binaryProjection(ProjectFromMe, neighbor, true, false);
					checkDomainForDeletions();
				}
			}

		}
    }
    
    /**
     * 
     * @param projectDirection: ProjectToMe or ProjectFromMe
     * @param neighbor
     * @param isLowerNeighbor: true: neighbor is my child/pseudochild; false: neighbor 
     * is my parent/pseudoparent
     * @param needRecord: this projection needs to be recorded in the stack
     */
    void binaryProjection(int projectDirection, int neighbor, boolean isLowerNeighbor,
    		boolean needRecord){
    	/* To record this projection in the stack? True: record False: don't record */
    	boolean recoverFlag = ProjectFromMe == projectDirection && 
    			((!isLowerNeighbor && -1 == ACEnforceOrder) 
    					|| (isLowerNeighbor && 1 == ACEnforceOrder) );;

    	int neighborIndex = myACConstruct.getNeighborIndex(neighbor);
    	
        if (!ACPreprocessFlag && recoverFlag && needRecord) {
        	myACConstruct.P_records[neighborIndex][myACConstruct.ACRecordsProjectFromMe
        	         [neighborIndex]] = new Double[getProblem().getAgentDomainSize(neighbor)];
        	if(debug){
        		System.out.println("The domain size of neighbor " + 
        	neighbor + "is " + getProblem().getAgentDomainSize(neighbor));
        	}
        }
    	
    	
    	double alpha = Double.MAX_VALUE;
    	double cost = 0;  	
    	if(ProjectToMe == projectDirection){
    		for(int myVal = 0; myVal < this.getAgentDomainSize(); myVal++){	
    			alpha = myACConstruct.checkAlphaProjectToMe(getId(), myVal, neighbor);
    			if(0 < alpha){
    				myACConstruct.updateCostsWhenProjectToMe(getId(), myVal, neighbor, 
    						alpha);
    			}	
    		}

    	}
    	if(ProjectFromMe == projectDirection){
    		for(int hisVal = 0; hisVal < this.getAgentDomainSize(neighbor); hisVal++){
    			alpha = myACConstruct.checkAlphaProjectToNeighbor(getId(), neighbor, 
    					hisVal);
    			if(!ACPreprocessFlag && recoverFlag && needRecord){
    				if (-1 == neighborIndex){
    					System.exit(-1);
    				}
    				
    				myACConstruct.P_records[neighborIndex][myACConstruct.ACRecordsProjectFromMe[neighborIndex]][hisVal] = alpha;
    			}
    			if(0 < alpha){
    				myACConstruct.updateCostsWhenProjectToNeighbor(getId(), neighbor,
    						hisVal, alpha);
    			}
    		}
    	}
    	
    	if(!ACPreprocessFlag &&  recoverFlag){
    		myACConstruct.ACRecordsProjectFromMe[neighborIndex] ++;
    	}
    	
    }
    
    void checkDomainForDeletions(){
    	Vector<Integer> valuesToDelete = new Vector();
//    	double cPhi =  cPhi;
//    	for(int myVal : this.getAgentDomain()){
		for(int myVal = 0; myVal < this.getAgentDomainSize(); myVal++){	
    		double cSelf = myACConstruct.unaryCosts[myVal];
    		if(cSelf + cPhi > bound && !myACConstruct.pruned[myVal]){
    			valuesToDelete.add(myVal);
    			myACConstruct.pruned[myVal] = true;
    			// SuWen Debug
    			if(debug){
        			System.out.println("Current my top:" + bound +
        					"\tCurrent my cPhi:"+cPhi + "\tUnaryCost of var["+
        					myVal + "]:"+cSelf);
        			System.out.println("["+ myVal + "] pruned from variable " + getId());
    			}

    			
    			myACConstruct.unaryCosts[myVal] = Double.MAX_VALUE;
    		}
    	}
    	
    	if(0 < valuesToDelete.size()){

    		if(0 >= myACConstruct.domainSizeAfterPruning()){
    			System.out.println("Trying to remove a value from a variable whose domain is empty!!");
                send("TERMINATE").toAllAgentsAfterMe();
    		}
    		
    		int neighborIndex = -1;
    		for(int neighbor : getProblem().getAgentNeighbors(getId())) {
    			send("DEL", getId(), valuesToDelete, myACConstruct).to(neighbor);
                //Debug
                if(debug){
                	System.out.println("DEL");
                }
                neighborIndex = myACConstruct.getNeighborIndex(neighbor);
                myACConstruct.ACRecordsProjectToMe[neighborIndex] = 0;
                if (neighbor < getId()) {
                	binaryProjection(ProjectFromMe, neighbor, false, true);
                }
                else {
                	binaryProjection(ProjectFromMe, neighbor, true, true);
                }

                
    		}
    	}
    	unaryProjection();
    }
    
    void unaryProjection(){
    	double alpha = Double.MAX_VALUE;
    	for(double unaryCost : myACConstruct.unaryCosts){
    		if(unaryCost < alpha){
    			alpha = unaryCost;
    		}
    	}
    	if(0 < alpha){
    		myACConstruct.myContribution += alpha;
//    		if(!this.isFirstAgent()){
//    			cPhi += alpha;
//    		}
    		for(int i = 0; i < myACConstruct.unaryCosts.length; i++){
    			myACConstruct.unaryCosts[i] -= alpha;
    		}
    	}
    }
    
    /**
     * 
     * @param p
     * @param dp
     * @param IDp
     * @param THp
     */
    @WhenReceived("DEL")
    public void handleDEL(int srcAgent, Vector<Integer> valuesToDelete, ACConstruct_AFB hisACConstruct){

//    	if(hisACConstruct.global_top < myACConstruct.global_top){
//    		myACConstruct.global_top = hisACConstruct.global_top;
//    		ACEnforcementNeed = true;
//    	}
    	
    	int srcAgentIndex = myACConstruct.getNeighborIndex(srcAgent);
    	for(int val : valuesToDelete){
    		myACConstruct.neighborsPruned.get(srcAgentIndex)[val] = true;
    	}
    	
    	int srcDepth = hisACConstruct.depth;
    	int myDepth = myACConstruct.depth;
    	//added Oliva temp
   if(!ACPreprocessFlag){ 	
    	if(srcDepth < myDepth && -1 == ACEnforceOrder){
    		boolean hasUnDone = undoAC(hisACConstruct);
    		resetProjectionsFromMe(srcAgentIndex);
    		binaryProjection(ProjectToMe, srcAgent, false, true);
    		if(hasUnDone)
    			binaryProjection(ProjectFromMe, srcAgent, false, false);
    	}
    	else{
    		binaryProjection(ProjectToMe, srcAgent, true, false);
    		myACConstruct.ACRecordsProjectToMe[srcAgentIndex] ++;
    	}
   }
   else{
	   binaryProjection(ProjectToMe, srcAgent, true, true);
   }
    }
    
    void resetProjectionsFromMe(int neighborIndex){
    	myACConstruct.ACRecordsProjectFromMe[neighborIndex] = 0;
    	myACConstruct.P_records[neighborIndex] = new Double[myACConstruct.MAX_PROJECTION_NUM_RECORDED][];   	
    }
    
    boolean undoAC(ACConstruct_AFB srcACConstruct){
    	int neighbor = srcACConstruct.agentID;
    	int neighborIndex = myACConstruct.getNeighborIndex(neighbor);
    	int projectionFromMeInMyCopy = myACConstruct.ACRecordsProjectFromMe[neighborIndex];
    	int myIndexInNeighbor = srcACConstruct.getNeighborIndex(getId());
    	int projectionFromMeInNeighborCopy = srcACConstruct.ACRecordsProjectToMe[myIndexInNeighbor];
    	/* Number of projections in self's copy than srcAgen's copy*/
    	int numMoreProjection = projectionFromMeInMyCopy - projectionFromMeInNeighborCopy;
    	if(0 == numMoreProjection){
    		return false;
    	}

    	for(int i = projectionFromMeInNeighborCopy; i < projectionFromMeInMyCopy; i++){
    		myACConstruct.ACRecordsProjectFromMe[neighborIndex] --;
    		for(int hisVal : myACConstruct.neighborsDomains.elementAt(neighborIndex)){
    			
    			if(myACConstruct.neighborsPruned.elementAt(neighborIndex)[hisVal]) continue;


				if(myACConstruct.P_records[neighborIndex][i] == null){
					// Olivia modified
					Double[] ACCounter = new Double[getProblem().getAgentDomainSize(neighbor)];
					for(int m = 0; m < ACCounter.length; m++){
						ACCounter[m] = (double) 0;
					}
					myACConstruct.P_records[neighborIndex][i] = ACCounter;
//					myACConstruct.P_records[neighborIndex][i] = new Double[getProblem().getDomainSize(neighbor)];
				}
				if(debug){
					System.out.println("k:" + myACConstruct.P_records[neighborIndex][i].length);
				}

    			double alpha = myACConstruct.P_records[neighborIndex][i][hisVal];
    			myACConstruct.updateCostsWhenRollBack(getId(), neighbor, hisVal, alpha);
    		}
    	}
    	return true;
    }
    
    private void assignCPA(){
//    	if(this.isFirstAgent()){
//        	double gc = subtreeContribution + myACConstruct.myContribution;
//        	if(gc > myACConstruct.global_cPhi) {
//        		myACConstruct.global_cPhi = gc;
//        		ACEnforcementNeed = true;
//        	}
        	
//        	if(bound < myACConstruct.global_top) {
//        		myACConstruct.global_top = bound;
//        		ACEnforcementNeed = true;
//        	}
//        	else {
//        		bound = (int) myACConstruct.global_top;
//        		ACEnforcementNeed = true;
//        	}
//        	
//        	if(cPhi < myACConstruct.global_cPhi){
//        		cPhi = (int)myACConstruct.global_cPhi;
//        		ACEnforcementNeed = true;
//        	}
//        	else{
//        		myACConstruct.global_cPhi = cPhi;
//        		ACEnforcementNeed = true;
//        	}
//    	}
    	
    	if(this.isFirstAgent()){
    		if (myACConstruct.myContribution > 0) {
    			cPhi += myACConstruct.myContribution;
    			ACEnforcementNeed = true;
    			myACConstruct.myContribution = 0;
    		}		   		
    	}
    	
    	clearEstimations();
    	int v = -1;
    	int lastAssignedValue = (cpa.isAssigned(this.getId()) ? cpa.getAssignment(this.getId()) : -1);
    	cpa.unassign(this.getId());
    	for (int i = lastAssignedValue + 1; i < getAgentDomainSize(); i++) {
    		if (!myACConstruct.pruned[i] &&
    				costOf(cpa) + calcFv(i, cpa) < bound) {
                v = i;
                break;
    		}
    	}
    	if (v == -1) {
            backtrack();
    	} else {
    		ACEnforcementNeed = true;
            cpa.assign(this.getId(), v);
            timeStamp.incLocalTime();
            if (cpa.isFull(getProblem())) {
                broadcast("NEW_SOLUTION", cpa);
                bound = costOf(cpa);
                assignCPA();
            } else {
                send("CPA_MSG", cpa, cPhi, bound).toNextAgent();
//                send("CPA_MSG", cpa).toNextAgent();
                send("FB_CPA", this.getId(), cpa, cPhi, bound).toAllAgentsAfterMe();
            }
    	}
    	
        // AC Enforcement
        if(ACEnforcementNeed){
        	checkDomainForDeletions();
        	ACEnforcementNeed = false;
        }
    }
    
    private void backtrack() {
    	//debug
//    	if(debug){
//    		System.out.println("In Agent " + getId());
//    		System.out.println("Current Cphi: " + cPhi
//    				+"Current top: " + bound);
//    		for (int i = 0; i < getProblem().getAgentDomainSize(this.getId()); i++) {
//    			if (myACConstruct.pruned[i]) {
//    				System.out.println("My Pruned Val: " + i);
//    			}
//    		}
//    		
//    	}
        clearEstimations();
        if (this.isFirstAgent()) {
             finish(bestCpa);
             finish();
             System.out.println("bound is: " + bound);
             for(int var : bestCpa.assignedVariables()) {
            	 System.out.println("Final Assginment!!\n var: " + var +" val: " +bestCpa.getAssignment(var));
             }
             File file = new File("statistics.txt");
             try {
                 FileWriter fileWriter = new FileWriter(file, true);
                 fileWriter.write("\t" + bound + "\t");
                 fileWriter.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
        } else {
             cpa.unassign(getId());
             send("CPA_MSG", cpa, cPhi, bound).toPreviousAgent();
//             send("CPA_MSG", cpa).toPreviousAgent();
        }

    }
    
    private void clearEstimations() {
        this.estimates = new int[getProblem().getNumberOfAgents()];
    }
    
    private int calcFv(int v, Assignment pa) {
    	//debug
//    	if(debug && myACConstruct.myContribution > 0) {
//    		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~");
//    		System.out.println("In agent [" + getId() +"] " +"h[v]: " + h[v] + " myContri: " + myACConstruct.myContribution);
//    	}

        return pa.calcAddedCost(getId(), v, getProblem()) + h[v];
    }
    
    private void fillH() {
        for (int v = 0; v < getProblem().getAgentDomainSize(this.getId()); v++) {
            h[v] = calculateHv(v);
        }
    }
    
    private int calculateHv(int v) {
        int ans = 0;
        for (int aj = getId() + 1; aj < this.getProblem().getNumberOfAgents(); aj++) {
            int tmp = Integer.MAX_VALUE;
            for (int u = 0; u < getProblem().getAgentDomainSize(aj); u++) {
                tmp = min(tmp, getAgentConstraintCost(this.getId(), v, aj, u));
            }
            ans += tmp;
        }
        return ans;
    }
    
    @Override
    protected void beforeMessageSending(Message m) {
        m.getMetadata().put("TIMESTAMP", timeStamp.deepCopy());
    }
    
    @Override
    protected Message beforeMessageProcessing(Message msg) {
        if (msg.getName().equals(SYS_TERMINATION_MESSAGE)) {
            return msg;
        }

        TimeStamp hisTimeStamp = (TimeStamp) msg.getMetadata().get("TIMESTAMP");
        if (hisTimeStamp.compare(timeStamp, this) >= 0) {
            timeStamp.copyFrom(hisTimeStamp);

        } else {
            return null;
        }
         return super.beforeMessageProcessing(msg);
    }
    
    @WhenReceived("FB_CPA")
    public void handleFBCPA(int aj, Assignment pa, int receivedCPhi, int receivedBound) {
        /** AC enforcement **/
//        double subtreeContri = subtreeContribution + myACConstruct.myContribution;
//        subtreeContribution = 0;
//        myACConstruct.myContribution = 0;
    	
        //AC Enforcement    	
        if(receivedCPhi > cPhi){
        	cPhi = receivedCPhi;
        	ACEnforcementNeed = true;
        }
        if(receivedBound < bound){
        	bound = receivedBound;
        	ACEnforcementNeed = true;
        }
    	
        int minf = Integer.MAX_VALUE;
        for (int v = 0; v < this.getProblem().getAgentDomainSize(this.getId()); v++) {
        	if (!myACConstruct.pruned[v]) {
        		minf = min(minf, calcFv(v, pa));
        	}
            
        }

//        send("FB_ESTIMATE", minf, getId(), 
//        		myACConstruct.global_cPhi, myACConstruct.global_top, subtreeContri).to(aj);
        send("FB_ESTIMATE", minf, getId(), 
        		cPhi, bound, myACConstruct.myContribution).to(aj);
        myACConstruct.myContribution = 0;
        if(ACEnforcementNeed){
        	checkDomainForDeletions();
        	ACEnforcementNeed = false;
        }
    }
    
    @WhenReceived("CPA_MSG")
    public void handleCPAMSG(Assignment pa, int receivedCPhi, int receivedBound) {
        //AC Enforcement
        if(receivedCPhi > cPhi){
        	cPhi = receivedCPhi;
        	ACEnforcementNeed = true;
        }
        if(receivedBound < bound){
        	bound = receivedBound;
        	ACEnforcementNeed = true;
        }
    	
        this.cpa = pa;
        Assignment tempCpa = pa.deepCopy();
        tempCpa.unassign(this.getId());
        if (costOf(tempCpa) >= bound) {
            backtrack();
        } else {
            assignCPA();
        }
    }
    
    @WhenReceived("FB_ESTIMATE")
    public void handleFBESTIMATE(int estimate, int aj, int receivedCPhi, int receivedBound,
    		double receivedContribution) {
        /** AC Enforcement **/
//        this.subtreeContribution += subtreeContri;
//        if(this.isFirstAgent()){
//        	myACConstruct.global_cPhi += subtreeContri;
//        }
    	
        //AC Enforcement
//    	cPhi += receivedContribution;
//        contributions[aj] = receivedContribution;
//        int contributionSum = 0;
//        for (int i = 0; i < contributions.length; i++) {
//        	contributionSum += contributions[i];
//        }
    	if(receivedContribution > 0) {
    		cPhi += receivedContribution;
    		ACEnforcementNeed = true;
    	}
    	
    	if(cPhi == bound) {
            finish(bestCpa);
            finish();
            System.out.println("bound is: " + bound);
            for(int var : bestCpa.assignedVariables()) {
           	 System.out.println("Final Assginment!!\n var: " + var +" val: " +bestCpa.getAssignment(var));
            }
            File file = new File("statistics.txt");
            try {
                FileWriter fileWriter = new FileWriter(file, true);
                fileWriter.write("\t" + bound + "\t");
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    	}
        
        if(receivedCPhi > cPhi){
        	cPhi = receivedCPhi;
        	ACEnforcementNeed = true;
        }
        if(receivedBound < bound){
        	bound = receivedBound;
        	ACEnforcementNeed = true;
        }
    	
    	
        estimates[aj] = estimate;
        int estimatesSum = 0;
        for (int i = 0; i < estimates.length; i++) {
            estimatesSum += estimates[i];
        }
        if (costOf(cpa) + estimatesSum >= bound) {
            assignCPA();
        } 
        
        if(ACEnforcementNeed){
        	checkDomainForDeletions();
        	ACEnforcementNeed = false;
        }
    }
    
    @WhenReceived("NEW_SOLUTION")
    public void handleNEWSOLUTION(Assignment pa) {
        bestCpa = pa;
        bound = costOf(pa);
    }
}
