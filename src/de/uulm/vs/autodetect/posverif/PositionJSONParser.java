package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.PositionContainer;
import de.uulm.vs.autodetect.veins.BaseJSONParser;
import de.uulm.vs.autodetect.veins.VeinsJSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Example implementation of a {@link BaseJSONParser}. Parses position information from a JSON message.
 * <p>
 * Created by Rens van der Heijden on 1/18/17.
 */
public class PositionJSONParser implements VeinsJSONParser {

    @Override
    public MaatContainer parse(JSONObject obj) {
        JSONArray pos = (JSONArray) obj.get("pos");
        double x = ((Number) pos.get(0)).doubleValue();
        double y = ((Number) pos.get(1)).doubleValue();
        double z;
        if (pos.size() > 2) z = ((Number) pos.get(2)).doubleValue();
        else z = 0.0;

        return new PositionContainer(new double[]{x, y, z});
    }
}
