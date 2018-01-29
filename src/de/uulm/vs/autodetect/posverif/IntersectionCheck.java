package de.uulm.vs.autodetect.posverif;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelDataVertex;
import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.PositionContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.TimeOfArrivalContainer;
import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.Iterator;

/**
 * Created by Florian Diemer on 05.04.2017.
 */
public class IntersectionCheck {
    public enum VertexPos {
        LR, RR, RF, LF
    }

    public static final double DS = 1.25;
    public static final double ALPHA = 1.0;

    // TODO find values
    public static final double LENGTH = 5.0;
    public static final double WIDTH = 2.0;
    public static final double MAX_WIDTH = 2.5;

    public static boolean intersects(WorldModelDataVertex worldModelDataVertex1, WorldModelDataVertex worldModelDataVertex2) throws IllegalArgumentException {
        if (worldModelDataVertex1.historySize() < 2 || worldModelDataVertex2.historySize() < 2) {
            throw new IllegalArgumentException("History size must be > 1.");
        }
        PositionContainer p1 = (PositionContainer) worldModelDataVertex1.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer p2 = (PositionContainer) worldModelDataVertex2.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer heading1 = getHeading(worldModelDataVertex1);
        PositionContainer heading2 = getHeading(worldModelDataVertex2);
        if (heading1 == null || heading2 == null) {
            // TODO: throw exception?
            return false;
        }

        return intersects(p1, heading1, p2, heading2);
    }

    private static boolean intersects(PositionContainer p1, PositionContainer heading1, PositionContainer p2, PositionContainer heading2) {
        PositionContainer p1Vertex1;
        PositionContainer p1Vertex2;
        PositionContainer p2Vertex1;
        double value;
        boolean positive = true;
        boolean detected = true;

        for (int i = 0; i < VertexPos.values().length; i++) {
            detected = true;
            p2Vertex1 = getVertexPos(p2, heading2, VertexPos.values()[i], LENGTH, WIDTH);
            for (int j = 0; j < VertexPos.values().length; j++) {
                p1Vertex1 = getVertexPos(p1, heading1, VertexPos.values()[j], LENGTH, WIDTH);
                p1Vertex2 = getVertexPos(p1, heading1, VertexPos.values()[(j + 1) % VertexPos.values().length], LENGTH, WIDTH);
                value = (p2Vertex1.getX() - p1Vertex1.getX())
                        * (p1Vertex2.getY() - p1Vertex1.getY())
                        - (p2Vertex1.getY() - p1Vertex1.getY())
                        * (p1Vertex2.getX() - p1Vertex1.getX());
                if (j > 0) {
                    if (positive != (value >= 0.0)) {
                        detected = false;
                        break;
                    }
                } else {
                    positive = (value >= 0.0);
                }
            }
            if (detected) {
                break;
            }
        }
        return detected;
    }

    public static long intersects(WorldModelDataVertex worldModelDataVertex1, WorldModelDataVertex worldModelDataVertex2, long maxSize) throws IllegalArgumentException {
        double velocity1 = getVelocity(worldModelDataVertex1);
        double velocity2 = getVelocity(worldModelDataVertex2);

        long detected = -1;
        for (int i = 0; i < maxSize; i++) {
            if (intersects(worldModelDataVertex1, worldModelDataVertex2, velocity1, velocity2, i, maxSize)) {
                detected = i;
                break;
            }
        }

        return detected;
    }

    private static boolean intersects(WorldModelDataVertex worldModelDataVertex1, WorldModelDataVertex worldModelDataVertex2, double velocity1, double velocity2, long size, long maxSize) {
        double width = getWidth(size, maxSize, WIDTH);
        double dimensions1[] = new double[]{getLength(size, maxSize, velocity1, LENGTH), width};
        double dimensions2[] = new double[]{getLength(size, maxSize, velocity2, LENGTH), width};

        return intersects(worldModelDataVertex1, worldModelDataVertex2, dimensions1, dimensions2);
    }

    private static boolean intersects(WorldModelDataVertex worldModelDataVertex1, WorldModelDataVertex worldModelDataVertex2, double[] dimensions1, double[] dimensions2) {
        PositionContainer p1 = (PositionContainer) worldModelDataVertex1.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer p2 = (PositionContainer) worldModelDataVertex2.getLatestValue().get(WorldModelDataTypeEnum.POSITION);
        PositionContainer heading1;
        PositionContainer heading2;
        try {
            heading1 = getHeading(worldModelDataVertex1);
            heading2 = getHeading(worldModelDataVertex2);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (heading1 == null) {
            heading1 = new PositionContainer(new double[]{1.0, 0.0, 0.0});
            dimensions1[1] = dimensions1[0];
        }
        if (heading2 == null) {
            heading2 = new PositionContainer(new double[]{1.0, 0.0, 0.0});
            dimensions2[1] = dimensions2[0];
        }

        double length1 = dimensions1[0];
        double width1 = dimensions1[1];
        double length2 = dimensions2[0];
        double width2 = dimensions2[1];

        PositionContainer p1Vertex1;
        PositionContainer p1Vertex2;
        PositionContainer p2Vertex1;
        double value;
        boolean positive = true;
        boolean detected = true;

        for (int i = 0; i < VertexPos.values().length; i++) {
            detected = true;
            p2Vertex1 = getVertexPos(p2, heading2, VertexPos.values()[i], length2, width2);
            for (int j = 0; j < VertexPos.values().length; j++) {
                p1Vertex1 = getVertexPos(p1, heading1, VertexPos.values()[j], length1, width1);
                p1Vertex2 = getVertexPos(p1, heading1, VertexPos.values()[(j + 1) % VertexPos.values().length], length1, width1);
                value = (p2Vertex1.getX() - p1Vertex1.getX())
                        * (p1Vertex2.getY() - p1Vertex1.getY())
                        - (p2Vertex1.getY() - p1Vertex1.getY())
                        * (p1Vertex2.getX() - p1Vertex1.getX());
                if (j > 0) {
                    if (positive != (value >= 0.0)) {
                        detected = false;
                        break;
                    }
                } else {
                    positive = (value >= 0.0);
                }
            }
            if (detected) {
                break;
            }
        }
        return detected;
    }

    private static double getLength(long size, long maxSize, double velocity, double length) {
        return ((((double) size / (double) maxSize) * (velocity / DS)) + (ALPHA * length));
    }

    private static double getWidth(long size, long maxSize, double width) {
        return ((((double) size / (double) maxSize) * (MAX_WIDTH - (ALPHA * width))) + (ALPHA * width));
    }

    private static double getVelocity(WorldModelDataVertex worldModelDataVertex) throws IllegalArgumentException {
        if (worldModelDataVertex.historySize() < 2) {
            throw new IllegalArgumentException("History size must be > 1.");
        }
        Iterator<DataContainer> dataContainerIterator = worldModelDataVertex.getHistory().iterator();
        DataContainer last = dataContainerIterator.next();
        DataContainer beforeLast = dataContainerIterator.next();
        PositionContainer currPos = (PositionContainer) last.get(WorldModelDataTypeEnum.POSITION);
        PositionContainer prevPos = (PositionContainer) beforeLast.get(WorldModelDataTypeEnum.POSITION);
        TimeOfArrivalContainer currTime = (TimeOfArrivalContainer) last.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
        TimeOfArrivalContainer prevTime = (TimeOfArrivalContainer) beforeLast.get(WorldModelDataTypeEnum.TIME_OF_ARRIVAL);
        double distance = Math.abs(currPos.distance(prevPos));
        double timeDiff = Math.abs(currTime.getTimeOfArrival() - prevTime.getTimeOfArrival());
        return distance / timeDiff;
    }

    private static PositionContainer getHeading(PositionContainer prevPos, PositionContainer currPos) {
        double p1[] = prevPos.getPosition();
        double p2[] = currPos.getPosition();

        if (p1.length != p2.length)
            return null;

        int length = p1.length;
        double heading[] = new double[length];
        double vectorLength = 0.0;

        for (int i = 0; i < length; i++) {
            heading[i] = p2[i] - p1[i];
            vectorLength += Math.pow(heading[i], 2.0);
        }

        if (vectorLength == 0.0) {
            return null;
        }
        vectorLength = Math.sqrt(vectorLength);

        for (int i = 0; i < length; i++) {
            heading[i] = heading[i] / vectorLength;
        }

        return new PositionContainer(heading);
    }

    private static PositionContainer getHeading(WorldModelDataVertex dataVertex) throws IllegalArgumentException {
        if (dataVertex.historySize() < 2) {
            throw new IllegalArgumentException("History size must be > 1.");
        }
        PositionContainer heading;
        Iterator<DataContainer> dataContainerIterator = dataVertex.getHistory().iterator();
        DataContainer last = dataContainerIterator.next();
        PositionContainer currPos = (PositionContainer) last.get(WorldModelDataTypeEnum.POSITION);
        DataContainer beforeLast;
        PositionContainer prevPos;
        while (dataContainerIterator.hasNext()) {
            beforeLast = dataContainerIterator.next();
            prevPos = (PositionContainer) beforeLast.get(WorldModelDataTypeEnum.POSITION);
            if ((heading = getHeading(prevPos, currPos)) != null) {
                return heading;
            }
        }
        return null;
    }

    private static PositionContainer getVertexPos(PositionContainer middle, PositionContainer heading, VertexPos vertexPos, double length, double width) {
        PositionContainer vertexPosContainer;
        double length2 = length / 2.0;
        double width2 = width / 2.0;
        double xPos = middle.getX();
        double yPos = middle.getY();
        double zPos = middle.getZ();
        double xHeading = heading.getX();
        double yHeading = heading.getY();

        switch (vertexPos) {
            case LR:
                // left
                xPos -= yHeading * width2;
                yPos += xHeading * width2;
                // rear
                xPos -= xHeading * length2;
                yPos -= yHeading * length2;
                vertexPosContainer = new PositionContainer(new double[]{xPos, yPos, zPos});
                break;
            case RR:
                // right
                xPos += yHeading * width2;
                yPos -= xHeading * width2;
                // rear
                xPos -= xHeading * length2;
                yPos -= yHeading * length2;
                vertexPosContainer = new PositionContainer(new double[]{xPos, yPos, zPos});
                break;
            case RF:
                // right
                xPos += yHeading * width2;
                yPos -= xHeading * width2;
                // front
                xPos += xHeading * length2;
                yPos += yHeading * length2;
                vertexPosContainer = new PositionContainer(new double[]{xPos, yPos, zPos});
                break;
            case LF:
                // left
                xPos -= yHeading * width2;
                yPos += xHeading * width2;
                // front
                xPos += xHeading * length2;
                yPos += yHeading * length2;
                vertexPosContainer = new PositionContainer(new double[]{xPos, yPos, zPos});
                break;
            default:
                return middle;
        }
        return vertexPosContainer;
    }
}
