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

public class SSCFactory extends AbstractDetectorFactory {
    public SSCFactory() {
    }

    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException {
        SSC instance = new SSC(output, reference, this);
        try {
            instance.setAttribute(this.pars, SSC.class);
        } catch (IllegalAccessException e) {
            SSC.l.error("", e);
        } catch (NoSuchFieldException e) {
            SSC.l.error("", e);
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
                if (vertex.hasTypes(SSC.dataTypes))
                    canProcess = true;
            }
        }

        if (!canProcess)
            return false; //nothing to process

        return true;
    }
}
