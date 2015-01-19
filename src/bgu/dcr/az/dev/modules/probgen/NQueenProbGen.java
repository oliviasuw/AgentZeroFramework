package bgu.dcr.az.dev.modules.probgen;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

import java.util.Random;

@Register(name = "nqueenProbGen")
public class NQueenProbGen extends AbstractProblemGenerator {

	@Variable(name = "n", description = "number of queens", defaultValue = "4")
    int n = 4;
	
    @Override
    public void generate(Problem p, Random rand) {
    	p.initialize(ProblemType.DCOP, n, n);
        for(int i = 0; i < n; i++)
        	for(int j = 0; j < n; j++)
        		for(int vi = 0; vi < n; vi++)
        			for(int vj = 0; vj < n; vj++)
        				if(vi == vj || abs(vi - vj) == abs(i - j)){
        					p.setConstraintCost(i, vi, j, vj, 1);
        					p.setConstraintCost(j, vj, i, vi, 1);
        				}
    }
}
