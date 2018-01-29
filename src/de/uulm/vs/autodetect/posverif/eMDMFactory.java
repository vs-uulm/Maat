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

public class eMDMFactory extends AbstractDetectorFactory {
    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference) throws WorldModelException {
        eMDM instance = new eMDM(output, reference, this);
        try {
            instance.setAttribute(this.pars, eMDM.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            eMDM.l.error("", e);
        }
        return instance;
    }

    @Override
    public String getName() {
        return eMDM.class.getCanonicalName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return eMDM.dataTypes;
    }

    @Override
    public boolean canProcess(WorldModelDiff diff) {
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = (WorldModelDataVertex) i;
                if (vertex.hasTypes(eMDM.dataTypes)
                        && vertex.historySize() > 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
