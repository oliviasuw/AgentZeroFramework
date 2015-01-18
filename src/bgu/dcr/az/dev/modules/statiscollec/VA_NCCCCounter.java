/**
 * multiVarPerAgentNCCCCounter.java
   Created by Su Wen
   Date: Jan 11, 2015
   Time: 8:27:10 PM 
 */
package bgu.dcr.az.dev.modules.statiscollec;

import bgu.dcr.az.api.Agent;
import bgu.dcr.az.api.Hooks;
import bgu.dcr.az.api.Message;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.exen.Execution;
import bgu.dcr.az.api.exen.Test;
import bgu.dcr.az.api.exen.stat.DBRecord;
import bgu.dcr.az.api.exen.stat.Database;
import bgu.dcr.az.api.exen.stat.VisualModel;
import bgu.dcr.az.api.exen.stat.vmod.LineVisualModel;
import bgu.dcr.az.exen.stat.AbstractStatisticCollector;
import bgu.dcr.az.exen.stat.NCCCStatisticCollector;
import bgu.dcr.az.exen.stat.db.DatabaseUnit;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Register(name = "NCCC_VA")
public class VA_NCCCCounter extends AbstractStatisticCollector<VA_NCCCCounter.MyNCCCRecord> {

    @Variable(name = "agentType", description = "type of an agent whether contains multiple variables.", defaultValue = "single")
    AgentType agentType = AgentType.single;
	private long[] varNccc;  // NCCC of each virtual agent
	private long[] realAgentNCCC;  // NCCC of each agent
    private long[] lastKnownCC;
    private Agent[] agents;

    Test test;

    @Override
    public void setTest(Test test) {
        this.test = test;
    }
    
    @Override
    public void submit(MyNCCCRecord record) {
        String ains = test.getCurrentExecutedAlgorithmInstanceName();
        record.setAlgorithmInstanceName(ains);
        record.setTestName(test.getName());
        record.setExecutionNumber(test.getCurrentExecutionNumber());
        DatabaseUnit.UNIT.insertLater(record, test);
    }
    
    @Override
    public VisualModel analyze(Database db, Test r) {
    	String query = "select SUM(value) as sum, testFile, "
    			+ "ALGORITHM_INSTANCE from NCCC_VA where TEST = '" + r.getName() 
    			+ "' group by ALGORITHM_INSTANCE, testFile order by testFile";
        LineVisualModel line = new LineVisualModel(r.getRunningVarName(), "sum(NCCC_VA)", "NCCC_VA");
        int index = 1;
        try {
            ResultSet rs = db.query(query);
            while (rs.next()) {
                line.setPoint(rs.getString("ALGORITHM_INSTANCE"), index++, rs.getFloat("sum"));
            }
            return line;
        } catch (SQLException ex) {
            Logger.getLogger(NCCCStatisticCollector.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public void hookIn(final Agent[] agents, final Execution ex) {
    	int fileNo = (int) test.getCurrentVarValue();
    	File dir = new File("problems");
    	File[] files = dir.listFiles();
    	final String testFile = files[fileNo-1].getName();
    	this.agents = agents;  //virtual agents

        switch (agentType){
            case single:
            	realAgentNCCC = new long[countNumOfRealAgents(agents)];  // number of real agents
                lastKnownCC = new long[agents.length];
                new Hooks.BeforeMessageProcessingHook() {
                    @Override
                    public void hook(Agent a, Message msg) {
                    	int realOwerAgent = a.getRealAgent();
                        if (msg.getMetadata().containsKey("nccc")) {
                            long newNccc = (Long) msg.getMetadata().get("nccc");
                            updateCurrentNccc(realOwerAgent, a.getId());
                            realAgentNCCC[realOwerAgent] = max(newNccc, realAgentNCCC[realOwerAgent]);
                        }
                    }
                }.hookInto(ex);
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {
                    	int senderAgent = agents[sender].getRealAgent();
                        if (sender >= 0) { //not system or something..
                            updateCurrentNccc(senderAgent, sender);
                            msg.getMetadata().put("nccc", realAgentNCCC[senderAgent]);
                        }
                    }
                }.hookInto(ex);
                break;
        }

        new Hooks.TerminationHook() {
            @Override
            public void hook() {
                submit(new MyNCCCRecord(testFile, max(realAgentNCCC)));
            }
        }.hookInto(ex);
    }

//    private void updateCurrentNccc(int agentId) {
//        long last = lastKnownCC[agentId];
//        lastKnownCC[agentId] = agents[agentId].getNumberOfConstraintChecks();
//        multiVarNccc[agentId] = multiVarNccc[agentId] + lastKnownCC[agentId] - last;
//    }

    private void updateCurrentNccc(int realAgentId, int runningAgentID) {
        long last = lastKnownCC[runningAgentID];
        lastKnownCC[runningAgentID] = agents[runningAgentID].getNumberOfConstraintChecks();
        realAgentNCCC[realAgentId] = realAgentNCCC[realAgentId] + 
        		lastKnownCC[runningAgentID] - last;
    }
    
    @Override
    public String getName() {
    	return "NCCC of VA approach";
    }

    public static class MyNCCCRecord extends DBRecord {
    	
        String testFile;
        long value;

        public MyNCCCRecord (String testFile, long value) {
        	this.testFile = testFile;
            this.value = value;
        }

        @Override
        public String provideTableName() {
            return "NCCC_VA";
        }
    }

    protected int countNumOfRealAgents(Agent [] agents){
    	HashMap<Integer, Integer> realAgentIDs = new HashMap();
    	for(Agent a : agents){
    		realAgentIDs.put(a.getRealAgent(), 1);
    	}
    	return realAgentIDs.size();
    }
    public static enum AgentType {
        single,
        multiple,
    }
}
