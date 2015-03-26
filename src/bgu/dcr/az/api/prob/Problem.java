package bgu.dcr.az.api.prob;

import bgu.dcr.az.api.prob.cpack.AbstractConstraintPackage;
import bgu.dcr.az.api.prob.cpack.AgentConstraintPackage;
import bgu.dcr.az.api.prob.cpack.ConstraintsPackage;
import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.tools.Assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An abstract class for problems that should let you build any type of problem
 *
 * @author guyafe, edited by bennyl
 */
public class Problem implements ImmutableProblem {

    public ArrayList<Integer> getConstrainedVars(int src, int dest) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
	
	/**
	 * 
	 * @author Olivia
	 *
	 */
    public static enum ModelType {
        single,  // one variable per agent, default one
        multiple_VA,  // multi variables per agent, virtual agent approach
        multiple_OC,  // multi variables per agent, online compilation approach
    }
    
    public ModelType modelType = ModelType.single; //default
    //For algorithm running, for multiple_VA, each agent has only one variable
    protected static HashMap<Integer, ArrayList<Integer>> agentVarMap = new HashMap(); 
    //For multiple_VA, this is the map between real agent and variable
    protected HashMap<Integer, ArrayList<Integer>> realAgentVarMap = new HashMap(); 
    
    /**calculated**/
    protected HashMap<Integer, ArrayList<Integer>> agentExternalVarMap = new HashMap();
    protected HashMap<Integer, ArrayList<Integer>> agentLocalVarMap = new HashMap();
    
    
    private HashMap<String, Object> metadata = new HashMap<>();  
    protected int numvars;
    protected ImmutableSetOfIntegers[] agentDomain;
    protected ConstraintsPackage constraintsOnVars;
    protected AgentConstraintPackage constraintsOnAgents;
    protected static ImmutableSetOfIntegers[] varDomain;
    protected ProblemType type;
    protected int maxCost = 0;
    protected static boolean singleDomain = true;  


    @Override
    public String toString() {
        return ProblemPrinter.toString(this);
    }
    
    
    /**
     *  Olivia added
     *  Add the agent layer
     */
    /****************** Agent layer start ******************/
    /************agent numbers**************/
    public int getNumberOfAgents() {
        return this.agentDomain.length; 
    }
    /************domains**************/
    /**
     * return the agentDomain size of the agent
     *
     * @param agent
     * @return
     */
    public int getAgentDomainSize(int agent) {
        return getAgentDomainOf(agent).size();
    }

    /**
     * return the agentDomain that belongs to agent
     */
    public ImmutableSet<Integer> getAgentDomainOf(int agent) {
        return agentDomain[agent];
        
    }
    /************my variables**************/
    /**
     * Get the variable Ids that belongs to given agentId
     */
    public List<Integer> getVariables(int agentId) {
        return agentVarMap.get(agentId);
    }
    
//    public List<Integer> getExternalVariables(int agentId) {
//    	return agentExternalVarMap.get(agentId);
//    }
//    
//    public List<Integer> geLocalVariables(int agentId) {
//    	return agentLocalVarMap.get(agentId);
//    }
    /************neighbors**************/
    /**
     * @param agent
     * @return all the agent that costrainted with the given agent
     */
    public Set<Integer> getAgentNeighbors(int agent) {
        return constraintsOnAgents.getNeighbores(agent);
    }
    
    public void addAgentNeighbor(int to, int neighbor) {
    	constraintsOnAgents.addNeighbor(to, neighbor);
    }
    /************constraints**************/
    /* only support unary and binary constraints on agents*/
    /**
     * @param agent1
     * @param agent2
     * @return true if there is a constraint between agent1 and agent2 operation
     * cost: o(d^2)cc
     */
    public boolean isAgentConstrained(int agent1, int agent2) {
        return getAgentNeighbors(agent1).contains(agent2);
    }

//    public void setAgentConstraintCost(int owner, int x1, int v1, int x2, int v2, int cost) {
//    	constraintsOnAgents.setConstraintCost(owner, x1, v1, x2, v2, cost);
//    }
//
//    public void setAgentConstraintCost(int owner, int x1, int v1, int cost) {
//    	constraintsOnAgents.setConstraintCost(owner, x1, v1, cost);
//    }
//
//    public void setAgentConstraintCost(int x1, int v1, int x2, int v2, int cost) {
//    	constraintsOnAgents.setConstraintCost(x1, x1, v1, x2, v2, cost);
//    }
//
//    public void setAgentConstraintCost(int x1, int v1, int cost) {
//    	constraintsOnAgents.setConstraintCost(x1, x1, v1, cost);
//    }

//    public void getAgentConstraintCost(int owner, int x1, int v1, ConstraintCheckResult result) {
//    	constraintsOnAgents.getConstraintCost(owner, x1, v1, result);
//    }
//
//    public void getAgentConstraintCost(int owner, int x1, int v1, int x2, int v2,
//    		ConstraintCheckResult result) {
//    	constraintsOnAgents.getConstraintCost(owner, x1, v1, x2, v2, result);
//    }
//
//    public int getAgentConstraintCost(int owner, int x1, int v1) {
//        ConstraintCheckResult result = new ConstraintCheckResult();
//        constraintsOnAgents.getConstraintCost(owner, x1, v1, result);
//        return result.getCost();
//    }
//
//    public int getAgentConstraintCost(int owner, int x1, int v1, int x2, int v2) {
//        ConstraintCheckResult result = new ConstraintCheckResult();
//        constraintsOnAgents.getConstraintCost(owner, x1, v1, x2, v2, result);
//        return result.getCost();
//    }

    public int getAgentConstraintCost(int x1, int v1) {
    	Integer[] myVarsValues = parseAgentValue(x1, v1);
    	ArrayList<Integer> myVars = agentVarMap.get(x1);
    	int cost = 0;
        ConstraintCheckResult result = new ConstraintCheckResult();
    	for(int i = 0; i < myVars.size(); i++) {
    		int varID1 = myVars.get(i);
    		int val1 = myVarsValues[i];
    		for(int j = i+1; j < myVars.size(); j++) {
        		int varID2 = myVars.get(j);
        		int val2 = myVarsValues[j];
        		constraintsOnVars.getConstraintCost(varID1, varID1, val1, varID2, val2, result);
        		cost += result.getCost();
    		}
    	}
    	return cost;
    }

    public int getAgentConstraintCost(int x1, int v1, int x2, int v2) {
    	Integer[] myVarsValues1 = parseAgentValue(x1, v1);
    	ArrayList<Integer> myVars1 = agentVarMap.get(x1);
    	Integer[] myVarsValues2 = parseAgentValue(x2, v2);
    	ArrayList<Integer> myVars2 = agentVarMap.get(x2);
    	ConstraintCheckResult result = new ConstraintCheckResult();
    	int cost = 0;
    	for(int i = 0; i < myVars1.size(); i++) {
    		int varID1 = myVars1.get(i);
    		int val1 = myVarsValues1[i];
    		for(int j = 0; j < myVars2.size(); j++) {
        		int varID2 = myVars2.get(j);
        		int val2 = myVarsValues2[j];
        		constraintsOnVars.getConstraintCost(varID1, varID1, val1, varID2, val2, result);
        		cost += result.getCost();
    		}
    	}
    	
    	return cost;
    }
    
	public static Integer[] parseAgentValue(int agentID, int agentValue){
		ArrayList<Integer> myVars = agentVarMap.get(agentID);	
		int varNum = myVars.size();
		int varDom = getVarDomain().size();
		int last = 0;
		int remain = agentValue;
		Integer[] parsedValue = new Integer[varNum];
		for(int i = 0; i < varNum; i++){
			last = remain % varDom;
			remain = remain / varDom;
			parsedValue[varNum - i - 1] = last;
		}
		return parsedValue;	
	}

//    public void getAgentConstraintCost(int owner, Assignment k, ConstraintCheckResult result) {
//    	constraintsOnAgents.getConstraintCost(owner, k, result);
//    }
//
//    public int getAgentConstraintCost(int owner, Assignment k) {
//        ConstraintCheckResult result = new ConstraintCheckResult();
//        constraintsOnAgents.getConstraintCost(owner, k, result);
//        return result.getCost();
//    }
   
    /****************** Agent layer finish ******************/
    /**-----------------------------------------------------------------------------------**/

    /****************** Variable layer start ******************/
    
    /************variable numbers**************/
    public int getNumberOfVars() {
        return this.varDomain.length;  // In Online Comppilation Approach, this is equal to number of agents
    }

    /************domains**************/   
    /**
     * return the agentDomain size of the variable var
     *
     * @param var
     * @return
     */
    public int getVarDomainSize(int var) {
        return getVarDomainOf(var).size();
    }
    
    /**
     * return the varDomain that belongs to variable var
     */
    public ImmutableSet<Integer> getVarDomainOf(int var) {
        return varDomain[var];
    }
    
    public static ImmutableSet<Integer> getVarDomain() {
//        if (!singleDomain) {
//            throw new UnsupportedOperationException("calling get domain on a "
//            		+ "problem with domain that is unique to each variable is unsupported - call  getDomainOf(int) instaed.");
//        }
        return varDomain[0];
    }
    /************neighbors**************/
    /**
     * @param var
     * @return all the variables that costrainted with the given var
     */
    public Set<Integer> getVarNeighbors(int var) {
        return constraintsOnVars.getNeighbores(var);
    }
    public void addVarNeighbor(int to, int neighbor) {
    	constraintsOnVars.addNeighbor(to, neighbor);
    }
    /*********Constraints************/
    /**
     * @param var1
     * @param var2
     * @return true if there is a constraint between var1 and var2 operation
     * cost: o(d^2)cc
     */
    public boolean isVarConstrained(int var1, int var2) {
        return getVarNeighbors(var1).contains(var2);
    }

    /**
     * @param var1
     * @param val1
     * @param var2
     * @param val2
     * @return true if var1=val1 consistent with var2=val2
     */
    public boolean isVarConsistent(int var1, int val1, int var2, int val2) {
        return getVarConstraintCost(var1, val1, var2, val2) == 0;
    }
    
    /**
     * tries to add new kary constraint - as can be seen from the owner eyes
     * replaces the k-ary constraint if it exists
     *
     * @param owner
     * @param constraint
     */
    public void setVarConstraint(int owner, KAryConstraint constraint) {
    	constraintsOnVars.setConstraintCost(owner, constraint);
    }

    /**
     * add into the existing constrains
     *
     * @param owner
     * @param constraint
     */
    public void addVarConstraint(int owner, KAryConstraint constraint) {
    	constraintsOnVars.addConstraintCost(owner, constraint);
    }

    /**
     * symmetrically adding the constraint to all of the participants
     *
     * @param constraint
     */
    public void addVarConstraint(KAryConstraint constraint) {
        for (int participant : constraint.getParicipients()) {
        	constraintsOnVars.addConstraintCost(participant, constraint);
        }
    }

    /**
     * symmetrically setting the constraint to all of the participants
     *
     * @see setConstraintCost
     *
     * @param constraint
     */
    public void setVarConstraint(KAryConstraint constraint) {
        for (int participant : constraint.getParicipients()) {
        	constraintsOnVars.setConstraintCost(participant, constraint);
        }
    }

    public void setVarConstraintCost(int owner, int x1, int v1, int x2, int v2, int cost) {
    	constraintsOnVars.setConstraintCost(owner, x1, v1, x2, v2, cost);
    }

    public void setVarConstraintCost(int owner, int x1, int v1, int cost) {
    	constraintsOnVars.setConstraintCost(owner, x1, v1, cost);
    }

    public void setVarConstraintCost(int x1, int v1, int x2, int v2, int cost) {
    	constraintsOnVars.setConstraintCost(x1, x1, v1, x2, v2, cost);
    }

    public void setVarConstraintCost(int x1, int v1, int cost) {
    	constraintsOnVars.setConstraintCost(x1, x1, v1, cost);
    }

    public void getVarConstraintCost(int owner, int x1, int v1, ConstraintCheckResult result) {
    	constraintsOnVars.getConstraintCost(owner, x1, v1, result);
    }

    public void getVarConstraintCost(int owner, int x1, int v1, int x2, int v2, ConstraintCheckResult result) {
    	constraintsOnVars.getConstraintCost(owner, x1, v1, x2, v2, result);
    }

    public int getVarConstraintCost(int owner, int x1, int v1) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        constraintsOnVars.getConstraintCost(owner, x1, v1, result);
        return result.getCost();
    }

    public int getVarConstraintCost(int owner, int x1, int v1, int x2, int v2) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        constraintsOnVars.getConstraintCost(owner, x1, v1, x2, v2, result);
        return result.getCost();
    }

    public int getVarConstraintCost(int x1, int v1) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        constraintsOnVars.getConstraintCost(x1, x1, v1, result);
        return result.getCost();
    }

    public int getVarConstraintCost(int x1, int v1, int x2, int v2) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        constraintsOnVars.getConstraintCost(x1, x1, v1, x2, v2, result);
        return result.getCost();
    }

    public void getVarConstraintCost(int owner, Assignment k, ConstraintCheckResult result) {
    	constraintsOnVars.getConstraintCost(owner, k, result);
    }

    public int getVarConstraintCost(int owner, Assignment k) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        constraintsOnVars.getConstraintCost(owner, k, result);
        return result.getCost();
    }

    /**
     * calculate the cost of the given assignment - taking into consideration
     * all the constraints that related to it, the method put the return value
     * in an array of 2 [cost, number of constraints checked] that is given to
     * the method as the parameter ans
     *
     * @param owner
     * @param assignment
     * @return
     */
    public void calculateCost(int owner, Assignment assignment, ConstraintCheckResult result) {
    	constraintsOnVars.calculateCost(owner, assignment, result);
    }

    public int calculateGlobalCost(Assignment a) {
        return constraintsOnVars.calculateGlobalCost(a);
    }
    
    /****************** Variable layer finish ******************/
    
    /**
     * @return this problem metadata
     */
    public HashMap<String, Object> getMetadata() {
        return metadata;
    }
    
    /************************Start of problem initialization*****************************************/
    
    /**
     * model type: single
     * @param size
     */
    protected void createAgentVarMapOfVariblesSize(int size) {
        for (int i = 0; i < size; i++) {
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(i);
            agentVarMap.put(i, temp);
            realAgentVarMap.put(i, temp);
        }     
    }

    /**
     * model type: single
     * @param type
     * @param agentDomain
     * @param singleDomain
     */
    protected void initialize(ProblemType type, List<? extends Set<Integer>> agentDomains, boolean singleDomain) {
        this.singleDomain = singleDomain;
        this.agentDomain = ImmutableSetOfIntegers.arrayOf(agentDomains);
        this.type = type;
        
        if (modelType != ModelType.multiple_OC) {  // If not OC, numvars is the number of variables
        	this.varDomain = agentDomain;
            this.numvars = this.varDomain.length;
            int maxDomainSize = 0;
            for(ImmutableSetOfIntegers dom : this.agentDomain){
            	if(maxDomainSize < dom.size()){
            		maxDomainSize = dom.size();
            	}
            }
            createAgentVarMapOfVariblesSize(numvars);
        	this.constraintsOnVars = type.newConstraintPackage(numvars, maxDomainSize); 
        	this.constraintsOnAgents = new AgentConstraintPackage(this.agentDomain.length);
        }
        else { // If OC, numvars here actually represents the number of agents
            this.numvars = this.varDomain.length;
            int maxDomainSize = 0;
            for(ImmutableSetOfIntegers dom : this.varDomain){
            	if(maxDomainSize < dom.size()){
            		maxDomainSize = dom.size();
            	}
            }
        	this.constraintsOnVars = type.newConstraintPackage(numvars, maxDomainSize); 
        	this.constraintsOnAgents = new AgentConstraintPackage(this.agentDomain.length);
        }
        
        
    }
    

    /**
     * initialize the problem with multiple domains the number of variables is
     * the agentDomain.size()
     *
     * @param type    the type of the problem
     * @param domains list of domains for each agent - this list also determines
     *                the number of variables that will be domains.size
     */
    public void initialize(ProblemType type, List<? extends Set<Integer>> domains) {
        initialize(type, domains, false);
    }

    /**
     * initialize the problem with a single agentDomain
     *
     * @param type              the problem type
     * @param numberOfVariables number of variables in this problem
     * @param agentDomain            the agentDomain for all the variables.
     */
    public void initialize(ProblemType type, int numberOfVariables, Set<Integer> agentDomain) {
        initialize(type, ImmutableSetOfIntegers.repeat(agentDomain, numberOfVariables), true);
    }

    /**
     * initialize the problem with a single agentDomain that its values are
     * 0..1-domainSize
     *
     * @param type
     * @param numberOfVariables
     * @param domainSize
     */
    public void initialize(ProblemType type, int numberOfVariables, int domainSize) {
        initialize(type, numberOfVariables, new HashSet<Integer>(Agt0DSL.range(0, domainSize - 1)));
    }
    
    /**
     * @author Olivia
     * @param type
     * @param numberOfVariables
     * @param domainSize
     * @param model
     * @param runningAgentVarMap
     * @param trueAgentVarMap
     * This method would be called by approaches other than OC
     */
    public void initialize(ProblemType type, int numberOfVariables, List<? extends Set<Integer>> agentDomain,
    		ModelType model, HashMap runningAgentVarMap, HashMap trueAgentVarMap) {   
    	modelType = model;
        initialize(type, agentDomain);
    	agentVarMap = runningAgentVarMap;
    	realAgentVarMap = trueAgentVarMap;
    }
    
    /**
     * @author Olivia
     * @param type
     * @param numberOfVariables
     * @param agentDomain
     * @param varDomainMap
     * @param model
     * @param runningAgentVarMap
     * @param trueAgentVarMap
     * This method would be called by OC approach
     */
    public void initialize(ProblemType type, int numberOfVariables, List<? extends Set<Integer>> agentDomain,
    		HashMap varDomainMap, ModelType model, HashMap runningAgentVarMap, HashMap trueAgentVarMap) {
    	modelType = model;
    	agentVarMap = runningAgentVarMap;
    	realAgentVarMap = trueAgentVarMap;
    	
    	List<Set<Integer>> varDomains = new ArrayList();
    	for (int varID = 0; varID < varDomainMap.size(); varID++) {
    		int varDomainSize = (int) varDomainMap.get(varID);
    		Set<Integer> dom = new HashSet<Integer>(Agt0DSL.range(0, varDomainSize - 1));
    		varDomains.add(dom);
    	}
    	this.varDomain = ImmutableSetOfIntegers.arrayOf(varDomains);
    	initialize(type, agentDomain);
    }
    
    public void setupAgentNeighbors(){
    	boolean isConstrained = false;
    	for (int agent1 : agentVarMap.keySet()) {
    		ArrayList<Integer> agent1Vars = agentVarMap.get(agent1);
    		for (int agent2: agentVarMap.keySet()) {
        		isConstrained = false;
    			if (agent1 != agent2) {
    				ArrayList<Integer> agent2Vars = agentVarMap.get(agent2);
    				for (int var1 : agent1Vars) {
    					for (int var2 : agent2Vars) {
    						if (this.isVarConstrained(var1, var2)) {
    							isConstrained = true;
    							break;
    						}
    					}
    					if (isConstrained) {
    						break;
    					}
    				}
    				
    			}
    			if (isConstrained) {
    				this.addAgentNeighbor(agent1, agent2);
    			}    			
    		}
    		
    	}
    }
    
    /************************End of problem initialization*****************************************/

    /**
     * @return the type of the problem
     */
    public ProblemType type() {
        return type;
    }

    public int getConstraintCost(Assignment ass) {
        throw new UnsupportedOperationException("Not supported without providing owner. "
        		+ "Please use getConstraintCost(int owner, Assignment ass)");
    }

    public int calculateCost(Assignment a) {
        throw new UnsupportedOperationException("Not supported when not accessed from inside of an agent code - "
        		+ "please use getGlobalCost");
    }

    /**
     * this class is required to allow array of this type as java cannot create
     * an array of generic types and we want to avoid uneccecery casting
     */
    protected static class ImmutableSetOfIntegers extends ImmutableSet<Integer> {

        public ImmutableSetOfIntegers(Collection<Integer> data) {
            super(data);
        }

        public static ImmutableSetOfIntegers[] arrayOf(List<? extends Set<Integer>> of) {
            ImmutableSetOfIntegers[] a = new ImmutableSetOfIntegers[of.size()];

            int i = 0;
            for (Set<Integer> o : of) {
                a[i++] = new ImmutableSetOfIntegers(o);
            }

            return a;
        }

        public static List<ImmutableSetOfIntegers> repeat(Set<Integer> set, int times) {
            ImmutableSetOfIntegers[] ret = new ImmutableSetOfIntegers[times];
            ImmutableSetOfIntegers is = new ImmutableSetOfIntegers(set);
            for (int i = 0; i < ret.length; i++) {
                ret[i] = is;
            }

            return Arrays.asList(ret);
        }
    }


    
    /**
     * 
     * @param varId
     * @return the agentID that contains the given variable
     *         if not found, return -1
     */
    public int getMyRealAgentId(int id){
    	if (modelType == ModelType.multiple_OC) {
    		return id;
    	}
    	for(int agent: realAgentVarMap.keySet()){
    		for(int myVarID : realAgentVarMap.get(agent)){
    			if(myVarID == id) {
    				return agent;
    			}
    		}
    	}
    	return -1;
    }

}
