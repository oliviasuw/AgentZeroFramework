/**
 * counter.java
   Created by Su Wen
   Date: Jan 12, 2015
   Time: 9:38:14 PM 
 */
package confs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import bgu.dcr.az.api.exen.ExecutionResult.State;

/**
 * @author Su Wen
 *
 */
public class Counter {
	public static int msgCounter = 0;
	public static int VALUEMsgCounter = 0;
	public static int COSTMsgCounter = 0;
	public static int DELMsgCounter = 0;
	public static int TERMINATEMsgCounter = 0;
	
	public static int treeBuildVisitMsgCounter = 0;
	public static int treeBuildAddAncestorMsgCounter = 0;
	public static int treeBuildSetChildMsgCounter = 0;
	public static int treeBuildSetPseudoChildMsgCounter = 0;
	public static int treeBuildDone = 0;
	public static int treeBuildVisitRefuse = 0;
	
	public static double nccc = 0;

	public static void reportStatistics(){
		System.out.println("                   #Msg#                         ");
		System.out.println("*********************TREE************************");
		int totalBuildTree = treeBuildVisitMsgCounter + treeBuildAddAncestorMsgCounter
				+ treeBuildSetChildMsgCounter + treeBuildSetPseudoChildMsgCounter
				+ treeBuildDone + treeBuildVisitRefuse;
		System.out.println("Buiding Tree Msg in total:\t" + totalBuildTree);
    	System.out.println("Visit Msg: " + Counter.treeBuildVisitMsgCounter);
    	System.out.println("Add Ancestor Msg: " + Counter.treeBuildAddAncestorMsgCounter);
    	System.out.println("Set Child Msg: " + Counter.treeBuildSetChildMsgCounter);
    	System.out.println("Set Pseudo Child Msg: " + Counter.treeBuildSetPseudoChildMsgCounter);
    	System.out.println("Visit Refuse Msg: " + Counter.treeBuildVisitRefuse);
    	System.out.println("DONE Msg: " + Counter.treeBuildDone);
		
		System.out.println("*************************************************\n");
		System.out.println("*********************Search************************");
    	System.out.println("Total Msg: " + Counter.msgCounter);
    	System.out.println("VALUE Msg: " + Counter.VALUEMsgCounter);
    	System.out.println("COST Msg: " + Counter.COSTMsgCounter);
    	System.out.println("DEL Msg: " + Counter.DELMsgCounter);
    	System.out.println("TERM Msg: " + Counter.TERMINATEMsgCounter);
    	System.out.println("*************************************************");
    	int total = totalBuildTree + msgCounter;
    	System.out.println("In total:\t" + total);
    	System.out.println("                   #NCCC#                         ");
    	System.out.println("NCCC:\t" + nccc);
	}
	public static void writeStatistics(State state) throws IOException{
        File file = new File("statistics.txt");
        FileWriter fileWriter = new FileWriter(file, true);
		if(state == State.SUCCESS) {
			int totalBuildTreeMsg = treeBuildVisitMsgCounter + treeBuildAddAncestorMsgCounter
					+ treeBuildSetChildMsgCounter + treeBuildSetPseudoChildMsgCounter
					+ treeBuildDone + treeBuildVisitRefuse;
			int totalSearchMsg = Counter.VALUEMsgCounter + Counter.COSTMsgCounter +
					Counter.DELMsgCounter + Counter.TERMINATEMsgCounter;
			fileWriter.write(Counter.msgCounter + "\t"
					+ totalSearchMsg + "\t"
					+ totalBuildTreeMsg + "\t"
					+ Counter.VALUEMsgCounter + "\t"
					+ Counter.COSTMsgCounter + "\t"
					+ Counter.DELMsgCounter + "\t"
					+ Counter.TERMINATEMsgCounter + "\t");
			fileWriter.write(nccc + "\t");
			fileWriter.write("\n");
		}
		else{
			fileWriter.write("Error!\n");
		}
		fileWriter.close();
		
	}
	
	public static void clearStatistics(){
		msgCounter = 0;
		VALUEMsgCounter = 0;
		COSTMsgCounter = 0;
		DELMsgCounter = 0;
		TERMINATEMsgCounter = 0;
		
		treeBuildVisitMsgCounter = 0;
		treeBuildAddAncestorMsgCounter = 0;
		treeBuildSetChildMsgCounter = 0;
		treeBuildSetPseudoChildMsgCounter = 0;
		treeBuildDone = 0;
		treeBuildVisitRefuse = 0;
	}
}
