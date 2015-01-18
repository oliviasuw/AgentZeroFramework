//package bgu.dcr.az.dev.tools;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.HashMap;
//
//public class VarAgentMap extends HashMap<Integer, Integer>{
//	
//	private static final long serialVersionUID = -7132226922211209400L;
//	
//	private int agentsNo;
//	
//	public VarAgentMap(File testFile){
//
//		agentsNo = 0;
//    	BufferedReader reader = null;
//    	try{
//    		reader = new BufferedReader(new FileReader(testFile));
//    		String line = null;
//    		while((line = reader.readLine())!=null){
//    			int i = 0, j = 0;
//    			if(line.contains("PSEUDOAGENT")){
//    				agentsNo++;
//    			}
//    			if(line.contains("VARIABLE")){
//    				String[] tokStrings = line.split("\\s");
//    				i = Integer.parseInt(tokStrings[1]);
//    				j = Integer.parseInt(tokStrings[2]);
//    				this.put(i, j);
//    			}
//    		}
//    	}catch (NumberFormatException | IOException exception){
//    		System.out.println(exception);
//    	}
//	}
//	
//	public int getAgentsNo(){
//		return agentsNo;
//	}
//	
//}
