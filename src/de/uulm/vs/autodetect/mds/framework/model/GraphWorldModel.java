package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.constants.ConstantDataTypes;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.inputAPI.ExternalOpinion;
import de.uulm.vs.autodetect.mds.framework.inputAPI.Identity;
import de.uulm.vs.autodetect.mds.framework.inputAPI.Measurement;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.OpinionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.mds.framework.model.ipc.*;
import no.uio.subjective_logic.opinion.Opinion;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public abstract class GraphWorldModel implements ReadOnlyWorldModel {
    protected final Logger l = LogManager.getLogger(getClass());

    protected static final String MDS_UID = "MDS-ROOT-NODE";
    protected static final long MDS_ROOT_VERSION_ID = 0;

    protected abstract DirectedGraph<WorldModelVertex, WorldModelEdge> getGraph();

    //unfair RW Lock (default, see javadoc)
    protected final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(false);
    protected final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    protected final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    /**
     * this variable identifies a unique version of the WorldModel.
     */
    private long versionID;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVersion() {
        long result = -1;
        readLock.lock();
        try {
            result = this.versionID;
        } finally {

            readLock.unlock();
        }
        return result;
    }

    /**
     * Increments the versionID.
     * <p>
     * <b>Requires write lock</b>
     */
    private long commit() {
        l.trace("Incrementing version!");
        if (!this.writeLock.isHeldByCurrentThread())
            l.warn("Commit without write lock!");
        this.versionID += 1;
        return this.versionID;
    }

    // Vertex search

    /**
     * {@inheritDoc}
     * <p>
     * <b>Warning:</b> Modifying the returned Vertex can lead to undefined behavior.
     * Changes made to the world model will propagate to the returned Vertex.
     */
    @Override
    public WorldModelVertex findDetectorVertex(AbstractDetectorFactory type) {
        readLock.lock();
        try {
            for (WorldModelVertex v : this.getGraph().vertexSet()) {
                if (v instanceof WorldModelDetectorVertex && ((WorldModelDetectorVertex) v).detectorType.equals(type))
                    return v;
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Warning:</b> Modifying the returned Vertex can lead to undefined behavior.
     * Changes made to the world model will propagate to the returned Vertex.
     */
    @Override
    public WorldModelVertex findVertex(String UID) {
        readLock.lock();
        try {
            for (WorldModelVertex v : this.getGraph().vertexSet()) {
                if (v.UID.equals(UID))
                    return v;
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Warning:</b> Modifying any of the vertices in the returned Set can lead to undefined behavior.
     * However, the set is safe to modify.
     * Changes made to the Graph will <b>not</b> propagate to the returned Set.
     * Changes made to the world model will propagate to the returned Vertices.
     */
    @Override
    public Set<WorldModelDataVertex> getVerticesByType(Set<WorldModelDataTypeEnum> types) {
        readLock.lock();
        try {
            Set<WorldModelDataVertex> resultSet = new HashSet<>();
            for (WorldModelVertex v : this.getGraph().vertexSet()) {
                if (v instanceof WorldModelDataVertex && ((WorldModelDataVertex) v).hasTypes(types))
                    resultSet.add((WorldModelDataVertex) v);
            }
            return resultSet;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Warning:</b> Modifying any of the vertices in the returned Set can lead to undefined behavior.
     * However, the set is safe to modify. Changes made to the Graph will <b>not</b> propagate to the returned Set.
     * Changes made to the world model will propagate to the returned Vertices.
     */
    @Override
    public Set<WorldModelVertex> getVertices() {
        //note: LinkedHashSet is used because its' behavior should be similar to the LinkedHashMap that underlies the graph implementations we use (see org.jgrapht.jgraph.AbstractBaseGraph).
        return new LinkedHashSet<>(this.getGraph().vertexSet());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses {@link #getVertices} to access the Graph.
     * Changes made to the Graph will <b>not</b> propagate to the returned Stream.
     * Changes made to the world model will propagate to the returned Vertices.
     */
    @Override
    public Stream<WorldModelVertex> dataVertexStream(long version) {
        return getVertices().stream().filter(s -> s instanceof WorldModelDataVertex).filter(v -> v.versionID <= version);
    }

    // Edge search

    /**
     * <b>Warning</b>: Modifying the returned WorldModelEdge may lead to undefined behavior.
     * Changes made to the world model will propagate to the returned Edge.
     *
     * @param opinionHolder
     * @param opinionObject
     * @return the opinion that opinionHolder has about opinionObject or
     * a default opinion if no such opinion existed yet.
     * @deprecated this getter may return default opinions that are not actually stored in the model. Instead, use {@link #getAllPaths(WorldModelVertex, WorldModelVertex)} or {@link #isTrustworthy(String, long)}.
     */
    public Pair<WorldModelEdge, Boolean> getOpinionBetween(WorldModelVertex opinionHolder,
                                                           WorldModelVertex opinionObject) {
        if (opinionObject == null || opinionHolder == null)
            throw new IllegalArgumentException("null is not allowed!");

        readLock.lock();
        try {
            WorldModelEdge wme = this.getGraph().getEdge(opinionHolder, opinionObject);
            boolean newOpinion = false;

            if (wme == null) {
                wme = createDefaultOpinion(opinionHolder, opinionObject, this.getVersion());
                newOpinion = true;
            }

            return new Pair<>(wme, newOpinion);
        } finally {
            readLock.unlock();
        }

    }

    /**
     * <b>Warning</b>: Modifying the returned WorldModelEdges may lead to undefined behavior.
     * Changes made to the Graph will <b>not</b> propagate to the returned List.
     * Changes made to the world model will propagate to the returned Edges.
     *
     * @param dataTypes
     * @return all edges that cover exactly the given dataTypes
     */
    public List<WorldModelEdge> getAllEdgesThatExactlyCoverDataTypes(Set<WorldModelDataTypeEnum> dataTypes) {
        if (dataTypes == null || dataTypes.isEmpty()) {
            return new ArrayList<>();
        } // else

        readLock.lock();
        try {
            Set<WorldModelEdge> edgeSet = this.getGraph().edgeSet();
            ArrayList<WorldModelEdge> result = new ArrayList<>(edgeSet.size());
            for (WorldModelEdge worldModelEdge : edgeSet) {
                if (worldModelEdge.exactlyCoversDataTypes(dataTypes)) {
                    result.add(worldModelEdge);
                }
            }
            return result;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * <b>Warning</b>: Modifying the returned WorldModelEdges may lead to undefined behavior.
     * Changes made to the Graph will <b>not</b> propagate to the returned List.
     * Changes made to the world model will propagate to the returned Edges.
     *
     * @param opinionObject
     * @return a Set containing all opinions (WorldModelEdges) about the given
     * opinionObject
     */
    public Set<WorldModelEdge> getOpinionsAbout(WorldModelVertex opinionObject) {
        if (opinionObject == null)
            throw new IllegalArgumentException("null is not allowed!");

        readLock.lock();
        try {
            return this.getGraph().incomingEdgesOf(opinionObject);
        } finally {
            readLock.unlock();
        }
    }

    // Linking

    /**
     * Links the given measurement information to some related information in
     * the graph. Returns an updated copy of the relevant WorldModelDataVertex that matches this constraint,
     * if such a match exists; returns null otherwise.
     * <p>
     * <b>Requires read lock!</b>
     *
     * @param measurement
     * @return
     */
    private WorldModelDataVertex match(Measurement measurement, long versionID) {
        WorldModelDataVertex newVertex;

        WorldModelVertex result = findVertex(measurement.getUID());
        if (result == null) {
            newVertex = null;
        } else {
            if (!(result instanceof WorldModelDataVertex))
                throw new IllegalArgumentException(
                        "The UID " + measurement.getUID() + " refers to a non-data vertex within the model...");

            //copy and write
            newVertex = new WorldModelDataVertex((WorldModelDataVertex) result, versionID);
            for (MaatContainer item : measurement.getValues())
                newVertex.update(item.getDataType(), item, versionID, measurement.getMessageID());
        }

        return newVertex;
    }

    /**
     * Returns a copy of the vertex with data on the given identity, if such a vertex exists. Returns null otherwise.
     * <p>
     * <b>Requires read lock!</b>
     *
     * @param identity
     * @return
     */
    private WorldModelNodeVertex match(Identity identity, long versionID) {
        WorldModelNodeVertex newVertex;

        WorldModelVertex result = findVertex(identity.getUID());
        if (result == null) {
            newVertex = null;
        } else if (result instanceof WorldModelNodeVertex) {
            newVertex = new WorldModelNodeVertex((WorldModelNodeVertex) result, versionID);
        } else
            throw new IllegalArgumentException("The UID " + identity.getUID() + " refers to a non-node vertex within the model...");

        return newVertex;
    }

    /**
     * link the given external opinion to one in the world model.
     * <br>Returns a {@link WorldModelDiffEdge} containing:
     * <br>1) opinion subject / the destination (i.e., data or node)
     * <br>2) the opinion (the edge)
     * <br>3) opinion holder / the source
     * <br>Note that 1 and 3 are <b>not</b> safe to access: they are
     * direct references to WM contents.
     * <p>
     * <b>Requires read lock!</b>
     *
     * @param opinion
     * @return null iff the opinion's subject is not in the WM
     */
    //TODO versionID superfluous? should always be WM version..
    private WorldModelDiffEdge match(ExternalOpinion opinion, long versionID) {
        WorldModelVertex opinionObject;
        if (opinion.isAboutData())
            opinionObject = findVertex(opinion.getData().getUID());
        else
            opinionObject = findVertex(opinion.getSubject().getUID());

        if (opinionObject == null)
            return null;

        WorldModelVertex opinionHolder = findVertex(opinion.getOpinionHolder().getUID());

        WorldModelEdge result = this.getGraph().getEdge(opinionHolder, opinionObject);

        if (result != null) {
            result = new WorldModelEdge(result, versionID + 1);
            result.update(opinion, versionID + 1);
        } else {
            result = new WorldModelEdge(opinion, versionID + 1, ConstantDataTypes.opinionType);
        }

        return new WorldModelDiffEdge(opinionHolder, opinionObject, result, versionID + 1);
    }

    /**
     * Returns a copy of the edge that exists between the items identified by the UIDs of the given items,
     * such that sourceVertex -> objectVertex, if any. Returns null if any of the parameters is null,
     * or if such a vertex does not exist
     *
     * @param sourceVertex
     * @param objectVertex
     * @param versionID    the new version that the copy should have
     * @return
     */
    private WorldModelEdge findExistingOpinion(WorldModelVertex sourceVertex, WorldModelVertex objectVertex, long versionID) {
        if (sourceVertex == null || objectVertex == null)
            return null;
        WorldModelEdge tmp = this.getGraph().getEdge(findVertex(sourceVertex.UID), findVertex(objectVertex.UID));
        if (tmp == null)
            return null;
        return new WorldModelEdge(tmp, versionID);
    }

    // Update methods

    /**
     * Method responsible for merging a diff with the current world model version to a new world model version.
     * This method is also responsible for resolving any potential conflicts.
     *
     * @param diff
     */
    protected abstract void merge(WorldModelDiff diff);

    /**
     * Note: this method does *not* commit! It should only be called by other
     * methods that perform commits; internally generated opinions should be
     * batch-added using {@link #update(NewSnapshotIPCMessage)}.
     * <p>
     * <b>requires write lock!</b>
     *
     * @param opinionMsg
     * @return
     * @throws WorldModelException
     */
    protected WorldModelEdge update(DetectionResultIPCMessage opinionMsg) throws WorldModelException {
        WorldModelVertex opinionHolder = this.findDetectorVertex(opinionMsg.getDetectorType());
        WorldModelEdge opinion = null;
        WorldModelVertex opinionObject = this.findVertex(opinionMsg.getOpinionObject().UID);

        if (opinionHolder == null) {
            opinionHolder = this.createAndAddDetectorVertex(opinionMsg.getDetectorType());
        }

        // TODO correct version in both branches
        opinion = this.getGraph().getEdge(opinionHolder, opinionObject);
        if (opinion != null)
            opinion.update(opinionMsg);
        else {
            opinion = new WorldModelEdge(opinionMsg, this.getVersion(), new HashSet<>());
            this.getGraph().addEdge(opinionHolder, opinionObject, opinion);
        }

        // note: commit & listener notification should be done by callee!

        return opinion;
    }

    // Creation helper methods

    private WorldModelVertex createAndAddDetectorVertex(AbstractDetectorFactory detectorType)
            throws WorldModelException {
        writeLock.lock();
        readLock.lock();
        try {
            WorldModelVertex vertex = new WorldModelDetectorVertex(detectorType);
            if (this.getGraph().containsVertex(vertex))
                throw new WorldModelException("Attempted to create existing detector vertex");
            this.getGraph().addVertex(vertex);

            WorldModelEdge defaultEdge = new WorldModelEdge(new SubjectiveOpinion(1, true), this.getVersion(),
                    ConstantDataTypes.opinionType);

            this.getGraph().addEdge(this.mdsNode, vertex, defaultEdge);

            return vertex;
        } finally {
            readLock.unlock();
            writeLock.unlock();
        }
    }

    private WorldModelNodeVertex createNodeVertex(Identity identity, long versionID) {
        return new WorldModelNodeVertex(identity, versionID);
    }

    private WorldModelDataVertex createDataVertex(Measurement measurement, long versionID) {
        return new WorldModelDataVertex(measurement, versionID);
    }

    private WorldModelEdge createDefaultOpinion(WorldModelVertex opinionHolder, WorldModelVertex opinionObject, long versionID) {
        if (opinionObject == null || opinionHolder == null)
            throw new IllegalArgumentException("null is not allowed!");

        return new WorldModelEdge(new SubjectiveOpinion(0, 0, 1, 0.5), versionID,
                ConstantDataTypes.opinionType);
    }

    public class Pair<A, B> {
        public final A left;
        public final B right;

        Pair(A left, B right) {
            this.left = left;
            this.right = right;
        }
    }

    /**
     * This is the root node, i.e., the node that points to all detectors.
     */
    protected final WorldModelNodeVertex mdsNode = new WorldModelNodeVertex(MDS_UID, MDS_ROOT_VERSION_ID);

    // paths

    public abstract List<GraphPath<WorldModelVertex, WorldModelEdge>> getAllPaths(WorldModelVertex start,
                                                                                  WorldModelVertex end);

    public List<GraphPath<WorldModelVertex, WorldModelEdge>> getAllPathsFromRootToVertex(WorldModelVertex target) {
        return getAllPaths(this.mdsNode, target);
    }


    /**
     * link the data in the given message against the WM, returning a diff that represents the changes wrt the current WM
     *
     * @param msg
     * @return
     */
    public WorldModelDiff link(InputDataIPCMessage msg) {
        //retrieve data from msg
        final Measurement measurement = msg.getMeasurement();
        final Identity source = msg.getSource();
        //l.info("Linking message: " + msg);

        //initialize local vars
        long versionID;
        WorldModelDataVertex worldModelData;
        WorldModelNodeVertex sender;
        WorldModelEdge opinionData, opinionSender;
        boolean newSender = false, defaultDataOpinion = false;

        /* start of read/write section */

        //match data & sender in the WM; if they do not exist, default instances are created.
        readLock.lock();
        try {
            //retrieve *copies* of relevant data!
            versionID = this.getVersion();
            sender = this.match(source, versionID);
            worldModelData = this.match(measurement, versionID);
            opinionData = this.findExistingOpinion(sender, worldModelData, versionID);
            opinionSender = this.findExistingOpinion(this.mdsNode, sender, versionID);

            if (sender == null || worldModelData == null || opinionData == null || opinionSender == null) {
                //"upgrade" lock
                readLock.unlock();
                writeLock.lock();
            }

            versionID = this.getVersion();
            //perform all necessary writes
            if (sender == null) { //item does not exist
                if (this.match(source, versionID) == null) { //ensure item still does not exist
                    sender = createNodeVertex(source, versionID + 1);
                    newSender = true;
                } else
                    l.warn("Expected to create default sender vertex, but after locking it already exists!");
            }

            if (worldModelData == null) {
                if (this.match(measurement, versionID) == null) { //ensure item still does not exist
                    worldModelData = createDataVertex(measurement, versionID + 1);
                } else
                    l.warn("Expected to create default data vertex, but after locking it already exists!");
            }

            if (opinionData == null) {
                if (this.getGraph().getEdge(sender, worldModelData) == null) { //ensure item still does not exist
                    opinionData = createDefaultOpinion(sender, worldModelData, versionID + 1);
                    defaultDataOpinion = true;
                } else
                    l.warn("Expected to create default opinion, but after locking it already exists!");
            }

            if (opinionSender == null) {
                if (this.getGraph().getEdge(this.mdsNode, sender) == null) { //ensure item still does not exist
                    opinionSender = createDefaultOpinion(this.mdsNode, sender, versionID + 1);
                } else
                    l.warn("Expected to create default opinion, but after locking it already exists!");
            }

        } finally {
            //ensure locks are always released
            if (writeLock.isHeldByCurrentThread()) {
                //increment version if write lock was held, i.e., if changes were made
                versionID = commit();
                writeLock.unlock();
            } else {
                readLock.unlock();
            }
        }

        /* end of read/write section */
        WorldModelDiff diff = new WorldModelDiff(this, versionID);
        diff.addChange(worldModelData);
        if (sender != null && newSender) {
            diff.addChange(sender);
            diff.addChange(new WorldModelDiffEdge(this.mdsNode, sender, opinionSender, versionID));
        }
        if (defaultDataOpinion)
            diff.addChange(new WorldModelDiffEdge(sender, worldModelData, opinionData, versionID));

        return diff;
    }

    public WorldModelDiff link(InputOpinionIPCMessage msg) {
        ExternalOpinion externalOpinion = msg.getExternalOpinion();
        Identity source = externalOpinion.getOpinionHolder();
        Object subject = externalOpinion.isAboutData() ? externalOpinion.getData() : externalOpinion.getSubject();

        long versionID;
        WorldModelDiffEdge worldModelOpinion;
        WorldModelVertex sourceVertex;
        WorldModelEdge opinionSource;
        WorldModelVertex subjectVertex;
        WorldModelEdge opinionSubject;
        boolean newSource = false, newSubject = false;

        readLock.lock();
        try {
            //fetch all data once
            versionID = this.getVersion();
            worldModelOpinion = this.match(externalOpinion, versionID);
            if (source != null) {
                sourceVertex = this.match(source, versionID);
            } else {
                //unknown sourceVertex
                sourceVertex = null;
            }
            opinionSource = this.findExistingOpinion(this.mdsNode, sourceVertex, versionID);


            if (externalOpinion.isAboutData()) {
                subjectVertex = this.match((Measurement) subject, versionID);
                opinionSubject = this.findExistingOpinion(sourceVertex, subjectVertex, versionID);
            } else {
                subjectVertex = this.match((Identity) subject, versionID);
                opinionSubject = this.findExistingOpinion(this.mdsNode, subjectVertex, versionID);
            }

            if ((source != null && sourceVertex == null) || opinionSource == null || opinionSubject == null || worldModelOpinion == null || (subject != null && subjectVertex == null)) {
                //"upgrade" lock
                readLock.unlock();
                writeLock.lock();
            }

            versionID = this.getVersion();
            //perform all necessary writes
            //source write, if needed
            if (source != null && sourceVertex == null) { //item does not exist
                if (this.match(source, versionID) == null) { //ensure item still does not exist
                    sourceVertex = createNodeVertex(source, versionID + 1);
                    newSource = true;
                } else
                    l.warn("Expected to create default sourceVertex vertex, but after locking it already exists!");
            }

            if (opinionSource == null) {
                if (this.getGraph().getEdge(this.mdsNode, sourceVertex) == null) { //ensure item still does not exist
                    opinionSource = createDefaultOpinion(this.mdsNode, sourceVertex, versionID + 1);
                    assert(newSource); //this should only happen if the source is also new
                } else
                    l.warn("Expected to create default opinion, but after locking it already exists!");
            }

            if (externalOpinion.isAboutData()) {
                Measurement subjectData = (Measurement) subject;
                if (subjectData != null && subjectVertex == null) { //item does not exist
                    if (this.match(subjectData, versionID) == null) { //ensure item still does not exist
                        subjectVertex = createDataVertex(subjectData, versionID + 1);
                        newSubject = true;
                    } else
                        l.warn("Expected to create default subjectDataVertex vertex, but after locking it already exists!");
                }

                if (opinionSubject == null) {
                    if (this.getGraph().getEdge(sourceVertex, subjectVertex) == null) { //ensure item still does not exist
                        opinionSubject = createDefaultOpinion(sourceVertex, subjectVertex, versionID + 1);
                        assert(newSubject);
                    } else
                        l.warn("Expected to create default opinion, but after locking it already exists!");
                }
            } else {
                Identity subjectID = (Identity) subject;
                if (subjectID != null && subjectVertex == null) { //item does not exist
                    if (this.match(subjectID, versionID) == null) { //ensure item still does not exist
                        subjectVertex = createNodeVertex(subjectID, versionID + 1);
                        newSubject = true;
                    } else
                        l.warn("Expected to create default subjectIDVertex vertex, but after locking it already exists!");
                }

                if (opinionSubject == null) {
                    if (this.getGraph().getEdge(this.mdsNode, subjectVertex) == null) { //ensure item still does not exist
                        opinionSubject = createDefaultOpinion(this.mdsNode, subjectVertex, versionID + 1);
                        assert(newSubject);
                    } else
                        l.warn("Expected to create default opinion, but after locking it already exists!");
                }
            }

            if (worldModelOpinion == null) {
                if (this.match(externalOpinion, versionID) == null) { //ensure item still does not exist
                    //worldModelOpinion = createDefaultOpinion(sourceVertex, subjectVertex, versionID + 1);
                } else
                    l.warn("Expected to create default opinion, but after locking it already exists!");
            }
        } finally {
            //ensure locks are always released
            if (writeLock.isHeldByCurrentThread()) {
                //increment version if write lock was held, i.e., if changes were made
                versionID = commit();
                writeLock.unlock();
            } else {
                readLock.unlock();
            }
        }

        /* end of read/write section */

        WorldModelDiff diff = new WorldModelDiff(this, versionID);

        diff.addChange(worldModelOpinion);

        if (source != null && newSource) {
            diff.addChange(sourceVertex);
            diff.addChange(new WorldModelDiffEdge(this.mdsNode, sourceVertex, opinionSource, versionID));
        }

        if(newSubject) {
            diff.addChange(subjectVertex);
            if(externalOpinion.isAboutData())
                diff.addChange(new WorldModelDiffEdge(sourceVertex, subjectVertex, opinionSubject, versionID));
            else
                diff.addChange(new WorldModelDiffEdge(this.mdsNode, subjectVertex, opinionSubject, versionID));
        }

        return diff;
    }

    /**
     * Update the WM based on the given snapshot message. This includes merging the diff into the current world model,
     * as well as adding new detection results stored separately in the message.
     *
     * @param msg
     */
    public void update(NewSnapshotIPCMessage msg) {
        //l.info("Storing to WM:" + msg);
        writeLock.lock();

        merge(msg.getChange());

        try {

            for (WorldModelItem item : msg.getChange().getChanges()) {
                if (item instanceof WorldModelVertex)
                    l.trace(((WorldModelVertex) item).UID + " -> " + this.isTrustworthy(this.findVertex(((WorldModelVertex) item).UID), msg.getChange().getVersion()));
                else
                    l.trace(item);
            }
        } catch (WorldModelException e) {
            l.error("Could not find paths from MDS to all vertices after merging -- there is a bug in the merging or update code.");
            l.debug("Detailed information: ", e);
        }

        List<WorldModelEdge> results = new ArrayList<>();

        // commit new internally generated opinions
        for (IPCMessage m : msg.getDetectionResults()) {
            DetectionResultSetIPCMessage resultSet = (DetectionResultSetIPCMessage) m;

            for (IPCMessage item : resultSet.resultSet) {
                DetectionResultIPCMessage individualResult = (DetectionResultIPCMessage) item;
                if (individualResult != null) {
                    try {
                        results.add(this.update(individualResult));
                    } catch (WorldModelException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // empty result
                }
            }
        }

        if (!msg.getDetectionResults().isEmpty()) {
            this.commit();
        }

        writeLock.unlock();
        this.notifyChangeListeners();
    }

    @Override
    public Map<String, Opinion> getDetectionResults(WorldModelDataVertex data, long versionID) throws WorldModelException {

        Set<WorldModelEdge> incomingEdges = this.getGraph().incomingEdgesOf(data);

        Map<String, Opinion> res = new HashMap<>(incomingEdges.size());

        for (WorldModelEdge edge : incomingEdges) {
            String srcUID = this.getGraph().getEdgeSource(edge).UID;

            DataContainer dataContainer = edge.getNewestFromHistory(versionID);
            if (dataContainer == null) {
                continue; // no opinion for current detector
            }

            Opinion opinion = ((OpinionContainer) dataContainer.get(WorldModelDataTypeEnum.OPINION)).getOpinion();
            res.put(srcUID, opinion);
        }

        return res;
    }

    @Override
    public Map<String, Opinion> getDetectionResults(WorldModelNodeVertex data, long versionID) throws WorldModelException {

        Set<WorldModelEdge> incomingEdges = this.getGraph().incomingEdgesOf(data);

        Map<String, Opinion> res = new HashMap<>(incomingEdges.size());

        for (WorldModelEdge edge : incomingEdges) {
            String srcUID = this.getGraph().getEdgeSource(edge).UID;
            Opinion opinion = ((OpinionContainer) edge.getNewestFromHistory(versionID).get(WorldModelDataTypeEnum.OPINION)).getOpinion();
            res.put(srcUID, opinion);
        }

        return res;
    }

}
