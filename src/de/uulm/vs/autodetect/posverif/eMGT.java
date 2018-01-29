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
 * Created by Florian Diemer on 22.03.2017.
 */
public class eMGT extends Detector {
    static final Logger l = LogManager.getLogger(eMGT.class);

    public double THRESHOLD; // 42m/s ~ 150 km/h
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

    protected eMGT(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(eMGT.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
                break;
            }
        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing an eMGT that cannot detect based on the supplied diff");

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

        if (dataContainer == null || dataContainer.historySize() < 2) {
            return new ArrayList<>(); //cannot detect..
        }

        Iterator<DataContainer> dataContainerIterator = dataContainer.getHistory().iterator();
        DataContainer last = dataContainerIterator.next();
        DataContainer beforeLast = dataContainerIterator.next();

        assert last == dataContainer.getLatestValue();
        assert last == dataContainer.getHistory().first();

        TimeOfArrivalContainer prevTime = (TimeOfArrivalContainer) beforeLast.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
        TimeOfArrivalContainer currTime = (TimeOfArrivalContainer) last.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
        double timeDiff = Math.abs(currTime.getTimeOfArrival() - prevTime.getTimeOfArrival());
        PositionContainer prevPos = (PositionContainer) beforeLast.get(WorldModelDataTypeEnum.POSITION);
        PositionContainer currPos = (PositionContainer) last.get(WorldModelDataTypeEnum.POSITION);
        double distance = Math.abs(currPos.distance(prevPos));
        double velocity = distance / timeDiff;

        SubjectiveOpinion opinion;

        // opinion for eMGT detector
        opinion = getDetectionResult(velocity, THRESHOLD);
        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
    }

    SubjectiveOpinion getDetectionResult(double velocity, double threshold) {
        // TODO test values
        double uncertainty = Math.exp(-(Math.pow(velocity - threshold, 2.0) / (2.0 * SIGMA2)));
        double belief = 0.0;
        double disbelief = 0.0;
        if (velocity <= threshold) {
            belief = 1.0 - uncertainty;
        } else {
            disbelief = 1.0 - uncertainty;
        }
        return new SubjectiveOpinion(belief, disbelief, uncertainty);
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
                if (vertex.hasTypes(eMGT.dataTypes)
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
