package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.Set;

public class SSVFactory extends AbstractDetectorFactory {
    public SSVFactory() {
    }

    @Override
    public Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException {
        SimpleSpeedVerifier instance = new SimpleSpeedVerifier(output, reference, this);
        try {
            instance.setAttribute(this.pars, SimpleSpeedVerifier.class);
            return instance;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            SimpleSpeedVerifier.l.error("", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return SimpleSpeedVerifier.class.getCanonicalName();
    }

    @Override
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return SimpleSpeedVerifier.dataTypes;
    }
}
