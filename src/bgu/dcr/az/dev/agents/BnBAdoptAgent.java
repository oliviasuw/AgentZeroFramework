package bgu.dcr.az.dev.agents;

import bgu.dcr.az.api.Continuation;
import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.DFSPsaudoTree;
import bgu.dcr.az.dev.tools.AssignmentInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Chris Qin on 10/9/2014.
 */
@Algorithm(name="BnBAdopt", useIdleDetector=true)
public class BnBAdoptAgent extends SimpleAgent {
	
	boolean debug = false;

    HashMap<Integer, AssignmentInfo> cpa = new HashMap<>();
    private int ID;
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

    public void InitChild(int child, int val){
        lbChildD[child][val] = 0;
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
    }

    public int positivInfinityPlus(int a, int b){
        if(a + b < 0)
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
            delta += getAgentConstraintCost(getId(), val, ancestor, cpa.get(ancestor).getValue());
        }
        return delta;
    }

    private void backtrack(){
        for(int i = 0; i < getAgentDomainSize(); i++) {
            LBD[i] = calcDelta(i) + sumlbORub(lbChildD, i);
            UBD[i] = positivInfinityPlus(calcDelta(i), sumlbORub(ubChildD, i));
        }
        LB = findMinimum(LBD, 1, 0);
        UB = findMinimum(UBD, 1, 0);
//        System.out.println("I am " + getId() + ", LB=" + LB + ", UB=" + UB);

        if(LBD[d] >= min(TH, UB)){
            d = findMinimum(LBD, 2, d);
            ID++;
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

        for(int child : tree.getChildren()){
            send("VALUE", getId(), d, ID, min(TH, UB) - calcDelta(d) - sumlbORub(lbChildD, d)
            		+ lbChildD[tree.getChildren().indexOf(child)][d]).to(child);
            //Debug
            if(debug){
            	System.out.println("VALUE: " + getId() +
            			" [" + d + "] to " + child);
            }
        }

        
        for(int pseudoChild : tree.getPsaudoChildren()){
        	send("VALUE", getId(), d, ID, Integer.MAX_VALUE).to(pseudoChild);            
            //Debug
            if(debug){
            	System.out.println("VALUE: " + getId() +
            			" [" + d + "] to " + pseudoChild);
            }

        	
        }
                   
        if(!tree.isRoot()){
        	send("COST", getId(), cpa, LB, UB).to(tree.getParent());        
            //Debug
            if(debug){
            	System.out.println("COST: " + getId() +
            			" [" + d + "] to " + tree.getParent());
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
        HashMap<Integer, AssignmentInfo> _cpaHashMap = new HashMap<>();
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa.entrySet()){
            _cpaHashMap.put(entry.getKey(), new AssignmentInfo(entry.getValue().getValue(), entry.getValue().getID()));
        }
        return _cpaHashMap;
    }

    public void priorityMerge(int p, int dp, int IDp, HashMap<Integer, AssignmentInfo> _cpa) {
        if(_cpa.containsKey(p) && IDp > _cpa.get(p).getID()){
            _cpa.remove(p);
            _cpa.put(p, new AssignmentInfo(dp, IDp));
        }
    }

    public void priorityMerge(HashMap<Integer, AssignmentInfo> cpa1, HashMap<Integer, AssignmentInfo> _cpa) {
        for(Map.Entry<Integer, AssignmentInfo> entry : cpa1.entrySet()){
            if(_cpa.containsKey(entry.getKey()) && entry.getValue().getID() > _cpa.get(entry.getKey()).getID()){
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
        HashMap<Integer, AssignmentInfo> _cpa = copyCPA(cpa);
        priorityMerge(p,  dp, IDp, cpa);
        if(!isCompatible(_cpa, cpa)){
            for(int i = 0; i < tree.getChildren().size(); i++){
                if(getSCA(tree.getChildren().get(i)).contains(p))
                    for(int j = 0; j < getAgentDomainSize(); j++){
                        InitChild(i, j);
                    }
            }
            InitSelf();
        }
        if(p == tree.getParent())
            TH = THp;
    }

    @WhenReceived("COST")
    public void handleCOST(int c, HashMap<Integer, AssignmentInfo>cCPA, int LBc, int UBc){
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

    /*String print(HashMap<Integer, AssignmentInfo> cCPA){
        String s = "";
        for (Map.Entry<Integer, AssignmentInfo> entry : cCPA.entrySet()) {
            s += entry.getKey() + ":" + entry.getValue().getValue() + ", ";
        }
        return s;
    }*/

    @WhenReceived("TERMINATE")
    public void handleTERMINATE(){
        for(int child : tree.getChildren()){
            send("TERMINATE").to(child);
        }
        finish();
    }
}
