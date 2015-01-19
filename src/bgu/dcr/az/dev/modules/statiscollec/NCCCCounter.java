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
import bgu.dcr.az.dev.modules.statiscollec.AllMsgs.Type;
import bgu.dcr.az.exen.stat.AbstractStatisticCollector;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bennyl
 */
@Register(name = "NCCCCounter")
public class NCCCCounter extends AbstractStatisticCollector<NCCCCounter.NCCCRecord> {

    private long[] nccc;
    private long[] lastKnownCC;
    private String runningVar;
    private Agent[] agents;

    long[] counts;
    @Variable(name = "type", description = "type of the graph to show (BY_AGENT/BY_TESTFILE)", defaultValue = "BY_TESTFILE")
    Type graphType = Type.BY_TESTFILE;
    Test test;
    
    
    @Override
    public VisualModel analyze(Database db, Test r) {
        String query = "select ALGORITHM_INSTANCE, sum(value) as all, testFile "
        		+ "from NCCC "
                + "where test = '" + r.getName() + "' "
                + "group by ALGORITHM_INSTANCE, testFile "
                + "order by testFile";
        LineVisualModel line = new LineVisualModel(runningVar, "Sum(NCCC)", "NCCC");
        try {
            ResultSet rs = db.query(query);
//            while (rs.next()) {
//                line.setPoint(rs.getString("ALGORITHM_INSTANCE"), rs.getString("testFile"), rs.getFloat("all"));
//            }
            return line;
        } catch (SQLException ex) {
            Logger.getLogger(NCCCCounter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public void hookIn(final Agent[] agents, final Execution ex) {
        this.agents = agents;
        System.out.println("NCCC Statistic Collector registered");

        nccc = new long[agents.length];
        lastKnownCC = new long[agents.length];
        runningVar = ex.getTest().getRunningVarName();
        
    	int fileNo = (int) test.getCurrentVarValue();
    	File dir = new File("problems");
    	File[] files = dir.listFiles();
    	final String testFile = files[fileNo-1].getName();

        new Hooks.BeforeMessageProcessingHook() {
            @Override
            public void hook(Agent a, Message msg) {
                if (msg.getMetadata().containsKey("nccc")) { //can be system message or something...
                    long newNccc = (Long) msg.getMetadata().get("nccc");

                    updateCurrentNccc(a.getId());
                    nccc[a.getId()] = max(newNccc, nccc[a.getId()]);
                }
            }
        }.hookInto(ex);
        new Hooks.BeforeMessageSentHook() {
            @Override
            public void hook(int sender, int recepiennt, Message msg) {
                if (sender >= 0) { //not system or something..
                    updateCurrentNccc(sender);
                    msg.getMetadata().put("nccc", nccc[sender]);
                }
            }
        }.hookInto(ex);

        new Hooks.TerminationHook() {
            @Override
            public void hook() {
                submit(new NCCCRecord(ex.getTest().getCurrentVarValue(), max(nccc)));
            }
        }.hookInto(ex);

    }

    @Override
    public String getName() {
        return "NCCC Counter";
    }

    private void updateCurrentNccc(int aid) {
        long last = lastKnownCC[aid];
        lastKnownCC[aid] = agents[aid].getNumberOfConstraintChecks();
        nccc[aid] = nccc[aid] + lastKnownCC[aid] - last;
    }

    public long currentNcccOf(int agent) {
        return nccc[agent];
    }

    public static class NCCCRecord extends DBRecord {

        double rVar;
        double value;

        public NCCCRecord(double rVar, double value) {
            this.rVar = rVar;
            this.value = value;
        }

        @Override
        public String provideTableName() {
            return "NCCC";
        }
    }
}

