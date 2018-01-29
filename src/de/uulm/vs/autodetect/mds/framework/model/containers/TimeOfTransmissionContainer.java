package de.uulm.vs.autodetect.mds.framework.model.containers;

/**
 * A container for the (claimed) time of transmission (in seconds).
 *
 * @author Rens van der Heijden
 */
//TODO merge this with TimeOfArrivalContainer to a TimeContainer, rely on TypeEnum for the semantics?
public class TimeOfTransmissionContainer implements MaatContainer {

    private final double tot;

    public TimeOfTransmissionContainer(double tot) {
        this.tot = tot;
    }

    public double getTimeOfTransmission() {
        return tot;
    }

    @Override
    public WorldModelDataTypeEnum getDataType() {
        return WorldModelDataTypeEnum.TIME_OF_TRANSMISSION;
    }

    @Override
    public String toString() {
        return "(t=" + tot + "s)";
    }
}
