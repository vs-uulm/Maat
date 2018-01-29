package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.OpinionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import no.uio.subjective_logic.opinion.Opinion;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.List;


public class AcyclicGraphWorldModel extends GraphWorldModel {
    /**
     * The actual world model, i.e., the container of relevant information
     */
    protected DirectedAcyclicGraph<WorldModelVertex, WorldModelEdge> worldModel = null;

    /**
     * Constructs a new graph world model, as a directed acyclic graph, and
     * initializes it with the MDS root node.
     */
    public AcyclicGraphWorldModel() {
        this.worldModel = new DirectedAcyclicGraph<>(WorldModelEdge.class);
        this.worldModel.addVertex(this.mdsNode);
    }

    //////////////////////////////////////////////////////////////////////////
    //                ReadOnlyWorldModel implementation                     //
    //////////////////////////////////////////////////////////////////////////


    // Path search

    /**
     * @param start
     * @param end
     * @return a list of all paths from start to end.
     */
    @Override
    public List<GraphPath<WorldModelVertex, WorldModelEdge>> getAllPaths(WorldModelVertex start, WorldModelVertex end) {
        readLock.lock();
        try {
            // our graph is free of circles
            // => we can use those parameters;
            // (Background: we are sure that our graph only
            // has "simple" (non-self-intersecting) paths,
            // so we can safely set "simplePathsOnly" to "true"
            // without missing any paths.
            // that allows "maxPathLength" to be set to
            // "infinite"; we know it will terminate.)
            return (new AllDirectedPaths<>(this.worldModel)).getAllPaths(start, end, true, null);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Opinion isTrustworthy(WorldModelDataVertex data, long referenceVersionID) throws WorldModelException {
        readLock.lock();
        try {
            Opinion result = new SubjectiveOpinion();

            List<GraphPath<WorldModelVertex, WorldModelEdge>> paths = this.getAllPaths(this.mdsNode, data);

            List<Opinion> pathResults = new ArrayList<>(paths.size());

            for (GraphPath<WorldModelVertex, WorldModelEdge> path : paths) {
                SubjectiveOpinion intermediate = null;
                boolean skip = false;
                for (WorldModelEdge edge : path.getEdgeList()) {
                    if (!edge.existsAt(referenceVersionID)) {
                        skip = true;
                        break;
                    }
                    DataContainer dc = edge.getNewestFromHistory(referenceVersionID);
                    if (intermediate == null && dc != null)
                        intermediate = ((OpinionContainer) dc.get(WorldModelDataTypeEnum.OPINION)).getOpinion();
                    else if (intermediate != null && dc != null)
                        intermediate = intermediate.discount(((OpinionContainer) dc.get(WorldModelDataTypeEnum.OPINION)).getOpinion());
                    else
                        l.error("Warning, null opinion encountered!");
                }
                if (!skip)
                    pathResults.add(intermediate);
            }

            if (!pathResults.isEmpty())
                result = SubjectiveOpinion.cumulativeFuse(pathResults);
            else
                throw new WorldModelException("Error: no paths to " + data.UID + " exist!");

            // TODO find equations and process

            return result;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Opinion isTrustworthy(WorldModelNodeVertex entity, long referenceVersionID) throws WorldModelException {
        readLock.lock();
        try {
            Opinion result = new SubjectiveOpinion();

            List<GraphPath<WorldModelVertex, WorldModelEdge>> paths = this.getAllPaths(this.mdsNode, entity);

            List<Opinion> pathResults = new ArrayList<>(paths.size());

            for (GraphPath<WorldModelVertex, WorldModelEdge> path : paths) {
                SubjectiveOpinion intermediate = null;
                boolean skip = false;
                for (WorldModelEdge edge : path.getEdgeList()) {
                    if (!edge.existsAt(referenceVersionID)) {
                        skip = true;
                        break;
                    }
                    DataContainer dc = edge.getNewestFromHistory(referenceVersionID);
                    if (intermediate == null && dc != null)
                        intermediate = ((OpinionContainer) dc.get(WorldModelDataTypeEnum.OPINION)).getOpinion();
                    else if (intermediate != null && dc != null)
                        //TODO check whether this produces the correct results; it does use SubjectiveOpinion.transitivity internally.
                        intermediate = intermediate.discount(((OpinionContainer) dc.get(WorldModelDataTypeEnum.OPINION)).getOpinion());
                    else
                        l.error("Warning, null opinion encountered!");
                }
                if (!skip)
                    pathResults.add(intermediate);
            }

            if (!pathResults.isEmpty())
                result = SubjectiveOpinion.cumulativeFuse(pathResults);
            else
                throw new WorldModelException("Error: no paths to " + entity.UID + " exist!");

            return result;
        } finally {
            readLock.unlock();
        }
    }

    // Updates
    @Override
    protected void merge(WorldModelDiff diff) {
        writeLock.lock();
        readLock.lock();
        try {
            diff.getVertexChanges().forEach(wmi -> {
                WorldModelVertex v = (WorldModelVertex) wmi;
                WorldModelVertex inWM = this.findVertex(v.UID);
                if (inWM != null) {
                    //modify the existing vertex to contain both data sets
                    inWM.merge(v);
                } else {
                    //TODO add a copy instead...?
                    // -> avoids bugs caused by detectors keeping a reference to this vertex
                    this.worldModel.addVertex(v);
                }
            });

            diff.getEdgeChanges().forEach(wmi -> {
                WorldModelDiffEdge edge = (WorldModelDiffEdge) wmi;
                WorldModelEdge inWM = this.worldModel.getEdge(edge.getSource(), edge.getDestination());
                if (inWM != null) {
                    inWM.update(edge.getEdge());
                } else {
                    WorldModelVertex src = this.findVertex(edge.getSource().UID);
                    WorldModelVertex dst = this.findVertex(edge.getDestination().UID);
                    //TODO should we maintain a version in WMDE instead? This would make it more consistent.
                    try {
                        this.worldModel.addDagEdge(src, dst, new WorldModelEdge(edge.getEdge(), edge.getVersionID()));
                    } catch (DirectedAcyclicGraph.CycleFoundException e) {
                        if (!cycleHook(diff, edge)) {
                            throw new IllegalArgumentException(e);
                        } else {
                            this.worldModel.addEdge(src, dst, new WorldModelEdge(edge.getEdge(), edge.getVersionID()));
                        }

                    }
                }
            });
        } finally {
            readLock.unlock();
            writeLock.unlock();
        }
    }

    /**
     * This hook is called when a cycle is encountered; it should return true iff the cycle was cleanly resolved.
     * Adding this edge will then be attempted again.
     * <p>
     * The current implementation always returns false.
     *
     * @param diff the diff currently being merged
     * @param edge the edge that caused the cycle
     * @return
     */
    private boolean cycleHook(WorldModelDiff diff, WorldModelDiffEdge edge) {
        return false;
    }

    @Override
    protected DirectedGraph<WorldModelVertex, WorldModelEdge> getGraph() {
        return this.worldModel;
    }
}