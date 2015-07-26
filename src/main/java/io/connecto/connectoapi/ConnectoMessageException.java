package io.connecto.connectoapi;

import org.json.JSONObject;

/**
 * Thrown when the library detects malformed or invalid Connecto messages.
 *
 * Connecto messages are represented as JSONObjects, but not all JSONObjects represent valid Connecto messages.
 * ConnectoMessageExceptions are thrown when a JSONObject is passed to the Connecto library that can't be
 * passed on to the Connecto service.
 *
 * This is a runtime exception, since in most cases it is thrown due to errors in your application architecture.
 */
public class ConnectoMessageException extends RuntimeException {

    /* package */
    ConnectoMessageException(String message, JSONObject cause) {
        super(message);
        mBadMessage = cause;
    }

    /**
     * @return the (possibly null) JSONObject message associated with the failure
     */
    public JSONObject getBadMessage() {
        return mBadMessage;
    }

    private JSONObject mBadMessage = null;
}
