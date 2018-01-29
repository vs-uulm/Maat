package de.uulm.vs.autodetect.mds.framework.model.ipc;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;

public class DetectionTriggerIPCMessage extends IPCMessage {

    /**
     * Identifies which version this DetectionTrigger contains changes from.
     */
    public final long version;

    protected final WorldModelDiff change;

    public long getVersion() {
        return this.version;
    }

    public DetectionTriggerIPCMessage(NewDiffIPCMessage msg) {
        this.version = msg.getVersion();
        this.change = msg.getChange();
    }

    public WorldModelDiff getChange() {
        return this.change;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.DetectionTrigger;
    }

    @Override
    public String toString() {
        return "DetectionTriggerIPC -- " + version;
    }
}
