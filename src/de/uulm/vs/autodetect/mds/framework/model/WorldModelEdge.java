package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.constants.ConstantDataTypes;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.inputAPI.ExternalOpinion;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.OpinionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;

import java.util.Set;

/**
 * An Edge contains opinions, which may come from internal or external sources.
 *
 * @author Rens van der Heijden
 * @author Christopher Keazor
 */
public class WorldModelEdge extends WorldModelItem {

    /**
     * a List of the WorldModelDataTypes that are covered by this edges'
     * opinion. With the exception of the time of its' creation, this should be
     * used read-only. Therefore it is recommended to have this be a reference
     * to the "final static Set" of the {@link Detector} that holds the opinion
     * (if the opinion comes from a detector). If it comes from a node, it
     * should contain the value {@link WorldModelDataTypeEnum.ALL_DATA}, since
     * opinions from nodes always cover all data types in our model. In case
     * that this Edge represents not an opinion but some other kind of edge,
     * dataTypes should be an empty Set or null.
     */
    private final Set<WorldModelDataTypeEnum> dataTypes;

    public WorldModelEdge(DetectionResultIPCMessage result, long versionID, Set<WorldModelDataTypeEnum> dataTypes) {
        this.versionID = versionID;
        this.dataTypes = dataTypes != null ? dataTypes : ConstantDataTypes.emptyDataTypesSet;
        DataContainer container = new DataContainer(result.getCause().version, null); //TODO why not use versionID..?
        container.put(WorldModelDataTypeEnum.OPINION, new OpinionContainer(result.getOpinion()));
        this.addToHistory(container);
    }

    public WorldModelEdge(WorldModelEdge other, long newVersionID) {
        super(other);
        this.versionID = newVersionID;
        this.dataTypes = other.getDataTypes();
    }

    public WorldModelEdge(ExternalOpinion opinion, long versionID, Set<WorldModelDataTypeEnum> dataTypes) {
        this.versionID = versionID;
        this.dataTypes = dataTypes != null ? dataTypes : ConstantDataTypes.emptyDataTypesSet;
        DataContainer container = new DataContainer(versionID, null);
        container.put(WorldModelDataTypeEnum.OPINION, new OpinionContainer(opinion.getOpinion()));
        this.addToHistory(container);
    }

    public WorldModelEdge(SubjectiveOpinion opinion, long versionID, Set<WorldModelDataTypeEnum> dataTypes) {
        this.versionID = versionID;
        this.dataTypes = dataTypes != null ? dataTypes : ConstantDataTypes.emptyDataTypesSet;
        DataContainer container = new DataContainer(versionID, null);
        container.put(WorldModelDataTypeEnum.OPINION, new OpinionContainer(opinion));
        this.addToHistory(container);
    }

    public void update(DetectionResultIPCMessage opinionMsg) {
        DataContainer container = new DataContainer(opinionMsg.getCause().version, null);
        container.put(WorldModelDataTypeEnum.OPINION, new OpinionContainer(opinionMsg.getOpinion()));
        addToHistory(container);
    }

    public void update(ExternalOpinion opinion, long newSnapshotID) {
        DataContainer container = new DataContainer(newSnapshotID, null);
        container.put(WorldModelDataTypeEnum.OPINION, new OpinionContainer(opinion.getOpinion()));
        addToHistory(container);
    }

    public void update(WorldModelEdge result) {
        addAllToHistory(result.getHistory());
    }

    /**
     * @param dataTypeSet
     * @return true iff this WorldModelEdge covers exactly the {@link @WorldModelDataTypeEnum}s
     * specified by <i>dataTypes</i>, false else.
     */
    public boolean exactlyCoversDataTypes(Set<WorldModelDataTypeEnum> dataTypeSet) {
        return this.dataTypes.equals(dataTypeSet);
    }

    protected Set<WorldModelDataTypeEnum> getDataTypes() {
        return this.dataTypes;
    }

}
