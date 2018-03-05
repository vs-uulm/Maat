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

public class eARTFactory extends AbstractDetectorFactory {
    public eARTFactory() {
    }

    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException {
        eART instance = new eART(output, reference, this);
        try {
            instance.setAttribute(this.pars, eART.class);
            return instance;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            eART.l.error("", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return eART.class.getName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return eART.dataTypes;
    }

    @Override
    public boolean canProcess(WorldModelDiff diff) {
        boolean canProcess = false;
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                //new data must be non-local
                if (vertex.hasTypes(eART.dataTypes) && !vertex.referent.equals(WorldModelDataVertex.LOCAL_REFERENT))
                    canProcess = true;
            }
        }

        if (!canProcess)
            return false; //nothing to process

        if (diff.dataVertexStream(diff.getVersion()).noneMatch(
                s -> ((WorldModelDataVertex) s).referent.equals(WorldModelDataVertex.LOCAL_REFERENT)))
            return false; // no local sensor data available, nothing to compare against

        return true;
    }
}
