/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.dcr.az.exen.mdelay;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.Message;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.exen.Execution;
import bgu.dcr.az.api.exen.mdef.MessageDelayer;
import bgu.dcr.az.api.exen.stat.NCSCToken;
import java.util.Random;

/**
 *
 * @author bennyl
 */
@Register(name = "default-message-delayer")
public class DefaultMessageDelayer implements MessageDelayer {

    @Variable(name = "type",
              description = "the type of the delay: nccc/ncsc or , each depends on the corresponding statistic collectors",
              defaultValue = "nccc")
    String type = "nccc";
    @Variable(name = "seed",
              description = "seed for the randomization of the delay",
              defaultValue = "42")
    int seed = 42;
    @Variable(name = "maximum-delay",
              description = "the maximum delay that the delayer can produce for a message",
              defaultValue = "100")
    int maximumDelay = 100;
    @Variable(name = "minimum-delay",
              description = "the minimum delay that the delayer can produce for a message",
              defaultValue = "0")
    int minimumDelay = 0;
    
    long[][] previousTime;
    Random rnd = null;

    @Override
    public long getInitialTime() {
        return 0;
    }

    @Override
    public long extractTime(Message m) {
        if (type.equals("ncsc")){
            return NCSCToken.extract(m).getValue();//((Long) m.getMetadata().get("ncsc")).intValue();
        }else{
            return ((Long) m.getMetadata().get(type)).intValue();
        }
    }

    @Override
    public void addDelay(Message m, int from, int to) {
        if (maximumDelay < minimumDelay) return; //nothinng to add..
        int delay = rnd.nextInt(maximumDelay - minimumDelay) + minimumDelay;
        long otime = extractTime(m);
        long ntime = Math.max(delay + previousTime[from][to], otime + delay);
        previousTime[from][to] = ntime;
        if (type.equals("ncsc")) {
            NCSCToken.extract(m).setValue((long)ntime);
//            m.getMetadata().put("ncsc", (long)ntime);
        }else{
            m.getMetadata().put(type, (long)ntime);    
        }
    }

    @Override
    public void initialize(Execution ex) {
        System.out.println("initializing message delayer... delaying on " + type );
        int n = ex.getGlobalProblem().getNumberOfVariables();
        previousTime = new long[n][n];
        rnd = new Random(seed);
    }
}
