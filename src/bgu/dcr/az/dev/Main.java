package bgu.dcr.az.dev;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import bgu.dcr.az.api.exen.stat.Database;
import bgu.dcr.az.exen.stat.db.DatabaseUnit;

/**
 * Created by ChrisQin on 10/7/2014.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
    	
        File file = new File("costs.txt");
        if(file.exists())
            file.delete();

        ExperimentExecutionController.UNIT.run(new File("test.xml"), true, false);
    }
}
