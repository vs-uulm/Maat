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
 * Created by Florian Diemer on 28.03.2017.
 * <p>
 * copied from DistanceMovedVerifier
 */
public class MDM extends Detector {
    static final Logger l = LogManager.getLogger(MDM.class);

    public double THRESHOLD_DISTANCE;
    public double THRESHOLD_TIME;

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

    protected MDM(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(MDM.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
                break;
            }
        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing a MDM that cannot detect based on the supplied diff");

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
                && distance < THRESHOLD_DISTANCE) {
            prevDataContainer = dataContainerIterator.next();
            prevPos = (PositionContainer) prevDataContainer.get(WorldModelDataTypeEnum.POSITION);
            prevTime = (TimeOfArrivalContainer) prevDataContainer.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
            timeDiff = Math.abs(currTime.getTimeOfArrival() - prevTime.getTimeOfArrival());
            distance = currPos.distance(prevPos);
            counter++;
        }

        SubjectiveOpinion opinion;
        if (distance > THRESHOLD_DISTANCE)
            opinion = new SubjectiveOpinion(1, 0, 0); //definitely 100% safe
        else if (timeDiff < THRESHOLD_TIME)
            opinion = new SubjectiveOpinion(0, 0, 1); //I have no idea!!
        else
            opinion = new SubjectiveOpinion(0, 1, 0); //did not move for a long time

        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
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
                if (vertex.hasTypes(MDM.dataTypes)
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
