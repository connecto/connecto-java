package io.connecto.connectoapi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A ClientDelivery can be used to send multiple messages to Connecto.
 */
public class ClientDelivery {
    /**
     * Adds an individual message to this delivery. Messages to Connecto are often more efficient when sent in batches.
     *
     * @param message a JSONObject produced by #{@link MessageBuilder}. Arguments not from MessageBuilder will throw
     * an exception.@throws ConnectoMessageException if the given JSONObject is not formatted appropriately.
     * @see MessageBuilder
     **/
    public void addMessage(JSONObject message) {
        if (! isValidMessage(message)) {
            throw new ConnectoMessageException("Given JSONObject was not a valid Connecto message", message);
        }
        // ELSE message is valid

        try {
            String messageType = message.getString("type");

            if (messageType.equals("track")) {
                mEventsMessages.add(message);
            }
            else if (messageType.equals("identify")) {
                mIdentifyMessages.add(message);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Apparently valid Connecto message could not be interpreted.", e);
        }
    }

    /**
     * Returns true if the given JSONObject appears to be a valid Connecto message, created with #{@link MessageBuilder}.
     * @param message a JSONObject to be tested
     * @return true if the argument appears to be a Connecto message
     */
    public boolean isValidMessage(JSONObject message) {
        // See MessageBuilder for how these messages are formatted.
        boolean ret = true;
        try {

            String messageType = message.getString("type");

            if (message == null) {
                ret = false;
            }
            else if (!messageType.equals("track") && !messageType.equals("identify")) {
                ret = false;
            }
        } catch (JSONException e) {
            ret = false;
        }

        return ret;
    }

    /* package */
    List<JSONObject> getEventsMessages() {
        return mEventsMessages;
    }

    /* package */
    List<JSONObject> getIdentifyMessages() {
        return mIdentifyMessages;
    }

    private final List<JSONObject> mEventsMessages = new ArrayList<JSONObject>();
    private final List<JSONObject> mIdentifyMessages = new ArrayList<JSONObject>();
}
