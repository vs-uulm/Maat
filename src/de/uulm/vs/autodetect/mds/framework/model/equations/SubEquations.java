package de.uulm.vs.autodetect.mds.framework.model.equations;

import de.uulm.vs.autodetect.mds.framework.model.GraphWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import no.uio.subjective_logic.opinion.Opinion;

/**
 * @author Yogita Suryawanshi
 **/
public class SubEquations {

    private Operator operator;
    private Object variable;
    private final GraphWorldModel worldModel;

    public SubEquations(Object variable, GraphWorldModel m, Operator operator) {
        this.operator = operator;
        this.variable = variable;
        this.worldModel = m;
    }

    public SubEquations(Object variable, GraphWorldModel m) {
        this.variable = variable;
        this.worldModel = m;
    }

    public Operator getOperator() {
        return this.operator;
    }

    public Object getVariable() {
        return this.variable;
    }

    public Opinion operate() {
        Opinion opinion = null;

        if (this.variable == null) {
            throw new NullPointerException();
        }
        if (this.operator != null) {
            WorldModelDataVertex dataNode = (WorldModelDataVertex) this.worldModel.findVertex(((WorldModelDataVertex) this.variable).UID);
            opinion = this.worldModel.isTrustworthy(dataNode, this.worldModel.getVersion());
            if (opinion == null)
                throw new NullPointerException();
            opinion = opinion.toSubjectiveOpinion().not();
        } else {
            WorldModelDataVertex dataNode = (WorldModelDataVertex) this.worldModel.findVertex(((WorldModelDataVertex) this.variable).UID);
            opinion = this.worldModel.isTrustworthy(dataNode, this.worldModel.getVersion());
            if (opinion == null)
                throw new NullPointerException();
        }
        return opinion;

    }

}
