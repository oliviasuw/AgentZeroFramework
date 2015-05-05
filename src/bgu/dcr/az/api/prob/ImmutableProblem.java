/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.dcr.az.api.prob;

import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.tools.Assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *
 * @author bennyl
 */
public interface ImmutableProblem {

    /**
     * @return the problem type.
     */
    ProblemType type();

    /**
     * return the cost of the k-ary constraint represented by the given
     * assignment
     *
     * @param ass
     * @return
     */
    int getConstraintCost(Assignment ass);
    
    /****************** Agent layer start ******************/
    int getNumberOfAgents();
    
    /** weaker principal **/
    void setWeakPrincipalVariables(int agentId, ArrayList<Integer> principleVars);
    
    int getWeakPrincipalVarsHashValue(int agentID, int hashVal);
    public ArrayList<Integer> getFullValListFromExternalVal(int agentID, int exterVal);
    
    List<Integer> getWeakPrincipalVariables(int agentId);
    
    /** strong principal **/
    void setStrongPrincipalVariables(int agentId, HashMap<Integer, ArrayList<Integer>> childPrincipleVarsMap);
    
    int getStrongPrincipalVarsHashValue(int agentID, int child, int hashVal);
    
    List<Integer> getStrongPrincipalVariables(int agentId, int child);

//    ImmutableSet<Integer> getAgentDomainOf(int agent);

    int getAgentDomainSize(int agent);

    Set<Integer> getAgentNeighbors(int agent);

    boolean isAgentConstrained(int agent1, int agent2);
    
    boolean hasAgentInitialized(int agentID);
    boolean setAgentInitialized(int agentID);
    boolean hasAllInitialized();
    void setChildren(int agent, List<Integer> list);
    List<Integer> getChildren(int agent);
    void setChildDescendMap(int agent, HashMap<Integer, List<Integer>> childDescendMap);
    HashMap<Integer, List<Integer>> getChildDescendMap(int agent);
    /**
    *
    * @param var1
    * @param val1
    * @param var2
    * @param val2
    * @return the cost of assigning var1=val1 when var2=val2
    */
   int getAgentConstraintCost(int var1, int val1, int var2, int val2);

   /**
    *
    * @param var1
    * @param val1
    * @return the cost of assigning var1=val1
    */
   int getAgentConstraintCost(int var1, int val1);
    /****************** Agent layer end ******************/
    
    /****************** Variable layer start ******************/

    /**
     * return the domain of the given variable
     *
     * @param var
     * @return
     */
    ImmutableSet<Integer> getVarDomainOf(int var);

    /**
     * return the domain size of the variable var
     *
     * @param var
     * @return
     */
    int getVarDomainSize(int var);

    /**
     * @param var
     * @return all the variables that costrainted with the given var
     */
    Set<Integer> getVarNeighbors(int var);

    /**
     * @return the number of variables defined in this problem
     */
    int getNumberOfVars();
    
    List<Integer> getVariables(int agentId);
    
    /**
     * @param var1
     * @param var2
     * @return true if there is a constraint between var1 and var2 operation
     * cost: o(d^2)cc
     */
    boolean isVarConstrained(int var1, int var2);
    
    /**
    *
    * @param var1
    * @param val1
    * @param var2
    * @param val2
    * @return the cost of assigning var1=val1 when var2=val2
    */
   int getVarConstraintCost(int var1, int val1, int var2, int val2);

   /**
    *
    * @param var1
    * @param val1
    * @return the cost of assigning var1=val1
    */
//   default int getConstraintCost(int var1, int val1) {
//       return getConstraintCost(var1, val1, var1, val1);
//   }
   int getVarConstraintCost(int var1, int val1);
    /****************** Variable layer end ******************/

    
    /**
     * @return this problem metadata
     */
    HashMap<String, Object> getMetadata();

    /**
     * @param var1
     * @param val1
     * @param var2
     * @param val2
     * @return true if var1=val1 consistent with var2=val2
     */
//    default boolean isConsistent(int var1, int val1, int var2, int val2) {
//        return getConstraintCost(var1, val1, var2, val2) == 0;
//    }
    boolean isVarConsistent(int var1, int val1, int var2, int val2);
    
	/**
	 * @param src
	 * @param dest
	 * @return
	 */
	ArrayList<Integer> getConstrainedVars(int src, int dest);

    // Deleted by Olivia
//  public ArrayList<Integer> getConstrainedVars(int src, int dest);

    /**
     * return the cost of the given assignment (taking into consideration all
     * the constraints that apply to it)
     *
     * @param a
     * @return
     */
    int calculateCost(Assignment a);


}
