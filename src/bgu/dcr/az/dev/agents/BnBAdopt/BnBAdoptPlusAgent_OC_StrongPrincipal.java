/**
 * BnBAdoptPlusAgent_OC_StrongPrincipal.java
   Created by Su Wen
   Date: Mar 30, 2015
   Time: 7:55:58 PM 
 */
/**
 * BnBADOPTPlusOCAgent_PrincipalVars.java
   Created by Su Wen
   Date: Mar 26, 2015
   Time: 8:56:06 PM 
 */
package bgu.dcr.az.dev.agents.BnBAdopt;

import bgu.dcr.az.api.Continuation;
import bgu.dcr.az.api.Message;
import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.exen.MessageQueue;
import bgu.dcr.az.api.tools.DFSPsaudoTree;
import bgu.dcr.az.dev.tools.AssignmentInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by ChrisQin on 10/9/2014.
 */
@Algorithm(name="BnBAdoptPlusAgent_OC_StrongPrincipal", useIdleDetector=true)   // Corresponds to the algorithm name in the .xml file
public class BnBAdoptPlusAgent_OC_StrongPrincipal extends SimpleAgent {

    boolean debug = false;
    /** Structures for BnB-ADOPT+ only **/
    boolean PlusOn = true;
    // key: child/pseudochild ID; value: lastSentVALUE
    HashMap<Integer, Integer> lastSentPrinHashVALUEs = new HashMap();
    // key: parent ID; value: lastSentCOST
    HashMap<Integer, AssignmentInfo> lastSentCPA = new HashMap();
    boolean cpaChanged = false;
    int lastSentLB = -1;
    int lastSentUB = Integer.MAX_VALUE;
    boolean myThReq = false;
    // Key: child/pseudochild ID; value: thReq
    HashMap<Integer, Boolean> receivedThReqs = new HashMap();

    HashMap<Integer, AssignmentInfo> cpa = new HashMap();  // key: agentID 
    private int ID;
    private int[][] lbChildD;
    private int[] LBD;
    private int LB;
    private int[][] ubChildD;
    private int[] UBD;
    private int UB;
    private Set<Integer> SCA = new HashSet();
    private int d;
    private int TH;
    DFSPsaudoTree tree;
    private int terminateValue = 0;

    @Override
    public void start() {

        tree = new DFSPsaudoTree();
        tree.calculate(this).andWhenDoneDo(new Continuation() {
            @Override
            public void doContinue () {
                if(!tree.isRoot()){
                    SCA = getSCA();
                    //System.out.println("I am " + getId() + ", SCA: " + SCA);
                    for(int ancestor : SCA){
                        cpa.put(ancestor, new AssignmentInfo(0, 0));
                    }
                }
                ID = 0;
                lbChildD = new int[tree.getChildren().size()][getAgentDomainSize()];
                LBD = new int[getAgentDomainSize()];
                ubChildD = new int[tree.getChildren().size()][getAgentDomainSize()];
                UBD = new int[getAgentDomainSize()];
                for(int i = 0; i < tree.getChildren().size(); i++){
                    for(int j = 0; j < getAgentDomainSize(); j++){
                        InitChild(i, j);
                    }
                }
                
//            	File file = new File("costs.txt");
//                FileWriter fileWriter = null;
//        		try {
//        			fileWriter = new FileWriter(file, true);
//        			fileWriter.write("agentID: "+ getId()+" depth: "+tree.getDepth() + "\n");
//        			fileWriter.close();
//        		} catch (IOException e1) {
//        			// TODO Auto-generated catch block
//        			e1.printStackTrace();
//        		}
        		
                InitSelf();
                backtrack();

            }
        });
    }

    @Override
    public void onIdleDetected() {
        backtrack();
    }

    //get the SCA of a child
    public Set<Integer> getSCA(){
        Set<Integer> _SCA = new HashSet();
        for(int ancestor : tree.getAncestors()){
            if(getProblem().getAgentNeighbors(getId()).contains(ancestor))
                _SCA.add(ancestor);
            else{
                for(int descendant : tree.getDescendants()){
                    if(getProblem().getAgentNeighbors(descendant).contains(ancestor)){
                        _SCA.add(ancestor);
                        break;
                    }
                }
            }
        }

        return _SCA;
    }

    public Set<Integer> getSCA(int id){
        Set<Integer> _SCA = new HashSet();
        _SCA.add(getId());
        for(int ancestor : tree.getAncestors()){
            if(getProblem().getAgentNeighbors(id).contains(ancestor))
                _SCA.add(ancestor);
            else{
                for(int descendant : tree.getChildDescendants(id)){
                    if(getProblem().getAgentNeighbors(descendant).contains(ancestor)){
                        _SCA.add(ancestor);
                        break;
                    }
                }
            }
        }
        return _SCA;
    }

    public void InitChild(int child, int val){
        lbChildD[child][val] = 0;       //should have been heuristic
        ubChildD[child][val] = Integer.MAX_VALUE;
    }

    private HashMap<Integer, ArrayList<Integer>> identifyPrincipalVars () {
    	HashMap<Integer, ArrayList<Integer>> childPrincipalVarsmap = new HashMap();
    	List<Integer> myVars = getProblem().getVariables(getId());
    	List<Integer> agentsInSubtree = new ArrayList();
    	
    	boolean isPrincipal = false;
    	
    	for(int child : tree.getChildren()) { //for each subtree rooted at a child
    		ArrayList<Integer> myPrincipalVars = new ArrayList();
    		agentsInSubtree.clear();
    		agentsInSubtree.add(child);
    		for(int descendant : tree.getChildDescendants(child)) {
    			agentsInSubtree.add(descendant);
    		}

        	for (int myVar : myVars) {
        		isPrincipal = false;     		
        		for (int descendantAgent : agentsInSubtree) {
        			List<Integer> descendantVars = getProblem().getVariables(descendantAgent);
        			for (int descendant : descendantVars) {
        				if (getProblem().isVarConstrained(myVar, descendant)) {
        					isPrincipal = true;
        					myPrincipalVars.add(myVar);
        					break;
        				}
        			}
        			if (isPrincipal) {
        				break;
        			}
        		}
        	}
        	childPrincipalVarsmap.put(child, myPrincipalVars);
    	}
   
    	return childPrincipalVarsmap;
    }
    
    private ArrayList<Integer> identifyWeakPrincipalVars () {
    	List<Integer> myVars = getProblem().getVariables(getId());
    	List<Integer> myDescendants = tree.getDescendants();
    	ArrayList<Integer> myPrincipalVars = new ArrayList();
   
    	boolean isPrincipal = false;
    	for (int myVar : myVars) {
    		isPrincipal = false;
    		for (int descendantAgent : myDescendants) {
    			List<Integer> descendantVars = getProblem().getVariables(descendantAgent);
    			for (int descendant : descendantVars) {
    				if (getProblem().isVarConstrained(myVar, descendant)) {
    					isPrincipal = true;
    					myPrincipalVars.add(myVar);
    					break;
    				}
    			}
    			if (isPrincipal) {
    				break;
    			}
    		}
    	}
    	return myPrincipalVars;
    }
    
    public void InitSelf(){
    	//debug
    	if(debug){
        	System.out.println("Agent: " + getId());
        	System.out.println("\tdescendants:" + tree.getDescendants());
        	System.out.println("\tchildDescendants:");
        	for(int child : tree.getChildren()){
        		System.out.print("\t\tchild:" + child );
        		for(int de : tree.getChildDescendants(child))
        			System.out.print(" myDesendant: " + de);
        		System.out.println();
        		
        	}
    	}

    		
    	
    	// Set my principle variables
    	getProblem().setStrongPrincipalVariables(getId(), identifyPrincipalVars());
    	getProblem().setWeakPrincipalVariables(getId(), identifyWeakPrincipalVars());
    	getProblem().setChildren(getId(), tree.getChildren());
    	HashMap<Integer, List<Integer>> childDescendants = new HashMap();
    	for(int child : tree.getChildren()) {
    		childDescendants.put(child, tree.getChildDescendants(child));  		
    	}
    	getProblem().setChildDescendMap(getId(), childDescendants);
    	
    	getProblem().setAgentInitialized(getId());

    	
        int min = Integer.MAX_VALUE;
//        for(int value : getAgentDomain()){
        for(int value = 0; value < getAgentDomainSize(); value ++){
            if(min > calcDelta(value) + sumlbORub(lbChildD, value)){
                min = calcDelta(value) + sumlbORub(lbChildD, value);
                d = value;
            }
        }
        ID++;
        TH = Integer.MAX_VALUE;
        if(PlusOn){
			myThReq = true;  // Always false to disable this function by Suwen
		}
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
        int delta = 0;
        
        //Olivia added
        // unary cost
        delta += getAgentConstraintCost(getId(), val);
        
        if(tree.isRoot())
            return delta;
        
        for(int ancestor : SCA){
            delta += getAgentConstraintCost(getId(), val, ancestor, cpa.get(ancestor).getValue());
        }
        //System.out.println("sca: " + SCA + " delta : " + delta);
        return delta;
    }

    private void backtrack(){
    	
        for(int i = 0; i < getAgentDomainSize(); i++){
            LBD[i] = calcDelta(i) + sumlbORub(lbChildD, i);
            UBD[i] = positivInfinityPlus(calcDelta(i), sumlbORub(ubChildD, i));
        }
        LB = findMinimum(LBD, 1, 0);
        UB = findMinimum(UBD, 1, 0);

        if(LBD[d] >= min(TH, UB)){
            d = findMinimum(LBD, 2, d);
            ID++;
        }
        if((tree.isRoot() && UB == LB)){
            for(int child : tree.getChildren()){
                send("TERMINATE").to(child);
                if(debug) {
                	System.out.println("#TERMINATE# from " + getId() +" to " + child);
                }
            }
            File file = new File("statistics.txt");
            try {
                FileWriter fileWriter = new FileWriter(file, true);
                fileWriter.write("\t" + UB + "\t");
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finishWithCost(UB);
            return;
        }
        
        int currentVal = getProblem().getWeakPrincipalVarsHashValue(getId(), d);
        for(int child : tree.getChildren()){
            if(PlusOn) {
            	if(getProblem().hasAllInitialized()) {
            		currentVal = getProblem().getStrongPrincipalVarsHashValue(getId(), child, d);
            	}
                
                if(!lastSentPrinHashVALUEs.containsKey(child) || lastSentPrinHashVALUEs.get(child) != currentVal
                		|| (receivedThReqs.containsKey(child) && receivedThReqs.get(child))){
                    send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                            + lbChildD[tree.getChildren().indexOf(child)][d]).to(child);
                    receivedThReqs.put(child, false);
                    if(debug){
                    	System.out.println("#VALUE# from [" + getId() + "] to [" 
                    + child + "] [d: " + d +"] [ID: " + ID +"]");
                    }
                }
                lastSentPrinHashVALUEs.put(child, currentVal);
                
            }
            else {
                send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                        + lbChildD[tree.getChildren().indexOf(child)][d]).to(child);
                
            }

        }
        
        for(int pseudoChild : tree.getPsaudoChildren()){
        	int myRoot = -1;
        	if(getProblem().hasAllInitialized()) {        		
            	for(int child : tree.getChildren()) {
            		if(tree.getChildDescendants(child).contains(pseudoChild)) {
            			myRoot = child;
            			break;
            		}
            	}
            	currentVal = getProblem().getStrongPrincipalVarsHashValue(getId(), myRoot, d);
        	}
        	
            if(PlusOn){

                if(!lastSentPrinHashVALUEs.containsKey(pseudoChild) || lastSentPrinHashVALUEs.get(pseudoChild) != currentVal
                		|| (receivedThReqs.containsKey(pseudoChild) &&  receivedThReqs.get(pseudoChild))){
                    send("VALUE", getId(), d, ID, Integer.MAX_VALUE).to(pseudoChild);
                    receivedThReqs.put(pseudoChild, false);
                    if(debug){
                    	System.out.println("#VALUE# from [" + getId() + "] to [" 
                    + pseudoChild + "] [d: " + d +"] [ID: " + ID +"]");
                    }
                }
                lastSentPrinHashVALUEs.put(pseudoChild, currentVal);
            }
            else{
                send("VALUE", getId(), d, ID, Integer.MAX_VALUE).to(pseudoChild);
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
                    send("COST", getId(), cpa, LB, UB, myThReq).to(tree.getParent());
                    myThReq = false;
                    if(debug){
                    	System.out.println("#COST# from [" + getId() + "] to [" + tree.getParent()
                    			+"] [LB: " + LB +"] [UB: " + UB +"]");
                    }
                }
                lastSentCPA = cpa;
                lastSentLB = LB;
                lastSentUB = UB;
            }
            else{
                send("COST", getId(), cpa, LB, UB).to(tree.getParent());
            }
        }

    }

    private int findMinimum(int[] array, int flag, int initValue){     //flag = 1, return minumum,  else, return minimum index
        int min = Integer.MAX_VALUE;
        int index = initValue;
        for(int i = 0; i < array.length; i++){
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
        HashMap<Integer, AssignmentInfo> _cpaHashMap = new HashMap();
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa.entrySet()){
            _cpaHashMap.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
        }
        return _cpaHashMap;
    }

    public void priorityMerge(int p, int dp, int IDp, HashMap<Integer, AssignmentInfo> _cpa) {
        if(_cpa.containsKey(p) && IDp > _cpa.get(p).getID()){
            // BnB-ADOPT+ only
            if(PlusOn){
            	
            	int prinHashVal_dp = dp;
            	int prinHashVal_cpa = _cpa.get(p).getValue();
            	
//            	if(getProblem().hasAllInitialized()) {
//                	prinHashVal_dp = getProblem().getStrongPrincipalVarsHashValue(p, rootChild, dp);
//                	prinHashVal_cpa = getProblem().getStrongPrincipalVarsHashValue(p, rootChild, _cpa.get(p).getValue());
//            	}

                if(prinHashVal_dp != prinHashVal_cpa){
                    this.cpaChanged = true;
                }
            }
            _cpa.remove(p);
            _cpa.put(p, new AssignmentInfo(dp, IDp));
        }
    }

    public void priorityMerge(HashMap<Integer, AssignmentInfo> cpa1, HashMap<Integer, AssignmentInfo> _cpa) {
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa1.entrySet()){
            if(_cpa.containsKey(entry.getKey()) && entry.getValue().getID() > 
			_cpa.get(entry.getKey()).getID()){
                // BnB-ADOPT+ only
                if(PlusOn){
                	int hisAgentID = entry.getKey();
                	int prinHashVal_received = entry.getValue().getValue();
                	int prinHashVal_old = _cpa.get(entry.getKey()).getValue();
//                	if(getProblem().hasAgentInitialized(hisAgentID)) {
//                    	prinHashVal_received = getProblem().
//                    			getWeakPrincipalVarsHashValue(entry.getKey(), entry.getValue().getValue());
//                    	prinHashVal_old = getProblem().
//                    			getWeakPrincipalVarsHashValue(entry.getKey(), _cpa.get(entry.getKey()).getValue());
//                	}

                    if(prinHashVal_received != prinHashVal_old){
                        this.cpaChanged = true;
                    }
                }
                _cpa.remove(entry.getKey());
                _cpa.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), 
				entry.getValue().getID()));
            }
        }
    }

    public boolean isCompatible(HashMap<Integer, AssignmentInfo> myCPA,
    		HashMap<Integer, AssignmentInfo> newCPA){
        for(Map.Entry<Integer, AssignmentInfo> entry : myCPA.entrySet()){
        	
        	
        	int prinHashVal_old = entry.getValue().getValue();
        	int ancestor = entry.getKey();
        	int rootChild = -1;
        	if(getProblem().hasAllInitialized()) {
            	HashMap<Integer, List<Integer>> childDescendMapForAncestor = 
            			getProblem().getChildDescendMap(ancestor);
            	for(int childOfAncesotr : getProblem().getChildren(ancestor)){
            		if(getId() == childOfAncesotr) {
            			rootChild = childOfAncesotr;
            			break;
            		}
            		if(childDescendMapForAncestor.get(childOfAncesotr).contains(getId())) {
            			rootChild = childOfAncesotr;
            			break;
            		}
            	}
            	prinHashVal_old = getProblem().
            			getStrongPrincipalVarsHashValue(ancestor, rootChild, entry.getValue().getValue());
        	}

            if(newCPA.containsKey(entry.getKey())){
            	int prinHashVal_new = newCPA.get(entry.getKey()).getValue();
            	if(getProblem().hasAllInitialized()) {
            		prinHashVal_new = getProblem().
                			getStrongPrincipalVarsHashValue(ancestor, rootChild, 
                					newCPA.get(ancestor).getValue());
            	}
            	
            	if (prinHashVal_new != prinHashVal_old) {
            		return false;
            	}
            }
        }
        return true;
    }
    
    public boolean isCompatible(HashMap<Integer, AssignmentInfo> receivedCPA,
    		HashMap<Integer, AssignmentInfo> myNewCPA, int child){
        for(Map.Entry<Integer, AssignmentInfo> entry : myNewCPA.entrySet()){
        	
        	
        	int prinHashVal_myNew = entry.getValue().getValue();
        	int ancestor = entry.getKey();
        	int rootChild = -1;
        	if(getProblem().hasAllInitialized()) {
            	HashMap<Integer, List<Integer>> childDescendMapForAncestor = 
            			getProblem().getChildDescendMap(ancestor);
            	for(int childOfAncesotr : getProblem().getChildren(ancestor)){
            		if(getId() == childOfAncesotr) {
            			rootChild = childOfAncesotr;
            		}
            		if(childDescendMapForAncestor.get(childOfAncesotr).contains(getId())) {
            			rootChild = childOfAncesotr;
            			break;
            		}
            	}
            	prinHashVal_myNew = getProblem().
            			getStrongPrincipalVarsHashValue(ancestor, rootChild, entry.getValue().getValue());
        	}

            if(receivedCPA.containsKey(entry.getKey())){
            	int prinHashVal_received = receivedCPA.get(entry.getKey()).getValue();
            	if(getProblem().hasAllInitialized()) {
            		prinHashVal_received = getProblem().
                			getStrongPrincipalVarsHashValue(ancestor, rootChild, 
                					receivedCPA.get(ancestor).getValue());
            	}
            	
            	if (prinHashVal_received != prinHashVal_myNew) {
            		return false;
            	}
            }
        }
        return true;
    }


    /**
     * 
     * @param p
     * @param dp
     * @param IDp
     * @param THp
     * @param fromChild: the child of p that is the root of the subtree containing the receiving agent
     */
    @WhenReceived("VALUE")
    public void handleVALUE(int p, int dp, int IDp, int THp){
    	if(debug){
    		System.out.println("*Processing VALUE* [" + getId() + "] receive from ["  + p + 
    				"] ID = " + IDp + " d = " + dp);
    	}
        //System.out.println("I am " + getId() + "VALUE received: " + "p = " + p + " dp = " + dp + " IDp = " + IDp + " THp = " + THp + ", mycpa: " + print(cpa));
        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(p,  dp, IDp, cpa);
        //System.out.println("_cpa : " + print(_cpa) + ", cpa: " + print(cpa));
        if(!isCompatible(_cpa, cpa)){
            //System.out.println("true1");
            for(int i = 0; i < tree.getChildren().size(); i++){
                //System.out.println("child: " + tree.getChildren().get(i) + ", CHILDSCA: " + getSCA(tree.getChildren().get(i)));
                if(getSCA(tree.getChildren().get(i)).contains(p))
                    for(int j = 0; j < getAgentDomainSize(); j++){
                        //System.out.println("true2");
                        InitChild(i, j);
                    }
            }
            InitSelf();
        }
        if(p == tree.getParent())
            TH = THp;
    }

    @WhenReceived("COST")
    public void handleCOST(int c, HashMap<Integer, AssignmentInfo>cCPA, int LBc, int UBc, boolean thReq){
    	if(debug){
    		System.out.println("*Processing COST* [" + getId() + "] receive from ["  + c + 
    				"] LBc = " + LBc + " UBc = " + UBc);
    	}
        

    	if(PlusOn){
    		receivedThReqs.put(c, thReq);
        }

        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(cCPA, cpa);

        if(!isCompatible(_cpa, cpa)){
            for(int i = 0; i < tree.getChildren().size(); i++){
                for(int j = 0; j < getAgentDomainSize(); j++){
                    HashMap<Integer, AssignmentInfo> tmpCPA = new HashMap<Integer, AssignmentInfo>();
                    for(Map.Entry<Integer, AssignmentInfo> entry : _cpa.entrySet()){
                        if(getSCA(tree.getChildren().get(i)).contains(entry.getKey()))
                            tmpCPA.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
                    }
                    if(!isCompatible(tmpCPA, cpa))
                        InitChild(i, j);
                }
            }
        }
        if(isCompatible(cCPA, cpa, c)){
        	// Not only update the bounds with the same value of in the received cost message
        	// But also update all the ones with the same values for pinciple variables
        	if(getProblem().hasAllInitialized()){
            	int d_received = cCPA.get(getId()).getValue();
            	int prinHashVal_received = getProblem().
            			getStrongPrincipalVarsHashValue(getId(), c, d_received);
            	for(int my_d = 0; my_d < getAgentDomainSize(); my_d++) {
            		int myPrinHash_d = getProblem().getStrongPrincipalVarsHashValue(getId(), c, my_d);
            		if (myPrinHash_d == prinHashVal_received) {
                        lbChildD[tree.getChildren().indexOf(c)][my_d] = 
                        		max(lbChildD[tree.getChildren().indexOf(c)][my_d], LBc);
                        ubChildD[tree.getChildren().indexOf(c)][my_d] = 
                        		min(ubChildD[tree.getChildren().indexOf(c)][my_d], UBc);
            		}
            	}
        	}
        	else{
                lbChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()] = max(lbChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()], LBc);
                ubChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()] = min(ubChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()], UBc);
        	}

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
            if(debug) {
            	System.out.println("#TERMINATE# from " + getId() +" to " + child);
            }
        }
        finish(d);
    }
}


