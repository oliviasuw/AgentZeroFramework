/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.dcr.az.api.prob;

import bgu.dcr.az.api.prob.cpack.ConstraintsPackage;
import bgu.dcr.az.api.prob.cpack.KAryTreeConstraintPackage;
import bgu.dcr.az.api.prob.cpack.BinaryMapConstraintPackage;
import bgu.dcr.az.api.exen.mdef.CorrectnessTester;
import bgu.dcr.az.api.prob.cpack.AsymmetricBinaryMapConstraintPackage;

/**
 *
 * @author bennyl
 */
public enum ProblemType {

    /**
     * Constraint Satisfaction Problem, K-Ary Constraints allowed
     */
    K_ARY_DCSP (false, false){
        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize) {
            return new KAryTreeConstraintPackage(numvars);
        }

        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    },
    /**
     * Constraint Optimization Problem, K-Ary Constraints allowed
     */
    K_ARY_DCOP (false, false) {
        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize) {
            return new KAryTreeConstraintPackage(numvars);
        }

        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    },
    /**
     * asymmetric Constraint Optimization Problem, K-Ary Constraints allowed
     */
    K_ARY_ADCOP (true, false) {
        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize) {
            return new KAryTreeConstraintPackage(numvars);
        }

        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    },
    /**
     * Constraint Satisfaction Problem, Only Binary and unary constraints
     * allowed
     */
    DCSP {
        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar) {
            return new BinaryMapConstraintPackage(numvars, maxDomainSize, multiVar);
        }

        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize) {
            return new BinaryMapConstraintPackage(numvars, maxDomainSize);
        }
    },
    /**
     * Constraint Optimization Problem, Only Binary and unary constraints
     * allowed
     */
    DCOP {
        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar) {
            return new BinaryMapConstraintPackage(numvars, maxDomainSize, multiVar);
        }

        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize) {
            return new BinaryMapConstraintPackage(numvars, maxDomainSize);
        }
    },
    /**
     * asymmetric Constraint Optimization Problem, Only Binary and unary
     * constraints allowed
     */
    ADCOP (true, true) {
        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize) {
            return new AsymmetricBinaryMapConstraintPackage(numvars, maxDomainSize);
        }

        @Override
        public ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    public abstract ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize);
    public abstract ConstraintsPackage newConstraintPackage(int numvars, int maxDomainSize, boolean multiVar);
    boolean asymmetric = false;
    boolean binary = true;

    private ProblemType() {
    }

    private ProblemType(boolean asymmetric, boolean binary) {
        this.asymmetric = asymmetric;
        this.binary = binary;
    }

    public boolean isAsymmetric() {
        return asymmetric;
    }

    public boolean isBinary() {
        return binary;
    }

}
