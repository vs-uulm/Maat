package de.uulm.vs.autodetect.mds.framework.view;

import de.uulm.vs.autodetect.mds.framework.controller.detectors.AbstractDetectorFactory;
import de.uulm.vs.autodetect.mds.framework.controller.detectors.DetectorIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by johannes on 14.06.17.
 */
public class LoadDetectors {
    private static final Logger l = LogManager.getLogger(LoadDetectors.class);

    public static void loadDetectors(File configFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();
            l.debug("Loading " + doc.getDocumentElement().getNodeName());
            NodeList detectors = doc.getElementsByTagName("det");

            for (int temp = 0; temp < detectors.getLength(); temp++) {
                Node nNode = detectors.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    String detectorName = "";
                    String factoryName = "";


                    Element eElement = (Element) nNode;
                    NodeList names = eElement.getElementsByTagName("name");
                    Node name = names.item(0);
                    detectorName = name.getTextContent();
                    l.info("Detector name: " + name.getTextContent());

                    NodeList factories = eElement.getElementsByTagName("factoryname");
                    if (factories.getLength() > 0) {
                        Node facname = factories.item(0);
                        factoryName = facname.getTextContent();
                        l.info("Factoryname: " + facname.getTextContent());
                    } else {
                        l.warn("No factories given");
                    }


                    NodeList parameters = eElement.getElementsByTagName("parameters");

                    int parameterSetCounter = 0;

                    while (parameterSetCounter < parameters.getLength()) {
                        Element fElement = (Element) parameters.item(parameterSetCounter);

                        l.info("Initializing parameter set " + (parameterSetCounter + 1) + " of " + parameters.getLength() + " for Detector " + detectorName + ".");

                        loadDetector(detectorName, factoryName, fElement);

                        parameterSetCounter++;
                    }
                }
            }

        } catch (ParserConfigurationException | IOException | SAXException e) {
            l.error("Error during detector load: ", e);
        }
    }

    private static void loadDetector(String detectorName, String factoryName, Element parameterSet) {
        NodeList keyValues = parameterSet.getElementsByTagName("key");

        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < keyValues.getLength(); i++) {
            Node node1 = keyValues.item(i);
            if (node1.getNodeType() == node1.ELEMENT_NODE) {
                Element key = (Element) node1;
                String type = key.getAttribute("type");
                switch (type) {
                    case "Boolean":
                        map.put(key.getAttribute("name"), Boolean.parseBoolean(key.getTextContent()));
                        break;
                    case "Integer":
                        map.put(key.getAttribute("name"), Integer.parseInt(key.getTextContent()));
                        break;
                    case "Byte":
                        map.put(key.getAttribute("name"), Byte.parseByte(key.getTextContent()));
                        break;
                    case "Double":
                        map.put(key.getAttribute("name"), Double.parseDouble(key.getTextContent()));
                        break;
                    case "Float":
                        map.put(key.getAttribute("name"), Float.parseFloat(key.getTextContent()));
                        break;
                    case "long":
                        map.put(key.getAttribute("name"), Long.parseLong(key.getTextContent()));
                        break;
                    case "Character":
                        if (key.getTextContent().length() > 0)
                            map.put(key.getAttribute("name"), key.getTextContent().charAt(0));
                        break;
                    case "char":
                        if (key.getTextContent().length() > 0)
                            map.put(key.getAttribute("name"), key.getTextContent().charAt(0));
                        break;
                    case "String":
                        map.put(key.getAttribute("name"), key.getTextContent());
                        break;
                    default:
                        map.put(key.getAttribute("name"), key.getTextContent());
                        break;
                }


                l.debug("Key: " + key.getAttribute("name") + "   Value:  " + key.getTextContent());
            }
        }


        try {

            //without package class not found
            String detectorClassFullName = "de.uulm.vs.autodetect.posverif." + detectorName;
            String factoryClassFullName = "de.uulm.vs.autodetect.posverif." + factoryName;

            Class<?> detectorFactoryClass = Class.forName(factoryClassFullName);
            Class<?> detectorClass = Class.forName(detectorClassFullName);

            if (detectorClass == null) {
                throw new ClassCastException("Attempted to use " + detectorClassFullName + " for factory " + factoryClassFullName + ", but this Detector class does not exist!");
            }


            Field[] fieldArray = detectorClass.getFields();
            for (int fieldC = 0; fieldC < fieldArray.length; fieldC++) {
                String fieldName = fieldArray[fieldC].getName();
                if (!map.keySet().contains(fieldName)) {
                    l.warn("Unset Field: " + fieldArray[fieldC].getName());
                }
            }

            Method setAttr = detectorFactoryClass.getMethod("storeAttributes", Map.class);

            AbstractDetectorFactory factoryInstance = (AbstractDetectorFactory) detectorFactoryClass.newInstance();

            DetectorIndex.INSTANCE.registerDetector(detectorName + " " + map.toString(), factoryInstance);
            setAttr.invoke(factoryInstance, map);


        } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            l.error("Error during detector load: ", e);
        }
    }
}
