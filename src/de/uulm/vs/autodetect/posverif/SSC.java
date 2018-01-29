package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelItem;
import de.uulm.vs.autodetect.mds.framework.model.containers.*;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionTriggerIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Leo Hnatek
 */
public class SSC extends Detector {
    static final Logger l = LogManager.getLogger(SSC.class);
    double TIME_THRESHOLD; // only use vectors within the boundary of { 0, TIME_THRESHOLD } - in sec.
    double REALISTIC_SPEED_DERIVATION; // speed that is believed 100%
    double UNREALISTIC_SPEED_DERIVATION; // speed that is believed 0%

    /**
     * a List of the WorldModelDataTypes that are of interest for this Detector
     */
    final static Set<WorldModelDataTypeEnum> dataTypes;

    static {
        dataTypes = new HashSet<>(1);
        dataTypes.add(WorldModelDataTypeEnum.SPEED);
        dataTypes.add(WorldModelDataTypeEnum.POSITION);
        dataTypes.add(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
    }
    /* END OF LIST */

    private final String beaconUID;

    SSC(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(SSC.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
            }

        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing a SpeedSanityCheck that cannot detect based on the supplied diff");

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

        // calculate actual speed
        PositionContainer startPos = (PositionContainer) dataContainer.getHistory().last().get(WorldModelDataTypeEnum.POSITION);
        TimeOfArrivalContainer startTime = (TimeOfArrivalContainer) dataContainer.getHistory().last().get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);

        PositionContainer endPos = (PositionContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        TimeOfArrivalContainer endTime = (TimeOfArrivalContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);

        double timeDiff = (endTime.getTimeOfArrival()-startTime.getTimeOfArrival());

        double distDiffX = endPos.getX()-startPos.getX();
        double distDiffY = endPos.getY()-startPos.getY();
        double distDiffZ = endPos.getZ()-startPos.getZ();

        double distDiff = Math.sqrt(distDiffX*distDiffX + distDiffY*distDiffY + distDiffZ*distDiffZ);

        // v = s/t
        double actualSpeed = distDiff/timeDiff;


        // compare to claimed speed
        SpeedContainer claimedSpeed = (SpeedContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.SPEED);

        // only compare if not too much time has passed
        // otherwise the length of the vector used to calculate distDiff might not
        // equal to length of the road
        // NOTE: one could use TIME_THRESHOLD as an index for uncertainty
        if (timeDiff < TIME_THRESHOLD) {
            SubjectiveOpinion opinion;
            double deltaSpeed = Math.abs(claimedSpeed.getSpeed()-actualSpeed);

            // belief 100% as long deltaSpeed is REALISTIC, then linearly scale belief and disbelief
            if (deltaSpeed < UNREALISTIC_SPEED_DERIVATION) {

                if (deltaSpeed <= REALISTIC_SPEED_DERIVATION) {
                    opinion = new SubjectiveOpinion(1, 0, 0);
                }
                else {
                    // disbelief = 0, when deltaSeed = REALISTIC_SPEED_DERIVATION
                    // disbelief = 1, when deltaSpeed = UNREALISTIC_SPEED_DERIVATION
                    double disbelief = (deltaSpeed - REALISTIC_SPEED_DERIVATION) / (UNREALISTIC_SPEED_DERIVATION - REALISTIC_SPEED_DERIVATION);
                    opinion = new SubjectiveOpinion(1-disbelief, disbelief, 0);
                }
            }
            else {
                opinion = new SubjectiveOpinion(0, 1, 0);
            }
            return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
        }

        return new ArrayList<>();
    }

    @Override
    public int responsesPerMessage(DetectionTriggerIPCMessage msg) {
        return 1;
    }

    /**
     * Return true if item contains information about SPEED, LOCATION and TIME,
     * as well as a history size of at least one.
     *
     * @param item
     * @return
     */
    @Override
    public boolean canProcess(WorldModelDiff item) {
        for (WorldModelItem i : item.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                if (vertex.hasTypes(SSC.dataTypes) && (vertex.UID.equals(this.beaconUID)) && vertex.historySize() > 1)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Object getState() {
        return null;
    }

    @Override
    public void setState(Object state) {
        return;
    }

}
