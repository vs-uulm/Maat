package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.PositionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.SpeedContainer;
import de.uulm.vs.autodetect.veins.BaseJSONParser;
import de.uulm.vs.autodetect.veins.VeinsJSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Example implementation of a {@link BaseJSONParser}. Parses directional speed information from a JSON message.
 * <p>
 * Created by Rens van der Heijden on 1/18/17.
 */
public class SpeedJSONParser implements VeinsJSONParser {

    @Override
    public MaatContainer parse(JSONObject obj) {
        JSONArray speed =  (JSONArray) obj.get("spd");
        if(speed == null)
            return null;
        double x = ((Number) speed.get(0)).doubleValue();
        double y = ((Number) speed.get(1)).doubleValue();
        double z;
        if (speed.size() > 2) z = ((Number) speed.get(2)).doubleValue();
        else z = 0.0;

        return new SpeedContainer(x, y, z);
    }
}
