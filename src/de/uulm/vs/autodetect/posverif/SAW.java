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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Florian Diemer on 30.03.2017.
 */
public class SAW extends Detector {
    static final Logger l = LogManager.getLogger(SAW.class);

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
    private final String ownDataUID;

    protected SAW(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String tmp = null;
        String myData = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(SAW.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
                break;
            }
        }

        //find the correct local data -- there should be exactly one data item that refers to it; get local data, filter it by the appropriate type, then convert it into a list.
        Stream<WorldModelVertex> s = diff.dataByReferent(WorldModelDataVertex.LOCAL_REFERENT, diff.getVersion());

        List<WorldModelVertex> candidateData = s.filter(
                i -> ((WorldModelDataVertex) i).hasTypes(SAW.dataTypes)).collect(Collectors.toList());

        //check that exactly one item exists, fail otherwise (because myData remains null)
        if (candidateData.size() == 1)
            myData = candidateData.get(0).UID;

        if (tmp == null || myData == null)
            throw new WorldModelException(
                    "Constructing an SAW that cannot detect based on the supplied diff");

        this.beaconUID = tmp;
        this.ownDataUID = myData;
    }

    @Override
    public Collection<DetectionResultIPCMessage> detect(DetectionTriggerIPCMessage msg) {
        WorldModelDataVertex dataContainer = null;
        WorldModelDataVertex ownData = null;
        WorldModelDiff toBeDetected = msg.getChange();

        for (WorldModelItem change : toBeDetected.getChanges()) {
            if (change instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) change).UID.equals(this.beaconUID)) {
                dataContainer = (WorldModelDataVertex) change;
                break;
            }
        }

        try {
            ownData = (WorldModelDataVertex) toBeDetected.findVertex(this.ownDataUID);
        } catch (ClassCastException c) {
            l.error("Could not fetch own data -- invalid WorldModelVertex!");
        }

        // TODO maybe change rating for dataContainer.historySize() > x?
        if (dataContainer == null || ownData == null || dataContainer.historySize() != 1)
            return new ArrayList<>(); //cannot detect..

        PositionContainer ownPos = (PositionContainer) ownData.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer receivedPos = (PositionContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        double distance = ownPos.distance(receivedPos);

        SubjectiveOpinion opinion;
        if (distance > THRESHOLD) {
            opinion = new SubjectiveOpinion(0, 0, 1);
        } else {
            opinion = new SubjectiveOpinion(0, 1, 0);
        }

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
                if (vertex.hasTypes(SAW.dataTypes)
                        //&& !vertex.referent.equals(WorldModelDataVertex.LOCAL_REFERENT)
                        && vertex.historySize() == 1
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
