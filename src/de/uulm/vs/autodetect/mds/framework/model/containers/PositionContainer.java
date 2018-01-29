package de.uulm.vs.autodetect.mds.framework.model.containers;

/**
 * A container for a (3D) position.
 *
 * @author Rens van der Heijden
 */
public class PositionContainer implements MaatContainer {

    private final double[] pos;

    public PositionContainer(double[] position) {
        this.pos = position;
    }

    public double[] getPosition() {
        return this.pos;
    }

    public double getX() {
        return this.pos[0];
    }

    public double getY() {
        return this.pos[1];
    }

    public double getZ() {
        return this.pos[2];
    }

    @Override
    public WorldModelDataTypeEnum getDataType() {
        return WorldModelDataTypeEnum.POSITION;
    }

    /**
     * Compute distance between two positions using simple Euclidean distance
     */
    public double distance(PositionContainer prevPos) {
        double diffX = Math.abs(prevPos.getX() - this.getX());
        double diffY = Math.abs(prevPos.getY() - this.getY());
        double diffZ = Math.abs(prevPos.getZ() - this.getZ());

        return Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
    }

    @Override
    public String toString() {
        return "(" + pos[0] + "," + pos[1] + "," + pos[2] + ")";
    }
}
