package bgu.dcr.az.dev.agents.AFB;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import bgu.dcr.az.api.*;
import bgu.dcr.az.api.agt.*;
import bgu.dcr.az.api.ano.*;
import bgu.dcr.az.api.tools.*;

@Algorithm(name="AFBOnVariables", useIdleDetector=false)
public class AFBAgent_OnVariables extends SimpleAgent {

	private int bound;
	private Assignment cpa, bestCpa;
	private int[] estimates;
	private int[] h;
	private TimeStamp timeStamp;
	
    @Override
    public void start() {
    	timeStamp = new TimeStamp(this);
    	bound = Integer.MAX_VALUE;
    	estimates = new int[this.getProblem().getNumberOfVars()];
    	h = new int[this.getProblem().getAgentDomainSize(this.getId())];
    	fillH();
        if (isFirstAgent()) {
        	cpa = new Assignment();    //generate CPA
        	assignCPA();
        }
    }
    
    private void assignCPA(){
    	clearEstimations();
    	int v = -1;
    	int lastAssignedValue = (cpa.isAssigned(this.getId()) ? cpa.getAssignment(this.getId()) : -1);
    	cpa.unassign(this.getId());
    	for (int i = lastAssignedValue + 1; i < this.getAgentDomainSize(); i++) {
    		if (costOf(cpa) + calcFv(i, cpa) < bound) {
                v = i;
                break;
    		}
    	}
    	if (v == -1) {
            backtrack();
    	} else {
            cpa.assign(this.getId(), v);
            timeStamp.incLocalTime();
            if (cpa.isFull(getProblem())) {
                broadcast("NEW_SOLUTION", cpa);
                bound = (int) costOf(cpa);
                assignCPA();
            } else {
                send("CPA_MSG", cpa).toNextAgent();
                send("FB_CPA", this.getId(), cpa).toAllAgentsAfterMe();
            }
    	}
    }
    
    private void backtrack() {
        clearEstimations();
        if (this.isFirstAgent()) {
             finish(bestCpa);
             System.out.println("bound is: " + bound);
             File file = new File("statistics.txt");
             try {
                 FileWriter fileWriter = new FileWriter(file, true);
                 fileWriter.write("\t" + bound + "\t");
                 fileWriter.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
        } else {
             cpa.unassign(this.getId());
             send("CPA_MSG", cpa).toPreviousAgent();
        }

    }
    
    private void clearEstimations() {
        this.estimates = new int[this.getProblem().getNumberOfVars()];
    }
    
    private int calcFv(int v, Assignment pa) {
        int ans = (int) (pa.calcAddedCost(this.getId(), v, this.getProblem()) + h[v]);
        return ans;
    }
    
    private void fillH() {
        for (int v = 0; v < this.getProblem().getAgentDomainSize(this.getId()); v++) {
            h[v] = calculateHv(v);
        }
    }
    
    private int calculateHv(int v) {
        int ans = 0;
        int cost = 0;
        int tmp = 0;
        for (int aj = this.getId() + 1; aj < this.getProblem().getNumberOfVars(); aj++) {
            tmp = Integer.MAX_VALUE;
            cost = 0;
            for (int u = 0; u < this.getProblem().getAgentDomainSize(aj); u++) {
                    cost = (int) this.getAgentConstraintCost(this.getId(), v, aj, u);
                    if (tmp > cost) {
                            tmp = cost;
                    }
            }
            ans += tmp;
        }
        return ans;
    }

    private int calcMinf(Assignment pa) {
        int ans = 0;
        int tmp = Integer.MAX_VALUE;
        int fv = 0;
        for (int v = 0; v < this.getProblem().getAgentDomainSize(this.getId()); v++) {
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
        for (int i = 0; i > estimates.length; i++) {
                ans += estimates[i];
        }
        return ans;
    }
    
    @Override
    protected void beforeMessageSending(Message m) {
        m.getMetadata().put("TIMESTAMP", timeStamp);
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
    public void handleFBCPA(int aj, Assignment pa) {
        int f = calcMinf(pa);
        send("FB_ESTIMATE", f, pa, this.getId()).to(aj);
    }
    
    @WhenReceived("CPA_MSG")
    public void handleCPAMSG(Assignment pa) {
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
    public void handleFBESTIMATE(int estimate, Assignment pa, int aj) {
        estimates[aj] = estimate;
        if (costOf(cpa) + estimatesSum() >= bound) {
                assignCPA();
        } 
    }
    
    @WhenReceived("NEW_SOLUTION")
    public void handleNEWSOLUTION(Assignment pa) {
        bestCpa = pa;
        bound = (int) costOf(pa);
    }
}
