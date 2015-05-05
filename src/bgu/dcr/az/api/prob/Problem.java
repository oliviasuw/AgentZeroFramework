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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        hybridize  // multi variables per agent
    }
    
    public ModelType modelType = ModelType.single; //default
    //For algorithm intialization, for multiple_VA, each agent has only one variable
    protected static HashMap<Integer, ArrayList<Integer>> intAgentVarMap = new HashMap(); 
    //For algorithm running, for multiple_VA, each agent has only one variable; for OC, each agent holds the exact variables belonging to it;
    //for hybridization,  an agent is a virtual agent that holds the variables in its clusters after redistributing the variables inside an agent
    protected static HashMap<Integer, ArrayList<Integer>> agentVarMap = new HashMap(); 
    //For multiple_VA, this is the map between real agent and variable
    protected HashMap<Integer, ArrayList<Integer>> realAgentVarMap = new HashMap(); 
    
    /**calculated**/
    protected static HashMap<Integer, ArrayList<Integer>> agentPrincipalVarMap = new HashMap();
    protected HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> agentStrongPrincipalVarMap = new HashMap();
    protected HashMap<Integer, Boolean> agentHasInitialized = new HashMap();
    protected HashMap<Integer, List<Integer>> agentChildrenMap = new HashMap();
    protected HashMap<Integer, HashMap<Integer, List<Integer>>> agentChildDescendMap = new HashMap();
    
    private HashMap<String, Object> metadata = new HashMap<>();  
    protected int numvars;
//    protected ImmutableSetOfIntegers[] agentDomain;
    protected HashMap<Integer, Integer> agentDomainSize;
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
        return this.agentDomainSize.size(); 
    }
    /************agent status**************/
    public boolean hasAgentInitialized(int agentID) {
    	return agentHasInitialized.get(agentID);
    }
    public boolean setAgentInitialized(int agentID) {
    	return agentHasInitialized.put(agentID, true);
    }
    
    public boolean hasAllInitialized() {
    	boolean allInitialized = true;
    	for(int agentID: agentVarMap.keySet()) {
    		if(!hasAgentInitialized(agentID)) {
    			allInitialized = false;
    		}
    	}
    	return allInitialized;
    }
    /************domains**************/
    /**
     * return the agentDomain size of the agent
     *
     * @param agent
     * @return
     */
    public int getAgentDomainSize(int agent) {
    	return this.agentDomainSize.get(agent);
    }
//    public int getAgentDomainSize(int agent) {
//        return getAgentDomainOf(agent).size();
//    }

    /**
     * return the agentDomain that belongs to agent
     */
//    public ImmutableSet<Integer> getAgentDomainOf(int agent) {
//        return agentDomain[agent];   
//    }
    /************my variables**************/
    /**
     * Get the variable Ids that belongs to given agentId
     */
    public List<Integer> getVariables(int agentId) {
        return agentVarMap.get(agentId);
    }
        
    /** tree structure **/
	public void setChildren(int agent, List<Integer> children) {
		agentChildrenMap.put(agent, children);
		
	}
	
	public List<Integer> getChildren(int agent) {
		return agentChildrenMap.get(agent);
	}
	
	public void setChildDescendMap(int agent,
			HashMap<Integer, List<Integer>> childDescendMap) {
		agentChildDescendMap.put(agent, childDescendMap);
		
	}

	public HashMap<Integer, List<Integer>> getChildDescendMap(int agent) {
		// TODO Auto-generated method stub
		return agentChildDescendMap.get(agent);
	}
	
    public void setWeakPrincipalVariables(int agentID, ArrayList<Integer> principleVars) {
    	agentPrincipalVarMap.put(agentID, principleVars);
    }
    
    public int getWeakPrincipalVarsHashValue(int agentID, int hashVal) {
    	int principleVarsHashVal = 0;
    	Integer[] myVarsValues = parseAgentValue(agentID, hashVal);
    	ArrayList<Integer> myVars = agentVarMap.get(agentID);

    	List<Integer> myPrincipalVars = agentPrincipalVarMap.get(agentID);
    	int myPrincipalVarNum = myPrincipalVars.size();
    	int varDom = getVarDomain().size();
    	int count = 0;
    	for (int i = 0; i < myVars.size(); i++) {
    		int myVarID = myVars.get(i);
    		if (myPrincipalVars.contains(myVarID)) {
    			count ++;
    			principleVarsHashVal += myVarsValues[i] * Math.pow(varDom, myPrincipalVarNum - count);
    		}
    	} 	
    	return principleVarsHashVal;

    }
    
    public ArrayList<Integer> getFullValListFromExternalVal(int agentID, int exterVal) {
    	ArrayList<Integer> fullValues = new ArrayList();
    	for(int i = 0; i < this.getAgentDomainSize(agentID); i++){
    		int myExternalValue = getWeakPrincipalVarsHashValue(agentID, i);
    		if(myExternalValue == exterVal) {
    			fullValues.add(i);
    		}
    	}
    	
    	return fullValues;

    }
    
    
    public List<Integer> getWeakPrincipalVariables(int agentId) {
    	return agentPrincipalVarMap.get(agentId);
    }
    
	public void setStrongPrincipalVariables(int agentId,
			HashMap<Integer, ArrayList<Integer>> childPrincipleVarsMap) {
		agentStrongPrincipalVarMap.put(agentId, childPrincipleVarsMap);
//		ArrayList<Integer> allWeakPrincipalVars = new ArrayList();
//		for(int child : childPrincipleVarsMap.keySet()) {
//			ArrayList<Integer> strongPrincipalVars = childPrincipleVarsMap.get(child);
//			for(int strongPrincipalVar : strongPrincipalVars) {
//				if(!allWeakPrincipalVars.contains(strongPrincipalVar)) {
//					allWeakPrincipalVars.add(strongPrincipalVar);
//				}
//			}
//		}
//		agentPrincipalVarMap.put(agentId, allWeakPrincipalVars);
		
	}
	
    public List<Integer> getStrongPrincipalVariables(int agentId, int child) {
    	return agentStrongPrincipalVarMap.get(agentId).get(child);
    }
    
    public int getStrongPrincipalVarsHashValue(int agentID, int child, int hashVal) {

    	int principleVarsHashVal = 0;
    	Integer[] myVarsValues = parseAgentValue(agentID, hashVal);
    	ArrayList<Integer> myVars = agentVarMap.get(agentID);

    	List<Integer> myPrincipalVars = agentStrongPrincipalVarMap.get(agentID).get(child);
    	int myPrincipalVarNum = myPrincipalVars.size();
    	int varDom = getVarDomain().size();
    	int count = 0;
    	for (int i = 0; i < myVars.size(); i++) {
    		int myVarID = myVars.get(i);
    		if (myPrincipalVars.contains(myVarID)) {
    			count ++;
    			principleVarsHashVal += myVarsValues[i] * Math.pow(varDom, myPrincipalVarNum - count);
    		}
    	} 	
    	return principleVarsHashVal;

    }
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

    public int getAgentConstraintCost(int x1, int v1, ConstraintCheckResult resultOnAgents) {
    	Integer[] myVarsValues = parseAgentValue(x1, v1);
    	ArrayList<Integer> myVars = agentVarMap.get(x1);
    	int cost = 0;
    	int cc = 0;
        ConstraintCheckResult result = new ConstraintCheckResult();
        
        // Unary costs between agents
        for(int i = 0; i < myVars.size(); i++) {
        	int varID = myVars.get(i);
        	int val = myVarsValues[i];
        	constraintsOnVars.getConstraintCost(varID, varID, val, result);
    		cost += result.getCost();
    		cc += result.getCheckCost();
        }
        
        
        // Binary costs between agents
    	for(int i = 0; i < myVars.size(); i++) {
    		int varID1 = myVars.get(i);
    		int val1 = myVarsValues[i];
    		for(int j = i+1; j < myVars.size(); j++) {
        		int varID2 = myVars.get(j);
        		int val2 = myVarsValues[j];
        		constraintsOnVars.getConstraintCost(varID1, varID1, val1, varID2, val2, result);
        		cost += result.getCost();
        		cc += result.getCheckCost();
    		}
    	}
    	
    	resultOnAgents.set(cost, cc);
    	return cost;
    }

    public int getAgentConstraintCost(int x1, int v1, int x2, int v2, ConstraintCheckResult resultOnAgents) {
    	Integer[] myVarsValues1 = parseAgentValue(x1, v1);
    	ArrayList<Integer> myVars1 = agentVarMap.get(x1);
    	Integer[] myVarsValues2 = parseAgentValue(x2, v2);
    	ArrayList<Integer> myVars2 = agentVarMap.get(x2);
    	ConstraintCheckResult resultOnVars = new ConstraintCheckResult();
    	int cost = 0;
    	int cc = 0;
    	for(int i = 0; i < myVars1.size(); i++) {
    		int varID1 = myVars1.get(i);
    		int val1 = myVarsValues1[i];
    		for(int j = 0; j < myVars2.size(); j++) {
        		int varID2 = myVars2.get(j);
        		int val2 = myVarsValues2[j];
        		constraintsOnVars.getConstraintCost(varID1, varID1, val1, varID2, val2, resultOnVars);
        		cost += resultOnVars.getCost();
        		cc += resultOnVars.getCheckCost();
    		}
    	}
    	
    	resultOnAgents.set(cost, cc);
    	return cost;
    }
    
	public static Integer[] parseAgentValue(int agentID, int agentValue){
		ArrayList<Integer> myVars = agentVarMap.get(agentID);
		int varNum = myVars.size();
		Integer[] parsedValue = new Integer[varNum];
		if(1 == varNum){
			parsedValue[0] = agentValue;
			return parsedValue;
		}
		
		int varDom = getVarDomain().size();
		int last = 0;
		int remain = agentValue;
		
		for(int i = 0; i < varNum; i++){
			last = remain % varDom;
			remain = remain / varDom;
			parsedValue[varNum - i - 1] = last;
		}
		return parsedValue;	
	}
	
//	public static Integer[] parseAgentExternalValue(int agentID, int agentExternalValue){
//		ArrayList<Integer> myExternalVars = agentPrincipalVarMap.get(agentID);	
//		int varNum = myExternalVars.size();
//		int varDom = getVarDomain().size();
//		int last = 0;
//		int remain = agentExternalValue;
//		Integer[] parsedValue = new Integer[varNum];
//		for(int i = 0; i < varNum; i++){
//			last = remain % varDom;
//			remain = remain / varDom;
//			parsedValue[varNum - i - 1] = last;
//		}
//		return parsedValue;	
//	}

   
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
//    	constraintsOnVars.calculateCost(owner, assignment, result);
    	calculateCostOnAgents(assignment, result);
    }
    
    public void calculateCostOnAgents(Assignment assignment, ConstraintCheckResult result) {
        int c = 0;
        int cc = 0;

        LinkedList<Map.Entry<Integer, Integer>> past = new LinkedList<Map.Entry<Integer, Integer>>();
        for (Map.Entry<Integer, Integer> e : assignment.getAssignments()) {
            int var = e.getKey();
            int val = e.getValue();
            getAgentConstraintCost(var, val, result);
            c += result.getCost();
            cc += result.getCheckCost();

            for (Map.Entry<Integer, Integer> pe : past) {
                int pvar = pe.getKey();
                int pval = pe.getValue();

                getAgentConstraintCost(pvar, pval, var, val, result);
                c += result.getCost();
                cc += result.getCheckCost();
            }
            past.add(e);
        }

        result.set(c, cc);
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
//        this.agentDomain = ImmutableSetOfIntegers.arrayOf(agentDomains);
        this.type = type;
        
        if (modelType != ModelType.multiple_OC && modelType != ModelType.hybridize) {  // If not OC, numvars is the number of variables
//        	this.agentDomain = ImmutableSetOfIntegers.arrayOf(agentDomains);
//        	this.varDomain = agentDomain;
        	this.varDomain = ImmutableSetOfIntegers.arrayOf(agentDomains);
            this.numvars = this.varDomain.length;
            int maxDomainSize = 0;
            for(ImmutableSetOfIntegers dom : this.varDomain){
            	if(maxDomainSize < dom.size()){
            		maxDomainSize = dom.size();
            	}
            }
            createAgentVarMapOfVariblesSize(numvars);

        	this.constraintsOnVars = type.newConstraintPackage(numvars, maxDomainSize); 
        	this.constraintsOnAgents = new AgentConstraintPackage(this.varDomain.length);
        }
        else { // If OC, numvars here actually represents the number of agents
        	if(modelType != ModelType.hybridize){
                this.numvars = this.varDomain.length;
                int maxDomainSize = 0;
                for(ImmutableSetOfIntegers dom : this.varDomain){
                	if(maxDomainSize < dom.size()){
                		maxDomainSize = dom.size();
                	}
                }
            	this.constraintsOnVars = type.newConstraintPackage(numvars, maxDomainSize); 
            	this.constraintsOnAgents = new AgentConstraintPackage(agentDomains.size());
            	
            	this.agentDomainSize = new HashMap();
                ArrayList<Integer> varsInAgent = new ArrayList();
                int agentDomainSize = 1;
                for(int k = 0; k < agentVarMap.size(); k++) {
                    varsInAgent = agentVarMap.get(k);
                    agentDomainSize = 1;
                    for(int varID : varsInAgent) {
                        int varDom = this.getVarDomainSize(varID);
                        agentDomainSize *= varDom;
                    }
                    this.agentDomainSize.put(k, agentDomainSize);
                }
        	}
        	else{
                this.numvars = this.varDomain.length;
                int maxDomainSize = 0;
                for(ImmutableSetOfIntegers dom : this.varDomain){
                	if(maxDomainSize < dom.size()){
                		maxDomainSize = dom.size();
                	}
                }
            	this.constraintsOnVars = type.newConstraintPackage(numvars, maxDomainSize); 
      
        	}

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
    	
    	this.agentDomainSize = new HashMap();
    	for(int agent : agentVarMap.keySet()){
    		agentDomainSize.put(agent, getVarDomainSize(agent));
    	}
    	
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
//    	intAgentVarMap = runningAgentVarMap;
    	realAgentVarMap = trueAgentVarMap;
    	
    	if(modelType != ModelType.hybridize){
    		agentVarMap = runningAgentVarMap;
        	for (int agentID : agentVarMap.keySet()) {
        		agentHasInitialized.put(agentID, false);
        	}
    	}
    	else{
    		intAgentVarMap = runningAgentVarMap;
    	}

//    	agentPrincipalVarMap = agentVarMap;  // initialize all the varibles as principal variables at first
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
    
    public void Hybridize(int domainsizeThreshold, double connThreshold){
    	HashMap<Integer, ArrayList<Integer>> newAgentVarMap = new HashMap();
    	int count = 0;
    	for(int agent : intAgentVarMap.keySet()){
    		ArrayList<Integer> myVariables = intAgentVarMap.get(agent);
    		ArrayList<ArrayList<Integer>> clusters = GraphSplit(agent, myVariables, 
    				domainsizeThreshold, connThreshold);
    		for(ArrayList<Integer> aCluster : clusters){
    			newAgentVarMap.put(count, aCluster);
    			count ++;
    		}
    	}
    	agentVarMap = newAgentVarMap;
    	extraInitialization();
    }
    
    private void extraInitialization(){
    	this.constraintsOnAgents = new AgentConstraintPackage(agentVarMap.size());
    	for (int agentID : agentVarMap.keySet()) {
    		agentHasInitialized.put(agentID, false);
    	}
    		
      int agentNum = agentVarMap.size();
      int agentDomainSize = 1;
      this.agentDomainSize = new HashMap();
      ArrayList<Integer> varsInAgent = new ArrayList();
      for(int k = 0; k < agentNum; k++) {
          varsInAgent = agentVarMap.get(k);
          agentDomainSize = 1;
          for(int varID : varsInAgent) {
              int varDom = this.getVarDomainSize(varID);
              agentDomainSize *= varDom;
          }
          this.agentDomainSize.put(k, agentDomainSize);
      }
    	
    }
    
    private ArrayList<ArrayList<Integer>> GraphSplit(int agentID, ArrayList<Integer> G, int sizeThresh, double connThresh){
    	ArrayList<Integer> Q = (ArrayList<Integer>) G.clone();
    	ArrayList<ArrayList<Integer>> sets = new ArrayList();
    	ArrayList<Integer> S = new ArrayList();
    	ArrayList<Integer> tempS = new ArrayList();
    	while(Q.size() > 0) {
    		int v = nextNode(Q, S);
    		tempS = (ArrayList<Integer>) S.clone();
    		tempS.add(v);
    		if(compiledDomainsize(tempS, agentID) <= sizeThresh 
    				&& connectivity(tempS) >= connThresh){
    			S = tempS;
    		}
    		else{
    			sets.add((ArrayList<Integer>) S.clone());
    			S.clear();
    			S.add(v);
    		}
    		
    		for(int i = 0; i < Q.size(); i++){
    			if(Q.get(i) == v){
    				Q.remove(i);
    			}
    		}
//    		Q.remove(v);
    	}
    	if(S.size() > 0) {
    		sets.add(S);
    	}
    	return sets;
    }
    
    private int nextNode(ArrayList<Integer> Q, ArrayList<Integer> S){
    	double maxConn = 0;
    	int v = -1;
    	int index = -1;
    	ArrayList<Integer> tempS = new ArrayList();
    	for (int i = 0; i < Q.size(); i++) {
    		int w = Q.get(i);
    		tempS = (ArrayList<Integer>) S.clone();
    		tempS.add(w);
    		double tempConn = connectivity(tempS);
    		if(tempConn >= maxConn) {
    			v = w;
    			index = i;
    			maxConn = tempConn;
    		}
    	}
//    	if(index != -1){
//    		Q.remove(index);
//    	}
    	
    	return v;
    }
    
    private double connectivity(ArrayList<Integer> S){
    	int size = S.size();
    	double constraintNum = 0;
    	for(int i = 0; i < size; i++){
    		int v = S.get(i);
    		for(int j = i+1; j < size; j++){
    			int w = S.get(j);
    			if(this.isVarConstrained(v, w)){
    				constraintNum ++;
    			}
    		}
    	}
    	double fullConn = (double)size * (size -1) / 2;
    	if(fullConn == 0){
    		return 1;
    	}
    	return constraintNum/fullConn;
    }
    
    private int compiledDomainsize(ArrayList<Integer> tempS, int agentID){  // currently only all the domainsize
    	int dom = 1;
    	for(int v : tempS){
    		dom *= this.getVarDomainSize(v);
    	}
    	return dom;
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
    	if (modelType == ModelType.multiple_VA) {
        	for(int agent: realAgentVarMap.keySet()){
        		for(int myVarID : realAgentVarMap.get(agent)){
        			if(myVarID == id) {
        				return agent;
        			}
        		}
        	}
    	}
    	if(modelType == ModelType.hybridize) {
    		int oneOfMyVar = this.getVariables(id).get(0);
    		for(int agent: realAgentVarMap.keySet()){
    			for(int myVarID : realAgentVarMap.get(agent)){
    				if(myVarID == oneOfMyVar){
    					return agent;
    				}
    			}
    		}
    	}

    	return -1;
    }


	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentConstraintCost(int, int, int, int)
	 */
	@Override
	public int getAgentConstraintCost(int var1, int val1, int var2, int val2) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}


	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentConstraintCost(int, int)
	 */
	@Override
	public int getAgentConstraintCost(int var1, int val1) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
