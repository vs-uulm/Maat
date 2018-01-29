package de.uulm.vs.autodetect.mds.framework.model;

import java.util.*;

/**
 * This class is intended to group sensor data of the same type, from the same
 * sensor, at different times. It provides access to the latest measurement and
 * to the complete history.
 *
 * @param <T> the type of the measurement; in most cases this should be Integer
 *            (for discrete values) or Double, but it is possible to represent
 *            more complex types.
 * @author Rens van der Heijden
 */
public class SensorDataItem<T> {

    /************** Inner class: Measurement **********************/
    /**
     * A sensor measurement is always tagged with a time stamp. This class wraps
     * these two values.
     */
    @SuppressWarnings("hiding")
    public class Measurement<T> {
        public final T measurement;
        public final long time;

        public Measurement(T m, long t) {
            this.measurement = m;
            this.time = t;
        }

        @Override
        public String toString() {
            return "(" + this.time + ", " + this.measurement + ")";
        }
    }

    /************** Inner class: MeasurementComparator **********************/
    /**
     * Simple Comparator that orders Measurements by time
     */
    public class MeasurementComparator implements Comparator<Measurement<T>> {
        @Override
        public int compare(Measurement<T> arg0, Measurement<T> arg1) {
            return Long.compare(arg0.time, arg1.time);
        }
    }

    /************** Implementation of SensorDataItem **********************/

    /**
     * Ordered Set containing the measurements, @see {@link Measurement}
     * {@link MeasurementComparator}
     */
    private final SortedSet<Measurement<T>> measurementHistory;


    /**
     * Constructor, creates an empty list.
     */
    public SensorDataItem() {
        this.measurementHistory = new TreeSet<>(new MeasurementComparator());
    }

    /**
     * Constructor used to copy SensorDataItems.
     *
     * @param sensor               the UID of the sensor
     * @param previousMeasurements the sorted measurement list
     */
    protected SensorDataItem(String sensor,
                             SortedSet<Measurement<T>> previousMeasurements) {
        this.measurementHistory = previousMeasurements;
    }

    /**
     * Add a measurement to this data structure. For time < 0, will replace time
     * with the current system time. Regardless, it is ensured that the
     * measurements remain sorted.
     * <p>
     * Requires that value is not null.
     *
     * @throws NullPointerException when null is passed.
     */
    public void addMeasurement(final T value, final long time) {
        if (value == null) {
            throw new NullPointerException(
                    "Cannot add null as measurement value!");
        }
        long timeToWrite = 0L;
        if (time < 0) {
            timeToWrite = System.currentTimeMillis();
        }
        this.measurementHistory.add(new Measurement<>(value, timeToWrite));
    }

    /**
     * Retrieve the most recently inserted measurement.
     */
    public Measurement<T> getLatestMeasurement() {
        return this.measurementHistory.last();
    }


    /**
     * Retrieve all measurements as an ordered List of the current state. Do
     * not modify the list elements!
     */
    public List<Measurement<T>> getAllMeasurements() {
        return new ArrayList<>(this.measurementHistory);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((this.measurementHistory == null) ? 0 : this.measurementHistory
                .hashCode());
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if (!(obj instanceof SensorDataItem<?>))
            return false;
        // add comparison of T here too.
        if (!(this.getClass().getGenericInterfaces().equals(obj.getClass()
                .getGenericInterfaces()))) {
            return false;
        }
        SensorDataItem<T> other = (SensorDataItem<T>) obj;
        if (this.measurementHistory == null) {
            if (other.measurementHistory != null)
                return false;
        } else if (!this.measurementHistory.equals(other.measurementHistory))
            return false;
        return true;
    }

    @Override
    public String toString() {
        String res = "";
        for (Object m : this.measurementHistory) {
            res += m.toString() + " ";//+ System.getProperty("line.separator");
        }
        return res;
    }

    public boolean isTypeString() {
        return this.measurementHistory.first().measurement instanceof String;
    }

    public boolean isTypeBoolean() {
        return this.measurementHistory.first().measurement instanceof Boolean;
    }

    public boolean isTypeInteger() {
        return this.measurementHistory.first().measurement instanceof Integer;
    }

    public boolean isTypeDouble() {
        return this.measurementHistory.first().measurement instanceof Double;
    }

    public long getTime() {
        return getLatestMeasurement().time;
    }
}
