package info.technikality.nzhighway.Route;

/**
 * Async Task to access the Google Direction API and return the routing data
 * which is then parsed and converting to a route overlay using some classes created by Hesham Saeed.
 * @author Joel Dean
 * Requires an instance of the map activity and the application's current context for the progress dialog.
 *
 */

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


public class Routing extends AsyncTask<String, Void, Route> {
    protected ArrayList<RoutingListener> _aListeners;
    protected TravelMode _mTravelMode;

    public Routing(TravelMode mTravelMode) {
        this._aListeners = new ArrayList<RoutingListener>();
        this._mTravelMode = mTravelMode;
    }

    public void registerListener(RoutingListener mListener) {
        _aListeners.add(mListener);
    }

    protected void dispatchOnStart() {
        for (RoutingListener mListener : _aListeners) {
            mListener.onRoutingStart();
        }
    }

    protected void dispatchOnFailure() {
        for (RoutingListener mListener : _aListeners) {
            mListener.onRoutingFailure();
        }
    }

    protected void dispatchOnSuccess(PolylineOptions mOptions, Route route) {
        for (RoutingListener mListener : _aListeners) {
            mListener.onRoutingSuccess(mOptions, route);
        }
    }

    /**
     * Performs the call to the google maps API to acquire routing data and
     * deserializes it to a format the map can display.
     *
     * @param aPoints
     * @return
     */
    @Override
    protected Route doInBackground(String... aPoints) {
        for (String mPoint : aPoints) {
            if (mPoint == null || mPoint.equalsIgnoreCase("")) return null;
        }

        return new GoogleParser(constructURL(aPoints)).parse();
    }

    protected String constructURL(String... points) {
        String start = points[0];
        String dest = points[1];
        String sJsonURL = "https://maps.googleapis.com/maps/api/directions/json?";

            // Edit this to handle string parameter
            /*final StringBuffer mBuf = new StringBuffer(sJsonURL);
            mBuf.append("origin=");
            mBuf.append(URLEncoder.encode(start, "UTF-8"));
            mBuf.append("&destination=");
            mBuf.append(URLEncoder.encode(dest, "UTF-8"));
            mBuf.append("&region=nz&mode=");
            mBuf.append(_mTravelMode.getValue());
            mBuf.append("&key=AIzaSyBH8z5LRQXBw3figE1D3XgnNEwR-52Ed-8");*/

        try {
            return sJsonURL + "origin=" + URLEncoder.encode(start, "UTF-8")
                    + "&destination=" + URLEncoder.encode(dest, "UTF-8")
                    + "&region=nz&mode=" + _mTravelMode.getValue()
                    + "&key=AIzaSyBH8z5LRQXBw3figE1D3XgnNEwR-52Ed-8";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        dispatchOnStart();
    }

    @Override
    protected void onPostExecute(Route result) {
        if (result == null) {
            dispatchOnFailure();
        } else {
            PolylineOptions mOptions = new PolylineOptions();

            for (LatLng point : result.getPoints()) {
                mOptions.add(point);
            }

            dispatchOnSuccess(mOptions, result);
        }
    }//end onPostExecute method

    public enum TravelMode {
        BIKING("biking"),
        DRIVING("driving"),
        WALKING("walking"),
        TRANSIT("transit");

        protected String _sValue;

        private TravelMode(String sValue) {
            this._sValue = sValue;
        }

        protected String getValue() {
            return _sValue;
        }
    }
}
