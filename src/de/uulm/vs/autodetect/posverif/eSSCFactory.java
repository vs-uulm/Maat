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

public class eSSCFactory extends AbstractDetectorFactory {
    public eSSCFactory() {
    }

    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException {
        eSSC instance = new eSSC(output, reference, this);
        try {
            instance.setAttribute(this.pars, eSSC.class);
            return instance;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            eSSC.l.error("", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return eSSC.class.getName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return eSSC.dataTypes;
    }

    @Override
    public boolean canProcess(WorldModelDiff diff) {
        boolean canProcess = false;
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                if (vertex.hasTypes(eSSC.dataTypes))
                    canProcess = true;
            }
        }

        if (!canProcess)
            return false; //nothing to process

        return true;
    }
}
