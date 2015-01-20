/**
 * TERMINATEMessageCounter.java
   Created by Su Wen
   Date: Dec 19, 2014
   Time: 4:06:21 PM 
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
import bgu.dcr.az.api.exen.stat.vmod.BarVisualModel;
import bgu.dcr.az.exen.stat.AbstractStatisticCollector;
import bgu.dcr.az.exen.stat.db.DatabaseUnit;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Register(name = "TERMINATEMessageCounter")
public class TERMINATEMessageCounter extends AbstractStatisticCollector<TERMINATEMessageCounter.TERMINATEMsgRecord> {

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
    public void submit(TERMINATEMsgRecord record) {
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
                bv = new BarVisualModel("TERMINATE Messages Count", "Agent", "Avg(Message Sent)");
                res = db.query(""
                        + "select ALGORITHM_INSTANCE, avg(messages) as m, agent "
                        + "from TERMINATEMsgs "
                        + "where test = '" + r.getName() + "' "
                        + "group by ALGORITHM_INSTANCE, agent "
                        + "order by agent");

                bv.load("ALGORITHM_INSTANCE", "testFile", "m", res);
                return bv;

            case BY_TESTFILE:
                String runVar = r.getRunningVarName();
                //System.out.println(runVar);
                bv = new BarVisualModel("TERMINATE Messages Count", runVar, "Sum(Message Sent)");
                res = db.query(""
                        + "select ALGORITHM_INSTANCE, sum(messages) as m, testFile "
                        + "from TERMINATEMsgs "
                        + "where test = '" + r.getName() + "' "
                        + "group by ALGORITHM_INSTANCE, testFile "
                        + "order by testFile");

                bv.load("ALGORITHM_INSTANCE", "testFile", "m", res);
                return bv;
            }
        } catch (SQLException ex) {
            Logger.getLogger(TERMINATEMessageCounter.class.getName()).log(Level.SEVERE, null, ex);
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
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {

                        if(msg.getName().equals("TERMINATE")){
                        	counts[sender]++;
                        	Counter.TERMINATEMsgCounter ++;
                        }
                            
                    }
                }.hookInto(ex);
                break;
            case VA:
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {
                      	int senderAgent = agents[sender].getRealAgent();
                      	int recepienntAgent = agents[recepiennt].getRealAgent();
                        if(senderAgent != recepienntAgent 
                        		&& msg.getName().equals("TERMINATE"))
                            counts[sender]++;
                    }
                }.hookInto(ex);
                break;
        }
        
        new Hooks.TerminationHook() {

            @Override
            public void hook() {
                for (int i = 0; i < counts.length; i++) {
                    submit(new TERMINATEMsgRecord(i, counts[i], testFile));
                }
            }
        }.hookInto(ex);
    }

    @Override
    public String getName() {
    	return "TERMINATE Messages Count";
    }

    public static class TERMINATEMsgRecord extends DBRecord {
    	
    	int agent;
    	long messages;
    	String testFile;

        public TERMINATEMsgRecord(int agent, long msgCount, String testFile) {
            this.agent = agent;
            this.messages = msgCount;
            this.testFile = testFile;
        }

        @Override
        public String provideTableName() {
            return "TERMINATEMsgs";
        }
    }
    
    public static enum Type {

        BY_AGENT,
        BY_TESTFILE,
    }

    public static enum AgentType {
        single,
        VA,
    }
}

