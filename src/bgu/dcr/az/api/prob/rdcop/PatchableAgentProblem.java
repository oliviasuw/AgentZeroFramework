/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.dcr.az.api.prob.rdcop;

import bgu.dcr.az.api.Agent;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.ConstraintCheckResult;
import bgu.dcr.az.api.prob.ImmutableProblem;
import bgu.dcr.az.api.prob.KAryConstraint;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.api.prob.cpack.KAryTreeConstraintPackage;
import bgu.dcr.az.api.tools.Assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author bennyl
 */
public class PatchableAgentProblem implements PossibleProblem {

    private Agent.AgentProblem underline;
    private KAryTreeConstraintPackage constraints;
    private Map<Integer, Set<Integer>> domains;
    private double probability = 0;

    public PatchableAgentProblem(Agent.AgentProblem underline) {
        this.underline = underline;
        this.constraints = new KAryTreeConstraintPackage(underline.getNumberOfVars());
    }

    @Override
    public ProblemType type() {
        return underline.type();
    }

    @Override
    public int getVarConstraintCost(int var1, int val1, int var2, int val2) {
        final ConstraintCheckResult qt = underline.getQueryTemp();
        constraints.getConstraintCost(var1, var1, val1, var2, val2, qt);
        underline.increaseCC(qt.getCheckCost());
        return qt.getCost();
    }

    @Override
    public int getConstraintCost(Assignment ass) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ImmutableSet<Integer> getVarDomainOf(int var) {
        final Set<Integer> v = domains.get(var);
        if (v == null) {
            throw new UnsupportedOperationException("variable " + var + " is not exists!");
        }
        return new ImmutableSet<>(v, true);
    }

    @Override
    public int getVarDomainSize(int var) {
        return getVarDomainOf(var).size();
    }

    @Override
    public HashMap<String, Object> getMetadata() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Set<Integer> getVarNeighbors(int var) {
        return constraints.getNeighbores(var);
    }

    @Override
    public int getNumberOfVars() {
        return domains.size();
    }

    @Override
    public boolean isVarConstrained(int var1, int var2) {
        return getVarNeighbors(var1).contains(var2);
    }

    @Override
    public int calculateCost(Assignment a) {
        final ConstraintCheckResult qt = underline.getQueryTemp();
        constraints.calculateCost(underline.getAgentId(), a, qt);
        underline.increaseCC(qt.getCheckCost());
        return qt.getCost();
    }

    public void createVariable(int variable) {
        if (domains.containsKey(variable)) {
            throw new UnsupportedOperationException("variable " + variable + " already exists");
        }

        domains.put(variable, new HashSet());
    }

    public boolean containsVariable(int variable) {
        return domains.containsKey(variable);
    }

    public void setVariableDomain(int variable, Set<Integer> domain) {
        domains.put(variable, domain);
    }

    public void addConstraint(int owner, KAryConstraint kAryConstraint) {
        constraints.setConstraintCost(owner, kAryConstraint);
    }

    @Override
    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    @Override
    public int getNumberOfAgents() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> getVariables(int agentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Integer> getConstrainedVars(int src, int dest) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getVarConstraintCost(int var1, int val1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isVarConsistent(int var1, int val1, int var2, int val2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentDomainOf(int)
	 */
	@Override
	public ImmutableSet<Integer> getAgentDomainOf(int agent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentDomainSize(int)
	 */
	@Override
	public int getAgentDomainSize(int agent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentNeighbors(int)
	 */
	@Override
	public Set<Integer> getAgentNeighbors(int agent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#isAgentConstrained(int, int)
	 */
	@Override
	public boolean isAgentConstrained(int agent1, int agent2) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentConstraintCost(int, int, int, int)
	 */
	@Override
	public int getAgentConstraintCost(int var1, int val1, int var2, int val2) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getAgentConstraintCost(int, int)
	 */
	@Override
	public int getAgentConstraintCost(int var1, int val1) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#setPrincipalVariables(int, java.util.ArrayList)
	 */
	@Override
	public void setPrincipalVariables(int agentId,
			ArrayList<Integer> principleVars) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getPrincipalVarsHashValue(int, int)
	 */
	@Override
	public int getPrincipalVarsHashValue(int agentID, int hashVal) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#getPrincipalVariables(int)
	 */
	@Override
	public List<Integer> getPrincipalVariables(int agentId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#hasAgentInitialized(int)
	 */
	@Override
	public boolean hasAgentInitialized(int agentID) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.ImmutableProblem#setAgentInitialized(int)
	 */
	@Override
	public boolean setAgentInitialized(int agentID) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
