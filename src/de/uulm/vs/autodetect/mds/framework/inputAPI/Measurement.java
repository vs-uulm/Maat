package de.uulm.vs.autodetect.mds.framework.inputAPI;

import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This wrapper stores a tuple (UID, value). The semantics are as follows:
 * <p>
 * - {@link #getValues()} returns the actual measured value(s).
 * - {@link #getReferent()} a unique identifier used to find matching data items.
 * <p>
 * A series of Measurements will be stored in data items.
 * Part of the list of input values should be the time (either of arrival, of transmission, or both)
 *
 * @author Rens van der Heijden
 */
public class Measurement {

    /**
     * Create a Measurement that returns the given objects.
     * <p>
     * Users of this constructor <b>must</b> ensure all values for a UID are of
     * the same type.
     * <p>
     * Values <b>should</b> implement {@link #clone()} correctly, i.e.,
     * <tt>value.clone().equals(value)</tt>, and they shall have no common
     * objects in their fields.
     * <p>
     *
     * @param mID message identifier
     * @param values data
     * @param referent the referent of this measurement
     */
    public Measurement(List<MaatContainer> values, String referent, String mID) {
        this.UID = "Measurement::" + referent;
        this.values.addAll(values);
        for (MaatContainer v : values)
            this.dataTypes.add(v.getDataType());
        this.referent = referent;
        this.messageID = mID;
    }

    /**
     * This constructor is the same as above, except it accepts exactly one value (exists for historical reasons)
     *
     * @param mID message identifier
     * @param value
     * @param referent
     * @deprecated
     */
    public Measurement(MaatContainer value, String referent, String mID) {
        this.UID = "Measurement::" + referent;
        this.values.add(value);
        this.dataTypes.add(value.getDataType());
        this.referent = referent;
        this.messageID = mID;
    }

    /**
     * The UID for this measurement. Uniquely identifies this measurement within this instance of Maat.
     */
    protected final String UID;

    /**
     *
     */
    private final String messageID;

    /**
     * This is the actual value that was taken in the measurement.
     */
    protected final List<MaatContainer> values = new ArrayList<>();

    /**
     * Data types associated with the list of values (see {@link #getValues()}).
     */
    protected final Set<WorldModelDataTypeEnum> dataTypes = new HashSet<>();

    /**
     * This identifies what the measurement is about, i.e., what the values are related to.
     */
    protected final String referent;

    /**
     * the UID of this measurement
     */
    public String getUID() {
        return this.UID;
    }

    /**
     * What this measurement refers to.
     */
    public String getReferent() {
        return this.referent;
    }

    /**
     * Measured value(s) to be stored in the WM
     */
    public List<MaatContainer> getValues() {
        return this.values;
    }

    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return this.dataTypes;
    }

    @Override
    public String toString() {
        return this.UID + "= (src: " + this.referent + ", data:" + this.values + " )";
    }

    public String getMessageID() {
        return messageID;
    }
}
