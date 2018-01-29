package de.uulm.vs.autodetect.mds.framework.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Florian Diemer on 19.04.2017.
 */
public class VarianceOutputApplication {
    private final Logger l = LogManager.getLogger(Maat.class);
    private PrintWriter writer;

    public VarianceOutputApplication(String fileName) {
        writer = null;
        open(fileName);
    }

    public void open(String fileName) {
        close();
        try {
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName, true),
                    "UTF-8"));
        } catch (IOException e) {
            l.error("IOException in application...", e);
        }
    }

    public void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    public void writeValues(ArrayList<Double> values) {
        for (Double value : values) {
            writer.println(value);
        }

        writer.flush();
    }
}
