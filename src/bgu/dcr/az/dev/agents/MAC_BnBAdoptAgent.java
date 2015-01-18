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
import bgu.dcr.az.dev.agents.MACConstruct.BinaryConstraint;
import bgu.dcr.az.dev.tools.AssignmentInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Algorithm(name="BnBAdoptPlusWithMAC", useIdleDetector=true)   // Corresponds to the algorithm name in the .xml file
public class MAC_BnBAdoptAgent extends SimpleAgent {

	boolean debug = true;
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

    /** Structures for MAC Enforcement**/
    boolean ACOn = true;
    int ACEnforceOrder = -1;  // -1 : first bottom to top;  1 : first top to bottom
    final static int ProjectToMe = 1;
    final static int ProjectFromMe = 2;
    boolean ACPreprocessFlag = true;
    
    final static int startPoint = 0;
    int myDepth;
    int copyNum;
    public MACConstruct[] myMACConstruct_N;
    double [] subtreeContribution_N;
    Integer[] ac_rootvar_N;
    Integer[] ac_rootvalue_N;
//    AssignmentInfo[] MAC_root_cpa;
//    AssignmentInfo[] MAC_cpa;
    HashMap<Integer, AssignmentInfo> MAC_cpa;
    boolean[] ACEnforcementNeed = false;
    HashMap<Integer, Integer> ancestor_depth_map;
    // flag for pending BTK
    boolean[] pending_BTK;
    
    
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
            		initMACConstruct();
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
    
    public void initMACConstruct(){
    	myDepth = tree.getDepth();
    	copyNum = myDepth + 1;
    	
    	myMACConstruct_N = new MACConstruct[copyNum];
    	subtreeContribution_N = new double[copyNum];
    	ac_rootvar_N = new Integer[copyNum];
    	ac_rootvalue_N = new Integer[copyNum];    	
    	ACEnforcementNeed = new boolean[copyNum];
    	pending_BTK = new boolean[copyNum];
    	
    	for(int i = 0; i < copyNum; i++){
    		MACConstruct macConstruct = new MACConstruct(this, i);
    		myMACConstruct_N[i] = macConstruct;
    		subtreeContribution_N[i] = 0;
    		ac_rootvar_N[i] = -1;
    		ac_rootvalue_N[i] = -1;		 
    		ACEnforcementNeed[i] = false;
    		pending_BTK[i] = false;
    	}
    	
    	MAC_cpa = new HashMap();
		AssignmentInfo initInfo = new AssignmentInfo(0,0); 
		MAC_cpa.put(getId(), initInfo);
    	for(int ancestor : tree.getAncestors()){
    		initInfo = new AssignmentInfo(0,0); 
    		MAC_cpa.put(ancestor, initInfo);
    	}
    	
    	ancestor_depth_map = new HashMap();
    	
    	for(int i = 0; i < tree.getAncestors().size(); i++){
    		int ancestorID = tree.getAncestors().get(i);
    		int ancestorDepth = tree.getAncestors().size() - i - 1;
    		ancestor_depth_map.put(ancestorID, ancestorDepth);
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
    	if(getId()==2){
    		System.out.println("debug!");
    	}
        for(int i = 0; i < getDomainSize(); i++){
            LBD[i] = calcDelta(i) + sumlbORub(lbChildD, i);
            UBD[i] = positivInfinityPlus(calcDelta(i), sumlbORub(ubChildD, i));
        }
        LB = findMinimum(LBD, 1, 0);
        UB = findMinimum(UBD, 1, 0);
        
        int last_d = d;

        if(LBD[d] >= min(TH, UB) || myMACConstruct_N[startPoint].pruned[d]){
            int bestVal = findMinimum(LBD, 2, d);
            if(bestVal == -1){
            	System.out.println("All the values are pruned in varible " + getId());
            	System.exit(-1);
            }
            d = bestVal;
            ID++;
            
            //MAC: update the context(including myself assign) for MAC
            if(!tree.isLeaf()){
            	AssignmentInfo newAssign = new AssignmentInfo(d, ID);
            	MAC_cpa.put(getId(), newAssign);
            }
        }
        
        if(d != last_d){
        	ACEnforcementNeed[startPoint] = true;
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
        	double gc = subtreeContribution_N[startPoint] + myMACConstruct_N[startPoint].myContribution;
        	if(gc > myMACConstruct_N[startPoint].global_cPhi) {
        		myMACConstruct_N[startPoint].global_cPhi = gc;
        		ACEnforcementNeed[startPoint] = true;
        	}
        	
        	if(UB < myMACConstruct_N[startPoint].global_top) {
        		myMACConstruct_N[startPoint].global_top = UB;
        		ACEnforcementNeed[startPoint] = true;
        	}
        	
        	if(LB < myMACConstruct_N[startPoint].global_cPhi){
        		LB = (int)myMACConstruct_N[startPoint].global_cPhi;
        		ACEnforcementNeed[startPoint] = true;
        	}
    	}
    	else{
            // MAC: depth d var has to handle cphi[d]
            double gcN = subtreeContribution_N[myDepth] + myMACConstruct_N[myDepth].myContribution;
            if (gcN > myMACConstruct_N[myDepth].global_cPhi) {
            	myMACConstruct_N[myDepth].global_cPhi = gcN;
            	ACEnforcementNeed[myDepth] = true;
            }
    	}
    	
    	//MAC
    	double[] globalCphi_N = new double[copyNum];
    	double[] globalTop_N = new double[copyNum];
    	double[] subtreeContri_N = new double[copyNum];
    	for(int i = 0 ; i < copyNum; i++){
    		globalCphi_N[i] = myMACConstruct_N[i].global_cPhi;
    		globalTop_N[i] = myMACConstruct_N[i].global_top;
    		subtreeContri_N[i] = subtreeContribution_N[i] + myMACConstruct_N[i].myContribution;
    	}
    	
    	
        for(int child : tree.getChildren()){
            if(PlusOn) {
//                if(!lastSentVALUEs.containsKey(child) || lastSentVALUEs.get(child) != d
//                		|| ThReq){
                if(!lastSentVALUEs.containsKey(child) || lastSentVALUEs.get(child) != d){
                	
                    send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                            + lbChildD[tree.getChildren().indexOf(child)][d],
                            globalCphi_N, globalTop_N, myDepth, MAC_cpa).to(child);
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
                        globalCphi_N, globalTop_N, myDepth, MAC_cpa).to(child);
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
                    		globalCphi_N, globalTop_N, myDepth, MAC_cpa).to(pseudoChild);
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
                		globalCphi_N, globalTop_N, myDepth, MAC_cpa).to(pseudoChild);
                //Debug
                if(debug){
                	System.out.println("VALUE: " + getId() +
                			" [" + d + "] to " + pseudoChild);
                }
                
            }
        }

        if(!tree.isRoot()){
            /** AC enforcement **/
            for(int i = 0; i < copyNum; i++){
                subtreeContribution_N[i] = 0;
                myMACConstruct_N[i].myContribution = 0;
            }
            
            if(PlusOn){
                if(lastSentCPA.isEmpty()
                        || lastSentLB != LB
                        || lastSentUB != UB
                        || !lastSentCPA.equals(cpa)
                        || cpaChanged){
                	
                    cpaChanged = false;
                    
                    //debug
                    if(getId()==3 && LB==UB && LB ==22){
                    	System.out.println("debug");
                    }

                    if(tree.getParent() != -1){
                    	// Suwen delted ThReq
//                        send("COST", getId(), cpa, LB, UB, ThReq, myMACConstruct_N.global_cPhi,
//                        		myMACConstruct_N.global_top, subtreeContri).to(tree.getParent());
                        send("COST", getId(), cpa, LB, UB, 
                        		globalCphi_N, globalTop_N, subtreeContri_N,
                        		myDepth, MAC_cpa).to(tree.getParent());
                        //Debug
                        if(debug){
                        	System.out.println("COST: " + getId() +
                        			" to " + tree.getParent() + " LB: "
                        			+LB + " UB: " + UB);
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
                // Suwen deleted
//                send("COST", getId(), cpa, LB, UB, ThReq, myMACConstruct_N.global_cPhi,
//                		myMACConstruct_N.global_top, subtreeContri).to(tree.getParent());
                send("COST", getId(), cpa, LB, UB,
                		globalCphi_N, globalTop_N, subtreeContri_N,
                		myDepth, MAC_cpa).to(tree.getParent());
                //Debug
                if(debug){
                	System.out.println("COST: " + getId() +
                			" to " + tree.getParent() + " LB: "
                			+LB + " UB: " + UB);
                }
            }
        }

        //MAC Enforcement
        performPruneDomainSelf();
        // AC Enforcement
//        if(ACEnforcementNeed[0]){
//        	checkDomainForDeletions();
//        	ACEnforcementNeed[0] = false;
//        }
    }

    private int findMinimum(int[] array, int flag, int initValue){     
    	//flag = 1, return minumum,  else, return minimum index
        int min = Integer.MAX_VALUE;
        int index = initValue;
        
        if(myMACConstruct_N[startPoint].pruned[index]){
        	index = -1;
        }
        
        for(int i = 0; i < array.length; i++){
        	/* AC enforcement*/
        	boolean pruned = false;
        	for(int depth = 0; depth < copyNum; depth ++){ //test 
        		if(myDepth >= depth && myMACConstruct_N[depth].pruned[i]){
        			pruned = true;
        			break;
        		}
        	}
        	if(pruned)
        		continue;

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

//    send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
//            + lbChildD[tree.getChildren().indexOf(child)][d],
//            myMACConstruct_N.global_cPhi, myMACConstruct_N.global_top).to(child);
    @WhenReceived("VALUE")
    public void handleVALUE(int p, int dp, int IDp, int THp, double[] global_cPhi, 
    		double[] global_top, int srcAgentDepth, HashMap<Integer, AssignmentInfo> src_cpa
//    		,MACConstruct[] srcMACConstruct_N
    		){
        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(p,  dp, IDp, cpa);
        //System.out.println("_cpa : " + print(_cpa) + ", cpa: " + print(cpa));
        if(!isCompatible(_cpa, cpa)){
            for(int i = 0; i < tree.getChildren().size(); i++){              
                if(getSCA(tree.getChildren().get(i)).contains(p))
                    for(int j = 0; j < getDomainSize(); j++){
                        InitChild(i, j);
                    }
            }
            InitSelf();
        }
        if(p == tree.getParent())
            TH = THp;
        
        //AC Enforcement
        if(global_cPhi[startPoint] > myMACConstruct_N[startPoint].global_cPhi){
        	myMACConstruct_N[startPoint].global_cPhi = global_cPhi[startPoint];
        	ACEnforcementNeed[startPoint] = true;
        }
        if(global_top[0] < myMACConstruct_N[startPoint].global_top){
        	myMACConstruct_N[startPoint].global_top = global_top[startPoint];
        	ACEnforcementNeed[startPoint] = true;
        }
       
        // MAC Enforcement
        Reinitialize(p, srcAgentDepth, src_cpa);
        if(global_top[startPoint] < UB){
        	UB = (int) global_top[startPoint];
        } 
        
        boolean compatible = true;
        for(int depth = 0; depth <= srcAgentDepth; depth++){
        	compatible = true;
        	for(int i = 0; i < depth; i++){
        		int ancestorIndex = tree.getAncestors().size() - i - 1;
        		int ancestor = tree.getAncestors().get(ancestorIndex);
        		int valueInMe = MAC_cpa.get(ancestor).value;
        		int valueInSrc = src_cpa.get(ancestor).value;
        		if(valueInMe != valueInSrc){
        			compatible = false;
        			break;
        		}
        	}
        	if(!compatible){
        		break;
        	}
        	
        	if(global_cPhi[depth] > myMACConstruct_N[depth].global_cPhi){
        		myMACConstruct_N[depth].global_cPhi = global_cPhi[depth];
        		ACEnforcementNeed[depth] = true;
        	}
        	
        }
        
    }

    /**
     * 
     * @param c
     * @param cCPA
     * @param LBc
     * @param UBc
     * @param global_cPhi
     * @param global_top
     * @param subtreeContri
     * @param srcAgentDepth
     * @param src_cpa
     */
    @WhenReceived("COST")
    public void handleCOST(int c, HashMap<Integer, AssignmentInfo>cCPA, int LBc, 
        		int UBc, double[] global_cPhi, double[] global_top, double[] subtreeContri,
        		int srcAgentDepth, HashMap<Integer, AssignmentInfo> src_cpa){        
        /** AC Enforcement **/
        subtreeContribution_N[startPoint] += subtreeContri[startPoint];
        if(!tree.isRoot()){
        	myMACConstruct_N[startPoint].global_cPhi += subtreeContri[startPoint];
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
        
        // MAC Enforcement
        Reinitialize(c, srcAgentDepth, src_cpa);
        boolean compatible = true;
        for(int depth = 0; depth <= myDepth; depth++){
        	compatible = true;
        	for(int i = 0; i < depth; i++){
        		int ancestorIndex = tree.getAncestors().size() - i - 1;
        		int ancestor = tree.getAncestors().get(ancestorIndex);
        		int valueInMe = MAC_cpa.get(ancestor).value;
        		int valueInSrc = src_cpa.get(ancestor).value;
        		if(valueInMe != valueInSrc){
        			compatible = false;
        			break;
        		}
        	}
        	if(!compatible){
        		break;
        	}
        	
        	subtreeContribution_N[depth] += subtreeContri[depth];
        	myMACConstruct_N[depth].global_cPhi += subtreeContri[depth];
        	
        }
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
				binaryProjection(ProjectToMe, pseudoParent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectFromMe, pseudoParent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
			}
			
			int parent = tree.getParent();
			if(-1 != parent){
				binaryProjection(ProjectToMe, parent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectFromMe, parent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
			}


			// Projection on self and children/pseudo-children
			for(int child : tree.getChildren()){
				binaryProjection(ProjectFromMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectToMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
			}
			for(int child : tree.getPsaudoChildren()){
				binaryProjection(ProjectFromMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectToMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
			}
		}
		
		/**
		 * default one. (Same with the one shown in Kobi's thesis P38)
		 */
		if(-1 == ACEnforceOrder) {
			// Projection on self and parents/pseudo-parents
			for(int pseudoParent : tree.getPsaudoParents()){
				binaryProjection(ProjectFromMe, pseudoParent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectToMe, pseudoParent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
			}
			int parent = tree.getParent();
			if(-1 != parent){
				binaryProjection(ProjectFromMe, parent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectToMe, parent, false, false, startPoint);
				checkDomainForDeletions(startPoint);
			}


			// Projection on self and children/pseudo-children
			for(int child : tree.getChildren()){
				binaryProjection(ProjectToMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectFromMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
			}
			for(int child : tree.getPsaudoChildren()){
				binaryProjection(ProjectToMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
				binaryProjection(ProjectFromMe, child, true, false, startPoint);
				checkDomainForDeletions(startPoint);
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
    		boolean needRecord, int copyIndex){
    	/* To record this projection in the stack? True: record False: don't record */
    	boolean recoverFlag = ProjectFromMe == projectDirection && 
    			((!isLowerNeighbor && -1 == ACEnforceOrder) 
    					|| (isLowerNeighbor && 1 == ACEnforceOrder) );;

    	int neighborIndex = myMACConstruct_N[copyIndex].getNeighborIndex(neighbor);
    	
        if (!ACPreprocessFlag && recoverFlag && needRecord) {
        	myMACConstruct_N[copyIndex].P_records[neighborIndex][myMACConstruct_N[copyIndex].ACRecordsProjectFromMe
        	         [neighborIndex]] = new Double[getDomainSize()]; 
        }
    	
    	
    	double alpha = Double.MAX_VALUE;
    	double cost = 0;  	
    	if(ProjectToMe == projectDirection){
    		for(int myVal : this.getDomain()){
    			alpha = myMACConstruct_N[copyIndex].checkAlphaProjectToMe(getId(), myVal, neighbor);
    			if(0 < alpha){
    				myMACConstruct_N[copyIndex].updateCostsWhenProjectToMe(getId(), myVal, neighbor, 
    						alpha);
    			}	
    		}

    	}
    	if(ProjectFromMe == projectDirection){
    		for(int hisVal : this.getDomainOf(neighbor)){
    			alpha = myMACConstruct_N[copyIndex].checkAlphaProjectToNeighbor(getId(), neighbor, 
    					hisVal);
    			if(!ACPreprocessFlag && recoverFlag && needRecord){
    				if (-1 == neighborIndex){
    					System.exit(-1);
    				}
    				
    				myMACConstruct_N[copyIndex].P_records[neighborIndex][myMACConstruct_N[copyIndex].ACRecordsProjectFromMe[neighborIndex]][hisVal] = alpha;
    			}
    			if(0 < alpha){
    				myMACConstruct_N[copyIndex].updateCostsWhenProjectToNeighbor(getId(), neighbor,
    						hisVal, alpha);
    			}
    		}
    	}
    	
    	if(!ACPreprocessFlag &&  recoverFlag){
    		myMACConstruct_N[copyIndex].ACRecordsProjectFromMe[neighborIndex] ++;
    	}
    	
    }
    
    void checkDomainForDeletions(int copyIndex){
    	Vector<Integer> valuesToDelete = new Vector();
    	Vector<Double> unaryCostsBackup = new Vector();
    	double cPhi =  myMACConstruct_N[copyIndex].global_cPhi;
    	for(int myVal : this.getDomain()){
    		double cSelf = myMACConstruct_N[copyIndex].unaryCosts[myVal];
    		if(cSelf + cPhi > myMACConstruct_N[copyIndex].global_top
    				&& !myMACConstruct_N[copyIndex].pruned[myVal]){
    			valuesToDelete.add(myVal);
    			myMACConstruct_N[copyIndex].pruned[myVal] = true;
    			
    			// SuWen Debug
    			System.out.println("Current my top:" + myMACConstruct_N[copyIndex].global_top +
    					"\tCurrent my cPhi:"+cPhi + "\tUnaryCost of var["+
    					myVal + "]:"+cSelf);
    			System.out.println("["+ myVal + "] pruned from variable " + getId());
    			
    			unaryCostsBackup.add(myMACConstruct_N[copyIndex].unaryCosts[myVal]);
    			myMACConstruct_N[copyIndex].unaryCosts[myVal] = Double.MAX_VALUE;
    		}
    	}
    	
    	boolean empty_domain_flag = false;
    	if(0 < valuesToDelete.size()){
    		empty_domain_flag = DeleteValues(copyIndex, valuesToDelete, unaryCostsBackup);
    	}
    	
    	//Transfer the deletion to all the copies with larger depth
    	if(copyIndex < myDepth){
    		for(int currentDepth = copyIndex + 1; currentDepth <= myDepth; currentDepth ++){
    			MACConstruct ac = myMACConstruct_N[currentDepth];
    			if(!empty_domain_flag && valuesToDelete.size() > 0){
    				for(int i = 0; i < valuesToDelete.size(); i++){
    					int val = valuesToDelete.get(i);
    					ac.pruned[val] = true;
    					ac.unaryCosts[val] = Double.MAX_VALUE;
    				}
    				empty_domain_flag = DeleteValues(currentDepth, valuesToDelete, unaryCostsBackup);
    				
    				if(empty_domain_flag){
    					break;
    				}
    			}
    			else{
    				break;
    			}
    		}
    	}
    	
    }
    
    boolean DeleteValues(int copyIndex, Vector<Integer> valuesToDelete, 
    		Vector<Double> unaryCostsBackup){
    	if(0 >= myMACConstruct_N[copyIndex].domainSizeAfterPruning()){
    		if(startPoint == copyIndex){
    			System.out.println("Trying to remove a value from a variable whose domain is empty!!");
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
                
    		}
    		
    		else{
    			
    			// Rollback the pruned value
    			for(int i = 0; i < valuesToDelete.size(); i++){
    				int val = valuesToDelete.get(i);
    				myMACConstruct_N[copyIndex].pruned[val] = false;
    				myMACConstruct_N[copyIndex].unaryCosts[val] = unaryCostsBackup.get(i);
    			}
    			
    			int targetDepth = myMACConstruct_N[copyIndex].depth;
    			HashMap<Integer, AssignmentInfo> context = new HashMap();
    			for(Map.Entry<Integer, AssignmentInfo> entry : MAC_cpa.entrySet()){
    				int ancestorID = entry.getKey();
    				int ancestorDepth = getAncestorDepth(ancestorID);
    				if(ancestorDepth < targetDepth){
    					int myVal = entry.getValue().value;
    					int myID = entry.getValue().ID;
    					AssignmentInfo newAssign = new AssignmentInfo(myVal, myID);
    					context.put(ancestorID, newAssign);
    				}
    			}
    			
    			int parent = tree.getParent();
    			if(-1 != parent){
    				if(!pending_BTK[copyIndex]){
    					pending_BTK[copyIndex] = true;  // Set to be false to disable this function
    					send("BTK", getId(), targetDepth, context).to(parent);
    				}
    				
    			}
    			else{
        			System.out.println("The domain of root node is empty!!");
        			for(int child : tree.getChildren()){
                        send("TERMINATE").to(child);
                    }
    			}
    			
    		}
    		return false;
    	}
    	else{
    		int neighborIndex = -1;
    		int neighborDepth = -1;
    		
            for(int neighbor : tree.getPsaudoParents()){
                
                binaryProjection(ProjectFromMe, neighbor, false, true, copyIndex);
                
                neighborIndex = myMACConstruct_N[copyIndex].getNeighborIndex(neighbor);
                neighborDepth = getAncestorDepth(neighbor);
                Integer[] ACProjectToMeRecord = new Integer[neighborDepth + 1];
                for(int depth = copyIndex; depth <= neighborDepth; depth++){
                	ACProjectToMeRecord[depth] = myMACConstruct_N[copyIndex].
                			ACRecordsProjectToMe[neighborIndex];
                	myMACConstruct_N[copyIndex].ACRecordsProjectToMe[neighborIndex] = 0;
                }
                
                send("DEL", getId(), myDepth, copyIndex, valuesToDelete, 
                		myMACConstruct_N[startPoint].global_top, MAC_cpa,
                		ACProjectToMeRecord).to(neighbor);
                //Debug
                if(debug){
                	System.out.println("DEL " + valuesToDelete.size());
                	for(int val : valuesToDelete){
                		System.out.println("delete "+val+"from "+getId());
                	}
                }
            }
            int parent = tree.getParent();
            if(-1 != parent){
                binaryProjection(ProjectFromMe, parent, false, true, copyIndex);
                
                neighborIndex = myMACConstruct_N[copyIndex].getNeighborIndex(parent);
                neighborDepth = myDepth - 1;;
                Integer[] ACProjectToMeRecord = new Integer[neighborDepth + 1];
                for(int depth = copyIndex; depth <= neighborDepth; depth++){
                	ACProjectToMeRecord[depth] = myMACConstruct_N[copyIndex].
                			ACRecordsProjectToMe[neighborIndex];
                	myMACConstruct_N[copyIndex].ACRecordsProjectToMe[neighborIndex] = 0;
                }

                send("DEL", getId(), myDepth, copyIndex, valuesToDelete, 
                		myMACConstruct_N[startPoint].global_top, MAC_cpa,
                		ACProjectToMeRecord).to(parent);
            	
                //Debug
                if(debug){
                	System.out.println("DEL"+ valuesToDelete.size());
                	for(int val : valuesToDelete){
                		System.out.println("delete "+val+"from "+getId());
                	}
                }            

            }

            for(int neighbor : tree.getChildren()){
            	binaryProjection(ProjectFromMe, neighbor, true, true, copyIndex);
            	
                neighborIndex = myMACConstruct_N[copyIndex].getNeighborIndex(neighbor);
                Integer[] ACProjectToMeRecord = new Integer[myDepth + 1];
                for(int depth = copyIndex; depth <= myDepth; depth++){
                	ACProjectToMeRecord[depth] = myMACConstruct_N[copyIndex].
                			ACRecordsProjectToMe[neighborIndex];
                	myMACConstruct_N[copyIndex].ACRecordsProjectToMe[neighborIndex] = 0;
                }
                
                send("DEL", getId(), myDepth, copyIndex, valuesToDelete, 
                		myMACConstruct_N[startPoint].global_top, MAC_cpa,
                		ACProjectToMeRecord).to(neighbor);
                
            	
                //Debug
                if(debug){
                	System.out.println("DEL"+ valuesToDelete.size());
                	for(int val : valuesToDelete){
                		System.out.println("delete "+val+"from "+getId());
                	}
                }
                
            }
            for(int neighbor : tree.getPsaudoChildren()){
            	binaryProjection(ProjectFromMe, neighbor, true, true, copyIndex);
            	
                neighborIndex = myMACConstruct_N[copyIndex].getNeighborIndex(neighbor);
                Integer[] ACProjectToMeRecord = new Integer[myDepth + 1];
                for(int depth = copyIndex; depth <= myDepth; depth++){
                	ACProjectToMeRecord[depth] = myMACConstruct_N[copyIndex].
                			ACRecordsProjectToMe[neighborIndex];
                	myMACConstruct_N[copyIndex].ACRecordsProjectToMe[neighborIndex] = 0;
                }
                
                send("DEL", getId(), myDepth, copyIndex, valuesToDelete, 
                		myMACConstruct_N[startPoint].global_top, MAC_cpa,
                		ACProjectToMeRecord).to(neighbor);

                //Debug
                if(debug){
                	System.out.println("DEL"+ valuesToDelete.size());
                	for(int val : valuesToDelete){
                		System.out.println("delete "+val+"from "+getId());
                	}
                }
                
            }
            unaryProjection(copyIndex);
            return true;
    	}
    }
    
    /**
     * 
     */
    void unaryProjection(int copyIndex){
    	double alpha = Double.MAX_VALUE;
    	for(double unaryCost : myMACConstruct_N[copyIndex].unaryCosts){
    		if(unaryCost < alpha){
    			alpha = unaryCost;
    		}
    	}
    	if(0 < alpha){
    		myMACConstruct_N[copyIndex].myContribution += alpha;
    		if(!tree.isRoot()){
    			myMACConstruct_N[copyIndex].global_cPhi += alpha;
    		}
    		for(int i = 0; i < myMACConstruct_N[copyIndex].unaryCosts.length; i++){
    			myMACConstruct_N[copyIndex].unaryCosts[i] -= alpha;
    		}
    	}
    }
    
/**
 * 
 * @param srcAgent
 * @param srcAgentDepth
 * @param srcCopyIndex
 * @param valuesToDelete
 * @param globalTop
 * @param src_cpa
 * @param ACRecordsProjectToMe
 */
    @WhenReceived("DEL")
    public void handleDEL(int srcAgent, int srcAgentDepth, int srcCopyIndex, Vector<Integer> valuesToDelete,
    		double globalTop, HashMap<Integer, AssignmentInfo> src_cpa, Integer[] ACRecordsProjectToMe){
    	
        // MAC Enforcement
        Reinitialize(srcAgent, srcAgentDepth, src_cpa);
    	
        // update top and cphi ???? only appear in Kobi's implementation, not in her thesis
        for (int d = 0; d < copyNum; d++) {
            if (globalTop < myMACConstruct_N[d].global_top) {
            	myMACConstruct_N[d].global_top = globalTop;
                ACEnforcementNeed[d] = true;
            }
        }
        
    	boolean compatible = true;
    	for(int ancestor : MAC_cpa.keySet()){
    		int ancestorDepth = getAncestorDepth(ancestor);
    		if(ancestorDepth < srcCopyIndex){
        		AssignmentInfo assignInMe = MAC_cpa.get(ancestor);
        		AssignmentInfo assignInSrc = src_cpa.get(ancestor);
        		if(assignInMe != null && assignInSrc != null &&
        				assignInMe.value != assignInSrc.value){
        			compatible = false;
        		}
    		}	
    	}
    	
    	MACConstruct ac = new MACConstruct();
    	int srcAgentIndex = -1;
    	if(compatible){
    		int minDepth = getMin(srcAgentDepth, myDepth);
    		for(int depth = srcCopyIndex; depth <= minDepth; depth++){
    			ac = myMACConstruct_N[depth];
    			srcAgentIndex = myMACConstruct_N[depth].getNeighborIndex(srcAgent);
    			for(int val : valuesToDelete){
    				myMACConstruct_N[depth].neighborsPruned.get(srcAgentIndex)[val] = true;
    			}
    			
    			int neighborDomainAfterPrunning = myMACConstruct_N[depth]
    					.neighborDomainSizeAfterPruning(srcAgentIndex);
    			if(neighborDomainAfterPrunning == 0){
    				// Undo pruning
        			for(int val : valuesToDelete){
        				myMACConstruct_N[depth].neighborsPruned.get(srcAgentIndex)[val] = false;
        			}
    			}
    			else{
                    // do AC_one_way
    				if(myDepth > srcAgentDepth && -1 == ACEnforceOrder){
    					boolean hasUnDone = undoAC(depth, srcAgent, ACRecordsProjectToMe[depth]);
    					resetProjectionsFromMe(depth, srcAgentIndex);
    		    		binaryProjection(ProjectToMe, srcAgent, false, true, depth);
    		    		if(hasUnDone)
    		    			binaryProjection(ProjectFromMe, srcAgent, false, false, depth);
                        }
                    else {
                    	binaryProjection(ProjectToMe, srcAgent, true, false, depth);
                    	myMACConstruct_N[depth].ACRecordsProjectToMe[srcAgentIndex] ++;

                    }
    			}

    		}
    	}

//    	if(hisACConstruct.global_top < myMACConstruct_N.global_top){
//    		myMACConstruct_N.global_top = hisACConstruct.global_top;
//    		ACEnforcementNeed = true;
//    	}
//    	
//    	int srcAgentIndex = myMACConstruct_N.getNeighborIndex(srcAgent);
//    	for(int val : valuesToDelete){
//    		myMACConstruct_N.neighborsPruned.get(srcAgentIndex)[val] = true;
//    	}
    	


    	//added Oliva temp
//   if(!ACPreprocessFlag){ 	
//    	if(srcDepth < myDepth && -1 == ACEnforceOrder){
//    		boolean hasUnDone = undoAC(hisACConstruct);
//    		resetProjectionsFromMe(srcAgentIndex);
//    		binaryProjection(ProjectToMe, srcAgent, false, true);
//    		if(hasUnDone)
//    			binaryProjection(ProjectFromMe, srcAgent, false, false);
//    	}
//    	else{
//    		binaryProjection(ProjectToMe, srcAgent, true, false);
//    		myMACConstruct_N.ACRecordsProjectToMe[srcAgentIndex] ++;
//    	}
//   }
//   else{
//	   binaryProjection(ProjectToMe, srcAgent, true, true);
//   }
    }
    
    void resetProjectionsFromMe(int copyIndex, int neighborIndex){
    	myMACConstruct_N[copyIndex].ACRecordsProjectFromMe[neighborIndex] = 0;
    	myMACConstruct_N[copyIndex].P_records[neighborIndex] = new Double[myMACConstruct_N[copyIndex].
    	                                                       MAX_PROJECTION_NUM_RECORDED][];   	
    }
    
    boolean undoAC(int copyIndex, int srcAgent, int ACRecordsProjectToMeInSrc){
    	
    	int neighborIndex = myMACConstruct_N[copyIndex].getNeighborIndex(srcAgent);
    	int projectionFromMeInMyCopy = myMACConstruct_N[copyIndex].ACRecordsProjectFromMe[neighborIndex];
    	int projectionFromMeInNeighborCopy = ACRecordsProjectToMeInSrc;
    	/* Number of projections in self's copy than srcAgen's copy*/
    	int numMoreProjection = projectionFromMeInMyCopy - projectionFromMeInNeighborCopy;
    	if(0 == numMoreProjection){
    		return false;
    	}
    	
        double[] PP = new double[getDomainSize()];
        
        for (int i = 0; i < getDomainSize(); i++) {
            PP[i] = 0;
            if (myMACConstruct_N[copyIndex].neighborsPruned.get(neighborIndex)[i]) continue;
            for (int c = ACRecordsProjectToMeInSrc; c < projectionFromMeInMyCopy; c++) {
                if (myMACConstruct_N[copyIndex].P_records[neighborIndex][c] == null) 
                    PP[i] += 0;
                else
                    PP[i] += myMACConstruct_N[copyIndex].P_records[neighborIndex][c][i];  
                
                
            }
        }
    	
    	while(numMoreProjection > 0){
    		numMoreProjection -- ;
    		myMACConstruct_N[copyIndex].ACRecordsProjectFromMe[neighborIndex] --;

    		for(int hisVal : myMACConstruct_N[copyIndex].neighborsDomains.get(neighborIndex)){
    			if(myMACConstruct_N[copyIndex].neighborsPruned.get(neighborIndex)[hisVal]) continue;
//    			//debug
//    			if(getId()==8 && P==1 && myMACConstruct_N[copyIndex].P_records.length == 2){
//        			System.out.println("In agent: " + getId() +
//        					"\tP:" + P+"\tsize: "+myMACConstruct_N[copyIndex].P_records.length);
//    			}
//    			System.out.println("In agent: " + getId() +
//    					"\tP:" + P+"\tsize: "+myMACConstruct_N[copyIndex].P_records.length);

    			double alpha = PP[hisVal];
    			myMACConstruct_N[copyIndex].updateCostsWhenRollBack(getId(), srcAgent, hisVal, alpha);
    		}
    	}
    	return true;
    }
    
    public void reset_AllPruningFlag() {
        for (int i = 0; i < copyNum; i++) {
            this.ACEnforcementNeed[i] = false;
        }
    }
    
    /**
     * 
     * @param srcAgent
     * @param srcMACConstruct
     * @param srcContext
     */
    public void Reinitialize(int srcAgent, int srcAgentDepth, 
    		HashMap<Integer, AssignmentInfo> srcContext){
    	int minDepth = Integer.MAX_VALUE;
    	int srcDepth = srcAgentDepth;
    	int ancestorNum = tree.getAncestors().size();
    	boolean higherReset = false;
    	for (int depth = srcDepth; depth >=0; depth --){
    		if(depth >= myDepth){
    			continue;
    		}
    		int myAncestor = getAncestorFromDepth(depth);
    		int myTimeStamp = MAC_cpa.get(myAncestor).ID;
    		int srcTimeStamp = srcContext.get(myAncestor).ID;
    		if(srcTimeStamp > myTimeStamp){
    			MAC_cpa.get(myAncestor).value = srcContext.get(myAncestor).getValue();
    			MAC_cpa.get(myAncestor).ID = srcContext.get(myAncestor).getID();
    			minDepth = depth + 1;
    			higherReset = true;
    		}
    	}
    	
    	if(higherReset){
    		for(int depth = minDepth; d <= myDepth; d++){
    			pending_BTK[depth] = false;
    			myMACConstruct_N[depth] = myMACConstruct_N[depth-1].duplicate();
    			myMACConstruct_N[depth].global_cPhi = myMACConstruct_N[startPoint].global_cPhi;
    			myMACConstruct_N[depth].global_top = myMACConstruct_N[startPoint].global_top;
    			myMACConstruct_N[depth].myContribution = 0;
                subtreeContribution_N[depth] = 0;
    			int higherAgent = getAncestorFromDepth(depth - 1);
    			TransformBinaryToUnary(depth, higherAgent);
    		}
    	}
    	
    }
    
    void TransformBinaryToUnary(int copyIndex, int higherAgent){
    	if(tree.getNeighbors().contains(higherAgent)){
        	int a = MAC_cpa.get(higherAgent).value;
        	for(int i = 0; i < this.getDomainSize(); i++){
        		int b = i;
        		double oriUnaryCost = myMACConstruct_N[copyIndex].unaryCosts[b];
        		double binaryCost = myMACConstruct_N[copyIndex].getConstraint(getId(), b, higherAgent, a);
        		myMACConstruct_N[copyIndex].unaryCosts[b] = oriUnaryCost + binaryCost;
        	}
    	}

    }
    
    //send("BTK", getId(), targetDepth, context).to(parent);
    @WhenReceived("BTK")
    public void handleBTK(int srcAgent, int targetDepth, HashMap<Integer, AssignmentInfo> srcCPA){
    	if(myDepth != targetDepth){
    		int parent = tree.getParent();
    		if(parent != -1)
    			send("BTK", getId(), targetDepth, srcCPA).to(parent);
    	}
    	else{
    		
    		boolean compatible = true;
    		for(Map.Entry<Integer, AssignmentInfo> entry : MAC_cpa.entrySet()){
    			int agent = entry.getKey();
    			int myVal = entry.getValue().value;
    			if(srcCPA.get(agent) != null){
        			int hisVal = srcCPA.get(agent).value;
        			if(myVal != hisVal){
        				compatible = false;
        			}
    			}

    		}
    		if(compatible){
    			MACConstruct ac = myMACConstruct_N[targetDepth];
    			if(srcCPA.get(getId())!=null){ 
        			int skipVal = srcCPA.get(getId()).value;
        			Vector<Integer> valuesToDelete = new Vector();
        			valuesToDelete.add(skipVal);
        			Vector<Double> unaryCostsBackup = new Vector();
        			double unaryCost = ac.unaryCosts[skipVal];
        			unaryCostsBackup.add(unaryCost);
        			DeleteValues(myDepth, valuesToDelete, unaryCostsBackup);
    			}

    		}
    	}
    }
    
    public int getAncestorFromDepth(int ancestorDepth){
    	int ancestorSize = tree.getAncestors().size();
    	int ancestorIndex = ancestorSize - ancestorDepth -1;
    	int ancestor = tree.getAncestors().get(ancestorIndex);
    	return ancestor;
    }
    
    public int getMin(int a, int b){
    	if(a < b)
    		return a;
    	else
    		return b;
    }
    
  public int getAncestorDepth(int ancestor){
	for(Map.Entry<Integer, Integer> entry: ancestor_depth_map.entrySet()){
		if(entry.getKey() == ancestor){
			return entry.getValue();
		}
	}
	return -1;
  }
  
  public void performPruneDomainSelf() {
      for (int dp = 0; dp < copyNum; dp++) {
          
          if (ACEnforcementNeed[dp]) {
              checkDomainForDeletions(dp);
              unaryProjection(dp);
          }
      }
      reset_AllPruningFlag();
  }
    
}
