package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.model.ListenableGraphWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.ReadOnlyWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelException;
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfArrivalContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import de.uulm.vs.autodetect.posverif.ART;
import de.uulm.vs.autodetect.posverif.MGT;
import de.uulm.vs.autodetect.posverif.eMGT;
import no.uio.subjective_logic.opinion.Opinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * This class implements an application that periodically checks for alarms
 * occurring in the world model.
 *
 * @author Rens van der Heijden
 */
public class NotifiedOutputApplication implements WorldModelChangeListener {
    private final Logger l = LogManager.getLogger(Maat.class);

    private final ReadOnlyWorldModel worldModel;
    private long versionID;
    private static final double DETECTION_THRESHOLD = 0.5;
    //private PrintWriter writer;

    public NotifiedOutputApplication(ReadOnlyWorldModel worldModel) {
        this.worldModel = worldModel;
        this.versionID = worldModel.getVersion();
        this.worldModel.registerChangeListener(this);
        /*writer = null;
        try {
            writer = new PrintWriter("results.txt", "UTF-8");
        } catch (IOException e) {
            l.error("IOException in application...", e);
        }*/
    }

    boolean first = true;

    @Override
    public void update(ListenableGraphWorldModel wm) {
        if (first) {
            first = false;
            System.out.println("Time,SRC,Fusion,ART,MGT");
            //writer.println("Time,SRC,Fusion,ART,MGT");
        }

        //TODO execute separately?
        try {
            this.versionID = this.worldModel.getVersion();
            // for all data vertices, if they are trustworthy, print a warning if it
            // crosses the detection threshold
            //writer.println(this.versionID);
            this.worldModel.dataVertexStream(this.versionID)
                    //.filter(v -> this.worldModel.isTrustworthy(v, this.versionID).getExpectation() <= DETECTION_THRESHOLD)

                    //only evaluate remote data.
                    .filter(s -> !((WorldModelDataVertex) s).referent.equals(WorldModelDataVertex.LOCAL_REFERENT))

                    //note: format here is <who>,<when>,<what>,<trust>
                    .forEach(v -> {
                        String outputLine = "";
                        outputLine += ((TimeOfArrivalContainer) v.getLatest().get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL)).getTimeOfArrival();
                        outputLine += "," + ((WorldModelDataVertex) v).referent;

                        l.info("Fusion," + v.getVersionID() + "," + v.UID + "," + this.worldModel.isTrustworthy(v, this.versionID));

                        Opinion o = this.worldModel.isTrustworthy(v, this.versionID);
                        outputLine += "," + o.getExpectation();

                        Map<String, Opinion> detectionResults = this.worldModel.getDetectionResults(v, this.versionID);
                        for (String detector : detectionResults.keySet()) {
                            l.info(detector + "," + v.getVersionID() + "," + v.UID + "," + detectionResults.get(detector));
                        }

                        outputLine += "," + ((detectionResults.containsKey(ART.class.getCanonicalName())) ? detectionResults.get(ART.class.getCanonicalName()).getExpectation() : "");
                        outputLine += "," + ((detectionResults.containsKey(MGT.class.getCanonicalName())) ? detectionResults.get(MGT.class.getCanonicalName()).getExpectation() : "");
                        outputLine += "," + ((detectionResults.containsKey(eMGT.class.getCanonicalName())) ? detectionResults.get(eMGT.class.getCanonicalName()).getExpectation() : "");
                        System.out.println(outputLine);
                        //writer.println(outputLine);
                        // TODO call flush() only once (before termination)
                        //writer.flush();
                    });
        } catch (WorldModelException e) {
            l.error("WorldModelException in application...", e);
        } catch (Exception e) {
            Maat.ERROR_COUNTER.incrementAndGet();
            this.l.error("An unknown error was encountered", e);
        }

    }
}
