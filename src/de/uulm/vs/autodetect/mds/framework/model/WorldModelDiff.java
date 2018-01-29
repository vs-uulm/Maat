package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import no.uio.subjective_logic.opinion.Opinion;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


/**
 * A class that represents the differences between one version
 * of a {@link GraphWorldModel} and a newer version.
 * <br><br>
 * The way this class works is as follows:
 * <br><br>
 * It basically contains two components:
 * <li>a reference to a (ReadOnly-Version) of the WorldModel upon which this WorldModelDiff is based</li>
 * <li>a list containing {@link WorldModelItem}s that are to replace
 * their counterparts inside the referenced WorldModel</li>
 * <br><br>
 * Thus, a diff is applied by replacing the respective WorldModelItems in the
 * referenced WorldModel by their counterparts found inside the List of this
 * WorldModelDiff.
 * <br><br>
 * Note that at the moment, deletions (of Nodes/Vertices) are not supported,
 * since we don't think we will need them. Deletion can still be retrofitted;
 * for example by using dedicated classses like "DeletedWorldModelItem".
 *
 * @author Christopher Keazor
 */

public class WorldModelDiff implements ReadOnlyWorldModel {
    /**
     * The versionID of the Object that this WorldModelDiff is based on
     */
    private final long versionID;

    /**
     * the read-only version of the WorldModel on which this WorldModelDiff is based on
     */
    private final ReadOnlyWorldModel base;

    private final Set<WorldModelItem> changedItems; // TODO: for many of the searches (e.g. findVertex(..) a set is not a good container.
    // => if it is too slow we might change this or we might use additional helpers

    public WorldModelDiff(ReadOnlyWorldModel base, long versionID) {
        this.base = base;
        this.changedItems = new HashSet<>();
        this.versionID = versionID;
    }


    public boolean addChange(WorldModelVertex change) {
        if (change == null)
            return false;
        return this.changedItems.add(change);
    }

    public boolean addChange(WorldModelDiffEdge change) {
        if (change == null)
            return false;
        return this.changedItems.add(change);
    }

    // *****************************************
    // 		          GETTERS
    // *****************************************

    public Set<WorldModelItem> getChanges() {
        return this.changedItems; // maybe the returned items should be read-only?
    }

    // *****************************************
    // 		   INTERFACE IMPLEMENTATION
    // *****************************************
    @Override
    public WorldModelVertex findDetectorVertex(AbstractDetectorFactory type) {
        for (WorldModelItem v : this.changedItems) {
            if (v instanceof WorldModelDetectorVertex && ((WorldModelDetectorVertex) v).detectorType.equals(type))
                return (WorldModelVertex) v;
        } // else:
        return this.base.findDetectorVertex(type);
    }

    @Override
    public WorldModelVertex findVertex(String UID) {
        for (WorldModelItem v : this.changedItems) {
            if ((v instanceof WorldModelDataVertex) && ((WorldModelDataVertex) v).UID.equals(UID))
                return (WorldModelVertex) v;
            else if ((v instanceof WorldModelNodeVertex) && ((WorldModelNodeVertex) v).UID.equals(UID))
                return (WorldModelVertex) v;
        } // else:
        return this.base.findVertex(UID);
    }

    // TODO: if requested items are already part of this WorldModelDiff (can be found inside "changedItems")
    //       return them instead of the ones from base.
    @Override
    public Opinion isTrustworthy(WorldModelDataVertex data, long referenceVersionID) throws WorldModelException {
        return this.base.isTrustworthy(data, referenceVersionID);
    }

    // TODO: if requested items are already part of this WorldModelDiff (can be found inside "changedItems")
    //       return them instead of the ones from base.
    @Override
    public Opinion isTrustworthy(WorldModelNodeVertex entity, long referenceVersionID) throws WorldModelException {
        return this.base.isTrustworthy(entity, referenceVersionID);
    }

    @Override
    public Set<WorldModelDataVertex> getVerticesByType(Set<WorldModelDataTypeEnum> type) {
        //TODO diff elements...
        return this.base.getVerticesByType(type);
    }

    @Override
    public Set<WorldModelVertex> getVertices() {
        //note: LinkedHashSet is used because its' behavior should be similar to the LinkedHashMap that underlies the graph implementations we use (see org.jgrapht.jgraph.AbstractBaseGraph).
        Set<WorldModelVertex> items = base.getVertices();
        for (WorldModelItem wmi : changedItems) {
            if (wmi instanceof WorldModelVertex)
                items.add((WorldModelVertex) wmi);
        }

        return items;
    }

    @Override
    public Stream<WorldModelVertex> dataVertexStream(long version) {
        return getVertices().stream().filter(s -> s instanceof WorldModelDataVertex).filter(v -> v.versionID <= version);
    }

    public Stream<WorldModelItem> getVertexChanges() {
        return this.changedItems.stream().filter(s -> s instanceof WorldModelVertex);
    }

    public Stream<WorldModelItem> getEdgeChanges() {
        return this.changedItems.stream().filter(s -> s instanceof WorldModelDiffEdge);
    }

    @Override
    public long getVersion() {
        return this.versionID;
    }

    @Override
    public Map<String, Opinion> getDetectionResults(WorldModelDataVertex data, long versionID) throws WorldModelException {
        throw new WorldModelException("Invalid operation 'getDetectionResults' on WorldModelDiff!");
    }

    @Override
    public Map<String, Opinion> getDetectionResults(WorldModelNodeVertex node, long versionID) throws WorldModelException {
        throw new WorldModelException("Invalid operation 'getDetectionResults' on WorldModelDiff!");
    }

    // *****************************************
    // 		          HELPERS
    // *****************************************
}
