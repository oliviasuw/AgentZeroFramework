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
import bgu.dcr.az.dev.tools.VarAgentMap;
import bgu.dcr.az.exen.stat.AbstractStatisticCollector;
import bgu.dcr.az.exen.stat.NCCCStatisticCollector;
import bgu.dcr.az.exen.stat.db.DatabaseUnit;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Register(name = "NCCC_MultiVarsPerAgent")
public class NCCCMultiVarsPerAgent extends AbstractStatisticCollector<NCCCMultiVarsPerAgent.MyNCCCRecord> {

    @Variable(name = "agentType", description = "type of an agent whether contains multiple variables.", defaultValue = "single")
    AgentType agentType = AgentType.single;
	private long[] multiVarNccc;
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
    	String query = "select AVG(value) as avg, testFile, "
    			+ "ALGORITHM_INSTANCE from NCCCMulti where TEST = '" + r.getName() 
    			+ "' group by ALGORITHM_INSTANCE, testFile order by testFile";
        LineVisualModel line = new LineVisualModel(r.getRunningVarName(), "Avg(NCCCMulti)", "NCCCMulti");
        int index = 1;
        try {
            ResultSet rs = db.query(query);
            while (rs.next()) {
                line.setPoint(rs.getString("ALGORITHM_INSTANCE"), index++, rs.getFloat("avg"));
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
    	this.agents = agents;
        switch (agentType){
            case single:
                final VarAgentMap varAgentMap = new VarAgentMap(files[fileNo-1]);
                multiVarNccc = new long[varAgentMap.getAgentsNo()];
                lastKnownCC = new long[varAgentMap.size()];
                new Hooks.BeforeMessageProcessingHook() {
                    @Override
                    public void hook(Agent a, Message msg) {
                        if (msg.getMetadata().containsKey("myNccc")) {
                            long newNccc = (Long) msg.getMetadata().get("myNccc");
                            updateCurrentNccc(varAgentMap.get(a.getId()), a.getId());
                            multiVarNccc[varAgentMap.get(a.getId())] = max(newNccc, multiVarNccc[varAgentMap.get(a.getId())]);
                        }
                    }
                }.hookInto(ex);
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {
                        if (sender >= 0) { //not system or something..
                            updateCurrentNccc(varAgentMap.get(sender), sender);
                            msg.getMetadata().put("myNccc", multiVarNccc[varAgentMap.get(sender)]);
                        }
                    }
                }.hookInto(ex);
            case multiple:
                multiVarNccc = new long[agents.length];
                lastKnownCC = new long[agents.length];
                new Hooks.BeforeMessageProcessingHook() {
                    @Override
                    public void hook(Agent a, Message msg) {
                        if (msg.getMetadata().containsKey("myNccc")) { //can be system message or something...
                            long newNccc = (Long) msg.getMetadata().get("myNccc");
                            updateCurrentNccc(a.getId());
                            multiVarNccc[a.getId()] = max(newNccc, multiVarNccc[a.getId()]);
                        }
                    }
                }.hookInto(ex);
                new Hooks.BeforeMessageSentHook() {
                    @Override
                    public void hook(int sender, int recepiennt, Message msg) {
                        if (sender >= 0) { //not system or something..
                            updateCurrentNccc(sender);
                            msg.getMetadata().put("myNccc", multiVarNccc[sender]);
                        }
                    }
                }.hookInto(ex);
        }

        new Hooks.TerminationHook() {
            @Override
            public void hook() {
                submit(new MyNCCCRecord(testFile, max(multiVarNccc)));
            }
        }.hookInto(ex);
    }

    private void updateCurrentNccc(int agentId) {
        long last = lastKnownCC[agentId];
        lastKnownCC[agentId] = agents[agentId].getNumberOfConstraintChecks();
        multiVarNccc[agentId] = multiVarNccc[agentId] + lastKnownCC[agentId] - last;
    }

    private void updateCurrentNccc(int agentId, int varId) {
        long last = lastKnownCC[varId];
        lastKnownCC[varId] = agents[varId].getNumberOfConstraintChecks();
        multiVarNccc[agentId] = multiVarNccc[agentId] + lastKnownCC[varId] - last;
    }
    
    @Override
    public String getName() {
    	return "NCCC Multiple Variables per Agent";
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
            return "NCCCMulti";
        }
    }

    public static enum AgentType {
        single,
        multiple,
    }
}
