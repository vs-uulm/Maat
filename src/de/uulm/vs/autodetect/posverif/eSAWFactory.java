package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelItem;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.Set;

public class eSAWFactory extends AbstractDetectorFactory {
    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference) throws WorldModelException {
        eSAW instance = new eSAW(output, reference, this);
        try {
            instance.setAttribute(this.pars, eSAW.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            eSAW.l.error("", e);
        }
        return instance;
    }

    @Override
    public String getName() {
        return eSAW.class.getCanonicalName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return eSAW.dataTypes;
    }

    @Override
    public boolean canProcess(WorldModelDiff diff) {
        boolean canProcess = false;
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = (WorldModelDataVertex) i;
                if (vertex.hasTypes(eSAW.dataTypes)
                        && !vertex.referent.equals(WorldModelDataVertex.LOCAL_REFERENT)
                        && vertex.historySize() == 1) {
                    canProcess = true;
                    break;
                }
            }
        }
        if (diff.dataVertexStream(diff.getVersion()).noneMatch(
                s -> ((WorldModelDataVertex) s).referent.equals(WorldModelDataVertex.LOCAL_REFERENT)))
            return false; // no local sensor data available, nothing to compare against

        return canProcess;
    }
}
