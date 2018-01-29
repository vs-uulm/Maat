package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.OpinionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import no.uio.subjective_logic.opinion.Opinion;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * a AcyclicGraphWorldModel that allows cycles.
 * <p>
 * By maat-design, such cycles can only occur between WorldModelNodeVertexes
 * (e.g. car A has an opinion about car B and vice versa).
 *
 * @author Christopher Keazor
 * <p>
 * TODO: what shall happen in case of cycles?
 */
public class CyclicGraphWorldModel extends GraphWorldModel {

    /**
     * The actual world model, i.e., the container of relevant information
     */
    protected DirectedGraph<WorldModelVertex, WorldModelEdge> worldModel = null;

    public CyclicGraphWorldModel() {
        this.worldModel = new SimpleDirectedGraph<>(WorldModelEdge.class);
        this.worldModel.addVertex(this.mdsNode);
    }

    /**
     * @param start
     * @param end
     * @return a list of all simple (non-self-intersecting) paths from start to end
     */
    @Override
    public List<GraphPath<WorldModelVertex, WorldModelEdge>> getAllPaths(WorldModelVertex start, WorldModelVertex end) {
        readLock.lock();
        try {
            // we only want (and need) "simple" (non-self-intersecting) paths;
            // A positive side effect of that is that it also allows
            // us to use an unrestricted maxPathLength. :)
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

    @Override
    protected void merge(WorldModelDiff diff) {
        writeLock.lock();
        readLock.lock();
        try {
            diff.getVertexChanges().forEach(wmi -> {
                WorldModelVertex v = (WorldModelVertex) wmi;
                WorldModelVertex inWM = this.findVertex(v.UID);
                if (inWM != null) {
                    inWM.merge(v);
                } else {
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
                    this.worldModel.addEdge(src, dst, new WorldModelEdge(edge.getEdge(), edge.getVersionID()));
                }
            });
        } finally {
            readLock.unlock();
            writeLock.unlock();
        }
    }

    @Override
    protected DirectedGraph<WorldModelVertex, WorldModelEdge> getGraph() {
        return this.worldModel;
    }

}
