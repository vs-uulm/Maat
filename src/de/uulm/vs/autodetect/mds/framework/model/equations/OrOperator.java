package de.uulm.vs.autodetect.mds.framework.model.equations;

/**
 * an Implementation of the OR operator
 *
 * @author Christopher Keazor
 * @author Yogita Suryawanshi
 */
public class OrOperator implements Operator {

    private Object variable;

    public OrOperator(Object variable) {
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
