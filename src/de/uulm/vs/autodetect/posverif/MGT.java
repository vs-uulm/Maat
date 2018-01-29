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
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfTransmissionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionTriggerIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * This Detector implements the Mobility Grade Threshold, originally proposed by Tim Leinmüller et al. in this paper:
 * http://doi.acm.org/10.1145/1161064.1161075
 * <p>
 * The detector works by examining subsequent positions from the same sender, and using the time between these beacons
 * to compute an average speed. If this speed is too high, the new beacon is considered malicious. This prevents a type
 * of attack described in the paper, related to false positions. The threshold defines how high "too high" is, and
 * according to the original paper, should include a buffer for speeding vehicles. This implementation uses meters per
 * second to specify this speed.
 * <p>
 * Note that this implementation also operates on local position samples, as long as time of arrival is available.
 *
 * @author Rens van der Heijden
 */
public class MGT extends Detector {
    static final Logger l = LogManager.getLogger(MGT.class);

    public double THRESHOLD; // 42m/s ~ 150 km/h

    /**
     * a List of the WorldModelDataTypes that are of interest for this Detector
     */
    final static Set<WorldModelDataTypeEnum> dataTypes;

    static {
        dataTypes = new HashSet<>(2);
        dataTypes.add(WorldModelDataTypeEnum.POSITION);
        dataTypes.add(WorldModelDataTypeEnum.TIME_OF_TRANSMISSION);
    }
    /* END OF LIST */

    private final String beaconUID;

    MGT(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(MGT.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
            }

        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing an MGT that cannot detect based on the supplied diff");

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

        if (dataContainer == null || dataContainer.historySize() < 2)
            return new ArrayList<>(); //cannot detect..

        Iterator<DataContainer> iterator = dataContainer.getHistory().iterator();
        DataContainer last = iterator.next();
        DataContainer beforeLast = iterator.next();

        assert last == dataContainer.getLatestValue();
        assert last == dataContainer.getHistory().first();

        TimeOfTransmissionContainer prevTime = (TimeOfTransmissionContainer) beforeLast.get(WorldModelDataTypeEnum.TIME_OF_TRANSMISSION);
        TimeOfTransmissionContainer currTime = (TimeOfTransmissionContainer) last.get(WorldModelDataTypeEnum.TIME_OF_TRANSMISSION);
        double time = currTime.getTimeOfTransmission() - prevTime.getTimeOfTransmission();
        PositionContainer prevPos = (PositionContainer) beforeLast.get(WorldModelDataTypeEnum.POSITION);
        PositionContainer currPos = (PositionContainer) last.get(WorldModelDataTypeEnum.POSITION);
        double distance = currPos.distance(prevPos);

        SubjectiveOpinion opinion;
        if (distance / time < THRESHOLD)
            opinion = new SubjectiveOpinion(0, 0, 1); //within threshold; no idea about validity.
        else
            opinion = new SubjectiveOpinion(0, 1, 0); //outside threshold; considered malicious.

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
                if (vertex.hasTypes(MGT.dataTypes) //correct data type
                        && vertex.historySize() > 1                  //more than one history item
                        && vertex.UID.equals(beaconUID))             //same source
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
