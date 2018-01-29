package de.uulm.vs.autodetect.mds.framework.controller;

import de.uulm.vs.autodetect.mds.framework.model.ipc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class DetectorManager extends ComputeGraphNode {

    /**
     * This list contains all actual individual detectors, which may each have
     * their own state.
     */
    protected final List<DetectorWrapper> detectorsWrappers;
    /**
     * This map contains a cache of results, and is used to ensure that all
     * results are collected before actually issuing a message to the aggregator
     * that puts those results into the graph model.
     */
    protected final Map<DetectionTriggerIPCMessage, DetectionResultContainer> resultCache;

    /**
     *
     */
    protected ComputeGraphNode output;

    public void setOutput(ComputeGraphNode node) {
        this.output = node;
    }

    public DetectorManager() {
        this.resultCache = new HashMap<>();
        this.detectorsWrappers = new ArrayList<>();
    }

    public void addDetectors(List<DetectorWrapper> detectorList) {
        this.detectorsWrappers.addAll(detectorList);
    }

    @Override
    protected void consume(IPCMessage msg) throws IPCException {

        if (msg instanceof NewDiffIPCMessage) {

            DetectionTriggerIPCMessage trigger = new DetectionTriggerIPCMessage((NewDiffIPCMessage) msg);

            // TODO selection of detectors instead of all (performance)

            this.resultCache.put(trigger, new DetectionResultContainer(this.detectorsWrappers.size()));

            for (DetectorWrapper detectorWrapper : this.detectorsWrappers) {
                detectorWrapper.deliverMessage(trigger);
            }
        } else if (msg instanceof DetectionResultSetIPCMessage)
            consumeDetectionResultSet((DetectionResultSetIPCMessage) msg);
        else
            throw new IPCException("Invalid IPC Message at " + this.getClass().getSimpleName());
    }

    private void consumeDetectionResultSet(DetectionResultSetIPCMessage msg) {
        DetectionTriggerIPCMessage trigger = msg.cause;
        DetectionResultContainer container = this.resultCache.get(trigger);

        if (container == null) {
            throw new NullPointerException("DetectionResultContainer does not exist for " + trigger);
        }

        container.addResult(msg);

        if (container.isComplete()) {
            this.resultCache.remove(trigger);
            NewSnapshotIPCMessage result = new NewSnapshotIPCMessage(trigger, container.results);
            this.output.deliverMessage(result);
        }
    }

    @Override
    protected boolean peek(IPCMessage msg) throws IPCException {
        return true;
    }
}
