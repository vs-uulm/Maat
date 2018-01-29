package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class WorldModelVertex extends WorldModelItem {
    private static final Logger l = LogManager.getLogger(WorldModelVertex.class);

    public final String UID;

    public WorldModelVertex(String UID) {
        this.UID = UID;
    }

    public WorldModelVertex(String UID, WorldModelItem item) {
        super(item);
        this.UID = UID;
    }

    /**
     * update this WMVertex. Will log overwrites in a warning.
     *
     * @param type             the type associated with updatedContainer.
     * @param updatedContainer new container to be stored.
     * @param version          the version at which this container is relevant.
     */
    public void update(WorldModelDataTypeEnum type, MaatContainer updatedContainer, long version, String messageID) {
        DataContainer container = this.getLatest();
        if (container == null || container.version != version)
            container = new DataContainer(version, messageID);
        if (container.containsKey(type))
            l.warn("Overwriting existing data!");
        container.put(type, updatedContainer);
        addToHistory(container);
    }

    public void merge(WorldModelVertex v) {
        if (!v.UID.equals(this.UID))
            l.warn("Attempting to merge vertices with different UIDs! Vertices: " + this + " and " + v);

        if (this.versionID >= v.versionID)
            l.warn("Overwriting more recent version in WorldModel! Current version " + this.versionID + " with new version: " + v.versionID);
        this.versionID = v.versionID;

        //TODO ensure this provides the necessary guarantees
        super.addAllToHistory(v.getHistory());
    }

}
