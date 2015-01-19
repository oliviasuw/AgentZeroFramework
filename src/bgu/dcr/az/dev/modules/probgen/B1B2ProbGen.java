package bgu.dcr.az.dev.modules.probgen;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

import java.io.*;
import java.util.Random;

@Register(name = "b1b2ProbGen")
public class B1B2ProbGen extends AbstractProblemGenerator {

	@Variable(name = "fileNo", description = "index of file", defaultValue = "1")
    int fileNo = 1;
    
	boolean first = true;
	String fileName;
	
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
    	try{
    		reader = new BufferedReader(new FileReader(files[fileNo-1]));
    		String line = null;
    		int i = 0, j = 0;
    		while((line = reader.readLine())!=null){	
    			if(line.contains("VARIABLE")){
    				String[] tokStrings = line.split("\\s");
    				agentsNo++;
    				domainSize = Integer.parseInt(tokStrings[4]);
    			}
    			if(line.contains("CONSTRAINT")){
    				if(!isInitialized){
    					p.initialize(ProblemType.DCOP, agentsNo, domainSize);
    					isInitialized = true;
    				}
    				String[] tokStrings = line.split("\\s");
    				i = Integer.parseInt(tokStrings[1]);
    				j = Integer.parseInt(tokStrings[3]);
    			}
    			if(line.contains("F")){
    				String[] tokStrings = line.split("\\s");
    				//System.out.println("i = " + i + ", vi = "  + Integer.parseInt(tokStrings[1]) + ", j = " + j + ", vj = "  + Integer.parseInt(tokStrings[2]) +", cost: " + Integer.parseInt(tokStrings[3]));
    				p.setConstraintCost(i, Integer.parseInt(tokStrings[1]), j, Integer.parseInt(tokStrings[2]), Integer.parseInt(tokStrings[3]));
    				p.setConstraintCost(j, Integer.parseInt(tokStrings[2]), i, Integer.parseInt(tokStrings[1]), Integer.parseInt(tokStrings[3]));
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
