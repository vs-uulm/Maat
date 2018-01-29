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

public class ARTFactory extends AbstractDetectorFactory {
    public ARTFactory() {
    }

    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException {
        ART instance = new ART(output, reference, this);
        try {
            instance.setAttribute(this.pars, ART.class);
            return instance;
        } catch (IllegalAccessException e) {
            ART.l.error("", e);
        } catch (NoSuchFieldException e) {
            ART.l.error("", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return ART.class.getCanonicalName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return ART.dataTypes;
    }

    @Override
    public boolean canProcess(WorldModelDiff diff) {
        boolean canProcess = false;
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                //new data must be non-local
                if (vertex.hasTypes(ART.dataTypes) && !vertex.referent.equals(WorldModelDataVertex.LOCAL_REFERENT))
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
