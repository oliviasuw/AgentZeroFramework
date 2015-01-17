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

@Register(name = "CPA_MessageCounter")
public class CPAMessageCounter extends AbstractStatisticCollector<CPAMessageCounter.CPAMsgRecord> {

    long[] counts;
    @Variable(name = "type", description = "type of the graph to show (BY_AGENT/BY_TESTFILE)", defaultValue = "BY_TESTFILE")
    Type graphType = Type.BY_TESTFILE;
    Test test;

    @Override
    public void setTest(Test test) {
        this.test = test;
    }
    
    @Override
    public void submit(CPAMsgRecord record) {
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
                bv = new BarVisualModel("CPA_MSG Messages Count", "Agent", "Avg(Message Sent)");
                res = db.query(""
                        + "select ALGORITHM_INSTANCE, avg(messages) as m, agent "
                        + "from CPAMsgs "
                        + "where test = '" + r.getName() + "' "
                        + "group by ALGORITHM_INSTANCE, agent "
                        + "order by agent");

                bv.load("ALGORITHM_INSTANCE", "testFile", "m", res);
                return bv;

            case BY_TESTFILE:
                String runVar = r.getRunningVarName();
                //System.out.println(runVar);
                bv = new BarVisualModel("CPA_MSG Messages Count", runVar, "Sum(Message Sent)");
                res = db.query(""
                        + "select ALGORITHM_INSTANCE, sum(messages) as m, testFile "
                        + "from CPAMsgs "
                        + "where test = '" + r.getName() + "' "
                        + "group by ALGORITHM_INSTANCE, testFile "
                        + "order by testFile");

                bv.load("ALGORITHM_INSTANCE", "testFile", "m", res);
                return bv;
            }
        } catch (SQLException ex) {
            Logger.getLogger(CPAMessageCounter.class.getName()).log(Level.SEVERE, null, ex);
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
    	final VarAgentMap varAgentMap = new VarAgentMap(files[fileNo-1]);
    	final String testFile = files[fileNo-1].getName();
    	//System.out.println("test file name: " + testFile);
        new Hooks.BeforeMessageSentHook() {

            @Override
            public void hook(int sender, int recepiennt, Message msg) {
            	if(!varAgentMap.get(sender).equals(varAgentMap.get(recepiennt)) && msg.getName().equals("CPA_MSG"))
            		{
            		counts[sender]++;
            		//System.out.println("sender: " + sender + ", recepient: " + recepiennt + ", msg: " + msg);
            		}
            }
        }.hookInto(ex);
        
        new Hooks.TerminationHook() {

            @Override
            public void hook() {
                for (int i = 0; i < counts.length; i++) {
                    submit(new CPAMsgRecord(i, counts[i], testFile));
                }
            }
        }.hookInto(ex);
    }

    @Override
    public String getName() {
        //YOU CAN CHANGE THE RETURNED VALUE SO THAT YOUR STATISTIC COLLECTOR NAME IN THE UI WILL BE MORE PLESENT
    	return "CPA_MSG Messages Count";
    }

    //THIS IS YOUR RECORD IN THE DATABASE DEFINITION
    public static class CPAMsgRecord extends DBRecord {
    	
    	int agent;
    	long messages;
    	String testFile;

        public CPAMsgRecord(int agent, long msgCount, String testFile) {
            this.agent = agent;
            this.messages = msgCount;
            this.testFile = testFile;
        }

        @Override
        public String provideTableName() {
            return "CPAMsgs";
        }
    }
    
    public static enum Type {

        BY_AGENT,
        BY_TESTFILE,
    }
}
