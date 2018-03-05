package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.*;
import de.uulm.vs.autodetect.mds.framework.model.containers.PositionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionTriggerIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by Florian Diemer on 05.04.2017.
 */
public class eOPD extends Detector {
    static final Logger l = LogManager.getLogger(eOPD.class);

    public double THRESHOLD_DISTANCE;
    public int RECTANGLE_COUNT;
    public double GAMMA;

    /**
     * a List of the WorldModelDataTypes that are of interest for this Detector
     */
    final static Set<WorldModelDataTypeEnum> dataTypes;

    static {
        dataTypes = new HashSet<>(1);
        dataTypes.add(WorldModelDataTypeEnum.POSITION);
    }
    /* END OF LIST */

    private final String beaconUID;

    protected eOPD(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(eOPD.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
                break;
            }
        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing an eOPD that cannot detect based on the supplied diff");

        this.beaconUID = tmp;
    }

    @Override
    public Collection<DetectionResultIPCMessage> detect(DetectionTriggerIPCMessage msg) {
        WorldModelDataVertex dataContainer = null;
        WorldModelDiff toBeDetected = msg.getChange();

        for (WorldModelItem change : toBeDetected.getChanges()) {
            if (change instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) change).UID.equals(this.beaconUID)) {
                dataContainer = (WorldModelDataVertex) change;
                break;
            }
        }

        if (dataContainer == null || dataContainer.historySize() < 2)
            return new ArrayList<>(); //cannot detect..

        PositionContainer pos1 = (PositionContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer pos2;
        SubjectiveOpinion opinion;
        Collection<DetectionResultIPCMessage> detectionResultIPCMessages = new ArrayList<DetectionResultIPCMessage>();
        long detected;
        SubjectiveOpinion currentOpinion = null;

        WorldModelDataVertex worldModelDataVertex;
        for (WorldModelVertex worldModelVertex : toBeDetected.getVertices()) {
            if (worldModelVertex instanceof WorldModelDataVertex) {
                worldModelDataVertex = (WorldModelDataVertex) worldModelVertex;
                if (worldModelDataVertex.historySize() >= 2 && !worldModelDataVertex.UID.equals(beaconUID)) {
                    pos2 = (PositionContainer) worldModelDataVertex.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
                    if (pos1.distance(pos2) < THRESHOLD_DISTANCE) {
                        detected = IntersectionCheck.intersects(worldModelDataVertex, dataContainer, RECTANGLE_COUNT);
                        opinion = getDetectionResult(detected);
                        if ((currentOpinion == null) ||
                                ((currentOpinion != null) && (opinion.getExpectation() < currentOpinion.getExpectation()))) {
                            currentOpinion = opinion;
                        }
                    }
                }
            }
        }

        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), currentOpinion, dataContainer));
    }

    SubjectiveOpinion getDetectionResult(long detected) {
        // TODO test values
        SubjectiveOpinion opinion;
        if (detected < 0) {
            opinion = new SubjectiveOpinion(0, 0, 1);
        } else {
            double uncertainty = 1.0 - (1.0 / Math.pow((double) detected + 1.0, GAMMA));
            double disbelief = 1.0 - uncertainty;
            opinion = new SubjectiveOpinion(0.0, disbelief, uncertainty);
        }
        return opinion;
    }

    @Override
    public int responsesPerMessage(DetectionTriggerIPCMessage msg) {
        WorldModelDataVertex dataContainer = null;
        WorldModelDiff toBeDetected = msg.getChange();

        for (WorldModelItem change : toBeDetected.getChanges()) {
            if (change instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) change).UID.equals(this.beaconUID)) {
                dataContainer = (WorldModelDataVertex) change;
                break;
            }
        }

        if (dataContainer == null || dataContainer.historySize() < 2)
            return 0; //cannot detect..

        PositionContainer pos1 = (PositionContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer pos2;

        WorldModelDataVertex worldModelDataVertex;
        for (WorldModelVertex worldModelVertex : toBeDetected.getVertices()) {
            if (worldModelVertex instanceof WorldModelDataVertex) {
                worldModelDataVertex = (WorldModelDataVertex) worldModelVertex;
                if (worldModelDataVertex.historySize() >= 2 && !worldModelDataVertex.UID.equals(beaconUID)) {
                    pos2 = (PositionContainer) worldModelDataVertex.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
                    if (pos1.distance(pos2) < THRESHOLD_DISTANCE) {
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean canProcess(WorldModelDiff item) {
        for (WorldModelItem i : item.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = (WorldModelDataVertex) i;
                if (vertex.hasTypes(eOPD.dataTypes)
                        && vertex.historySize() > 1
                        //&& !vertex.referent.equals(WorldModelDataVertex.LOCAL_REFERENT)
                        && vertex.UID.equals(beaconUID)) {
                    return true;
                }
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
