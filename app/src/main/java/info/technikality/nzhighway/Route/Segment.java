package info.technikality.nzhighway.Route;
//by Haseem Saheed

import com.google.android.gms.maps.model.LatLng;

public class Segment {
    /**
     * Points in this segment. *
     */
    private LatLng start;
    /**
     * Turn instruction to reach next segment. *
     */
    private String instruction;
    /**
     * Length of segment. *
     */
    private int length;
    /**
     * Distance covered. *
     */
    private double distance;

    /**
     * Create an empty segment.
     */

    public Segment() {
    }

    /**
     * Get the turn instruction to reach next segment.
     *
     * @return a String of the turn instruction.
     */

    public String getInstruction() {
        return instruction;
    }

    /**
     * Set the turn instruction.
     *
     * @param turn Turn instruction string.
     */

    public void setInstruction(final String turn) {
        this.instruction = turn;
    }

    /**
     * Add a point to this segment.
     *
     * @param point GeoPoint to add.
     */

    public void setPoint(final LatLng point) {
        start = point;
    }

    /**
     * Get the starting point of this
     * segment.
     *
     * @return a GeoPoint
     */

    public LatLng startPoint() {
        return start;
    }

    /**
     * Creates a segment which is a copy of this one.
     *
     * @return a Segment that is a copy of this one.
     */

    public Segment copy() {
        final Segment copy = new Segment();
        copy.start = start;
        copy.instruction = instruction;
        copy.length = length;
        copy.distance = distance;
        return copy;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(final int length) {
        this.length = length;
    }

    /**
     * @return the distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

}