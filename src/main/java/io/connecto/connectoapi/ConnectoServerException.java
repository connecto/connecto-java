package io.connecto.connectoapi;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Thrown when the Connecto server refuses to accept a set of messages.
 *
 * This exception can be thrown when messages are too large,
 * or the api key is invalid, etc.
 */
public class ConnectoServerException extends IOException {

    public ConnectoServerException(String message, List<JSONObject> badDelivery) {
        super(message);
        mBadDelivery = badDelivery;
    }

    public List<JSONObject> getBadDeliveryContents() {
        return mBadDelivery;
    }

    private final List<JSONObject> mBadDelivery;
}
