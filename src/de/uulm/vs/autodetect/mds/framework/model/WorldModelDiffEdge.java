package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.Set;

public class WorldModelDiffEdge extends WorldModelItem {

    private final WorldModelVertex src, dst;
    private final WorldModelEdge edge;

    public WorldModelDiffEdge(WorldModelVertex src, WorldModelVertex dst, WorldModelEdge e, long version) {
        //super(e);
        this.versionID = version;
        this.src = src;
        this.dst = dst;
        this.edge = e;
    }

    public WorldModelVertex getSource() {
        return this.src;
    }

    public WorldModelVertex getDestination() {
        return this.dst;
    }

    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return this.edge.getDataTypes();
    }

    public WorldModelEdge getEdge() {
        return this.edge;
    }

    @Override
    public String toString() {
        return src.UID + " -" + "-> " + dst.UID;
    }
}
