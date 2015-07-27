package io.connecto.connectoapi;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * A model class which wraps around a list of segment objects.
 *
 * An instance of this class is returned by the getSegments api.
 * @see ConnectoAPI#getSegments(String, String)
 * */
public class SegmentResponse {

    /**
     * Formats a generic identify message.
     * @param segmentList a payload of the operation. Will be converted to JSON, and should be of types
     *           Boolean, Double, Integer, Long, String, JSONArray, JSONObject, the JSONObject.NULL object, or null.
     *           NaN and negative/positive infinity will throw an IllegalArgumentException

     * @throws JSONException if the JSON object is not convertible to a segment array
     *
     * @see ConnectoAPI#getSegments(String, String)
     */
    public SegmentResponse(JSONArray segmentList) throws JSONException {
        if (segmentList.length() != 0) {
            mSegments = new ArrayList<Segment>();
            for (int i = 0; i < segmentList.length(); i++) {
                Segment segmentObj = null;
                segmentObj = new Segment(segmentList.getJSONObject(i));
                mSegments.add(segmentObj);
            }
        }
    }

    /* package */
    public List<Segment> getSegments() {
        return mSegments;
    }

    private List<Segment> mSegments;
}
