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

public class eDMVFactory extends AbstractDetectorFactory {

    public eDMVFactory() {
    }

    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException {
        eDMV instance = new eDMV(output, reference, this);
        try {
            instance.setAttribute(this.pars, eDMV.class);
            return instance;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            eDMV.l.error("", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return eDMV.class.getName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return eDMV.dataTypes;
    }

    @Override
    public boolean canProcess(WorldModelDiff diff) {
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                if (vertex.hasTypes(eDMV.dataTypes)
                        && vertex.historySize() > 1)
                    return true;
            }
        }
        return false;
    }
}
