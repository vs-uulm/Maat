package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.model.GraphWorldModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Florian Diemer on 19.04.2017.
 */
public class TerminationManager implements Runnable {
    private final Logger l = LogManager.getLogger(getClass());
    private GraphWorldModel worldModel;
    private long worldModelVersion;
    private long counter;
    private final long MAX_COUNTER = 0;

    TerminationManager(GraphWorldModel worldModel) {
        this.worldModel = worldModel;
        worldModelVersion = worldModel.getVersion();
        counter = 0;
    }

    @Override
    public void run() {
        if (worldModelVersion != worldModel.getVersion()) {
            worldModelVersion = worldModel.getVersion();
            counter = 0;
        } else {
            counter++;
            l.info("c: " + counter);
        }

        if (counter > MAX_COUNTER) {
            new Thread(new Maat()).start();
        }
    }
}
