package de.uulm.vs.autodetect.mds.framework.model.ipc;

import de.uulm.vs.autodetect.mds.framework.inputAPI.ExternalOpinion;

/**
 * Message used to signify new <b>external</b> opinions, i.e., those received
 * from other instances of Maat, or some other external source that is capable
 * of generating valid opinions.
 *
 * @author Rens van der Heijden
 */
public class InputOpinionIPCMessage extends IPCMessage {

    protected final ExternalOpinion opinion;

    public InputOpinionIPCMessage(ExternalOpinion eo) {
        this.opinion = eo;
    }

    public ExternalOpinion getExternalOpinion() {
        return this.opinion;
    }

    @Override
    public IPCMessageType getMessageType() {
        return IPCMessageType.InputOpinion;
    }
}
