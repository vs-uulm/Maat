package de.uulm.vs.autodetect.mds.framework.controller;

import de.uulm.vs.autodetect.mds.framework.model.GraphWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.ipc.*;
import de.uulm.vs.autodetect.mds.framework.view.Maat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ScheduledExecutorService;

/**
 * This class is responsible for many core framework features, such as starting
 * detectors, triggering storage into the framework, and generating default
 * edges.
 *
 * @author Rens van der Heijden
 */
public class SnapshotManager extends ComputeGraphNode {
    Logger l = LogManager.getLogger(getClass());

    public static final boolean SEQUENTIAL = true;
    private NewDiffIPCMessage sequentialMessage = null;

    /**
     * This node is responsible for storage operations on the data graph.
     */
    private final GraphWorldModel worldModel;

    /**
     * This node is responsible for the detection process.
     */
    private final DetectorManager detection;

    public SnapshotManager(GraphWorldModel m, DetectorManager d) {
        this.worldModel = m;
        this.detection = d;
    }

    @Override
    protected void consume(IPCMessage msg) throws IPCException {
        synchronized (this) { //synchronize to ensure that at most one message is consumed at the same time
            if ((msg instanceof InputDataIPCMessage || msg instanceof InputOpinionIPCMessage)
                    && SEQUENTIAL && sequentialMessage != null) {
                this.deliverMessage(msg);
            } else {
                if (msg instanceof InputDataIPCMessage) {
                    NewDiffIPCMessage diffMsg = new NewDiffIPCMessage(this.worldModel.link((InputDataIPCMessage) msg));
                    sequentialMessage = diffMsg;
                    this.detection.deliverMessage(diffMsg);
                } else if (msg instanceof InputOpinionIPCMessage) {
                    NewDiffIPCMessage diffMsg = new NewDiffIPCMessage(this.worldModel.link((InputOpinionIPCMessage) msg));
                    sequentialMessage = diffMsg;
                    this.detection.deliverMessage(diffMsg);
                } else if (msg instanceof NewSnapshotIPCMessage) {
                    sequentialMessage = null;
                    this.worldModel.update((NewSnapshotIPCMessage) msg);
                    ComputeGraphNode.lastUpdate.set(System.currentTimeMillis());
                } else
                    throw new IPCException("Invalid IPC message type at PreProcessor!");
            }
        }
    }

    @Override
    protected boolean peek(IPCMessage msg) throws IPCException {
        //TODO this is a pretty serious hack to ensure sequential processing...
        if (SEQUENTIAL && sequentialMessage != null) {
            IPCMessage reply = findReply(sequentialMessage.getChange());
            if (reply != null)
                consume(reply);
            else
                return false;
        }
        return true;
    }

}
