package io.connecto.connectoapi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * A model class which represents information related to a segment within Connecto.
 */
public class Segment {
    /**
     * Formats a segment/rule object.
     * @param jsonObject a payload of the segment returned via Connecto.
     * NaN and negative/positive infinity will throw an IllegalArgumentException

     * @throws JSONException if the jsonObject does not contain the required properties.
     **/
    public Segment(JSONObject jsonObject) throws JSONException {
        this.mTitle = jsonObject.getString(TITLE_KEY);
        this.mRuleId = jsonObject.getString(ID_KEY);
    }

    /* package */
    public String getTitle() { return this.mTitle; }

    /* package */
    public String getRuleId() { return this.mRuleId; }

    private String mTitle;
    private String mRuleId;

    private static final String TITLE_KEY = "title";
    private static final String ID_KEY = "_id";
}
