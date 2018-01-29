package de.uulm.vs.autodetect.mds.framework.model.containers;

/**
 * A container for vehicle speed.
 *
 * @author Rens van der Heijden
 */
public class SpeedContainer implements MaatContainer {

    private final double speedX, speedY, speedZ;

    public SpeedContainer(double speedX, double speedY, double speedZ) {
        this.speedX = speedX;
        this.speedY = speedY;
        this.speedZ = speedZ;
    }

    public double getSpeedX() {
        return this.speedX;
    }

    public double getSpeedY() {
        return this.speedY;
    }

    public double getSpeedZ() {
        return this.speedZ;
    }

    public double getSpeed(){
        return Math.sqrt(Math.pow(this.speedX, 2) + Math.pow(this.speedY, 2) + Math.pow(this.speedZ, 2));
    }

    @Override
    public WorldModelDataTypeEnum getDataType() {
        return WorldModelDataTypeEnum.SPEED;
    }

}
