/**
 * Hybridization.java
   Created by Su Wen
   Date: Apr 20, 2015
   Time: 11:06:08 AM 
 */
package bgu.dcr.az.dev.modules.probgen;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import confs.defs;

@Register(name = "HybridProbGen")
public class Hybridization extends AbstractProblemGenerator {

    //ADD ANY VARIABLES YOU NEED HERE LIKE THIS:
    @Variable(name = "fileNo", description = "index of file", defaultValue = "1")
    int fileNo = 1;
    boolean first = true;
    String fileName;
	
    public void generate(Problem p, Random rand) {
    	File dir = new File("../problems");
    	File[] files = dir.listFiles();
    	fileName = files[fileNo-1].getName();
    	System.out.println(fileName);
    	writeToFile();
    	BufferedReader reader = null;
    	boolean isInitialized = false;
    	try{
    		reader = new BufferedReader(new FileReader(files[fileNo-1]));
    		String line = null;
    		int i = 0, j = 0;
    		HashMap<Integer, ArrayList<Integer>> agentVarMap = new HashMap<>();
            HashMap<Integer, Integer> varDomainMap = new HashMap();  //key: varID  value: domainSize of the var
    		while((line = reader.readLine())!=null){
    			if(line.contains("VARIABLE")){
    				String[] tokStrings = line.split("\\s");
                                int varID = Integer.parseInt(tokStrings[1]);
                                int agentID = Integer.parseInt(tokStrings[2]);
    				int domainSize = Integer.parseInt(tokStrings[4]);
                                varDomainMap.put(varID, domainSize);
    				if(agentVarMap.containsKey(agentID)){
    					agentVarMap.get(agentID).add(varID);
    				}else{
    					ArrayList<Integer> vars = new ArrayList<>();
    					vars.add(varID);
    					agentVarMap.put(agentID, vars);
    				}
    			}
    			if(line.contains("CONSTRAINT")){
    				if(!isInitialized){
                                        List<Set<Integer>> agentDomains = new ArrayList();
//                                        int agentNum = agentVarMap.size();
//                                        int agentDomainSize = 1;
//                                        ArrayList<Integer> varsInAgent = new ArrayList<>();
//                                        for(int k = 0; k < agentNum; k++) {
//                                            varsInAgent = agentVarMap.get(k);
//                                            agentDomainSize = 1;
//                                            for(int varID : varsInAgent) {
//                                                int varDom = varDomainMap.get(varID);
//                                                agentDomainSize *= varDom;
//                                            }
//                                            Set<Integer> dom = new HashSet<Integer>(Agt0DSL.range(0, agentDomainSize - 1));
//                                            agentDomains.add(dom);
//                                        }
                                        p.initialize(ProblemType.DCOP, agentVarMap.size(), agentDomains,
                                        		varDomainMap, Problem.ModelType.hybridize, 
    							agentVarMap, agentVarMap);
    					isInitialized = true;
    				}
    				String[] tokStrings = line.split("\\s");
    				i = Integer.parseInt(tokStrings[1]);
    				j = Integer.parseInt(tokStrings[3]);
    			}
    			if(line.contains("F")){
    				String[] tokStrings = line.split("\\s");
    				//System.out.println("i = " + i + ", vi = "  + Integer.parseInt(tokStrings[1]) + ", j = " + j + ", vj = "  + Integer.parseInt(tokStrings[2]) +", cost: " + Integer.parseInt(tokStrings[3]));
    				p.setVarConstraintCost(i, Integer.parseInt(tokStrings[1]), j, 
    						Integer.parseInt(tokStrings[2]), Integer.parseInt(tokStrings[3]));
    				p.setVarConstraintCost(j, Integer.parseInt(tokStrings[2]), i, 
    						Integer.parseInt(tokStrings[1]), Integer.parseInt(tokStrings[3]));
    			}
    		}
    		p.Hybridize(defs.MAX_DOMAIN_PER_VIRTUAL_AGENT, defs.MIN_CONNECTIVITY_INSIDE_VIRTUAL_AGENT);
    		p.setupAgentNeighbors();
    	
    	}catch (NumberFormatException | IOException exception){
    		System.out.println(exception);
    	}
    }
      
    private void writeToFile(){
    	File costFile = new File("statistics.txt");
    	try {
        	if(first){
        		if(costFile.exists()){
        			costFile.delete();
        			costFile = new File("statistics.txt");
        		}
        		first = false;
        	}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter
					(new FileOutputStream(costFile, true)));
			writer.write(fileName + " ");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}


