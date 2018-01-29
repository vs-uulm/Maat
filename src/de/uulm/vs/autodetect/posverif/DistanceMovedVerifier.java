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
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionResultIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.DetectionTriggerIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by Rens van der Heijden on 1/18/17.
 */
public class DistanceMovedVerifier extends Detector {
    static final Logger l = LogManager.getLogger(DistanceMovedVerifier.class);

    public double THRESHOLD;

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

    DistanceMovedVerifier(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(DistanceMovedVerifier.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
            }

        }

        if (tmp == null)
            throw new WorldModelException(
                    "Constructing a DistanceMovedVerifier that cannot detect based on the supplied diff");

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

        //TODO this seems overcomplicated...
        int i = 0;
        final Iterator<DataContainer> iterator = dataContainer.getHistory().iterator();
        while (i + 2 < dataContainer.getHistory().size()) {
            iterator.next();
            i++;
        }
        DataContainer beforeLast = iterator.next();
        DataContainer last = iterator.next();
        assert last == dataContainer.getLatestValue();
        assert last == dataContainer.getHistory().last();

        PositionContainer prevPos = (PositionContainer) beforeLast.get(WorldModelDataTypeEnum.POSITION);
        PositionContainer currPos = (PositionContainer) last.get(WorldModelDataTypeEnum.POSITION);

        SubjectiveOpinion opinion;
        if (currPos.distance(prevPos) < THRESHOLD)
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
                if (vertex.hasTypes(DistanceMovedVerifier.dataTypes) //correct data type
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
