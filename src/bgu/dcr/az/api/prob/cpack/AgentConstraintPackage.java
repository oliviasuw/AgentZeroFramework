/**
 * AgentConstraintPackage.java
   Created by Su Wen
   Date: Mar 25, 2015
   Time: 2:39:10 PM 
 */
package bgu.dcr.az.api.prob.cpack;

import java.util.Set;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.prob.ConstraintCheckResult;
import bgu.dcr.az.api.prob.KAryConstraint;
import bgu.dcr.az.api.prob.NeighboresSet;
import bgu.dcr.az.api.tools.Assignment;

/**
 * @author Su Wen
 *
 */
public class AgentConstraintPackage implements ConstraintsPackage {

    NeighboresSet[] neighbores;

    public AgentConstraintPackage(int numvar) {
        this.neighbores = new NeighboresSet[numvar];
        for (int i = 0; i < this.neighbores.length; i++) {
            this.neighbores[i] = new NeighboresSet(numvar);
        }
    }

    @Override
    public Set<Integer> getNeighbores(int xi) {
        return neighbores[xi];
    }

    @Override
    public void addNeighbor(int to, int neighbor) {
        if (to != neighbor) { //you cannot be yourown neighbor!
            neighbores[to].add(neighbor);
        }else {
            Agt0DSL.panic("agent cannot be its own neighbor");
        }
        
    }

    protected int getNumberOfNeighbors() {
        return neighbores.length;
    }

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#setConstraintCost(int, bgu.dcr.az.api.prob.KAryConstraint)
	 */
	@Override
	public void setConstraintCost(int owner, KAryConstraint constraint) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#addConstraintCost(int, bgu.dcr.az.api.prob.KAryConstraint)
	 */
	@Override
	public void addConstraintCost(int owner, KAryConstraint constraint) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#setConstraintCost(int, int, int, int, int, int)
	 */
	@Override
	public void setConstraintCost(int owner, int x1, int v1, int x2, int v2,
			int cost) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#setConstraintCost(int, int, int, int)
	 */
	@Override
	public void setConstraintCost(int owner, int x1, int v1, int cost) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#getConstraintCost(int, int, int, bgu.dcr.az.api.prob.ConstraintCheckResult)
	 */
	@Override
	public void getConstraintCost(int owner, int x1, int v1,
			ConstraintCheckResult result) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#getConstraintCost(int, int, int, int, int, bgu.dcr.az.api.prob.ConstraintCheckResult)
	 */
	@Override
	public void getConstraintCost(int owner, int x1, int v1, int x2, int v2,
			ConstraintCheckResult result) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#getConstraintCost(int, bgu.dcr.az.api.tools.Assignment, bgu.dcr.az.api.prob.ConstraintCheckResult)
	 */
	@Override
	public void getConstraintCost(int owner, Assignment k,
			ConstraintCheckResult result) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#calculateCost(int, bgu.dcr.az.api.tools.Assignment, bgu.dcr.az.api.prob.ConstraintCheckResult)
	 */
	@Override
	public void calculateCost(int owner, Assignment a,
			ConstraintCheckResult result) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.prob.cpack.ConstraintsPackage#calculateGlobalCost(bgu.dcr.az.api.tools.Assignment)
	 */
	@Override
	public int calculateGlobalCost(Assignment a) {
		// TODO Auto-generated method stub
		return 0;
	}
}
