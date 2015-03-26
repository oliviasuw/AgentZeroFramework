package bgu.dcr.az.dev.agents;

import static bgu.dcr.az.api.Agt0DSL.max;
import static bgu.dcr.az.api.Agt0DSL.min;
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
@Algorithm(name="BnBAdoptPlusOC", useIdleDetector=true)   // Corresponds to the algorithm name in the .xml file
public class BnBAdoptPlusOCAgent extends SimpleAgent {

    boolean debug = true;
    /** Structures for BnB-ADOPT+ only **/
    boolean PlusOn = true;
    // key: child/pseudochild ID; value: lastSentVALUE
    HashMap<Integer, Integer> lastSentVALUEs = new HashMap();
    // key: parent ID; value: lastSentCOST
    HashMap<Integer, AssignmentInfo> lastSentCPA = new HashMap();
    boolean cpaChanged = false;
    int lastSentLB = -1;
    int lastSentUB = Integer.MAX_VALUE;
    boolean myThReq = false;
    // Key: child/pseudochild ID; value: thReq
    HashMap<Integer, Boolean> receivedThReqs = new HashMap();

    HashMap<Integer, AssignmentInfo> cpa = new HashMap();
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
//            	if(debug){
//            		System.out.println("Agent " + getId() + " Depth:" 
//            	+ tree.getDepth() + " Parent:" + tree.getParent());
//            	}
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
        //System.out.println("id : " + id + ", SCA: " + _SCA);
        return _SCA;
    }

    public void InitChild(int child, int val){
        lbChildD[child][val] = 0;       //should have been heuristic
        ubChildD[child][val] = Integer.MAX_VALUE;
    }

    public void InitSelf(){
        int min = Integer.MAX_VALUE;
        for(int value : getAgentDomain()){
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
    	//debug
//    	MessageQueue tmp = getMailbox();
//    	PriorityBlockingQueue<Message> tmpDQ = tmp.getDelayedQueue();
//    	System.out.println("Delayed Queue of Agent[" + getId() +"] :    "+ tmpDQ.toString());
//    	for(Message msg : tmpDQ){
//    		long msgtime = ((Long) msg.getMetadata().get("nccc")).intValue();
//    		System.out.println("Msg Time: " + msgtime);
//    	}
    	
    	//debug
    	if(getId()==2) {
    		System.out.println();
    	}
        for(int i = 0; i < getAgentDomainSize(); i++){
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
        //    System.out.println((LBD[d] >= min(TH, UB)) + "lbd1 = " + LBD[1] + " d = " + findMinimum(LBD, 2, d));
        //}

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
        
//    	if(getId() == 0) {
//    		System.out.println("test");
//    	}
        for(int child : tree.getChildren()){
            if(PlusOn) {
                if(!lastSentVALUEs.containsKey(child) || lastSentVALUEs.get(child) != d
                		|| (receivedThReqs.containsKey(child) && receivedThReqs.get(child))){
                    send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                            + lbChildD[tree.getChildren().indexOf(child)][d]).to(child);
                    receivedThReqs.put(child, false);
                    if(debug){
                    	System.out.println("#VALUE# from [" + getId() + "] to [" 
                    + child + "] [d: " + d +"] [ID: " + ID +"]");
                    }
                }
                lastSentVALUEs.put(child, d);
                
            }
            else {
                send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
                        + lbChildD[tree.getChildren().indexOf(child)][d]).to(child);
                
            }

        }
        for(int pseudoChild : tree.getPsaudoChildren()){
            if(PlusOn){
                if(!lastSentVALUEs.containsKey(pseudoChild) || lastSentVALUEs.get(pseudoChild) != d
                		|| (receivedThReqs.containsKey(pseudoChild) &&  receivedThReqs.get(pseudoChild))){
                    send("VALUE", getId(), d, ID, Integer.MAX_VALUE).to(pseudoChild);
                    receivedThReqs.put(pseudoChild, false);
                    if(debug){
                    	System.out.println("#VALUE# from [" + getId() + "] to [" 
                    + pseudoChild + "] [d: " + d +"] [ID: " + ID +"]");
                    }
                }
                lastSentVALUEs.put(pseudoChild, d);
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
                if(_cpa.get(p).getValue() != dp){
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
                // BnB-ADOPT+ only
                if(PlusOn){
                    if(_cpa.get(entry.getKey()).getValue() != entry.getValue().getValue()){
                        this.cpaChanged = true;
                    }
                }
                _cpa.remove(entry.getKey());
                _cpa.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
            }
        }
    }

    public boolean isCompatible(HashMap<Integer, AssignmentInfo> cpa1, HashMap<Integer, AssignmentInfo> _cpa){
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa1.entrySet()){
            if(_cpa.containsKey(entry.getKey()) && entry.getValue().getValue() != _cpa.get(entry.getKey()).getValue()){
                return false;
            }
        }
        return true;
    }

    @WhenReceived("VALUE")
    public void handleVALUE(int p, int dp, int IDp, int THp){
    	if(debug && getId()==2){
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
        if(isCompatible(cCPA, cpa)){
            lbChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()] = max(lbChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()], LBc);
            ubChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()] = min(ubChildD[tree.getChildren().indexOf(c)][cCPA.get(getId()).getValue()], UBc);
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

