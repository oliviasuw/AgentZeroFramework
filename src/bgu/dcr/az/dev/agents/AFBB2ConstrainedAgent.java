package bgu.dcr.az.dev.agents;

import bgu.dcr.az.api.Message;
import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.Assignment;
import bgu.dcr.az.api.tools.TimeStamp;

import java.io.*;

@Algorithm(name="AFBB2Constrained", useIdleDetector=false)
public class AFBB2ConstrainedAgent extends SimpleAgent {

	private int bound;
	private Assignment cpa, bestCpa;
	private int[] estimates;
	private int[] h;
	private TimeStamp timeStamp;
	private boolean isLogged = false;
	
    @Override
    public void start() {
    	timeStamp = new TimeStamp(this);
    	bound = Integer.MAX_VALUE;
    	estimates = new int[this.getProblem().getNumberOfVariables()];
    	h = new int[this.getProblem().getDomainSize(this.getId())];
    	fillH();
    	//System.out.print("I am " + getId() + ", " + getConstraintCost(getId(), 0));
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
    	for (int i = lastAssignedValue + 1; i < this.getDomainSize(); i++) {
            if (costOf(cpa) + calcFv(i, cpa) < bound) {
                v = i;
                break;
            }
    	}
    	//if(v != -1)
    		//System.out.println("last assigned value =  " + lastAssignedValue + ", v = " + v);
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
                //System.out.println("I am " + getId() + ", cpa: " + cpamsgs);
                //send("FB_CPA", timeStamp, this.getId(), cpa).toAllAgentsAfterMe();
                for(int agent : getProblem().getNeighbors(getId())){
            		if(getId() < agent)
            			send("FB_CPA", this.getId(), cpa).to(agent);
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
             finish(bestCpa);
        } else {
             cpa.unassign(this.getId());
             send("CPA_MSG", cpa).toPreviousAgent();
             //System.out.println("I am " + getId() + ", cpa: " + cpamsgs);
        }

    }
    
    private void writeToFile(){
    	File costFile = new File("costs.txt");
    	try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(costFile, true)));
			writer.write(costOf(bestCpa) + "\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void clearEstimations() {
        this.estimates = new int[this.getProblem().getNumberOfVariables()];
    }
    
    private int calcFv(int v, Assignment pa) {
        int ans = (int) (pa.calcAddedCost(this.getId(), v, this.getProblem()) + h[v]);
        return ans;
    }
    
    private void fillH() {
        for (int v = 0; v < this.getProblem().getDomainSize(this.getId()); v++) {
            h[v] = calculateHv(v);
        }
    }
    
    private int calculateHv(int v) {
        int ans = 0;
        int cost = 0;
        int tmp = 0;
        for (int aj = this.getId() + 1; aj < this.getProblem().getNumberOfVariables(); aj++) {
            tmp = Integer.MAX_VALUE;
            cost = 0;
            for (int u = 0; u < this.getProblem().getDomainSize(aj); u++) {
                cost = (int) this.getConstraintCost(this.getId(), v, aj, u);
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
        for (int v = 0; v < this.getProblem().getDomainSize(this.getId()); v++) {
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
        //System.out.println("This is a FB_CPA message.");
    }
    
    @WhenReceived("CPA_MSG")
    public void handleCPAMSG(Assignment pa) {
    	//System.out.println("This is a CPA_MSG message.");
    	//if(pa.isAssigned(getId()) && cpa.isAssigned(getId())){
    	//	if(pa.getAssignment(getId()) < cpa.getAssignment(getId()))
    	//		return;
    	//}
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
    	//System.out.println("This is a FB_ESTIMATE message.");
    	if(!cpa.equals(pa))
    		return;
        estimates[aj] = estimate;
        if (costOf(cpa) + estimatesSum() >= bound) {
            assignCPA();
        } 
    }
    
    @WhenReceived("NEW_SOLUTION")
    public void handleNEWSOLUTION(Assignment pa) {
    	//System.out.println("This is a NEW_SOLUTION message.");
    	bestCpa = pa;
        bound = (int) costOf(pa);
    }
}
