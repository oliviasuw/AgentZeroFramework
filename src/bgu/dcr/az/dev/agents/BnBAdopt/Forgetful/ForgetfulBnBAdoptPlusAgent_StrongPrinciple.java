/**
 * ForgetfulBnBAdoptPlusAgent_StrongPrinciple.java
   Created by Su Wen
   Date: May 27, 2015
   Time: 3:42:42 PM 
 */

package bgu.dcr.az.dev.agents.BnBAdopt.Forgetful;
import bgu.dcr.az.api.Continuation;
import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.DFSPsaudoTree;
import bgu.dcr.az.dev.tools.AssignmentInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import confs.defs;


/**
 * Not complete yet
 * @author Su Wen
 *
 */
@Algorithm(name="ForgetfulBnBAdoptPlusAgent_StrongPrinciple", useIdleDetector=true)
public class ForgetfulBnBAdoptPlusAgent_StrongPrinciple extends SimpleAgent {
	// for debug
	boolean debug = false;
	HashMap<Integer,Integer> concernedVariables = new HashMap(){
		{
		put(0,1); 
		put(1,1);
		put(2,1);
		put(3,1);
		put(4,1);
		put(5,1);
		put(6,1); 
		put(7,1);
		put(8,1);
		put(9,1);
		put(10,1);
		put(11,1);
		put(12,1);
		put(13,1);
		put(14,1);
		put(15,1);
		}
	};
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

	/** To record the so far best d under current CPA**/
	class BestInfo{
		int d;
		int lb;
		int ub;
	}
	private BestInfo bestInfo = new BestInfo();
	private boolean FinshingSearchingCurrentCAP = false;

    HashMap<Integer, AssignmentInfo> cpa = new HashMap<>();
    private int ID;

    private int[] lbChildD;
    private int[] LBD;
    private int LB;
    private int[] ubChildD;
    private int[] UBD;
    private int UB;
    private Set<Integer> SCA = new HashSet<>();
    private int d;
    private int TH;
    DFSPsaudoTree tree;

    @Override
    public void start() {

        tree = new DFSPsaudoTree();
        tree.calculate(this).andWhenDoneDo(new Continuation() {
            @Override
            public void doContinue () {
                if(!tree.isRoot()){
                    SCA = getSCA();
                    for(int ancestor : SCA){
                        cpa.put(ancestor, new AssignmentInfo(0, 0));
                    }
                }
                                
                ID = 0;
                lbChildD = new int[tree.getChildren().size()];
                ubChildD = new int[tree.getChildren().size()];
                for(int i = 0; i < tree.getChildren().size(); i++){
                	InitChild(i);
                }
                InitSelf();
                backtrack();
                
            	File file = new File("costs.txt");
                FileWriter fileWriter = null;
      		    try {
      			    fileWriter = new FileWriter(file, true);
      			    fileWriter.write("agentID: "+ getId()+" depth: "+tree.getDepth() + "\n");
      			    fileWriter.close();
      		    } catch (IOException e1) {
      			// TODO Auto-generated catch block
      			      e1.printStackTrace();
      		}
                
                
            }
        });
    }

    @Override
    public void onIdleDetected() {
        backtrack();
    }

    public Set<Integer> getSCA(){
        Set<Integer> _SCA = new HashSet<>();
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
        Set<Integer> _SCA = new HashSet<>();
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

    public void InitChild(int child){
        lbChildD[child] = 0;
        ubChildD[child] = defs.MAX_VALUE;
    }
    
    public void InitSoFarBest(){
    	if(debug){
    		System.out.println("#"+getId()+"#"+"Init current best!");
    	}
    	
    	bestInfo.d = d;
    	bestInfo.lb = calcDelta(d);
    	bestInfo.ub = defs.MAX_VALUE;
    	FinshingSearchingCurrentCAP = false;
    }

    public int firstVal(){
    	
    	int minDelta = defs.MAX_VALUE;
    	int minDelta_d = -1;
    	for(int val = 0; val < getAgentDomainSize(); val++){
    		if(minDelta > calcDelta(val)){
    			minDelta = calcDelta(val);
    			minDelta_d = val;
    		}
    		
    	}
    	
    	return minDelta_d;
    }
    public int nextVal(){
    	if(!FinshingSearchingCurrentCAP){
        	int currentDelta = calcDelta(d);
        	int nextMinDelta = defs.MAX_VALUE;
        	int nextVal = -1;
        	for(int val = 0; val < getAgentDomainSize(); val++){
        		int delta = calcDelta(val);
        		if(delta < currentDelta || (delta ==currentDelta && val <= d)){
        			continue;
        		}
        		else{
            		if(nextMinDelta > delta) {
            			nextMinDelta = delta;
            			nextVal = val;
            		}
        		}


        	}
        	return nextVal;	
    	}
    	else{
    		return -1;
    	}

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
//        int min = Integer.MAX_VALUE;
//        for(int value : getAgentDomain()){
//            if(min > calcDelta(value) + sumlbORub(lbChildD, value)){
//                min = calcDelta(value) + sumlbORub(lbChildD, value);
//                d = value;
//            }
//        }
//    	d = nextVal();
//        ID++;
    	getProblem().setStrongPrincipalVariables(getId(), identifyPrincipalVars());
    	getProblem().setWeakPrincipalVariables(getId(), identifyWeakPrincipalVars());
    	getProblem().setChildren(getId(), tree.getChildren());
    	HashMap<Integer, List<Integer>> childDescendants = new HashMap();
    	for(int child : tree.getChildren()) {
    		childDescendants.put(child, tree.getChildDescendants(child));  		
    	}
    	getProblem().setChildDescendMap(getId(), childDescendants);
    	
    	getProblem().setAgentInitialized(getId());
    	
    	
    	d = firstVal();
    	InitSoFarBest();
    	ID ++;
        TH = defs.MAX_VALUE;
        if(PlusOn){
			myThReq = true;  // Always false to disable this function by Suwen
		}
    }

    public int positivInfinityPlus(int a, int b){
        if(a + b < 0 || a + b >= defs.MAX_VALUE)
            return defs.MAX_VALUE;
        return a + b;
    }

    public int sumlbORub(int[] arrayList){
        int sum = 0;
        for(int i = 0; i < tree.getChildren().size(); i++)
            sum = positivInfinityPlus(sum, arrayList[i]);
        return sum;
    }

    public int calcDelta(int val){
        int delta = 0;
        //Olivia added
        // unary cost
        delta += getAgentConstraintCost(getId(), val);
        if(tree.isRoot())
            return delta;
        
        // binary cost
        for(int ancestor : SCA){
            delta += getAgentConstraintCost(getId(), val, ancestor, cpa.get(ancestor).getValue());
        }
        return delta;
    }

    private void backtrack(){
    	//debug
    	if(debug  && concernedVariables.containsKey(getId())) {
    		System.out.println("In variable #" + getId() + "# bestD:"+bestInfo.d
    				+"  bestLB: " + bestInfo.lb
    				+" bestUB: " + bestInfo.ub
    				+" LB:" + LB
    				+" UB:" + UB);
    	}
    	
    	
    	int current_d = d;
    	int currentLB = positivInfinityPlus(calcDelta(d), sumlbORub(lbChildD));
    	int currentUB = positivInfinityPlus(calcDelta(d), sumlbORub(ubChildD));
    	if(1 == compareDeltas(bestInfo.ub, currentUB)){
    		bestInfo.lb = currentLB;
    		bestInfo.ub = currentUB;
    		bestInfo.d = d;
    	}
    	if(0 == compareDeltas(bestInfo.ub, currentUB) && 1 == compareDeltas(currentLB, bestInfo.lb)){
    		bestInfo.lb = currentLB;
    		bestInfo.ub = currentUB;
    		bestInfo.d = d;
    	}
    	
    	int nextCandidate_d = nextVal();
    	int nextDelta = defs.MAX_VALUE;
    	if(-1 != nextCandidate_d) {
    		nextDelta = calcDelta(nextCandidate_d);
    	}
    	
    	LB = min(currentLB, bestInfo.lb, nextDelta);
    	UB = min(currentUB, bestInfo.ub);
    	
    	if(currentLB >= UB){
    		if(debug){
    			if(tree.isRoot()){
    				System.out.println("Root with value " + d +"LB:" + currentLB + "UB: " +currentUB);
    			}
    		}

        	if(-1 == nextCandidate_d
        			|| 0 == compareDeltas(calcDelta(nextCandidate_d), bestInfo.ub) 
        			|| 1 == compareDeltas(calcDelta(nextCandidate_d), bestInfo.ub)) {
        		FinshingSearchingCurrentCAP = true;
        		d = bestInfo.d;
        		LB = bestInfo.lb;
        		UB = bestInfo.ub;
        	}
        	else{
        		d = nextCandidate_d;

        		LB = min(calcDelta(nextCandidate_d), bestInfo.lb);
        		if(0 == tree.getChildren().size()) {
        			currentUB = calcDelta(nextCandidate_d);
        		}
        		else{
        			currentUB = defs.MAX_VALUE;
        		}
        		UB = min(currentUB, bestInfo.ub);
        		
        	}

    	}


    	if( d != current_d){
    		ID ++;
    		for(int i = 0; i < tree.getChildren().size(); i++){
    			InitChild(i);
    		}
    	}

        if((tree.isRoot() && UB == LB)){
            for(int child : tree.getChildren()){
                send("TERMINATE").to(child);
            }
            
            System.out.println("############ Minimum cost: " + UB+" ################");
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
                    send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD)
                            + lbChildD[tree.getChildren().indexOf(child)]).to(child);
                    receivedThReqs.put(child, false);
                    if(debug && concernedVariables.containsKey(getId())){
                    	System.out.println("#VALUE# from #" + getId() + "# to #" 
                    + child + "# [d: " + d +"] [ID: " + ID +"]");
                    }
                }
                lastSentPrinHashVALUEs.put(child, currentVal);
                
            }
            else {
                send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD)
                        + lbChildD[tree.getChildren().indexOf(child)]).to(child);
                
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
                    if(debug && concernedVariables.containsKey(getId())){
                    	System.out.println("#VALUE# from #" + getId() + "# to #" 
                    + pseudoChild + "# [d: " + d +"] [ID: " + ID +"]");
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
                    if(debug && concernedVariables.containsKey(getId())){
                    	System.out.println("#COST# from #" + getId() + "# to #" + tree.getParent()
                    			+"# [LB: " + LB +"] [UB: " + UB +"]");
                    	System.out.println("#cpa:#");
                    	for(int key : cpa.keySet()){
                    		System.out.println("var: " + key + " val: " + cpa.get(key).value + " ID: "+ 
                    	cpa.get(key).ID);
                    	}
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
        int min = defs.MAX_VALUE;
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
        HashMap<Integer, AssignmentInfo> _cpaHashMap = new HashMap<>();
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa.entrySet()){
            _cpaHashMap.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
        }
        return _cpaHashMap;
    }

    public void priorityMerge(int p, int dp, int IDp, HashMap<Integer, AssignmentInfo> _cpa) {
        if(_cpa.containsKey(p) && IDp > _cpa.get(p).getID()){
            if(PlusOn){
            	int prinHashVal_dp = dp;
            	int prinHashVal_cpa = _cpa.get(p).getValue();
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
            if(_cpa.containsKey(entry.getKey()) && entry.getValue().getID() > _cpa.get(entry.getKey()).getID()){
                if(PlusOn){
                	int hisAgentID = entry.getKey();
                	int prinHashVal_received = entry.getValue().getValue();
                	int prinHashVal_old = _cpa.get(entry.getKey()).getValue();
                	if(prinHashVal_received != prinHashVal_old){
                        this.cpaChanged = true;
                    }
                }
            	_cpa.remove(entry.getKey());
                _cpa.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
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

    @WhenReceived("VALUE")
    public void handleVALUE(int p, int dp, int IDp, int THp){
    	if(debug && concernedVariables.containsKey(getId())){
    		System.out.println("*Processing VALUE in #" + getId() + "# receive from #"  + p + 
    				"# ID = " + IDp + " d = " + dp);
    	}
    	
        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(p,  dp, IDp, cpa);
        if(!isCompatible(_cpa, cpa)){
        	int old_d = d;
            InitSelf();
            for(int i = 0; i < tree.getChildren().size(); i++){
                if(getSCA(tree.getChildren().get(i)).contains(p) || old_d != d)
                	InitChild(i);
//                    for(int j = 0; j < getAgentDomainSize(); j++){
//                        InitChild(i, j);
//                    }
            }

        }
        if(p == tree.getParent())
            TH = THp;
    }

    @WhenReceived("COST")
    public void handleCOST(int c, HashMap<Integer, AssignmentInfo>cCPA, int LBc, int UBc, 
    		boolean thReq){
    	// && getId()==9 && cCPA.get(5).getValue() == 1
    	if(debug && concernedVariables.containsKey(getId())){
    		System.out.println("*Processing COST in #" + getId() + "# receive from #"  + c + 
    				"# LBc = " + LBc + " UBc = " + UBc);
    	}
    	
    	if(PlusOn){
    		receivedThReqs.put(c, thReq);
        }
    	
    	HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(cCPA, cpa);
        if(!isCompatible(_cpa, cpa)){
        	int old_d = d;
        	InitSelf();
            for(int i = 0; i < tree.getChildren().size(); i++){
//                for(int j = 0; j < getAgentDomainSize(); j++){
                    HashMap<Integer, AssignmentInfo> tmpCPA = new HashMap<Integer, AssignmentInfo>();
                    for(Map.Entry<Integer, AssignmentInfo> entry : _cpa.entrySet()){
                        if(getSCA(tree.getChildren().get(i)).contains(entry.getKey()))
                            tmpCPA.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
                    }
                    if(!isCompatible(tmpCPA, cpa) || old_d != d)
                        InitChild(i);
//                }
            }
        }
        if(isCompatible(cCPA, cpa, c)){
        	if(cCPA.get(getId()).getValue() == d){
            	// Not only update the bounds with the same value of in the received cost message
            	// But also update all the ones with the same values for pinciple variables
            	if(getProblem().hasAllInitialized()){
                	int d_received = cCPA.get(getId()).getValue();
                	int prinHashVal_received = getProblem().
                			getStrongPrincipalVarsHashValue(getId(), c, d_received);
                	for(int my_d = 0; my_d < getAgentDomainSize(); my_d++) {
                		int myPrinHash_d = getProblem().getStrongPrincipalVarsHashValue(getId(), c, my_d);
                		if (myPrinHash_d == prinHashVal_received) {
                            lbChildD[tree.getChildren().indexOf(c)] = 
                            		max(lbChildD[tree.getChildren().indexOf(c)], LBc);
                            ubChildD[tree.getChildren().indexOf(c)] = 
                            		min(ubChildD[tree.getChildren().indexOf(c)], UBc);
                		}
                	}
            	}
            	else{
            		lbChildD[tree.getChildren().indexOf(c)] = max(lbChildD[tree.getChildren().indexOf(c)], LBc);
                    ubChildD[tree.getChildren().indexOf(c)] = min(ubChildD[tree.getChildren().indexOf(c)], UBc);
            	}
        		
        	}
        }

            
            

            
            
//        if(!isCompatible(_cpa, cpa))
//            InitSelf();
    }

    @WhenReceived("TERMINATE")
    public void handleTERMINATE(){
        for(int child : tree.getChildren()){
            send("TERMINATE").to(child);
        }
        finish();
    }
    
    /** Static Methods **/
	/* d1 > d2 -> return 1
	 * d1 > d2  -> return -1
	 * d1 = d2  -> return 0*/
	public static double compareDeltas(double d1, double d2){

		if((d1 == defs.MAX_VALUE) &&
				(d2 != defs.MAX_VALUE)) {
			/* d1 > d2 */
			return 1;
		} else if((d1 != defs.MAX_VALUE) &&
				(d2 == defs.MAX_VALUE)) {
			/* d2 < d1 */
			return -1;
		} else if(d1 == d2) {
			return 0;
		} else{
			if(d1 > d2) {
				return 1;
			} else {
				return -1;
			}
		}
	}
}




