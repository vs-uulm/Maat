package de.uulm.vs.autodetect.mds.framework.model.ipc;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;

import java.util.Set;

/**
 * This Message passes new detection results that shall be added to the world
 * model.
 *
 * @author Rens van der Heijden
 */
public class NewSnapshotIPCMessage extends IPCMessage {

    protected final DetectionTriggerIPCMessage source;

    protected final Set<IPCMessage> detectionResults;

    public NewSnapshotIPCMessage(DetectionTriggerIPCMessage cause, Set<IPCMessage> results) {
        this.detectionResults = results;
        this.source = cause;
    }

    public Set<IPCMessage> getDetectionResults() {
        return this.detectionResults;
    }

    public DetectionTriggerIPCMessage getSource() {
        return this.source;
    }

    public WorldModelDiff getChange() {
        return this.source.getChange();
    }

    @Override
    public String toString() {
        return "New snapshot, based on: " + this.getChange().getChanges() + " with detection results: " + this.detectionResults;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.NewSnapshot;
    }
}
