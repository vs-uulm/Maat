package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.model.ReadOnlyWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class implements an application that periodically checks for alarms
 * occurring in the world model.
 *
 * @author Rens van der Heijden
 */
public class PeriodicAlarmApplication implements Runnable {
    private final Logger l = LogManager.getLogger(Maat.class);

    private final ReadOnlyWorldModel worldModel;
    private long versionID;
    private static final double DETECTION_THRESHOLD = 0.5;

    public PeriodicAlarmApplication(ReadOnlyWorldModel worldModel) {
        this.worldModel = worldModel;
        this.versionID = worldModel.getVersion();
    }

    @Override
    public void run() {
        try {
            // for all data vertices, if they are trustworthy, print a warning if it
            // crosses the detection threshold
            this.worldModel.dataVertexStream(this.versionID)
                    .filter(v -> this.worldModel.isTrustworthy(v, this.versionID).getExpectation() <= DETECTION_THRESHOLD)
                    .forEach(v -> this.l.info("Data " + v + " has crossed the detection threshold in version " + this.versionID + " and appears to be untrustworthy."));
            this.versionID = this.worldModel.getVersion();
        } catch (WorldModelException e) {
            l.trace("WorldModelException in application...", e);
        } catch (Exception e) {
            Maat.ERROR_COUNTER.incrementAndGet();
            this.l.error("An unknown error was encountered", e);
        }
    }
}
