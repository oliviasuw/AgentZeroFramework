/**
 * AC_BnBAdoptAgent.java
   Created by Su Wen
   Date: Dec 20, 2014
   Time: 4:27:19 PM 
 */
package bgu.dcr.az.dev.agents;

import bgu.dcr.az.api.Continuation;
import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.DFSPsaudoTree;
import bgu.dcr.az.dev.agents.ACConstruct.BinaryConstraint;
import bgu.dcr.az.dev.tools.AssignmentInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Algorithm(name="BnBAdoptPlusWithAC", useIdleDetector=true)   // Corresponds to the algorithm name in the .xml file
public class AC_BnBAdoptAgent extends SimpleAgent {

	boolean debug = false;
    /** Structures for BnB-ADOPT+ only **/
    boolean PlusOn = true;
    // key: child/pseudochild ID; value: lastSentVALUE
    HashMap<Integer, Integer> lastSentVALUEs = new HashMap();
    // key: parent ID; value: lastSentCOST
    HashMap<Integer, AssignmentInfo> lastSentCPA = new HashMap<>();
    boolean cpaChanged = false;
    int lastSentLB = -1;
    int lastSentUB = Integer.MAX_VALUE;
    // Suwen delted
//    boolean ThReq = false;

    HashMap<Integer, AssignmentInfo> cpa = new HashMap<>();
    public int ID;
    private int[][] lbChildD;
    private int[] LBD;
    private int LB;
    private int[][] ubChildD;
    private int[] UBD;
    private int UB;
    private Set<Integer> SCA = new HashSet<>();
    private int d;
    private int TH;
    DFSPsaudoTree tree;
    private int terminateValue = 0;

    /** Structures for AC Enforcement**/
    boolean ACOn = true;
    int ACEnforceOrder = -1;  // -1 : first bottom to top;  1 : first top to bottom
    final static int ProjectToMe = 1;
    final static int ProjectFromMe = 2;
    public ACConstruct myACConstruct;
    double subtreeContribution = 0;
    boolean ACEnforcementNeed = false;
    boolean ACPreprocessFlag = true;
    
    @Override
    public void start() {

        tree = new DFSPsaudoTree();
        tree.calculate(this).andWhenDoneDo(new Continuation() {
            @Override
            public void doContinue () {
            	/**
            	 * AC enforcement
            	 */
            	if(ACOn){
            		initACConstruct();
            		ACPreprocess();
            		ACPreprocessFlag = false;
            	}
            	
                if(!tree.isRoot()){
                    SCA = getSCA();
                    //System.out.println("I am " + getId() + ", SCA: " + SCA);
                    for(int ancestor : SCA){
                        cpa.put(ancestor, new AssignmentInfo(0, 0));
                    }
                }
                ID = 0;
                lbChildD = new int[tree.getChildren().size()][getDomainSize()];
                LBD = new int[getDomainSize()];
                ubChildD = new int[tree.getChildren().size()][getDomainSize()];
                UBD = new int[getDomainSize()];
                for(int i = 0; i < tree.getChildren().size(); i++){
                    for(int j = 0; j < getDomainSize(); j++){
                        InitChild(i, j);
                    }
                }
                
                InitSelf();
                backtrack();
                
//                // AC Enforcement
//                if(ACEnforcementNeed){
//                	checkDomainForDeletions();
//                	ACEnforcementNeed = false;
//                }

            }
        });
    }

    @Override
    public void onIdleDetected() {
        backtrack();
    }

    //get the SCA of a child
    public Set<Integer> getSCA(){
        Set<Integer> _SCA = new HashSet<>();
        for(int ancestor : tree.getAncestors()){
            if(getProblem().getNeighbors(getId()).contains(ancestor))
                _SCA.add(ancestor);
            else{
                for(int descendant : tree.getDescendants()){
                    if(getProblem().getNeighbors(descendant).contains(ancestor)){
                        _SCA.add(ancestor);
                        break;
                    }
                }
            }
        }
//        //Debug
//        if(getId()==6){
//        	for(int x : tree.getAncestors()){
//        		System.out.println("ancestor: " + x);
//        	}
//        }
//        if(getId()==6){
//        	for(int x : tree.getAncestorDepths()){
//        		System.out.println("depth: " + x);
//        	}
//        }

        return _SCA;
    }

    public Set<Integer> getSCA(int id){
        Set<Integer> _SCA = new HashSet<>();
        _SCA.add(getId());
        for(int ancestor : tree.getAncestors()){
            if(getProblem().getNeighbors(id).contains(ancestor))
                _SCA.add(ancestor);
            else{
                for(int descendant : tree.getChildDescendants(id)){
                    if(getProblem().getNeighbors(descendant).contains(ancestor)){
                        _SCA.add(ancestor);
                        break;
                    }
                }
            }
        }
        //System.out.println("id : " + id + ", SCA: " + _SCA);
        return _SCA;
    }

    public void InitChild(int child, int val){
        lbChildD[child][val] = 0;       //should have been heuristic
        ubChildD[child][val] = Integer.MAX_VALUE;
    }

    public void InitSelf(){
        int min = Integer.MAX_VALUE;
        for(int value : getDomain()){
            if(min > calcDelta(value) + sumlbORub(lbChildD, value)){
                min = calcDelta(value) + sumlbORub(lbChildD, value);
                d = value;
            }
        }
        ID++;
        TH = Integer.MAX_VALUE;
        // Suwen delted
//        if(PlusOn){
//            ThReq = true;
//        }
    }
    
    public void initACConstruct(){
    	myACConstruct = new ACConstruct(this);
    }

    public int positivInfinityPlus(int a, int b){
        if(a + b < 0)   //overflow
            return Integer.MAX_VALUE;
        return a + b;
    }

    public int sumlbORub(int[][] arrayList, int val){
        int sum = 0;
        for(int i = 0; i < tree.getChildren().size(); i++)
            sum = positivInfinityPlus(sum, arrayList[i][val]);
        return sum;
    }

    public int calcDelta(int val){
        if(tree.isRoot())
            return 0;
        int delta = 0;
        for(int ancestor : SCA){
            delta += getConstraintCost(getId(), val, ancestor, cpa.get(ancestor).getValue());
        }
        //System.out.println("sca: " + SCA + " delta : " + delta);
        return delta;
    }

    private void backtrack(){
        for(int i = 0; i < getDomainSize(); i++){
            //if(getId()==2 && !tree.isLeaf()){
            //System.out.println("delta = " + calcDelta(i) +" i = " + i + " " + lbChildD[0][i] + " " + ubChildD[0][i]);
            //System.out.println("SCA: " + SCA);
            //}
            LBD[i] = calcDelta(i) + sumlbORub(lbChildD, i);
            UBD[i] = positivInfinityPlus(calcDelta(i), sumlbORub(ubChildD, i));
        }
        LB = findMinimum(LBD, 1, 0);
        UB = findMinimum(UBD, 1, 0);
        //if(LB > UB){
        //    System.out.println((LBD[d] >= min(TH, UB)) + "lbd1 = " + LBD[1] + " d = " + 
        //findMinimum(LBD, 2, d));
        //}
        
        int last_d = d;

        if(LBD[d] >= min(TH, UB) || myACConstruct.pruned[d]){
            int bestVal = findMinimum(LBD, 2, d);
            if(bestVal == -1){
            	System.out.println("All the values are pruned in varible " + getId());
            	System.exit(-1);
            }
            d = bestVal;
            ID++;
        }
        
        if(d != last_d){
        	ACEnforcementNeed = true;
        }
        
        if((tree.isRoot() && UB == LB)){
            for(int child : tree.getChildren()){
                send("TERMINATE").to(child);
            }
            File file = new File("costs.txt");
            try {
                FileWriter fileWriter = new FileWriter(file, true);
                fileWriter.write("#" + UB + "\t");
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finishWithCost(UB);
            return;
        }
        
    	//AC Enforcement
    	if(tree.isRoot()){
        	double gc = subtreeContribution + myACConstruct.myContribution;
        	if(gc > myACConstruct.global_cPhi) {
        		myACConstruct.global_cPhi = gc;
        		ACEnforcementNeed = true;
        	}
        	
        	if(UB < myACConstruct.global_top) {
        		myACConstruct.global_top = UB;
        		ACEnforcementNeed = true;
        	}
        	
        	if(LB < myACConstruct.global_cPhi){
        		LB = (int)myACConstruct.global_cPhi;
        		ACEnforcementNeed = true;
        	}
    	}
    	
        for(int child : tree.getChildren()){
            if(PlusOn) {
//                if(!lastSentVALUEs.containsKey(child) || lastSentVALUEs.get(child) != d
//                		|| ThReq){
                if(!lastSentVALUEs.containsKey(child) || lastSentVALUEs.get(child) != d){
                	
                    send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                            + lbChildD[tree.getChildren().indexOf(child)][d],
                            myACConstruct.global_cPhi, myACConstruct.global_top).to(child);
                    //Debug
                    if(debug){
                    	System.out.println("VALUE: " + getId() +
                    			" [" + d + "] to " + child);
                    }
                    
                }
                lastSentVALUEs.put(child, d);
            }
            else {
                send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                        + lbChildD[tree.getChildren().indexOf(child)][d],
                        myACConstruct.global_cPhi, myACConstruct.global_top).to(child);
                //Debug
                if(debug){
                	System.out.println("VALUE: " + getId() +
                			" [" + d + "] to " + child);
                }
            }

        }
    	
        for(int pseudoChild : tree.getPsaudoChildren()){
            if(PlusOn){
//                if(!lastSentVALUEs.containsKey(pseudoChild) || lastSentVALUEs.get(pseudoChild) != d
//                		|| ThReq){
            	if(!lastSentVALUEs.containsKey(pseudoChild) || lastSentVALUEs.get(pseudoChild) != d){
                    send("VALUE", getId(), d, ID, Integer.MAX_VALUE,
                            myACConstruct.global_cPhi, myACConstruct.global_top).to(pseudoChild);
                    //Debug
                    if(debug){
                    	System.out.println("VALUE: " + getId() +
                    			" [" + d + "] to " + pseudoChild);
                    }
                }
                lastSentVALUEs.put(pseudoChild, d);
            }
            else{
                send("VALUE", getId(), d, ID, Integer.MAX_VALUE,
                        myACConstruct.global_cPhi, myACConstruct.global_top).to(pseudoChild);
                //Debug
                if(debug){
                	System.out.println("VALUE: " + getId() +
                			" [" + d + "] to " + pseudoChild);
                }
                
            }
        }

        if(!tree.isRoot()){
            if(PlusOn){
                if(lastSentCPA.isEmpty()
                        || lastSentLB != LB
                        || lastSentUB != UB
                        || !lastSentCPA.equals(cpa)
                        || cpaChanged){
                	
                    cpaChanged = false;
                    
                    /** AC enforcement **/
                    double subtreeContri = subtreeContribution + myACConstruct.myContribution;
                    subtreeContribution = 0;
                    myACConstruct.myContribution = 0;
                    
                    if(tree.getParent() != -1){
                    	// Suwen delted ThReq
//                        send("COST", getId(), cpa, LB, UB, ThReq, myACConstruct.global_cPhi,
//                        		myACConstruct.global_top, subtreeContri).to(tree.getParent());
                        send("COST", getId(), cpa, LB, UB, myACConstruct.global_cPhi,
                        		myACConstruct.global_top, subtreeContri).to(tree.getParent());
                        //Debug
                        if(debug){
                        	System.out.println("COST: " + getId() +
                        			" to " + tree.getParent() + " LB: "
                        			+LB + " UB: " + UB);
                        }
                        
                        if(getId() == 4 && LB==UB && LB == 67){
                        	System.out.println("debug");
                        }

                    }
                 // Suwen delted ThReq
//                    ThReq = false;

                }
                lastSentCPA = cpa;
                lastSentLB = LB;
                lastSentUB = UB;
            }
            else{
            	/** AC enforcement **/
            	double subtreeContri = subtreeContribution + myACConstruct.myContribution;
                subtreeContribution = 0;
                myACConstruct.myContribution = 0;
                // Suwen deleted
//                send("COST", getId(), cpa, LB, UB, ThReq, myACConstruct.global_cPhi,
//                		myACConstruct.global_top, subtreeContri).to(tree.getParent());
                send("COST", getId(), cpa, LB, UB, myACConstruct.global_cPhi,
                		myACConstruct.global_top, subtreeContri).to(tree.getParent());
                //Debug
                if(debug){
                	System.out.println("COST: " + getId() +
                			" to " + tree.getParent() + " LB: "
                			+LB + " UB: " + UB);
                }
            }
        }

        // AC Enforcement
        if(ACEnforcementNeed){
        	checkDomainForDeletions();
        	ACEnforcementNeed = false;
        }
    }

    private int findMinimum(int[] array, int flag, int initValue){     
    	//flag = 1, return minumum,  else, return minimum index
        int min = Integer.MAX_VALUE;
        int index = initValue;
        
        if(myACConstruct.pruned[index]){
        	index = -1;
        }
        
        for(int i = 0; i < array.length; i++){
        	/* AC enforcement*/
        	if(myACConstruct.pruned[i]){
        		continue;
        	}
        	
            if(min > array[i]) {
                min = array[i];
                index = i;
            }
        }
        
        if(flag == 1)
            return min;
        else
            return index;
    }

    public HashMap<Integer, AssignmentInfo> copyCPA(HashMap<Integer, AssignmentInfo> cpa){
        HashMap<Integer, AssignmentInfo> _cpaHashMap = new HashMap<>();
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa.entrySet()){
            _cpaHashMap.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), 
            		entry.getValue().getID()));
        }
        return _cpaHashMap;
    }

    public void priorityMerge(int p, int dp, int IDp, HashMap<Integer, AssignmentInfo> _cpa) {
        if(_cpa.containsKey(p) && IDp > _cpa.get(p).getID()){
            // BnB-ADOPT+ only
            if(PlusOn){
                if(_cpa.get(p).getValue() != dp){
                    this.cpaChanged = true;
                }
            }
            _cpa.remove(p);
            _cpa.put(p, new AssignmentInfo(dp, IDp));
        }
    }

    public void priorityMerge(HashMap<Integer, AssignmentInfo> cpa1, HashMap<Integer, 
    		AssignmentInfo> _cpa) {
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa1.entrySet()){
            if(_cpa.containsKey(entry.getKey()) && entry.getValue().getID() > 
            _cpa.get(entry.getKey()).getID()){
                // BnB-ADOPT+ only
                if(PlusOn){
                    if(_cpa.get(entry.getKey()).getValue() != entry.getValue().getValue()){
                        this.cpaChanged = true;
                    }
                }
                _cpa.remove(entry.getKey());
                _cpa.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(),
                		entry.getValue().getID()));
            }
        }
    }

    public boolean isCompatible(HashMap<Integer, AssignmentInfo> cpa1, HashMap<Integer, 
    		AssignmentInfo> _cpa){
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa1.entrySet()){
            if(_cpa.containsKey(entry.getKey()) && entry.getValue().getValue() != 
            		_cpa.get(entry.getKey()).getValue()){
                return false;
            }
        }
        return true;
    }

    @WhenReceived("VALUE")
    public void handleVALUE(int p, int dp, int IDp, int THp, double global_cPhi, double global_top){
        //System.out.println("I am " + getId() + "VALUE received: " + "p = " + p + " dp = " + dp + " IDp = " + IDp + " THp = " + THp + ", mycpa: " + print(cpa));
        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(p,  dp, IDp, cpa);
        //System.out.println("_cpa : " + print(_cpa) + ", cpa: " + print(cpa));
        if(!isCompatible(_cpa, cpa)){
            //System.out.println("true1");
            for(int i = 0; i < tree.getChildren().size(); i++){
                //System.out.println("child: " + tree.getChildren().get(i) + ", CHILDSCA: " + getSCA(tree.getChildren().get(i)));
                if(getSCA(tree.getChildren().get(i)).contains(p))
                    for(int j = 0; j < getDomainSize(); j++){
                        //System.out.println("true2");
                        InitChild(i, j);
                    }
            }
            InitSelf();
        }
        if(p == tree.getParent())
            TH = THp;
        
        //AC Enforcement
        if(global_cPhi > myACConstruct.global_cPhi){
        	myACConstruct.global_cPhi = global_cPhi;
        	ACEnforcementNeed = true;
        }
        if(global_top < myACConstruct.global_top){
        	myACConstruct.global_top = global_top;
        	ACEnforcementNeed = true;
        }
        
    }


//    public void handleCOST(int c, HashMap<Integer, AssignmentInfo>cCPA, int LBc, 
//    		int UBc, boolean thReq, double global_cPhi, double global_top, double subtreeContri){
    	
//    	send("COST", getId(), cpa, LB, UB, ThReq, myACConstruct.global_cPhi,
//        		myACConstruct.global_top, subtreeContri).to(tree.getParent());
        //System.out.println("I am " + getId() + "COST received: " + "c = " + c + " cCPA = " + print(cCPA) + " LBc = " + LBc + " UBc = " + UBc);

    	//Suwen delted
//        if(PlusOn){
//            ThReq = thReq;
//        }
    @WhenReceived("COST")
    public void handleCOST(int c, HashMap<Integer, AssignmentInfo>cCPA, int LBc, 
        		int UBc, double global_cPhi, double global_top, double subtreeContri){        
    	if(getId()==5){
    		System.out.println("debug!!");
    	}
        /** AC Enforcement **/
        this.subtreeContribution += subtreeContri;
        if(!tree.isRoot()){
        	myACConstruct.global_cPhi += subtreeContri;
        }
        		
        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(cCPA, cpa);
        if(!isCompatible(_cpa, cpa)){
            for(int i = 0; i < tree.getChildren().size(); i++){
                for(int j = 0; j < getDomainSize(); j++){
                    HashMap<Integer, AssignmentInfo> tmpCPA = new HashMap<Integer,
                    		AssignmentInfo>();
                    for(Map.Entry<Integer, AssignmentInfo> entry : _cpa.entrySet()){
                        if(getSCA(tree.getChildren().get(i)).contains(entry.getKey()))
                            tmpCPA.put(entry.getKey(), new AssignmentInfo(entry.getValue()
                            		.getValue(), entry.getValue().getID()));
                    }
                    if(!isCompatible(tmpCPA, cpa))
                        InitChild(i, j);
                }
            }
        }
        if(isCompatible(cCPA, cpa)){
            lbChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()] = 
            		max(lbChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()], LBc);
            ubChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()] =
            		min(ubChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()], UBc);
        }
        if(!isCompatible(_cpa, cpa))
            InitSelf();
    }

    String print(HashMap<Integer, AssignmentInfo> cCPA){
        String s = "";
        for (Map.Entry<Integer, AssignmentInfo> entry : cCPA.entrySet()) {
            s += entry.getKey() + ":" + entry.getValue().getValue() + ", ";
        }
        return s;
    }

    @WhenReceived("TERMINATE")
    public void handleTERMINATE(){
        for(int child : tree.getChildren()){
            send("TERMINATE").to(child);
        }
        finish(d);
    }
    
    /**
     * AC Enforcement
     */
    void ACPreprocess(){
		if(1 == ACEnforceOrder) { // Upper Agents are prefered
			// Projection on self and parents/pseudo-parents
			for(int pseudoParent : tree.getPsaudoParents()){
				binaryProjection(ProjectToMe, pseudoParent, false, false);
				checkDomainForDeletions();
				binaryProjection(ProjectFromMe, pseudoParent, false, false);
				checkDomainForDeletions();
			}
			
			int parent = tree.getParent();
			if(-1 != parent){
				binaryProjection(ProjectToMe, parent, false, false);
				checkDomainForDeletions();
				binaryProjection(ProjectFromMe, parent, false, false);
				checkDomainForDeletions();
			}


			// Projection on self and children/pseudo-children
			for(int child : tree.getChildren()){
				binaryProjection(ProjectFromMe, child, true, false);
				checkDomainForDeletions();
				binaryProjection(ProjectToMe, child, true, false);
				checkDomainForDeletions();
			}
			for(int child : tree.getPsaudoChildren()){
				binaryProjection(ProjectFromMe, child, true, false);
				checkDomainForDeletions();
				binaryProjection(ProjectToMe, child, true, false);
				checkDomainForDeletions();
			}
		}
		
		/**
		 * default one. (Same with the one shown in Kobi's thesis P38)
		 */
		if(-1 == ACEnforceOrder) {
			// Projection on self and parents/pseudo-parents
			for(int pseudoParent : tree.getPsaudoParents()){
				binaryProjection(ProjectFromMe, pseudoParent, false, false);
				checkDomainForDeletions();
				binaryProjection(ProjectToMe, pseudoParent, false, false);
				checkDomainForDeletions();
			}
			int parent = tree.getParent();
			if(-1 != parent){
				binaryProjection(ProjectFromMe, parent, false, false);
				checkDomainForDeletions();
				binaryProjection(ProjectToMe, parent, false, false);
				checkDomainForDeletions();
			}


			// Projection on self and children/pseudo-children
			for(int child : tree.getChildren()){
				binaryProjection(ProjectToMe, child, true, false);
				checkDomainForDeletions();
				binaryProjection(ProjectFromMe, child, true, false);
				checkDomainForDeletions();
			}
			for(int child : tree.getPsaudoChildren()){
				binaryProjection(ProjectToMe, child, true, false);
				checkDomainForDeletions();
				binaryProjection(ProjectFromMe, child, true, false);
				checkDomainForDeletions();
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
        	         [neighborIndex]] = new Double[getDomainSize()]; 
        }
    	
    	
    	double alpha = Double.MAX_VALUE;
    	double cost = 0;  	
    	if(ProjectToMe == projectDirection){
    		for(int myVal : this.getDomain()){
    			alpha = myACConstruct.checkAlphaProjectToMe(getId(), myVal, neighbor);
    			if(0 < alpha){
    				myACConstruct.updateCostsWhenProjectToMe(getId(), myVal, neighbor, 
    						alpha);
    			}	
    		}

    	}
    	if(ProjectFromMe == projectDirection){
    		for(int hisVal : this.getDomainOf(neighbor)){
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
    	double cPhi =  myACConstruct.global_cPhi;
    	for(int myVal : this.getDomain()){
    		double cSelf = myACConstruct.unaryCosts[myVal];
    		if(cSelf + cPhi > myACConstruct.global_top && !myACConstruct.pruned[myVal]){
    			valuesToDelete.add(myVal);
    			myACConstruct.pruned[myVal] = true;
    			// SuWen Debug
    			System.out.println("Current my top:" + myACConstruct.global_top +
    					"\tCurrent my cPhi:"+cPhi + "\tUnaryCost of var["+
    					myVal + "]:"+cSelf);
    			System.out.println("["+ myVal + "] pruned from variable " + getId());
    			
    			myACConstruct.unaryCosts[myVal] = Double.MAX_VALUE;
    		}
    	}
    	
    	if(0 < valuesToDelete.size()){

    		if(0 >= myACConstruct.domainSizeAfterPruning()){
    			System.out.println("Trying to remove a value from a variable whose domain is empty!!");
//    			// SuWen Debug
//    			System.out.println("["+ myVal + "] pruned from variable " + getId());
    			
    			for(int child : tree.getChildren()){
                    send("TERMINATE").to(child);
                }
    		}
    		
    		int neighborIndex = -1;
            for(int neighbor : tree.getPsaudoParents()){
                send("DEL", getId(), valuesToDelete, myACConstruct).to(neighbor);
                //Debug
                if(debug){
                	System.out.println("DEL");
                }
                
                neighborIndex = myACConstruct.getNeighborIndex(neighbor);
                myACConstruct.ACRecordsProjectToMe[neighborIndex] = 0;
                binaryProjection(ProjectFromMe, neighbor, false, true);
            }
            int parent = tree.getParent();
            if(-1 != parent){
                send("DEL", getId(), valuesToDelete, myACConstruct).to(parent);
                //Debug
                if(debug){
                	System.out.println("DEL");
                }
                
                neighborIndex = myACConstruct.getNeighborIndex(parent);
                myACConstruct.ACRecordsProjectToMe[neighborIndex] = 0;
                binaryProjection(ProjectFromMe, parent, false, true);
            }

            for(int neighbor : tree.getChildren()){
                send("DEL", getId(), valuesToDelete, myACConstruct).to(neighbor);
                //Debug
                if(debug){
                	System.out.println("DEL");
                }
                
                neighborIndex = myACConstruct.getNeighborIndex(neighbor);
                myACConstruct.ACRecordsProjectToMe[neighborIndex] = 0;
                binaryProjection(ProjectFromMe, neighbor, true, true);
            }
            for(int neighbor : tree.getPsaudoChildren()){
                send("DEL", getId(), valuesToDelete, myACConstruct).to(neighbor);
                //Debug
                if(debug){
                	System.out.println("DEL");
                }
                
                neighborIndex = myACConstruct.getNeighborIndex(neighbor);
                myACConstruct.ACRecordsProjectToMe[neighborIndex] = 0;
                binaryProjection(ProjectFromMe, neighbor, true, true);
            }
    	}
    	unaryProjection();
    }
    
    /**
     * 
     */
    void unaryProjection(){
    	double alpha = Double.MAX_VALUE;
    	for(double unaryCost : myACConstruct.unaryCosts){
    		if(unaryCost < alpha){
    			alpha = unaryCost;
    		}
    	}
    	if(0 < alpha){
    		myACConstruct.myContribution += alpha;
    		if(!tree.isRoot()){
    			myACConstruct.global_cPhi += alpha;
    		}
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
    public void handleDEL(int srcAgent, Vector<Integer> valuesToDelete, ACConstruct hisACConstruct){

    	if(hisACConstruct.global_top < myACConstruct.global_top){
    		myACConstruct.global_top = hisACConstruct.global_top;
    		ACEnforcementNeed = true;
    	}
    	
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
    
    boolean undoAC(ACConstruct srcACConstruct){
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
    			double alpha = myACConstruct.P_records[neighborIndex][i][hisVal];
    			myACConstruct.updateCostsWhenRollBack(getId(), neighbor, hisVal, alpha);
    		}
    	}
    	return true;
    }
}
