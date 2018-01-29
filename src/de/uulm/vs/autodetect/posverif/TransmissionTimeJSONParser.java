package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfTransmissionContainer;
import de.uulm.vs.autodetect.veins.VeinsJSONParser;
import org.json.simple.JSONObject;

/**
 * Parses transmission time information from a JSON message.
 * <p>
 * Created by Rens van der Heijden on 1/18/17.
 */
public class TransmissionTimeJSONParser implements VeinsJSONParser {

    @Override
    public MaatContainer parse(JSONObject obj) {
        double time = ((Number) obj.get("sendTime")).doubleValue();
        return new TimeOfTransmissionContainer(time);
    }
}
