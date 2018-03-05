package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.controller.DetectorManager;
import de.uulm.vs.autodetect.mds.framework.controller.DetectorWrapper;
import de.uulm.vs.autodetect.mds.framework.controller.SnapshotManager;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.Detector;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.DetectorIndex;
import de.uulm.vs.autodetect.mds.framework.model.AcyclicGraphWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.GraphWorldModel;
import de.uulm.vs.autodetect.posverif.PositionJSONParser;
import de.uulm.vs.autodetect.posverif.SpeedJSONParser;
import de.uulm.vs.autodetect.posverif.TransmissionTimeJSONParser;
import de.uulm.vs.autodetect.veins.BaseJSONParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for initiating all (internal) components of the
 * framework.
 *
 * @author Rens van der Heijden
 */
public class Maat implements Runnable {
    private static final Logger l = LogManager.getLogger(Maat.class);

    /**
     * Timeout (ms) for which SnapshotManager can be idle -- after this timeout expires, it initiates the termination
     */
    public static final long TIMEOUT = 100;
    public static final AtomicInteger ERROR_COUNTER = new AtomicInteger(0);
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    private static final ArrayList<WorldModelChangeListener> apps = new ArrayList<>(2);
    private static GraphWorldModel worldModel = new AcyclicGraphWorldModel();

    private static long fileNumber = -1;

    /**
     * Launch the framework.
     */
    public static void main(String[] args) throws IOException {
        l.info("starting Maat");

        // instantiate processing components
        DetectorManager detectorManager = new DetectorManager();
        SnapshotManager snapshotManager = new SnapshotManager(worldModel, detectorManager);
        detectorManager.setOutput(snapshotManager);

        //TODO use correct java.io stuff to make sure this works cross platform
        LoadDetectors.loadDetectors(new File("./detectors.xml"));

        // instantiate DetectorWrappers
        List<DetectorWrapper> detectors = new ArrayList<>();
        for (AbstractDetectorFactory detectorClass : DetectorIndex.INSTANCE.getAllFactories()) {
            DetectorWrapper wrapper = new DetectorWrapper(detectorManager, detectorClass, executor);
            detectors.add(wrapper);
        }

        detectorManager.addDetectors(detectors);

        // start IO threads
        // input:
        InputConverter ic = new InputConverter(snapshotManager);

        List<Runnable> parsers = new ArrayList<>(args.length);

        // input from files
        // TODO dynamic selection of parsers for different files...
        String splitRegex = File.separator;
        if (splitRegex.equals("\\")) {
            splitRegex = "\\\\";
        }
        for (String arg : args) {
            String[] path = arg.split(splitRegex);
            String name = path[path.length - 1].toLowerCase();
            if (fileNumber < 0) {
                int startIndex = name.indexOf('-') + 1;
                int endIndex = name.indexOf('-', startIndex);
                if (endIndex < 0) {
                    endIndex = name.lastIndexOf('.');
                }
                fileNumber = Long.parseLong(name.substring(startIndex, endIndex));
            }
            BaseJSONParser parser = new BaseJSONParser(ic, arg, Arrays.asList(
                    new PositionJSONParser(),
                    new TransmissionTimeJSONParser(),
                    new SpeedJSONParser()
                    //TODO add parsers here as needed
            ));
            parsers.add(parser);
        }

        // executor is responsible for all jobs that should be resubmitted
        //executor.schedule(snapshotManager, 0, TimeUnit.MILLISECONDS);
        //executor.schedule(detectorManager, 0, TimeUnit.MILLISECONDS);
        //for (DetectorWrapper detectorWrapper : detectors) {
        //    executor.schedule(detectorWrapper, 0, TimeUnit.MILLISECONDS);
        //}

        List<ScheduledFuture> futures = new ArrayList<>(parsers.size());
        for (Runnable fdp : parsers) {
            futures.add(executor.schedule(fdp, 0, TimeUnit.MILLISECONDS));
        }

        //TODO make async
        for(ScheduledFuture f : futures)
            try {
                f.get();
            } catch (ExecutionException e) {
                l.warn("A parser failed: ", e);
                System.exit(1);
            } catch (InterruptedException e) {
                // interruptions can be ignored here
            }


        // limit execution time to 120 seconds
        //Executors.newScheduledThreadPool(1).schedule(new Maat(), 300, TimeUnit.SECONDS);
    }

    public static void startTermination(){
        l.info("DetectorWrapper.consume calls: " + DetectorWrapper.c.get());
        l.info("Processing complete: terminating Maat.");
        TerminationManager terminationManager = new TerminationManager(worldModel);
        executor.schedule(terminationManager, 1, TimeUnit.SECONDS);
    }

    public static void terminate() {
        try {
            OutputApplication outputApplication = new OutputApplication(worldModel, "results-" + fileNumber + ".json");
            outputApplication.writeResults();
            outputApplication.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*VarianceOutputApplication varianceOutputApplication = new VarianceOutputApplication("variance_distance_eART_" + fileNumber + ".txt");
        varianceOutputApplication.writeValues((ArrayList<Double>) eART.getDistances());
        varianceOutputApplication.close();

        varianceOutputApplication.open("variance_distance_eSAW_" + fileNumber + ".txt");
        varianceOutputApplication.writeValues((ArrayList<Double>) eSAW.getDistances());
        varianceOutputApplication.close();

        varianceOutputApplication.open("variance_velocity_eMGT_" + fileNumber + ".txt");
        varianceOutputApplication.writeValues((ArrayList<Double>) eMGT.getVelocities());
        varianceOutputApplication.close();

        varianceOutputApplication.open("variance_velocity_eMDM_" + fileNumber + ".txt");
        varianceOutputApplication.writeValues((ArrayList<Double>) eMDM.getVelocities());
        varianceOutputApplication.close();*/


        l.info("terminating Maat");

        for (WorldModelChangeListener app : apps) {
            worldModel.removeChangeListener(app);
        }

        // to terminate our framework:
        // modified from
        // http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (@SuppressWarnings("unused") InterruptedException e) {
            /* do nothing */
        } finally {
            if (!executor.isTerminated()) {
                l.error("Some tasks did not terminate in time...");
            }
            executor.shutdownNow();
            l.info("shutdown finished");
        }
        // end code excerpt
    }

    @Override
    public void run() {
        terminate();
        System.exit(ERROR_COUNTER.get());
    }
}
