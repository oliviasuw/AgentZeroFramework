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
import bgu.dcr.az.api.exen.stat.vmod.BarVisualModel;
import bgu.dcr.az.dev.tools.VarAgentMap;
import bgu.dcr.az.exen.stat.AbstractStatisticCollector;
import bgu.dcr.az.exen.stat.db.DatabaseUnit;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Register(name = "FB_CPAMessageCounter")
public class FBCPAMessageCounter extends AbstractStatisticCollector<FBCPAMessageCounter.FBCPAMsgRecord> {

	long[] counts;
    @Variable(name = "type", description = "type of the graph to show (BY_AGENT/BY_TESTFILE)", defaultValue = "BY_TESTFILE")
    Type graphType = Type.BY_TESTFILE;
    @Variable(name = "agentType", description = "type of an agent whether contains multiple variables.", defaultValue = "single")
    AgentType agentType = AgentType.single;
    Test test;

    @Override
    public void setTest(Test test) {
        this.test = test;
    }
    
    @Override
    public void submit(FBCPAMsgRecord record) {
        String ains = test.getCurrentExecutedAlgorithmInstanceName();
        record.setAlgorithmInstanceName(ains);
        record.setTestName(test.getName());
        record.setExecutionNumber(test.getCurrentExecutionNumber());
        DatabaseUnit.UNIT.insertLater(record, test);
    }
    
    @Override
    public VisualModel analyze(Database db, Test r) {
    	try {
            ResultSet res;
            BarVisualModel bv;
            switch (graphType) {
            case BY_AGENT:
                bv = new BarVisualModel("FB_CPA Messages Count", "Agent", "Avg(Message Sent)");
                res = db.query(""
                        + "select ALGORITHM_INSTANCE, avg(messages) as m, agent "
                        + "from FBCPAMsgs "
                        + "where test = '" + r.getName() + "' "
                        + "group by ALGORITHM_INSTANCE, agent "
                        + "order by agent");

                bv.load("ALGORITHM_INSTANCE", "testFile", "m", res);
                return bv;

            case BY_TESTFILE:
                String runVar = r.getRunningVarName();
                //System.out.println(runVar);
                bv = new BarVisualModel("FB_CPA Messages Count", runVar, "Sum(Message Sent)");
                res = db.query(""
                        + "select ALGORITHM_INSTANCE, sum(messages) as m, testFile "
                        + "from FBCPAMsgs "
                        + "where test = '" + r.getName() + "' "
                        + "group by ALGORITHM_INSTANCE, testFile "
                        + "order by testFile");

                bv.load("ALGORITHM_INSTANCE", "testFile", "m", res);
                return bv;
            }
        } catch (SQLException ex) {
            Logger.getLogger(FBCPAMessageCounter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public long currentMessageCountOf(int agent){
        return counts[agent];
    }
    
    @Override
    public void hookIn(final Agent[] agents, final Execution ex) {

    	counts = new long[agents.length];
    	int fileNo = (int) test.getCurrentVarValue();
    	File dir = new File("problems");
    	File[] files = dir.listFiles();
    	final String testFile = files[fileNo-1].getName();
        switch(agentType){
            case single:
                final VarAgentMap varAgentMap = new VarAgentMap(files[fileNo-1]);
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {
                        if(!varAgentMap.get(sender).equals(varAgentMap.get(recepiennt)) && msg.getName().equals("FB_CPA"))
                            counts[sender]++;
                    }
                }.hookInto(ex);
            case multiple:
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {
                        if(msg.getName().equals("FB_CPA"))
                            counts[sender]++;
                    }
                }.hookInto(ex);
        }
        
        new Hooks.TerminationHook() {

            @Override
            public void hook() {
                for (int i = 0; i < counts.length; i++) {
                    submit(new FBCPAMsgRecord(i, counts[i], testFile));
                }
            }
        }.hookInto(ex);
    }

    @Override
    public String getName() {
    	return "FB_CPA Messages Count";
    }

    public static class FBCPAMsgRecord extends DBRecord {
    	
    	int agent;
    	long messages;
    	String testFile;

        public FBCPAMsgRecord(int agent, long msgCount, String testFile) {
            this.agent = agent;
            this.messages = msgCount;
            this.testFile = testFile;
        }

        @Override
        public String provideTableName() {
            return "FBCPAMsgs";
        }
    }
    
    public static enum Type {

        BY_AGENT,
        BY_TESTFILE,
    }

    public static enum AgentType {
        single,
        multiple,
    }
}
