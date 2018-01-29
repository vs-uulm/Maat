package de.uulm.vs.autodetect.mds.framework.model.equations;

/**
 * an Implementation of the NOT operator
 *
 * @author Christopher Keazor
 * @author Yogita Suryawanshi
 */
public class NotOperator implements Operator {

    private Object variable;

    public NotOperator(Object variable) {
        this.variable = variable;
    }

    public Object getVariable() {
        return this.variable;
    }

    @Override
    public OperatorEnum getOperatorType() {
        return Operator.OperatorEnum.UNARY;
    }

}
