package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.inputAPI.Measurement;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.Set;

/**
 * A DataVertex contains data such as speed, location, ...
 *
 * @author Rens van der Heijden
 * @author Christopher Keazor
 */
public class WorldModelDataVertex extends WorldModelVertex {

    public static final String LOCAL_REFERENT = "LOCAL";

    public final String referent;

    public WorldModelDataVertex(Measurement measurement, long snapshotID) {
        super(measurement.getUID());
        this.versionID = snapshotID;
        this.referent = measurement.getReferent();
        DataContainer container = new DataContainer(snapshotID, measurement.getMessageID());
        for (MaatContainer value : measurement.getValues()) {
            container.put(value.getDataType(), value);
        }
        addToHistory(container);
    }

    /**
     * copy constructor
     *
     * @param worldModelDataVertex
     */
    public WorldModelDataVertex(WorldModelDataVertex worldModelDataVertex, long newVersionID) {
        super(worldModelDataVertex.UID, worldModelDataVertex);
        this.versionID = newVersionID;
        this.referent = worldModelDataVertex.referent;
    }

    /**
     * returns true iff this WorldModelDataVertex has <b>all</b> types listed in
     * types, false else.
     *
     * @param requestedTypes A List containing all the WorldModelDataTypeEnum-types that
     *                       this object should have.
     * @return true, if this object has <b>all</b> the types given in types,
     * false else.
     */
    public boolean hasTypes(Set<WorldModelDataTypeEnum> requestedTypes) {
        boolean returnValue = true;
        for (WorldModelDataTypeEnum requestedType : requestedTypes)
            returnValue = returnValue && this.getLatestValue().containsKey(requestedType);
        return returnValue;
    }

    public boolean hasType(WorldModelDataTypeEnum requestedType) {
        return this.getLatestValue().containsKey(requestedType);
    }

    public DataContainer getLatestValue() {
        return this.getFromHistory(this.getVersionID());
    }

    @Override
    public String toString() {
        return "Data item " + this.UID + " with " + this.historySize() + " entries";
    }
}
