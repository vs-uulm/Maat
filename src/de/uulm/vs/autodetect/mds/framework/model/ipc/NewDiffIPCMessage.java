package de.uulm.vs.autodetect.mds.framework.model.ipc;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;

public class NewDiffIPCMessage extends IPCMessage {

    protected final WorldModelDiff diff;

    public NewDiffIPCMessage(WorldModelDiff diff) {
        this.diff = diff;
    }

    public long getVersion() {
        return this.diff.getVersion();
    }

    public WorldModelDiff getChange() {
        return this.diff;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.NewDiff;
    }
}
