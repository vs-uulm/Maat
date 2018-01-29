package de.uulm.vs.autodetect.mds.framework.model.equations;

/**
 * an Implementation of the AND operator
 *
 * @author Christopher Keazor
 * @author Yogita Suryawanshi
 */
public class AndOperator implements Operator {

    private Object variable;

    public AndOperator(Object variable) {
        this.variable = variable;
    }

    public Object getVariable() {
        return this.variable;
    }

    @Override
    public OperatorEnum getOperatorType() {
        return Operator.OperatorEnum.BINARY;
    }

}
