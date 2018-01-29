package de.uulm.vs.autodetect.mds.framework.inputAPI;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelVertex;
import de.uulm.vs.autodetect.mds.framework.model.containers.IdentityMetaDataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;

//TODO revise this when we actually use the MetaDataContainer -- time may need to be represented in that case.
// probably that will work the same way as in Measurements: using an additional container.

/**
 * This wrapper stores a tuple (UID, metaData). The semantics are as follows:
 * <p>
 * - {@link #getMetaData()} returns identity information (name, cert, or something similar)
 * - {@link #getUID()} a unique identifier used to find matching data items
 * <p>
 * This class represents an identity outside of Maat (such as: another instance
 * of Maat, a remote IDS, another vehicle, or a sensor). Such an identity has
 * meta data (name, identifier, certificate, type, flags) that may or may not
 * change over time. Maat will use this information to create a history of
 * changes in the identity (iff !metaData.equals(olderMetaData)). Because
 * identifying information may change over time, this class also requires a UID.
 * The UID is Maat-generated (by the constructor of this class) and must
 * uniquely identify this Identity over time wherever possible.
 *
 * @author Rens van der Heijden
 * @see WorldModelVertex
 */
public class Identity {

    /**
     * Create a Measurement that returns the given objects.
     * <p>
     * Users of this constructor <b>must</b> ensure all values for a UID are of
     * the same type.
     * <p>
     * Values <b>should</b> implement {@link #clone()} correctly, i.e.,
     * <tt>metaData.clone().equals(metaData)</tt>, and they shall have no common
     * objects in their fields.
     *
     * @param UID
     * @param metaData
     */
    public Identity(String UID, IdentityMetaDataContainer metaData) {
        this.UID = "Identity::" + UID;
        this.metaData = metaData;
    }

    /**
     * This is the source of the measurement (for example, this can be a signing
     * certificate or an address)
     */
    protected final String UID;
    /**
     * This is the actual value that was taken in the measurement.
     */
    protected final MaatContainer metaData;

    /**
     * Uniquely identifying String
     *
     * @return
     */
    public String getUID() {
        return this.UID;
    }

    /**
     * Measured value(s) to be stored in the WM
     *
     * @return
     */
    public MaatContainer getMetaData() {
        return this.metaData;
    }

    @Override
    public String toString() {
        return this.getUID();
    }
}
