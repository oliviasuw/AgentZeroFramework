package bgu.dcr.az.dev;

import java.io.File;

/**
 * Created by ChrisQin on 10/7/2014.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        File file = new File("costs.txt");
        if(file.exists())
            file.delete();
        ExperimentExecutionController.UNIT.run(new File("test.xml"), true, false);
    }
}
