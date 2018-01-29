package de.uulm.vs.autodetect.mds.framework.model.equations;

import de.uulm.vs.autodetect.mds.framework.model.GraphWorldModel;
import no.uio.subjective_logic.opinion.Opinion;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yogita Suryawanshi
 **/
public class Equations {

    private OrOperator operator;
    private List<ConjunctionEquation> conjunctionEquations;
    private final GraphWorldModel worldModel;

    public Equations(OrOperator orOperator, List<ConjunctionEquation> conjunctionEquations, GraphWorldModel m) {
        this.operator = orOperator;
        this.conjunctionEquations = conjunctionEquations;
        this.worldModel = m;
    }

    public OrOperator getOperator() {
        return this.operator;
    }

    public List<ConjunctionEquation> getConjunctionEquations() {
        return this.conjunctionEquations;
    }

    public List<Opinion> operate() {
        List<Opinion> opinions = new ArrayList<>();

        if (this.conjunctionEquations == null || this.worldModel == null || this.operator == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < this.conjunctionEquations.size(); i++) {
            ConjunctionEquation conjunctionEquation = new ConjunctionEquation(this.conjunctionEquations.get(i), this.worldModel);
            opinions.add(conjunctionEquation.operate());
        }
        return opinions;
    }

}
