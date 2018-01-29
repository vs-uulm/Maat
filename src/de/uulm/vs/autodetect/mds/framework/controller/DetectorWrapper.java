package de.uulm.vs.autodetect.mds.framework.controller;

import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.ipc.*;
import de.uulm.vs.autodetect.mds.framework.view.Maat;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class implements a wrapper around a detector type. All instances of the
 * specified type of detector will be accessed through here. This Wrapper class
 * can be regarded as the detector's representation within the world model, and
 * it allows a detector to have distinct state for each processed message (or
 * indeed, the same state for all).
 *
 * @author Rens van der Heijden
 */
public class DetectorWrapper extends ComputeGraphNode {

    /**
     * This list contains all actual individual detectors, which may each have
     * their own state.
     */
    protected final List<Detector> detectorStates;

    /**
     * This map contains a cache of results, and is used to ensure that all
     * results are collected before actually issuing a message to the aggregator
     * that puts those results into the graph model.
     */
    protected final Map<DetectionTriggerIPCMessage, DetectionResultContainer> resultCache;

    /**
     * This is the aggregator node, to which generated messages are sent. It is
     * the only output for this class.
     */
    protected final ComputeGraphNode output;

    protected final AbstractDetectorFactory detectorType;

    /**
     * Ref to thread pool, needed to be able to start new Detector instances
     */
    //protected final ScheduledExecutorService threadPool;

    public DetectorWrapper(final ComputeGraphNode output, AbstractDetectorFactory detectorType, final ScheduledExecutorService pool) {
        this.detectorStates = new LinkedList<>();
        this.resultCache = new HashMap<>();
        this.output = output;
        this.detectorType = detectorType;

        //this.threadPool = pool;
    }

    public static AtomicLong c = new AtomicLong(0);

    protected void consume(DetectionTriggerIPCMessage message){
        WorldModelDiff diff = message.getChange();
        List<Detector> detectorsToStart = new ArrayList<>();

        for (Detector detector : this.detectorStates) {
            if (detector.canProcess(diff)) {
                detectorsToStart.add(detector);
            }
        }

        if (detectorsToStart.isEmpty() && this.detectorType.canProcess(diff)) {
            try {
                Detector res = this.detectorType.getNewInstance(this, diff);

                this.detectorStates.add(res);

                //this.threadPool.scheduleAtFixedRate(res, 0, Maat.EXECUTION_PERIOD, TimeUnit.MILLISECONDS);

                detectorsToStart.add(res);
            } catch (WorldModelException | IllegalArgumentException | SecurityException e) {
                e.printStackTrace();
                throw new RuntimeException("attempted to create instance of " + this.getName() + ", but encountered an Exception!", e);
            }
        }

        int expectedResponseCount = 0;

        for (Detector detector : detectorsToStart) {
            //detector.detect(message);
            expectedResponseCount += detector.responsesPerMessage(message);
        }
        if (expectedResponseCount == 0) //this detector cannot do anything with this trigger
            this.output.deliverMessage(new DetectionResultSetIPCMessage(message, new HashSet<IPCMessage>(0)));
        else {
            this.resultCache.put(message, new DetectionResultContainer(expectedResponseCount));
            for (Detector detector : detectorsToStart) {
                detector.deliverMessage(message);
            }
        }
    }

    protected void consume(DetectionResultIPCMessage message){

        DetectionTriggerIPCMessage trigger = ((DetectionResultIPCMessage) message).getCause();
        DetectionResultContainer container = this.resultCache.get(trigger);
        if (container == null) {
            throw new NullPointerException("DetectionResultContainer does not exist for " + trigger);
        }

        container.addResult(message);

        if (container.isComplete()) {
            this.resultCache.remove(trigger);
            this.output.deliverMessage(new DetectionResultSetIPCMessage(trigger, container.results));
        }
    }

    @Override
    protected void consume(IPCMessage msg) throws IPCException {
        c.getAndIncrement();
        if (msg.getMessageType().equals(IPCMessageType.DetectionTrigger)) {
            consume( (DetectionTriggerIPCMessage) msg);
        } else if (msg.getMessageType().equals(IPCMessageType.DetectionResult)) {
            consume((DetectionResultIPCMessage) msg);
        } else {
            throw new IPCException("Invalid IPC message type at DetectionWrapper!");
        }
    }

    public String getName() {
        return this.detectorType.getName();
    }

    @Override
    protected boolean peek(IPCMessage msg) throws IPCException {
        return true;
    }
}
