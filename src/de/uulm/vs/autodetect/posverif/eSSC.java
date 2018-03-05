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
public class eSSC extends Detector {
    static final Logger l = LogManager.getLogger(eSSC.class);
    public double OK_DEV=0; // speed that is believed 100%
    public double BAD_DEV; // speed that is believed 0%
    public double UNCERTAINTY_FACTOR=0.1; //uncertainty of this detector

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

    eSSC(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(eSSC.dataTypes)) {
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
        SubjectiveOpinion opinion;
        double deltaSpeed = Math.abs(claimedSpeed.getSpeed()-actualSpeed);

        // belief 100% as long deltaSpeed is REALISTIC, then linearly scale belief and disbelief
        if (deltaSpeed < BAD_DEV) {

            if (deltaSpeed <= OK_DEV) {
                opinion = new SubjectiveOpinion(1-UNCERTAINTY_FACTOR, 0, UNCERTAINTY_FACTOR);
            }
            else {
                // disbelief = 0, when deltaSpeed = OK_DEV
                // disbelief = 1, when deltaSpeed = BAD_DEV
                double disbelief = (deltaSpeed - OK_DEV) / (BAD_DEV - OK_DEV) * (1-UNCERTAINTY_FACTOR);
                double belief = 1 - UNCERTAINTY_FACTOR - disbelief;
                opinion = new SubjectiveOpinion(belief, disbelief, UNCERTAINTY_FACTOR);
            }
        }
        else {
            opinion = new SubjectiveOpinion(0, 1-UNCERTAINTY_FACTOR, UNCERTAINTY_FACTOR);
        }
        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
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
                if (vertex.hasTypes(eSSC.dataTypes) && (vertex.UID.equals(this.beaconUID)) && vertex.historySize() > 1)
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
