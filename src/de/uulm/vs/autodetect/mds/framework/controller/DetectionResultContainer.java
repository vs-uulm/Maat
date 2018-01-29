package de.uulm.vs.autodetect.mds.framework.controller;

import de.uulm.vs.autodetect.mds.framework.model.ipc.IPCMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * Internal struct to represent cache contents, used by {@link DetectorWrapper}
 * and {@link DetectorManager}.
 *
 * @author Rens van der Heijden
 */
public class DetectionResultContainer {
    /**
     * Messages that have arrived so far.
     */
    public final Set<IPCMessage> results;
    /**
     * Amount of messages that should arrive.
     */
    public final int target;

    public DetectionResultContainer(int targetLength) {
        this.results = new HashSet<>(targetLength);
        this.target = targetLength;
    }

    public boolean isComplete() {
        return this.target == this.results.size();
    }

    public void addResult(IPCMessage msg) {
        if (this.isComplete())
            throw new RuntimeException("Adding result to a full container! This can cause weird memory bugs, so we fail here.");
        this.results.add(msg);
    }
}
