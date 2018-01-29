package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.inputAPI.Identity;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

/**
 * A NodeVertex represents other nodes within the network,
 * for instance other cars.
 *
 * @author Rens van der Heijden
 * @author Christopher Keazor
 */
public class WorldModelNodeVertex extends WorldModelVertex {

    public WorldModelNodeVertex(String UID, long versionID) {
        super(UID);
        this.versionID = versionID;
    }

    public WorldModelNodeVertex(Identity identity, long versionID) {
        super(identity.getUID());
        this.versionID = versionID;
        DataContainer container = new DataContainer(versionID, null);
        container.put(WorldModelDataTypeEnum.NODE_META_DATA, identity.getMetaData());
        this.addToHistory(container);
    }

    public WorldModelNodeVertex(WorldModelNodeVertex other, long versionID) {
        super(other.UID, other);
        this.versionID = versionID;
    }

}
