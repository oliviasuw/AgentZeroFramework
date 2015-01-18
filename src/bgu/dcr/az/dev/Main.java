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
        
        
//        // Added by suwen
//        /**
//         * Output Statistics directly
//         */
//        Database db = DatabaseUnit.UNIT.getDatabase();
//        ResultSet res = null;
//        String testName = "bnbadoptPlus";
//        file = new File("costs.txt");
//        FileWriter fileWriter = null;
//		try {
//			fileWriter = new FileWriter(file, true);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        
//        // ALL Msg
//        try {
//			res = db.query(""
//			        + "select ALGORITHM_INSTANCE, sum(messages), testFile "
//			        + "from AllMsgs "
//			        + "where test = '" + testName + "' "
//			        + "group by ALGORITHM_INSTANCE, testFile "
//			        + "order by testFile");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        try {
//        	fileWriter.write("\n");
//			while(res.next()){
//				String algorithmInstanceName = res.getString(1);
//				int AllMsgNum = res.getInt(2);
//				System.out.println("AllMsgs: " +AllMsgNum);
//	            
//	            try {        
//	                fileWriter.write(AllMsgNum + "\t");
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
//        // my NCCC
//        try {
//			res = db.query("select sum(value) as sum, testFile, "
//	    			+ "ALGORITHM_INSTANCE from myNCCCs where TEST = '" + testName
//	    			+ "' group by ALGORITHM_INSTANCE, testFile order by testFile");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        try {
//        	fileWriter.write("\n");
//			while(res.next()){
//				int NCCCs = res.getInt(1);
//				System.out.println("NCCC:  " +NCCCs);
//	            try {
//	                fileWriter.write(NCCCs + "\t");
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
              
//        try {
//			res = db.query(""
//			        + "select ALGORITHM_INSTANCE, sum(messages), testFile "
//			        + "from VALUEMsgs "
//			        + "where test = '" + testName + "' "
//			        + "group by ALGORITHM_INSTANCE, testFile "
//			        + "order by testFile");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        try {
//        	fileWriter.write("\n");
//			while(res.next()){
//				String algorithmInstanceName = res.getString(1);
//				int VALUEMsgNum = res.getInt(2);
//				System.out.println("VALUE: " +VALUEMsgNum);
//	            try {
//	                fileWriter.write(VALUEMsgNum + "\t");
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
        
//        try {
//			res = db.query(""
//			        + "select ALGORITHM_INSTANCE, sum(messages), testFile "
//			        + "from COSTMsgs "
//			        + "where test = '" + testName + "' "
//			        + "group by ALGORITHM_INSTANCE, testFile "
//			        + "order by testFile");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        try {
//        	fileWriter.write("\n");
//			while(res.next()){
//				String algorithmInstanceName = res.getString(1);
//				int COSTMsgNum = res.getInt(2);
//				System.out.println("COST: " +COSTMsgNum);
//	            try {
//	                fileWriter.write(COSTMsgNum + "\t");
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        try {
//			res = db.query(""
//			        + "select ALGORITHM_INSTANCE, sum(messages), testFile "
//			        + "from DELMsgs "
//			        + "where test = '" + testName + "' "
//			        + "group by ALGORITHM_INSTANCE, testFile "
//			        + "order by testFile");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        try {
//        	fileWriter.write("\n");
//			while(res.next()){
//				String algorithmInstanceName = res.getString(1);
//				int DELMsgNum = res.getInt(2);
//				System.out.println("DEL: " +DELMsgNum);
//	            try {
//	                fileWriter.write(DELMsgNum + "\t");
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        try {
//			fileWriter.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    }
}
