package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelItem;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionTriggerIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * This Detector implements the so-called ART (Acceptance Range Threshold)
 * detector, which checks whether the position provided to it is in
 * communication range.
 *
 * @author Rens van der Heijden
 */
public class SimpleSpeedVerifier extends Detector {
    static final Logger l = LogManager.getLogger(SimpleSpeedVerifier.class);

    /**
     * a List of the WorldModelDataTypes that are of interest for this Detector
     */
    final static Set<WorldModelDataTypeEnum> dataTypes;

    static {
        dataTypes = new HashSet<>(1);
        dataTypes.add(WorldModelDataTypeEnum.SPEED);
    }
    /* END OF LIST */

    private final String beaconUID;

    SimpleSpeedVerifier(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(SimpleSpeedVerifier.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
            }

        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing a SimpleSpeedVertifier that cannot detect based on the supplied diff");

        this.beaconUID = tmp;
    }

    @Override
    public Collection<DetectionResultIPCMessage> detect(DetectionTriggerIPCMessage msg) {
        WorldModelDataVertex dataContainer = null;
        WorldModelDiff toBeDetected = msg.getChange();

        for (WorldModelItem change : toBeDetected.getChanges()) {
            if (change instanceof WorldModelDataVertex && ((WorldModelDataVertex) change).UID.equals(this.beaconUID))
                dataContainer = (WorldModelDataVertex) change;
        }

        if (dataContainer == null)
            return new ArrayList<>(); //cannot detect..

        SubjectiveOpinion opinion;
        if (dataContainer.equals(this.beaconUID))
            opinion = new SubjectiveOpinion(1, 0, 0); //definitely 100% safe
        else
            opinion = new SubjectiveOpinion(0, 0, 1); //I have no idea!!

        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
    }

    @Override
    public int responsesPerMessage(DetectionTriggerIPCMessage msg) {
        return 1;
    }

    /**
     * Return true iff item fits in the context of the Detector's internal
     * state. This is only important for Detectors of which multiple instances
     * will exist.
     *
     * @param item
     * @return
     */
    @Override
    public boolean canProcess(WorldModelDiff item) {
        for (WorldModelItem i : item.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                if (vertex.hasTypes(SimpleSpeedVerifier.dataTypes)
                        && vertex.historySize() > 1)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Object getState() {
        return new Object[0];
    }

    @Override
    public void setState(Object state) {
        return;
    }
}
