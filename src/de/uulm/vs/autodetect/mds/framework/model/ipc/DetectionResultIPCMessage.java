package de.uulm.vs.autodetect.mds.framework.model.ipc;

import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelVertex;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;

public class DetectionResultIPCMessage extends IPCMessage {

    /**
     * This points to the DetectionTriggerIPCMessage that was responsible for
     * the generation of this DetectionResultIPCMessage. Used to match results
     * to triggers.
     */
    private final DetectionTriggerIPCMessage cause;

    private final SubjectiveOpinion opinion;

    private final WorldModelVertex opinionObject;

    private final AbstractDetectorFactory detectorType;

    public DetectionResultIPCMessage(DetectionTriggerIPCMessage trigger, AbstractDetectorFactory detectorType, SubjectiveOpinion opinion, WorldModelVertex object) {
        this.cause = trigger;
        this.opinion = opinion;
        this.opinionObject = object;
        this.detectorType = detectorType;
    }

    public DetectionTriggerIPCMessage getCause() {
        return this.cause;
    }

    public SubjectiveOpinion getOpinion() {
        return this.opinion;
    }

    public WorldModelVertex getOpinionObject() {
        return this.opinionObject;
    }

    public AbstractDetectorFactory getDetectorType() {
        return this.detectorType;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.DetectionResult;
    }

    @Override
    public String toString() {
        return "DetectionResult for " + cause;
    }
}
