package bgu.dcr.az.dev.agents;

import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.Assignment;
import bgu.dcr.az.api.tools.TimeStamp;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

@Algorithm(name="AFBB3Constrained", useIdleDetector=false)
public class AFBB3ConstrainedAgent extends SimpleAgent {

    private int bound;
    private HashMap<Integer, HashMap<Integer, Integer>> cpa, bestCpa;
    private int[] estimates;
    private int[] h;
    private boolean isLogged = false;
    private List<Integer> variables;
    private ArrayList<HashMap<Integer, Integer>> domain;
    private int domainSize;
    private TimeStamp timeStamp;
    private HashMap<HashMap<Integer, HashMap<Integer, Integer>>, Double> fv;

    @Override
    public void start() {
        variables = getProblem().getVariables(getId());
        initDomain();
        timeStamp = new TimeStamp(this);
        bound = Integer.MAX_VALUE;
        estimates = new int[getProblem().getNumberOfAgents()];
        h = new int[domainSize];
        fv = new HashMap<HashMap<Integer,HashMap<Integer,Integer>>, Double>();
        fillH();
        if (isFirstAgent()) {
            cpa = new HashMap<>();
            assignCPA();
        }
    }

    private void initDomain(){
        domain = new ArrayList<HashMap<Integer, Integer>>();
        domainSize = (int) Math.pow(getDomainOf(variables.get(0)).size(), variables.size());
        for(int i = 0; i < domainSize; i++){
            HashMap<Integer, Integer> aLocalAssignment = new HashMap<>();
            for(int j = 0; j < variables.size(); j++){
                int k = 0;
                int tmp = i;
                while(k < j){
                    tmp = tmp / getDomainOf(variables.get(0)).size();
                    k++;
                }
                aLocalAssignment.put(variables.get(j), tmp % getDomainOf(variables.get(0)).size());
            }
            domain.add(aLocalAssignment);
        }
    }

    private Assignment cpaToAssignment(HashMap<Integer, HashMap<Integer, Integer>> cpa){
        Assignment assignment = new Assignment();
        for(HashMap<Integer, Integer> entry : cpa.values()){
            for(Entry<Integer, Integer> aEntry : entry.entrySet()){
                assignment.assign(aEntry.getKey(), aEntry.getValue());
            }
        }
        return assignment;
    }

    private int getLastAssigendIndex(HashMap<Integer, HashMap<Integer, Integer>> cpa){
        if(!cpa.containsKey(getId()))
            return -1;
        for(HashMap<Integer, Integer> item : domain){
            if(item.equals(cpa.get(getId())))
                return domain.indexOf(item);
        }
        return -1;
    }

    @Override
    protected int costOf(Assignment a) {
        return (a == null ? Integer.MAX_VALUE : getProblem().calculateCost(a));
    }

    private void assignCPA(){
        clearEstimations();
        int v = -1;
        int lastAssignedIndex = getLastAssigendIndex(cpa);
        HashMap<Integer, Integer> lastCpa = cpa.remove(getId());
        for (int i = lastAssignedIndex + 1; i < domainSize; i++) {
            if (costOf(cpaToAssignment(cpa)) + calcFv(i, cpa) < bound) {
                v = i;
                break;
            }
        }
        if (v == -1) {
            backtrack();
        } else {
            cpa.put(getId(), domain.get(v));
            timeStamp.incLocalTime();
            if (cpa.size() == getProblem().getNumberOfAgents()) {
                broadcast("NEW_SOLUTION", timeStamp, cpa);
                bound = (int) costOf(cpaToAssignment(cpa));
                assignCPA();
            } else {
                send("CPA_MSG", timeStamp, cpa).toNextAgent();
                if(lastCpa == null || isConstrainedVarChanged(lastCpa, domain.get(v))){
                    send("FB_CPA", timeStamp, this.getId(), cpa).toAllAgentsAfterMe();
                    for(int agent : getConstraintAgentsAfterMe()){
                        send("FB_CPA", timeStamp, this.getId(), cpa).to(agent);
                    }
                }
            }
        }
    }

    private void backtrack() {
        clearEstimations();
        if (this.isFirstAgent()) {
            if(!isLogged){
                writeToFile();
                isLogged = true;
            }
            finish(cpaToAssignment(bestCpa));
        } else {
            cpa.remove(this.getId());
            send("CPA_MSG", timeStamp, cpa).toPreviousAgent();
        }
    }

    private boolean isConstrainedVarChanged(HashMap<Integer, Integer> lastCpa, HashMap<Integer, Integer> newCpa){
        for(int constrainedVar : getConstrainedVars()){
            if(!lastCpa.get(constrainedVar).equals(newCpa.get(constrainedVar)))
                return true;
        }
        return false;
    }

    private ArrayList<Integer> getConstrainedVars(){
        ArrayList<Integer> constrainedVars = new ArrayList<>();
        for(int agent = getId() + 1; agent < getProblem().getNumberOfAgents(); agent++){
            constrainedVars.addAll(getProblem().getConstrainedVars(getId(), agent));
        }
        return constrainedVars;
    }

    private ArrayList<Integer> getConstraintAgentsAfterMe(){
        ArrayList<Integer> constraintAgents = new ArrayList<>();
        for(int agent = getId() + 1; agent < getProblem().getNumberOfAgents(); agent++){
            if(getProblem().getConstrainedVars(getId(), agent).size()!=0)
                constraintAgents.add(agent);
        }
        return constraintAgents;
    }

    private void writeToFile(){
        File costFile = new File("costs.txt");
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(costFile, true)));
            writer.write(costOf(cpaToAssignment(bestCpa)) + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearEstimations() {
        this.estimates = new int[getProblem().getNumberOfAgents()];
    }

    private int calcFv(int i, HashMap<Integer, HashMap<Integer, Integer>> pa){
        int withoutCost = costOf(cpaToAssignment(pa));
        pa.put(getId(), domain.get(i));
        int addedCost = costOf(cpaToAssignment(pa)) - withoutCost;
        double resultFromCache = getFvFromCache(pa);
        pa.remove(getId());
        return (int) (resultFromCache > h[i] ? addedCost + resultFromCache : addedCost + h[i]);
    }

    private double getFvFromCache(HashMap<Integer, HashMap<Integer, Integer>> pa){
        for(Entry<HashMap<Integer, HashMap<Integer, Integer>>, Double> aPa : fv.entrySet()){
            HashMap<Integer, Integer> aAssignment = aPa.getKey().get(getId());
            aPa.getKey().remove(getId());
            HashMap<Integer, Integer> bAssignment = pa.get(getId());
            pa.remove(getId());
            if(aPa.getKey().equals(pa) && !isConstrainedVarChanged(aAssignment, bAssignment)){
                aPa.getKey().put(getId(), aAssignment);
                pa.put(getId(), bAssignment);
                return aPa.getValue();
            }else{
                aPa.getKey().put(getId(), aAssignment);
                pa.put(getId(), bAssignment);
            }
        }
        return -1;
    }

    private void fillH() {
        for (int v = 0; v < domainSize; v++) {
            h[v] = calculateHv(v);
        }
    }

    private int calculateHv(int v) {
        int ans = 0;
        int tmp = 0;
        for (int aj = variables.get(variables.size()-1) + 1; aj < this.getProblem().getNumberOfVariables(); aj++) {
            tmp = Integer.MAX_VALUE;
            for (int u = 0; u < this.getProblem().getDomainSize(aj); u++) {
                int cost = 0;
                for(Entry<Integer, Integer> entry : domain.get(v).entrySet()){
                    cost += getProblem().getConstraintCost(entry.getKey(), entry.getValue(), aj, u);
                }
                if (tmp > cost) {
                    tmp = cost;
                }
            }
            ans += tmp;
        }
        return ans;
    }

    private int calcMinf(HashMap<Integer, HashMap<Integer, Integer>> pa) {
        int ans = 0;
        int tmp = Integer.MAX_VALUE;
        int fv = 0;
        for (int v = 0; v < domainSize; v++) {
            fv = calcFv(v, pa);
            if (tmp > fv) {
                tmp = fv;
            }
        }
        ans = tmp;
        return ans;
    }

    private double estimatesSum() {
        int ans = 0;
        for (int i = 0; i < estimates.length; i++) {
            ans += estimates[i];
        }
        return ans;
    }

    @WhenReceived("FB_CPA")
    public void handleFBCPA(TimeStamp hisTimeStamp, int aj, HashMap<Integer, HashMap<Integer, Integer>> pa) {
        if (hisTimeStamp.compare(timeStamp, this) >= 0) {
            timeStamp.copyFrom(hisTimeStamp);
        } else {
            return;
        }
        int f = calcMinf(pa);
        send("FB_ESTIMATE", timeStamp, f, pa, this.getId()).to(aj);
    }

    @WhenReceived("CPA_MSG")
    public void handleCPAMSG(TimeStamp hisTimeStamp, HashMap<Integer, HashMap<Integer, Integer>> pa) {
        if (hisTimeStamp.compare(timeStamp, this) >= 0) {
            timeStamp.copyFrom(hisTimeStamp);
        } else {
            return;
        }
        this.cpa = pa;
        HashMap<Integer, Integer> assignment = pa.get(getId());
        pa.remove(this.getId());
        if (costOf(cpaToAssignment(pa)) >= bound) {
            if(assignment!=null)
                pa.put(getId(), assignment);
            backtrack();
        } else {
            if(assignment != null)
                pa.put(getId(), assignment);
            assignCPA();
        }
    }

    @WhenReceived("FB_ESTIMATE")
    public void handleFBESTIMATE(TimeStamp hisTimeStamp, int estimate,
                                 HashMap<Integer, HashMap<Integer, Integer>> pa, int aj) {
        // Timestamp fixed
        if (hisTimeStamp.compare(timeStamp, this) >= 0) {
            timeStamp.copyFrom(hisTimeStamp);
        } else {
            return;
        }
        if(!pa.equals(cpa)){
            return;
        }
        estimates[aj] = estimate;
        fv.put(pa, estimatesSum());
        if (costOf(cpaToAssignment(cpa)) + estimatesSum() >= bound) {
            assignCPA();
        }
    }

    @WhenReceived("NEW_SOLUTION")
    public void handleNEWSOLUTION(TimeStamp hisTimeStamp,HashMap<Integer, HashMap<Integer, Integer>> pa) {
        if (hisTimeStamp.compare(timeStamp, this) >= 0) {
            timeStamp.copyFrom(hisTimeStamp);
        } else {
            return;
        }
        bestCpa = pa;
        bound = (int) costOf(cpaToAssignment(pa));
    }
}
