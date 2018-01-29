package de.uulm.vs.autodetect.mds.framework.controller.detectors;

import de.uulm.vs.autodetect.mds.framework.controller.ComputeGraphNode;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDiff;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelItem;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sub-Types of this factory are used to create instances of their
 * respective detectors (see
 * {@link #getNewInstance(ComputeGraphNode, WorldModelDiff)}).
 * The following provides a simple example: <code>
 * static enum ExampleFactory implements Detector.AbstractDetectorFactory {
 * INSTANCE;
 * <p>
 * private ExampleFactory(){
 * }
 * <p>
 * &#64;Override
 * public Detector getInstance(ComputeGraphNode output, WorldModelDiff reference) {
 * return new ExampleDetector(output, reference, pool);
 * }
 * <p>
 * }
 * </code>
 * <p>
 * Also note that Detectors should register their DetectorFactory at
 * {@link DetectorIndex} like this:
 * <code>DetectorIndex.INSTANCE.registerDetector(ExampleDetector.class.getCanonicalName(), ExampleFactory.INSTANCE);</code>
 * The idea is that this allows us to set unique identifiers for detectors
 * on the one hand, and have a central index of detectors to iterate over on
 * the other. The framework will create detector instances using
 * {@link #getNewInstance(ComputeGraphNode, WorldModelDiff)}
 * if all other instances of the Detector return false. For detectors that
 * have exactly one instance, this means the factory is essentially only
 * used once.
 *
 * @author Rens van der Heijden
 */
public abstract class AbstractDetectorFactory {

    /**
     * Create an instance of the associated Detector type.
     *
     * @param output
     * @param reference
     * @return
     * @throws WorldModelException
     */
    public abstract Detector getNewInstance(ComputeGraphNode output, WorldModelDiff reference)
            throws WorldModelException;

    public abstract String getName();

    public abstract Set<WorldModelDataTypeEnum> getDataTypes();

    /**
     * Returns whether this type of detector is capable of processing the given diff in general.
     * This is distinct from Detector.canProcess, which determines whether the concrete instance can produce meaningful output.
     * @param diff
     * @return
     */
    public boolean canProcess(WorldModelDiff diff) {
        for (WorldModelItem i : diff.getChanges()) {
            if (i instanceof WorldModelDataVertex) {
                WorldModelDataVertex vertex = ((WorldModelDataVertex) i);
                if (vertex.hasTypes(this.getDataTypes()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Map of detector parameters
     */
    protected Map<String, Object> pars = new HashMap<>();

    /*
     * Note: this method is called through reflection.
     */
    public void storeAttributes(Map<String, Object> parameterSet) throws IllegalAccessException {
        pars.putAll(parameterSet);
    }

    /**
     * Parameter description (can be overridden by Subclasses to produce a more accurate description)
     * @return
     */
    public String getParDescription() {
        return pars.toString();
    }
}
