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
 * This Detector implements the Acceptance Range Threshold, originally proposed by Tim Leinmüller et al. in this paper:
 * http://doi.acm.org/10.1145/1161064.1161075
 * <p>
 * The detector works by examining the own position and comparing it to the received position: the resulting value is
 * compared to a threshold. The reasoning behind it is that messages that are delivered over a single hop will never
 * originate outside the transmission range of a receiver. In other words, a message received from a position outside
 * the receivers' (expected) transmission range is considered to be untrustworthy.
 * <p>
 * The original threshold was set at 250, but others have shown that 400 may be a better value (see 10.18725/OPARU-4080)
 * although in practice the actual transmission range will be strongly dependent on the surrounding area and the
 * presence of objects (e.g., buildings or vegetation).
 *
 * @author Rens van der Heijden
 */
public class eART extends Detector {
    static final Logger l = LogManager.getLogger(eART.class);

    public double TH; //See 10.18725/OPARU-4080
    //public final double SIGMA = 225.0; //See 10.18725/OPARU-4080
    public double SIGMA2=27380;
    public double MAX_UNCERTAINTY=1; //See 10.18725/OPARU-4080

    /**
     * a List of the WorldModelDataTypes that are of interest for this Detector
     */
    final static Set<WorldModelDataTypeEnum> dataTypes;

    static {
        dataTypes = new HashSet<>(1);
        dataTypes.add(WorldModelDataTypeEnum.POSITION);
    }
    /* END OF LIST */

    /**
     * The DataVertex we're analyzing
     */
    private final String beaconUID;

    private final String ownDataUID;

    protected eART(ComputeGraphNode output, WorldModelDiff diff, AbstractDetectorFactory factory)
            throws WorldModelException {
        super(output, factory);

        String myData = null;
        String tmp = null;

        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex
                    && ((WorldModelDataVertex) i).hasTypes(eART.dataTypes)) {
                tmp = ((WorldModelDataVertex) i).UID;
            }

        }

        //find the correct local data -- there should be exactly one data item that refers to it; get local data, filter it by the appropriate type, then convert it into a list.
        Stream<WorldModelVertex> s = diff.dataByReferent(WorldModelDataVertex.LOCAL_REFERENT, diff.getVersion());

        List<WorldModelVertex> candidateData = s.filter(
                i -> ((WorldModelDataVertex) i).hasTypes(eART.dataTypes)).collect(Collectors.toList());

        //List<WorldModelVertex> candidateData = s.collect(Collectors.toList());
        //Iterator<WorldModelVertex> it = candidateData.iterator(); while(it.hasNext()) if (!((WorldModelDataVertex)it.next()).hasTypes(ART.dataTypes)) it.remove();

        //check that exactly one item exists, fail otherwise (because myData remains null)
        if (candidateData.size() == 1)
            myData = candidateData.get(0).UID;

        if (tmp == null || myData == null)
            throw new WorldModelException(
                    "Constructing an ART that cannot detect based on the supplied diff");

        this.beaconUID = tmp;
        this.ownDataUID = myData;
    }

    @Override
    public Collection<DetectionResultIPCMessage> detect(DetectionTriggerIPCMessage msg) {
        WorldModelDataVertex dataContainer = null;
        WorldModelDataVertex ownData = null;
        WorldModelDiff toBeDetected = msg.getChange();

        for (WorldModelItem change : toBeDetected.getChanges()) {
            if (change instanceof WorldModelDataVertex && ((WorldModelDataVertex) change).UID.equals(this.beaconUID))
                dataContainer = (WorldModelDataVertex) change;
        }

        try {
            ownData = (WorldModelDataVertex) toBeDetected.findVertex(this.ownDataUID);
        } catch (ClassCastException c) {
            l.error("Could not fetch own data -- invalid WorldModelVertex!");
        }

        if (dataContainer == null || ownData == null)
            return new ArrayList<>(); //cannot detect..

        //TODO does this work if we .get() a value without position? If not, we need a .getLatest(WMDTE.Position) for the WMDV
        PositionContainer ownPos = (PositionContainer) ownData.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer receivedPos = (PositionContainer) dataContainer.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        double distance = ownPos.distance(receivedPos);

        SubjectiveOpinion opinion;

        // opinion for eART detector
        opinion = getDetectionResult(distance, TH);

        return Arrays.asList(new DetectionResultIPCMessage(msg, this.getDetectorSpecification(), opinion, dataContainer));
    }

    SubjectiveOpinion getDetectionResult(double distance, double threshold) {
        // TODO test values
        double uncertainty = Math.exp(-(Math.pow(distance - threshold, 2.0) / (2.0 * SIGMA2)));
        double belief = 0.0;
        double disbelief = 0.0;
        if (distance <= threshold) {
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
        boolean canProcess = false;
        //data belongs to same beacon?
        for (WorldModelItem i : item.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                if (vertex.UID.equals(beaconUID) && vertex.hasTypes(eART.dataTypes))
                    canProcess = true;
            }
        }

        if (canProcess) //check that own data is actually available (this is probably not needed though?)
            if (item.findVertex(this.ownDataUID) == null)
                canProcess = false;

        return canProcess;
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
