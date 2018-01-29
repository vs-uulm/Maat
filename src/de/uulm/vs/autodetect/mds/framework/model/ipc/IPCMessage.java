package de.uulm.vs.autodetect.mds.framework.model.ipc;

public abstract class IPCMessage {

    /**
     * UID for messages: memory address will do here, because this prototype
     * will not run on multiple machines anyway.
     */
    private final long UID = super.hashCode();

    /**
     * Returns a machine-wide unique identifier for this IPCMessage, which is
     * generated at creation time.
     *
     * @return the UID of this message.
     */
    public long getUID() {
        return this.UID;
    }

    public abstract IPCMessageType getMessageType();
}
