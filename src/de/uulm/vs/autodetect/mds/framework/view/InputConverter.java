package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.controller.SnapshotManager;
import de.uulm.vs.autodetect.mds.framework.inputAPI.ExternalOpinion;
import de.uulm.vs.autodetect.mds.framework.inputAPI.Identity;
import de.uulm.vs.autodetect.mds.framework.inputAPI.Measurement;
import de.uulm.vs.autodetect.mds.framework.model.containers.IdentityMetaDataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.ipc.InputDataIPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.InputOpinionIPCMessage;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;

import java.util.List;

/**
 * This class is responsible for converting data from the Input model (
 * <code>.comm.model</code>) to the actual model (<code>.framework.model</code>
 * ).
 *
 * @author Rens van der Heijden
 */
public class InputConverter {

    protected final SnapshotManager processor;

    public InputConverter(SnapshotManager preprocess) {
        this.processor = preprocess;
    }

    /**
     * This method gives Maat a new input data message; the list of MaatContainers represents a single message, transmitted by the given source.
     * For example, this could be a beacon with position, time, speed, acceleration; the source identifier is the sender of the beacon. The time of arrival can additionally be included in a container.
     *
     * @param containerList
     * @param sourceIdentifier
     */
    public void process(final List<MaatContainer> containerList, final String sourceIdentifier, final String messageID) {
        Identity source = new Identity(sourceIdentifier, new IdentityMetaDataContainer(sourceIdentifier));
        Measurement newData = new Measurement(containerList, sourceIdentifier, messageID);
        InputDataIPCMessage newDataMessage = new InputDataIPCMessage(newData, source);
        this.processor.deliverMessage(newDataMessage);
    }

    /**
     * This method assumes the caller already created the Identity and Measurement instances; when those are newly created, the call is similar to {@link #process(List, String, String)}.
     * @deprecated
     */
    public void process(final Identity source, final Measurement newData) {
        InputDataIPCMessage newDataMessage = new InputDataIPCMessage(newData, source);
        this.processor.deliverMessage(newDataMessage);
    }

    /**
     * This method submits a new opinion to Maat between two entities.
     *
     * @param opinionFromFile
     * @param sourceIdentifier
     */
    public void process(final SubjectiveOpinion opinionFromFile, final String sourceIdentifier) {
        // TODO: meta-data object (currently, no meta-data is available, so we
        // provide source ID)
        String[] ids = sourceIdentifier.split("_"); // => [0]==IDA, [2]==IDB

        Identity opinionHolder = new Identity(ids[0], new IdentityMetaDataContainer(ids[0]));

        Identity subject = new Identity(ids[2], new IdentityMetaDataContainer(ids[2]));

        ExternalOpinion newOpinion = new ExternalOpinion(opinionHolder, opinionFromFile, subject);

        InputOpinionIPCMessage newOpinionMessage = new InputOpinionIPCMessage(newOpinion);
        this.processor.deliverMessage(newOpinionMessage);
    }
}
