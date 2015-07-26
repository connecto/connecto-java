package io.connecto.connectoapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.PublicKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

/**
 * This class writes JSONObjects of a form appropriate to send as Connecto events and
 * identify calls to user profiles via the ConnectoAPI class.
 *
 * Instances of this class can be instantiated separately from instances of ConnectoAPI,
 * and the resulting messages are suitable for enqueuing or sending over a local network.
 */
public class MessageBuilder {
    public MessageBuilder(String writeKey) {
        mWriteKey = writeKey;
    }

    public String guid() {
        return String.valueOf(UUID.randomUUID());
    };

    public String returnISODate() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }

    public JSONObject getDefaultPayload(String eventType) {
        JSONObject payload = new JSONObject();
        String messageId = guid();
        long time = System.currentTimeMillis() / 1000;
        try {
            payload.put("sentAt", returnISODate());
            JSONObject library = new JSONObject();
            library.put("name", "connecto-java");
            library.put("version", "1.0");
            JSONObject libraryDetails = new JSONObject();
            libraryDetails.putOnce("library", library);
            payload.putOnce("context", libraryDetails);
            payload.put("messageId", messageId);
            payload.put("type", eventType);
            payload.put("channel", "JDK");
            payload.put("writeKey", mWriteKey);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return payload;
    };

    /**
     * @param distinctId a string uniquely identifying the individual with whom this event is related.
     * (for example, the user id of a signing-in user, or the hostname of a server)
     * For userId it is always better to give database id for unique identification
     * @param eventType should be "track" for the track calls
     * @param eventName a human readable name for the event, for example "Add to Cart", or "Viewed Dashboard Page"
     * @param properties a JSONObject associating properties with the event. These are useful
     *           for reporting and segmentation of events. It is often useful not only to include
     *           properties of the event itself (for example { 'Item Purchased' : 'Dress' } or {'variantId':'1234'}
    **/

    public JSONObject event(String distinctId, String eventName, String eventType, JSONObject properties) {
        // Nothing below should EVER throw a JSONException.
        try {
            JSONObject dataObj = getDefaultPayload(eventType);
            dataObj.put("event", eventName);
            if (distinctId != null) {
                dataObj.put("profileId", distinctId);
                dataObj.put("userId", distinctId);
            }
            JSONObject propertiesObj = null;
            if (properties == null) {
                propertiesObj = new JSONObject();
            }
            else {
                propertiesObj = new JSONObject(properties.toString());
            }

            dataObj.put("properties", propertiesObj);
            return dataObj;
        } catch (JSONException e) {
            throw new RuntimeException("Can't construct a Connecto message", e);
        }
    }

    /**
     * Sets User property on the profile associated with the given distinctId or userID.
     * When sent, this message will overwrite any existing values for the given traits.
     * So, to set some traits on user ABCD, one might call:
     * <pre>
     * {@code
     *     JSONObject traits = new JSONObject();
     *     traits.put("Company", "Uneeda Medical Supply");
     *     trauts.put("Easter Eggs", "Hatched");
     *     JSONObject message = messageBuilder.set("ABCD", traits);
     *     connectoApi.sendMessage(message);
     * }
     * </pre>
     *
     * @param distinctId a string uniquely identifying the user.
     * The optimal solution is to choose your Database Id as unique identifier as that
     * will remain constant throughout even if the user will change email id etc.
     * If no profile exists for the given id, a new one will be created.
     * @param traits a collection of traits to set on the associated profile. Each key
     * in the traits argument will be updated on on the user profiles.
     */
    public JSONObject set(String distinctId, String eventType, JSONObject traits) {
        return identify(distinctId, eventType, traits);
    }

    /**
     * Formats a generic identify message.
     * @param distinctId a string uniquely identifying the individual cause associated with this event
     *           (for example, the user id of a signing-in user, or the hostname of a server)
     * @param eventType should be "identify" for the identify calls
     * @param traits a payload of the operation. Will be converted to JSON, and should be of types
     *           Boolean, Double, Integer, Long, String, JSONArray, JSONObject, the JSONObject.NULL object, or null.
     *           NaN and negative/positive infinity will throw an IllegalArgumentException

     * @throws IllegalArgumentException if traits is not intelligible as a JSONObject property
     *
     * @see MessageBuilder#set(String distinctId, String eventType, JSONObject traits)
     */

    public JSONObject identify(String distinctId, String eventType, Object traits) {
        JSONObject dataObj = getDefaultPayload(eventType);
        if (null == traits) {
            throw new IllegalArgumentException("Cannot send null traits, use JSONObject.NULL instead");
        }

        try {
            dataObj.put("traits", traits);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot interpret traits as a JSON payload", e);
        }


        // At this point, nothing should ever throw a JSONException
        try {
            dataObj.put("writeKey", mWriteKey);
            if (distinctId != null) {
                dataObj.put("profileId", distinctId);
                dataObj.put("userId", distinctId);
            }
            return dataObj;
        } catch (JSONException e) {
            throw new RuntimeException("Can't construct a Connecto message", e);
        }
    }

    private final String mWriteKey;

    private static final String ENGAGE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
}
