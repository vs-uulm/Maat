package de.uulm.vs.autodetect.mds.framework.model.ipc;

import de.uulm.vs.autodetect.mds.framework.inputAPI.Identity;
import de.uulm.vs.autodetect.mds.framework.inputAPI.Measurement;

/**
 * Message with a new measurement received from an external source, to be processed into Maat.
 *
 * @author Rens van der Heijden
 */
public class InputDataIPCMessage extends IPCMessage {

    protected final Measurement measurement;
    /**
     * the source of this message. May be null.
     */
    protected final Identity source;

    /**
     * @param m
     * @param source the source of this message. null is allowed.
     */
    public InputDataIPCMessage(Measurement m, Identity source) {
        this.measurement = m;
        this.source = source;
    }

    public Measurement getMeasurement() {
        return this.measurement;
    }

    /**
     * @return the source of this message. Might be null.
     */
    public Identity getSource() {
        return this.source;
    }

    @Override
    public String toString() {
        return "Framework input: " + this.measurement + " originating from " + this.source;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.InputData;
    }
}
