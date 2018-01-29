package de.uulm.vs.autodetect.mds.framework.model.ipc;

import java.util.Set;

public class DetectionResultSetIPCMessage extends IPCMessage {

    /**
     * This points to the DetectionTriggerIPCMessage that was responsible for
     * the generation of this DetectionResultIPCMessage. Used to match results
     * to triggers.
     */
    public final DetectionTriggerIPCMessage cause;

    public final Set<IPCMessage> resultSet;

    // TODO the actual result (in the model, one or more new edges)

    public DetectionResultSetIPCMessage(DetectionTriggerIPCMessage trigger, Set<IPCMessage> results) {
        this.resultSet = results;
        this.cause = trigger;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.DetectionResultSet;
    }

    @Override
    public String toString() {
        return "DetectionResultSet for " + cause;
    }
}
