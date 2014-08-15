package info.technikality.nzhighway;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Daniel (Tuck Wah) Leong on 30/07/2014.
 */
public class TREIS_E implements Serializable {

    // General Information
    private String location_Area;
    private String location;
    private String impact;
    private String event_Description;
    private String event_Comments;
    private String alternative_Route;
    private String expected_Resolution;
    private String[] direct_Line_Distance;

    // Event Details
    private int event_ID;
    private String event_Type;
    private String status;

    // Event Timeline
    private Date start_Date;
    private boolean planned;

    // More Information
    private String island;

    public TREIS_E () {}

    public TREIS_E setLocation_Area(String location_Area) {
        this.location_Area = location_Area;
        return this;
    }

    public TREIS_E setImpact(String impact) {
        this.impact = impact;
        return this;
    }

    public TREIS_E setEvent_Description(String event_Description) {
        this.event_Description = event_Description;
        return this;
    }

    public TREIS_E setEvent_Comments(String event_Comments) {
        this.event_Comments = event_Comments;
        return this;
    }

    public TREIS_E setAlternative_Route(String alternative_Route) {
        this.alternative_Route = alternative_Route;
        return this;
    }

    public TREIS_E setExpected_Resolution(String expected_Resolution) {
        this.expected_Resolution = expected_Resolution;
        return this;
    }

    public TREIS_E setDirect_Line_Distance(String[] direct_Line_Distance) {
        this.direct_Line_Distance = direct_Line_Distance;
        return this;
    }

    public TREIS_E setEvent_ID(int event_ID) {
        this.event_ID = event_ID;
        return this;
    }

    public TREIS_E setEvent_Type(String event_Type) {
        this.event_Type = event_Type;
        return this;
    }

    public TREIS_E setStatus(String status) {
        this.status = status;
        return this;
    }

    public TREIS_E setStart_Date(Date start_Date) {
        this.start_Date = start_Date;
        return this;
    }

    public TREIS_E setPlanned(boolean planned) {
        this.planned = planned;
        return this;
    }

    public TREIS_E setIsland(String island) {
        this.island = island;
        return this;
    }
}
