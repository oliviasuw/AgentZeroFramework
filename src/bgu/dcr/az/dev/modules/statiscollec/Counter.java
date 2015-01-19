/**
 * counter.java
   Created by Su Wen
   Date: Jan 12, 2015
   Time: 9:38:14 PM 
 */
package bgu.dcr.az.dev.modules.statiscollec;

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

	public static void reportStatistics(){
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
