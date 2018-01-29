package de.uulm.vs.autodetect.veins;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfArrivalContainer;
import de.uulm.vs.autodetect.mds.framework.view.InputConverter;
import de.uulm.vs.autodetect.posverif.GPSJSONParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses data produced by a VEINS simulation in JSON format. Assumes the input is ordered by time.
 * <p>
 * Created by Rens van der Heijden on 3/30/17.
 */
public class BaseJSONParser implements Runnable {
    static final Logger l = LogManager.getLogger(BaseJSONParser.class);

    //NOTE: see also the source code of the corresponding VEINS component
    public static final int TYPE_GPS = 2;
    public static final int TYPE_BEACON = 3;

    protected final InputConverter input;
    protected final String fileName;
    protected final List<VeinsJSONParser> parserList;
    protected final VeinsJSONParser GPSParser = new GPSJSONParser();

    private BufferedReader bufferedReader;

    public BaseJSONParser(InputConverter ic, String filename, List<VeinsJSONParser> parserList) throws IOException {
        this.input = ic;
        this.fileName = filename;
        this.parserList = parserList;
        bufferedReader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filename),
                        "UTF-8"));
    }

    @Override
    public void run() {
        String line;
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    jsonObject = (JSONObject) parser.parse(line);
                } catch (ParseException e) {
                    l.error(e);
                    continue;
                } catch (ClassCastException e) {
                    l.error("Invalid JSON format, expecting a JSON object!");
                    continue;
                }

                double time = ((Number) jsonObject.get("rcvTime")).doubleValue();
                MaatContainer timeContainer = new TimeOfArrivalContainer(time);

                List<MaatContainer> parsedData = new ArrayList<>();
                parsedData.add(timeContainer);
                int type = ((Number) jsonObject.get("type")).intValue();

                switch (type) {
                    case TYPE_BEACON:
                        String sender = jsonObject.get("sender").toString();
                        String messageID = jsonObject.get("messageID").toString();
                        for (VeinsJSONParser subParser : parserList) {
                            try {
                                MaatContainer parseResult = subParser.parse(jsonObject);
                                if (parseResult != null)
                                    parsedData.add(parseResult);
                            } catch (IllegalArgumentException e) {
                                l.debug("Failed to parse " + jsonObject + " with " + parser.getClass().getCanonicalName());
                            }
                        }

                        this.input.process(parsedData, sender, messageID);
                        break;

                    case TYPE_GPS:
                        parsedData.add(GPSParser.parse(jsonObject));

                        this.input.process(parsedData, WorldModelDataVertex.LOCAL_REFERENT, WorldModelDataVertex.LOCAL_REFERENT);
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            l.error("Cannot read file!");
        }
    }
}
