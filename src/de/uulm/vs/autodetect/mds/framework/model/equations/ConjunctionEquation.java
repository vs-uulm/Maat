package de.uulm.vs.autodetect.mds.framework.model.equations;

import de.uulm.vs.autodetect.mds.framework.model.GraphWorldModel;
import no.uio.subjective_logic.opinion.Opinion;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Yogita Suryawanshi
 **/
public class ConjunctionEquation {

    private AndOperator operator;
    private List<SubEquations> subEquations;
    private final GraphWorldModel worldModel;

    public ConjunctionEquation(ConjunctionEquation conjunctionEquation, GraphWorldModel m) {
        this.operator = conjunctionEquation.operator;
        this.subEquations = conjunctionEquation.subEquations;
        this.worldModel = m;
    }

    public ConjunctionEquation(AndOperator operator, List<SubEquations> subEquations, GraphWorldModel m) {
        this.operator = operator;
        this.subEquations = subEquations;
        this.worldModel = m;
    }

    public AndOperator getOperator() {
        return this.operator;
    }

    public List<SubEquations> getSubEquations() {
        return this.subEquations;
    }

    public Opinion operate() {
        List<Opinion> opinions = new ArrayList<>();
        SubEquations subEquation;

        if (this.subEquations == null || this.subEquations.size() == 0) {
            throw new NullPointerException();
        }
        for (int i = 0; i < this.subEquations.size(); i++) {
            if (this.subEquations.get(i).getOperator() == null) {
                subEquation = new SubEquations(this.subEquations.get(i).getVariable(), this.worldModel);
            } else {
                subEquation = new SubEquations(this.subEquations.get(i).getVariable(), this.worldModel, this.subEquations.get(i).getOperator());
            }
            opinions.add(subEquation.operate());
        }
        return SubjectiveOpinion.and(opinions);
    }
}

