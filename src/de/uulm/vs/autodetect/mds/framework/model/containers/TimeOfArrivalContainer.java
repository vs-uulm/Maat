package de.uulm.vs.autodetect.mds.framework.model.containers;

/**
 * A container for the time of arrival (in seconds).
 * A future implementation could use a datetime data structure for this.
 *
 * @author Rens van der Heijden
 */
//TODO merge this with TimeOfTransmissionContainer to a TimeContainer, rely on TypeEnum for the semantics?
public class TimeOfArrivalContainer implements MaatContainer {

    private final double toa;

    public TimeOfArrivalContainer(double toa) {
        this.toa = toa;
    }

    public double getTimeOfArrival() {
        return toa;
    }

    @Override
    public WorldModelDataTypeEnum getDataType() {
        return WorldModelDataTypeEnum.TIME_OF_ARRIVAL;
    }

    @Override
    public String toString() {
        return "(t=" + toa + "s)";
    }
}
