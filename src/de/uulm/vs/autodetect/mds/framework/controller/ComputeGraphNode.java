package de.uulm.vs.autodetect.mds.framework.controller;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.ipc.IPCException;
import de.uulm.vs.autodetect.mds.framework.model.ipc.IPCMessage;
import de.uulm.vs.autodetect.mds.framework.model.ipc.NewSnapshotIPCMessage;
import de.uulm.vs.autodetect.mds.framework.view.Maat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This interface represents an item in the compute graph. The compute graph is
 * documented in Chapter 2 of AutoDetect Report 2. Basically, it ensures that
 * each node in this graph can operate in parallel, and each of them has
 * well-defined interfaces through message queues. This is akin to the "think
 * like a vertex" paradigm in graph computing.
 *
 * @author Rens van der Heijden
 */
public abstract class ComputeGraphNode implements Runnable {
    private final Logger l = LogManager.getLogger(getClass());

    /**
     * Message input queue for this node.
     */
    private final ConcurrentLinkedQueue<IPCMessage> inputQueue = new ConcurrentLinkedQueue<>();

    protected static AtomicLong lastUpdate = new AtomicLong(Long.MAX_VALUE);
    private static AtomicBoolean terminating = new AtomicBoolean(false);

    public ComputeGraphNode() {
    }

    public synchronized void deliverMessage(IPCMessage msg) {
        l.trace("Message arrived: " + msg);
        this.inputQueue.add(msg);
        Maat.executor.schedule(this, 1, TimeUnit.MILLISECONDS);
    }

    protected abstract boolean peek(IPCMessage msg) throws IPCException;

    protected abstract void consume(IPCMessage msg) throws IPCException;

    @Override
    public void run() {
        if (!terminating.get()) {
            try {
                synchronized (this) {
                    IPCMessage msg = this.inputQueue.peek();
                    if (msg != null && peek(msg)) {
                        this.inputQueue.remove(msg);
                        consume(msg);
                        l.trace("Message processed: " + msg);
                    }
                    //TODO this is a bit of a hack for termination (see below..)
                    if (this instanceof SnapshotManager && this.inputQueue.isEmpty()) {
                        l.warn("SnapshotManager Queue is empty, we might be done..");
                        Maat.executor.schedule(this, Maat.TIMEOUT, TimeUnit.MILLISECONDS);
                    }
                }
            } catch (RejectedExecutionException e) {
                this.l.error("Thread pool rejected execution; this should only happen when Maat is terminating.", e);
            } catch (Exception e) {
                Maat.ERROR_COUNTER.incrementAndGet();
                this.l.error("A Maat component failed for unknown reasons.", e);
            }

            //TODO this is not exactly a nice solution..
            long t = System.currentTimeMillis();
            long diff = t - lastUpdate.get();
            if (this instanceof SnapshotManager && inputQueue.isEmpty()) {
                l.warn("Queue empty! Timeout? " + diff + " = " + t + " - " + lastUpdate);
                if (diff > Maat.TIMEOUT) {
                    if (!terminating.getAndSet(true))
                        Maat.startTermination();
                }
            }
        }
    }

    //TODO this is a pretty serious hack to ensure sequential processing...
    protected IPCMessage findReply(WorldModelDiff diff) {
        IPCMessage result = null;
        for (IPCMessage item : inputQueue) {
            if (item instanceof NewSnapshotIPCMessage && ((NewSnapshotIPCMessage) item).getSource().getChange().equals(diff))
                result = item;
        }

        inputQueue.remove(result);

        return result;
    }
}
