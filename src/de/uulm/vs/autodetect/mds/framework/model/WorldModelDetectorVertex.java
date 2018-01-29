package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

/**
 * A DetectorVertex represents a detector.
 *
 * @author Rens van der Heijden
 * @author Christopher Keazor
 */
public class WorldModelDetectorVertex extends WorldModelVertex {

    public final AbstractDetectorFactory detectorType;

    /**
     * Creates a Vertex representing the given type of detector. There should be
     * <b>exactly one</b> WorldModelDetectorVertex for each type. However, this
     * is not guaranteed by this constructor: the caller is responsible for
     * this!
     *
     * @param type the type of the Detector.
     */
    public WorldModelDetectorVertex(AbstractDetectorFactory type) {
        super(type.getName() + type.getParDescription());
        this.detectorType = type;
    }

    /**
     * Detectors do not currently have a history. In the future, this maybe be
     * used to have parameter sets that change over time.
     */
    @Override
    public int historySize() {
        return 0;
    }

    /**
     * Detectors do not currently have a history, and currently can't be updated.
     * This may change in the future.
     */
    @Override
    public void update(WorldModelDataTypeEnum type, MaatContainer update, long snapshot, String messageID) {
        return;
    }

}
