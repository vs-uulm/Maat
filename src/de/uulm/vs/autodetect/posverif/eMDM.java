package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelItem;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.PositionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfArrivalContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionTriggerIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by Florian Diemer on 29.03.2017.
 */
public class eMDM extends Detector {
    static final Logger l = LogManager.getLogger(eMDM.class);

    public double THRESHOLD_DISTANCE;
    public double THRESHOLD_TIME;
    public double SIGMA2;

    /**
     * a List of the WorldModelDataTypes that are of interest for this Detector
     */
    final static Set<WorldModelDataTypeEnum> dataTypes;

    static {
        dataTypes = new HashSet<>(2);
        dataTypes.add(WorldModelDataTypeEnum.POSITION);
        dataTypes.add(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
    }
    /* END OF LIST */

    private final String beaconUID;

    protected eMDM(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(eMDM.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
                break;
            }
        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing a eMDM that cannot detect based on the supplied diff");

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

        SubjectiveOpinion opinion;
        // opinion for eMDM detector
        opinion = getDetectionResult(dataContainer, THRESHOLD_DISTANCE);
        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
    }

    SubjectiveOpinion getDetectionResult(WorldModelDataVertex dataContainer, double threshold_distance) {
        Iterator<DataContainer> dataContainerIterator = dataContainer.getHistory().iterator();
        DataContainer last = dataContainerIterator.next();
        TimeOfArrivalContainer currTime = (TimeOfArrivalContainer) last.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
        PositionContainer currPos = (PositionContainer) last.get(WorldModelDataTypeEnum.POSITION);
        DataContainer prevDataContainer;
        TimeOfArrivalContainer prevTime;
        PositionContainer prevPos;
        double timeDiff = 0.0;
        double distance = 0.0;

        int counter = 1;
        while (counter < dataContainer.getHistory().size()
                && timeDiff < THRESHOLD_TIME
                && distance < threshold_distance) {
            prevDataContainer = dataContainerIterator.next();
            prevPos = (PositionContainer) prevDataContainer.get(WorldModelDataTypeEnum.POSITION);
            prevTime = (TimeOfArrivalContainer) prevDataContainer.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
            timeDiff = Math.abs(currTime.getTimeOfArrival() - prevTime.getTimeOfArrival());
            distance = currPos.distance(prevPos);
            counter++;
        }
        return getDetetectionResult(distance, timeDiff, threshold_distance);
    }

    SubjectiveOpinion getDetetectionResult(double distance, double timeDiff, double threshold_distance) {
        // TODO test values
        SubjectiveOpinion opinion;
        double threshold_velocity = threshold_distance / THRESHOLD_TIME;
        if (distance <= threshold_distance && timeDiff <= THRESHOLD_TIME) {
            opinion = new SubjectiveOpinion(0, 0, 1); //I have no idea!!
        } else {
            double velocity = distance / timeDiff;

            double uncertainty = Math.exp(-(Math.pow(velocity - threshold_velocity, 2.0) / (2.0 * SIGMA2)));
            double belief = 0.0;
            double disbelief = 0.0;

            if (distance > threshold_distance) {
                //belief = 1.0 - uncertainty;
                uncertainty = 1.0;
            } else {
                disbelief = 1.0 - uncertainty;
            }
            opinion = new SubjectiveOpinion(belief, disbelief, uncertainty);
        }
        return opinion;
    }

    @Override
    public int responsesPerMessage(DetectionTriggerIPCMessage msg) {
        return 1;
    }

    @Override
    public boolean canProcess(WorldModelDiff item) {
        for (WorldModelItem i : item.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = (WorldModelDataVertex) i;
                if (vertex.hasTypes(eMDM.dataTypes)
                        && vertex.historySize() > 1
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
