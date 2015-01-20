/**
 * OC_multiVarsProbGen.java
   Created by Su Wen
   Date: Jan 15, 2015
   Time: 5:27:18 PM 
 */
package bgu.dcr.az.dev.modules.probgen;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.Problem.ModelType;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.dev.modules.statiscollec.COSTMessageCounter.AgentType;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

@Register(name = "OC_ProbGen")
public class OC_multiVarsProbGen extends AbstractProblemGenerator {

	@Variable(name = "fileNo", description = "index of file", defaultValue = "1")
    int fileNo = 1;
    
	boolean first = true;
	String fileName;
    protected HashMap<Integer, ArrayList<Integer>> agentVarMap; //For algorithm running, for multiple_VA, each agent has only one variable
    protected HashMap<Integer, ArrayList<Integer>> realAgentVarMap; //For multiple_VA, this is the map between real agent and variable
	
    @Override
    public void generate(Problem p, Random rand) {
    	File dir = new File("problems");
    	File[] files = dir.listFiles();
    	fileName = files[fileNo-1].getName();
    	writeToFile();
    	BufferedReader reader = null;
    	boolean isInitialized = false;
    	int agentsNo = 0;
    	int domainSize = 0;
    	agentVarMap = new HashMap();
    	realAgentVarMap = new HashMap();
    	try{
    		reader = new BufferedReader(new FileReader(files[fileNo-1]));
    		String line = null;
    		int i = 0, j = 0;
    		while((line = reader.readLine())!=null){	
    			if(line.contains("VARIABLE")){
    				String[] tokStrings = line.split("\\s");
    				agentsNo++;
    				domainSize = Integer.parseInt(tokStrings[4]);
    				
    				int varID = Integer.parseInt(tokStrings[1]);
    				int agentID = Integer.parseInt(tokStrings[2]);
    				
    	            ArrayList<Integer> temp = new ArrayList<>();
    	            temp.add(varID);
    				
    				if(realAgentVarMap.containsKey(agentID)){
    					temp = realAgentVarMap.get(agentID);
    					temp.add(varID);
    				}
    				realAgentVarMap.put(agentID, temp);
    				agentVarMap.put(agentID, temp);  // The running agent is always the same as the real agent
    				
    			}
    			if(line.contains("CONSTRAINT")){
    				if(!isInitialized){
    					p.initialize(ProblemType.DCOP, agentsNo, domainSize, ModelType.multiple_OC, agentVarMap, realAgentVarMap);
    					isInitialized = true;
    				}
    				String[] tokStrings = line.split("\\s");
    				i = Integer.parseInt(tokStrings[1]);
    				j = Integer.parseInt(tokStrings[3]);
    			}
    			if(line.contains("F")){
    				String[] tokStrings = line.split("\\s");
    				//System.out.println("i = " + i + ", vi = "  + Integer.parseInt(tokStrings[1]) + ", j = " + j + ", vj = "  + Integer.parseInt(tokStrings[2]) +", cost: " + Integer.parseInt(tokStrings[3]));
    				p.setConstraintCost(i, Integer.parseInt(tokStrings[1]), j, Integer.parseInt(tokStrings[2]),
    						Integer.parseInt(tokStrings[3]));
    				p.setConstraintCost(j, Integer.parseInt(tokStrings[2]), i, Integer.parseInt(tokStrings[1]),
    						Integer.parseInt(tokStrings[3]));
    			}
    		}
    	}catch (NumberFormatException | IOException exception){
    		System.out.println(exception);
    	}
    }
    
    private void writeToFile(){
    	File costFile = new File("costs.txt");
    	try {
        	if(first){
        		if(costFile.exists()){
        			costFile.delete();
        			costFile = new File("costs.txt");
        		}
        		first = false;
        	}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(costFile, true)));
			writer.write(fileName + " ");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}

