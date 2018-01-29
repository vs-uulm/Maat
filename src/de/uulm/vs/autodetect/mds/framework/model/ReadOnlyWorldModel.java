package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import no.uio.subjective_logic.opinion.Opinion;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This interface describes methods for read-only access to the world model.
 *
 * @author Rens van der Heijden
 */
public interface ReadOnlyWorldModel extends ListenableGraphWorldModel {

    /**
     * Find the {@link WorldModelDetectorVertex} related to the given detector type.
     *
     * @param type a {@link Class} that represents a subtype of {@link AbstractDetectorFactory}, representing
     *             Detector with corresponding parameters.
     * @return The unique Vertex representing the Detector in the framework;
     * null if the given class does not exist.
     */
    public WorldModelVertex findDetectorVertex(AbstractDetectorFactory type);

    /**
     * Fetches an entry from the world model given a unique identifier. Unique
     * identifiers are typically defined where data enters the world model (for
     * {@link WorldModelDataVertex}), by a certificate or similar identifier
     * (for {@link WorldModelNodeVertex}), or by the full class name of a Java
     * class (for {@link WorldModelDetectorVertex}).
     *
     * @param UID a String identifier that uniquely specifies the desired
     *            {@link WorldModelVertex}.
     * @return
     */
    public WorldModelVertex findVertex(String UID);

    /**
     * equivalent to {@link #isTrustworthy(WorldModelDataVertex, long)} on the
     * result of {@link #findDetectorVertex(AbstractDetectorFactory)}, but (possibly) more
     * efficient, if the underlying implementation provides an improved
     * implementation.
     *
     * @param UID       UID of the object of which the trustworthiness should be
     *                  checked.
     * @param versionID the logical timestamp.
     * @return
     */
    public default Opinion isTrustworthy(String UID, long versionID) throws WorldModelException {
        WorldModelVertex v = this.findVertex(UID);
        if (v instanceof WorldModelNodeVertex)
            return this.isTrustworthy((WorldModelNodeVertex) v, versionID);
        else if (v instanceof WorldModelDataVertex)
            return this.isTrustworthy((WorldModelDataVertex) v, versionID);
        else
            return null;
    }

    /**
     * Checks the trustworthiness of a given node, if applicable; returns null,
     * otherwise.
     *
     * @param vertex
     * @param versionID
     * @return
     */
    public default Opinion isTrustworthy(WorldModelVertex vertex, long versionID) throws WorldModelException {
        if (vertex instanceof WorldModelNodeVertex)
            return this.isTrustworthy((WorldModelNodeVertex) vertex, versionID);
        else if (vertex instanceof WorldModelDataVertex)
            return this.isTrustworthy((WorldModelDataVertex) vertex, versionID);
        else
            return null;
    }

    /**
     * Obtain all detection results related to a particular item. Intended mostly for output or post-processing of
     * detection results, as well as applications that are capable of dealing with uncertainty directly.
     *
     * @param vertex
     * @param versionID
     * @return
     */
    public default Map<String, Opinion> getDetectionResults(WorldModelVertex vertex, long versionID) throws WorldModelException {
        if (vertex instanceof WorldModelNodeVertex)
            return this.getDetectionResults((WorldModelNodeVertex) vertex, versionID);
        else if (vertex instanceof WorldModelDataVertex)
            return this.getDetectionResults((WorldModelDataVertex) vertex, versionID);
        else
            return null;
    }

    /**
     * Determine the trustworthiness of the given data vertex based on
     * information available at the current logical timestamp, specified by
     * versionID.
     *
     * @param data      a Vertex that references a particular node in the framework
     *                  (see {@link #findVertex(String)}).
     * @param versionID the logical timestamp.
     * @return
     */
    public Opinion isTrustworthy(WorldModelDataVertex data, long versionID) throws WorldModelException;

    /**
     * Determine the trustworthiness of the given entity, based on information
     * available at the current logical timestamp, specified by versionID.
     *
     * @param data      a Vertex that references a particular node in the framework
     *                  (see {@link #findVertex(String)}).
     * @param versionID the logical timestamp.
     * @return
     */
    public Opinion isTrustworthy(WorldModelNodeVertex data, long versionID) throws WorldModelException;

    /**
     * Given a type of data (e.g., position information), retrieve all the
     * vertices that contain this type of data. This can be used to find related
     * information.
     *
     * @param type The type of data to be retrieved.
     * @return
     */
    public Set<WorldModelDataVertex> getVerticesByType(Set<WorldModelDataTypeEnum> type);

    public Stream<WorldModelVertex> dataVertexStream(long versionID);

    /**
     * Returns a <b>copy</b> of the set of vertices in this WM.
     */
    public Set<WorldModelVertex> getVertices();

    /**
     * Returns a version identifier for this world model
     */
    public long getVersion();

    /**
     *
     */
    public Map<String, Opinion> getDetectionResults(WorldModelDataVertex data, long versionID) throws WorldModelException;

    /**
     *
     */
    public Map<String, Opinion> getDetectionResults(WorldModelNodeVertex data, long versionID) throws WorldModelException;

    /**
     * Query data by the "subject", i.e., the thing the data refers to (information about local vehicle, another vehicle, etc)
     *
     * @see WorldModelDataVertex#referent
     */
    public default Stream<WorldModelVertex> dataByReferent(String referent, long versionID) {
        return this.dataVertexStream(versionID).filter(s -> ((WorldModelDataVertex) s).referent.equals(referent));
    }
}