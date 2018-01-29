package de.uulm.vs.autodetect.mds.framework.model.equations;

/**
 * Interface for Operators.
 *
 * @author Christopher Keazor
 * <p>
 * NOTE: using sub-interfaces (BinaryOperator, UnaryOperator) would
 * be better with regards to extensibility if we expect many
 * different types of unary/binary operators
 * (because we then can have shared code in one location only)
 * But as I think that will not be the case, I chose this approach,
 * using an enum, because it doesn't produce so many classes. ^^
 */
public interface Operator {

    /**
     * can be used to specify the type of an operator (unary, binary, ...)
     */
    public enum OperatorEnum {
        UNARY,
        BINARY
        // add more types here
    }

    /**
     * return the type of the Operator (unary, binary, ...)
     */
    public Operator.OperatorEnum getOperatorType();
}
