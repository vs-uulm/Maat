package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.model.ReadOnlyWorldModel;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.WorldModelVertex;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfArrivalContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;
import no.uio.subjective_logic.opinion.Opinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

/**
 * Created by Florian Diemer on 23.03.2017.
 */
public class OutputApplication {
    private final Logger l = LogManager.getLogger(Maat.class);
    private final ReadOnlyWorldModel worldModel;
    private BufferedWriter bufferedWriter;

    public OutputApplication(ReadOnlyWorldModel worldModel) throws IOException {
        this(worldModel, "results.json");
    }

    public OutputApplication(ReadOnlyWorldModel worldModel, String fileName) throws IOException {
        this.worldModel = worldModel;
        bufferedWriter = null;
        open(fileName);
    }

    public void open(String fileName) throws IOException {
        close();
        bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(fileName),
                        "UTF-8"));
    }

    public void close() throws IOException {
        if (bufferedWriter != null) {
            bufferedWriter.close();
            bufferedWriter = null;
        }
    }

    //due to the way JSONObject is implemented, .put() will always throw unchecked warnings due to raw type HashMap
    @SuppressWarnings("unchecked")
    public void writeResults() throws IOException {
        Iterator<WorldModelVertex> wmIterator = worldModel.dataVertexStream(worldModel.getVersion()).iterator();
        SortedSet<DataContainer> dataContainerSortedSet;
        WorldModelVertex wmV;
        WorldModelDataVertex wmDV;

        while (wmIterator.hasNext()) {
            wmV = wmIterator.next();
            if (wmV instanceof WorldModelDataVertex) {
                wmDV = (WorldModelDataVertex) wmV;
                if (!wmDV.referent.equals(WorldModelDataVertex.LOCAL_REFERENT)) {
                    dataContainerSortedSet = wmDV.getHistory();
                    for (DataContainer dc : dataContainerSortedSet) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("time", ((TimeOfArrivalContainer) dc.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL)).getTimeOfArrival());
                        jsonObject.put("senderID", ((WorldModelDataVertex) wmV).referent);
                        jsonObject.put("messageID", dc.getMessageID());
                        jsonObject.put("trust", worldModel.isTrustworthy((WorldModelDataVertex) wmV, dc.version).getExpectation());

                        Map<String, Opinion> detectionResults = worldModel.getDetectionResults((WorldModelDataVertex) wmV, dc.version);
                        String detectorName;
                        JSONObject detectionResultsJSONObject = new JSONObject();
                        for (Map.Entry<String, Opinion> detectionResult : detectionResults.entrySet()) {
                            detectorName = detectionResult.getKey();
                            detectionResultsJSONObject.put(detectorName, detectionResult.getValue().getExpectation());
                        }
                        jsonObject.put("results", detectionResultsJSONObject);
                        bufferedWriter.write(jsonObject.toJSONString());
                        bufferedWriter.newLine();
                    }
                }
            }
        }
        bufferedWriter.flush();
    }
}
