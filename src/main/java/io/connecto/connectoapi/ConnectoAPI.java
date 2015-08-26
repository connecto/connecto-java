package io.connecto.connectoapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Simple interface to the Connecto tracking API, intended for use in
 * server-side applications. Users are encouraged to review our Javascript
 * API for reporting user events in web applications, and our Android API
 * for use in Android mobile applications.
 *
 * The Java API doesn't provide or assume any threading model, and is designed
 * such that recording events and sending them can be easily separated.
 *
 *
 */
public class ConnectoAPI {

    /**
     * Constructs a ConnectoAPI object associated with the production, Connecto services.
     */
    public ConnectoAPI() {
        this(Config.BASE_ENDPOINT + "/import", Config.BASE_ENDPOINT + "/api/rules?userId=",
             DEFAULT_READ_TIMEOUT_MILLIS);
    }

    /**
     * Create a ConnectoAPI associated with custom URLS for the Connecto service.
     *
     * Useful for testing and proxying. Most callers should use the constructor with no arguments.
     *
     * @param endpoint a URL that will accept Connecto events and identify messages
     * @see #ConnectoAPI()
     */
    public ConnectoAPI(String endpoint, String rulesendpoint, int timeout) {
        mEventsEndpoint = endpoint;
        mRulesEndPoint = rulesendpoint;
        mTimeoutMilliseconds = timeout;
    }

    /**
     * Set timeout for HTTP cals.
     *
     * @param timeoutInMs an integer representing milliseconds
     */
    public void setTimeout(int timeoutInMs) {
        mTimeoutMilliseconds = timeoutInMs;
    }

    /**
     * Sends a single message to Connecto servers.
     *
     * Each call to sendMessage results in a blocking call to remote Connecto servers.
     * To send multiple messages at once, see #{@link #deliver(ClientDelivery)}
     *
     * @param message A JSONObject formatted by #{@link MessageBuilder}
     * @throws ConnectoMessageException if the given JSONObject is not (apparently) a Connecto message. This is a RuntimeException, callers should take care to submit only correctly formatted messages.
     * @throws IOException if
     */
    public void sendMessage(JSONObject message) throws ConnectoMessageException, IOException {
        ClientDelivery delivery = new ClientDelivery();
        delivery.addMessage(message);
        deliver(delivery);
    }

    /**
     * Attempts to send a given delivery to the Connecto servers. Will block,
     * possibly on multiple server requests. For most applications, this method
     * should be called in a separate thread or in a queue consumer.
     *
     * @param toSend a ClientDelivery containing a number of Connecto messages
     * @throws IOException if its unable to parse messages
     * @see ClientDelivery
     */
    public void deliver(ClientDelivery toSend) throws IOException {

        String postUrl = mEventsEndpoint;
        List<JSONObject> events = toSend.getEventsMessages();
        sendMessages(events, postUrl);

        List<JSONObject> user = toSend.getIdentifyMessages();
        sendMessages(user, postUrl);
    }

    private String getAuthorizationHeader(String readKey) {
        return "Basic " + new String(new Base64Coder().encode(readKey.getBytes()));
    }

    /**
     * This api will get the user segment that is a list of matched segment rules.
     *
     * @param readKey an authorization key provided to you in Connecto admin
     * @param userId a user id whose segments are being requested.
     */

    public SegmentResponse getSegments(String readKey, String userId) throws IOException {
        URL endpoint = new URL(mRulesEndPoint + userId);
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(mTimeoutMilliseconds);
        conn.setReadTimeout(mTimeoutMilliseconds);
        String basicAuth = getAuthorizationHeader(readKey);
        conn.setRequestProperty("Authorization", basicAuth);

        InputStream responseStream = null;
        SegmentResponse segmentResponse = null;
        try {
            responseStream = conn.getInputStream();
            String response = slurp(responseStream);
            try {
                JSONArray segments = new JSONArray(response);
                segmentResponse = new SegmentResponse(segments);
            } catch (JSONException e) {
            }
            return segmentResponse;
        } finally {
            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    // ignore, in case we've already thrown
                }
            }
        }
    }

    /**
     * Package scope for mocking purposes
     */
    /* package */
    boolean sendData(String dataString, String endpointUrl) throws IOException {
        URL endpoint = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setConnectTimeout(mTimeoutMilliseconds);
        conn.setReadTimeout(mTimeoutMilliseconds);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        byte[] utf8data;
        try {
            utf8data = dataString.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Connecto library requires utf-8 support", e);
        }

        OutputStream postStream = null;
        try {
            postStream = conn.getOutputStream();
            postStream.write(utf8data);
        } finally {
            if (postStream != null) {
                try {
                    postStream.close();
                } catch (IOException e) {
                    // ignore, in case we've already thrown
                }
            }
        }

        InputStream responseStream = null;
        String response = null;
        try {
            responseStream = conn.getInputStream();
            response = slurp(responseStream);
        } finally {
            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    // ignore, in case we've already thrown
                }
            }
        }

        return ((response != null));
    }

    private void sendMessages(List<JSONObject> messages, String endpointUrl) throws IOException {
        for (int i = 0; i < messages.size(); i += Config.MAX_MESSAGE_SIZE) {
            int endIndex = i + Config.MAX_MESSAGE_SIZE;
            endIndex = Math.min(endIndex, messages.size());
            List<JSONObject> batch = messages.subList(i, endIndex);

            if (batch.size() > 0) {
                String messagesString = dataString(batch);
                boolean accepted = sendData(messagesString, endpointUrl);

                if (! accepted) {
                    throw new ConnectoServerException("Server refused to accept messages, they may be malformed.", batch);
                }
            }
        }
    }

    private String dataString(List<JSONObject> messages) {
        JSONArray array = new JSONArray();
        for (JSONObject message:messages) {
            array.put(message);
        }

        return array.toString();
    }

    private String slurp(InputStream in) throws IOException {
        final StringBuilder out = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(in, "utf8");

        char[] readBuffer = new char[BUFFER_SIZE];
        int readCount = 0;
        do {
            readCount = reader.read(readBuffer);
            if (readCount > 0) {
                out.append(readBuffer, 0, readCount);
            }
        } while(readCount != -1);

        return out.toString();
    }

    private final String mEventsEndpoint;
    private final String mRulesEndPoint;
    private int mTimeoutMilliseconds;

    private static final int BUFFER_SIZE = 256; // Small, we expect small responses.
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 120000; // Two minutes should be more than enough for a response.

}
