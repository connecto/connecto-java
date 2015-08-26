package io.connecto.connectoapi;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Unit test for simple App.
 */
public class ConnectoAPITest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ConnectoAPITest(String testName) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( ConnectoAPITest.class );
    }

    @Override
    public void setUp() {
        //This will create both the identify and events messages such that they can be tested
        mTimeZero = System.currentTimeMillis() / 1000;
        mBuilder = new MessageBuilder("a token");

        try {
            mSampleProps = new JSONObject();
            mSampleProps.put("prop key", "prop value");
            mSampleProps.put("ratio", "\u03C0");
        } catch (JSONException e) {
            throw new RuntimeException("Error in test setup");
        }

        final Map<String, String> sawData = new HashMap<String, String>();

        ConnectoAPI api = new ConnectoAPI("http://localhost:3003/import", "http://localhost:3003/api/rules/", 12000) {
            @Override
            public boolean sendData(String dataString, String endpointUrl) {
                sawData.put(endpointUrl, dataString);
                return true;
            }
        };

        ClientDelivery c = new ClientDelivery();

        JSONObject event = mBuilder.event("a distinct id", "login", "track", mSampleProps);
        c.addMessage(event);

        try {
            api.deliver(c);
        } catch (IOException e) {
            throw new RuntimeException("Impossible IOException", e);
        }

        try {
            api.deliver(c);
        } catch (IOException e) {
            throw new RuntimeException("Impossible IOException", e);
        }

        mIpEventsMessages = sawData.get("http://localhost:3003/import");

        mEventsMessages = sawData.get("http://localhost:3003/import");

        JSONObject set = mBuilder.set("a distinct id", "identify", mSampleProps);
        c.addMessage(set);

        try {
            api.deliver(c);
        } catch (IOException e) {
            throw new RuntimeException("Impossible IOException", e);
        }
        mIdentifyMessages = sawData.get("http://localhost:3003/import");
        sawData.clear();

        try {
            api.deliver(c);
        } catch (IOException e) {
            throw new RuntimeException("Impossible IOException", e);
        }

        mIpIdentifyMessages = sawData.get("http://localhost:3003/import");
    }



    //testing for empty properties in idenfity calls

    public void testEmptyJSON() {
        JSONObject empty = new JSONObject();
        JSONObject built = mBuilder.set("a distinct id", "identify", empty);
    }

    public void testIdentifyMessageBuilds()
            throws JSONException {
        {
            JSONObject set = mBuilder.set("a distinct id","identify", mSampleProps);
            checkModifiers(set);
            checkIdentifyProps(set);
        }

    }

    public void testIdentifyWithBadArguments() {

        mBuilder.identify("id", "action", JSONObject.NULL);
        // Current, less than wonderful behavior- we'll just call toString()
        // on random objects passed in.
        mBuilder.identify("id", "action", new Object());

        JSONArray jsa = new JSONArray();
        mBuilder.identify("id", "action", jsa);

        JSONObject jso = new JSONObject();
        mBuilder.identify("id", "action", jso);

        try {
            mBuilder.identify("id", "action",null);
            fail("identify did not throw an exception on null");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            mBuilder.identify("id", "action",  Double.NaN);
            fail("identify did not throw on NaN");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            mBuilder.identify("id", "action", Double.NaN);
            fail("identify did not throw on NaN");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            mBuilder.identify("id", "action", Double.NEGATIVE_INFINITY);
            fail("identify did not throw on infinity");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testMessageFormat() {
        ClientDelivery c = new ClientDelivery();
        assertFalse(c.isValidMessage(mSampleProps));

        JSONObject event = mBuilder.event("a distinct id", "login", "track", mSampleProps);
        assertTrue(c.isValidMessage(event));

        JSONObject set = mBuilder.set("a distinct id", "identify", mSampleProps);
        assertTrue(c.isValidMessage(set));
    }

    public void testModifiers() {
        JSONObject set = mBuilder.set("a distinct id", "identify", mSampleProps);
        checkModifiers(set);
    }

    public void testEmptyMessageFormat() {
        ClientDelivery c = new ClientDelivery();
        JSONObject eventMessage = mBuilder.event("a distinct id", "empty event", "track", null);
        assertTrue(c.isValidMessage(eventMessage));
    }

    public void testValidate() {
        ClientDelivery c = new ClientDelivery();
        JSONObject event = mBuilder.event("a distinct id", "login", "track", mSampleProps);
        assertTrue(c.isValidMessage(event));
        try {
            JSONObject rebuiltMessage = new JSONObject(event.toString());
            assertTrue(c.isValidMessage(rebuiltMessage));
            assertEquals(c.getEventsMessages().size(), 0);
            c.addMessage(rebuiltMessage);
            assertEquals(c.getEventsMessages().size(), 1);
        } catch (JSONException e) {
            fail("Failed to build JSONObject");
        }
    }

    public void testClientDelivery() {
        ClientDelivery c = new ClientDelivery();
        try {
            c.addMessage(mSampleProps);
            fail("addMessage did not throw");
        } catch (ConnectoMessageException e) {
            // This is expected, we pass
        }
        try {
            JSONObject event = mBuilder.event("a distinct id", "login", "track", mSampleProps);
            c.addMessage(event);
            JSONObject set = mBuilder.set("a distinct id", "identify", mSampleProps);
            c.addMessage(set);
        } catch (ConnectoMessageException e) {
            fail("Threw exception on valid message");
        }
    }

    public void testApiSendIpArgs() {
        assertEquals(mEventsMessages, mIpEventsMessages);
        assertEquals(mIdentifyMessages, mIpIdentifyMessages);
    }

    public void testApiSendEvent() {
        try {
            JSONArray messageArray = new JSONArray(mEventsMessages);
            assertTrue("Only one message sent", messageArray.length() == 1);

            JSONObject eventSent = messageArray.getJSONObject(0);
            String eventName = eventSent.getString("event");
            assertTrue("Event name had expected value", "login".equals(eventName));

            JSONObject eventProps = eventSent.getJSONObject("properties");
            String propValue = eventProps.getString("prop key");
            assertTrue("Property had expected value", "prop value".equals(propValue));
        } catch (JSONException e) {
            fail("Data message can't be interpreted as expected: " + mEventsMessages);
        }
    }

    public void testApiSendIdentify() {
        try {
            JSONArray messageArray = new JSONArray(mIdentifyMessages);
            System.out.print(mIdentifyMessages);

            JSONObject message = messageArray.getJSONObject(0);

            JSONObject setMessage = null;
            if (message.has("traits")) {
                setMessage = message;
            }

            else {
                fail("Can't find $set message in " + mIdentifyMessages);
            }
            JSONObject setProps = setMessage.getJSONObject("traits");
            String propValue = setProps.getString("prop key");
            assertTrue("Set prop had expected value", "prop value".equals(propValue));


        } catch (JSONException e) {
            fail("Messages can't be interpreted as expected: " + mIdentifyMessages);
        }
    }

    public void testExpectedEventProperties() {
        try {
            JSONArray messageArray = new JSONArray(mEventsMessages);
            JSONObject eventSent = messageArray.getJSONObject(0);
            JSONObject eventProps = eventSent.getJSONObject("properties");

            //assertTrue("Time is included", eventSent.getLong("timestamp") >= mTimeZero);

            String distinctId = eventSent.getString("userId");
            assertTrue("Distinct id as expected", "a distinct id".equals(distinctId));

            String token = eventSent.getString("writeKey");
            assertTrue("Token as expected", "a token".equals(token));
        } catch (JSONException e) {
            fail("Data message can't be interpreted as expected: " + mEventsMessages);
        }
    }

    public void testExpectedIdentifyParams() {
        try {
            JSONArray messageArray = new JSONArray(mIdentifyMessages);
            JSONObject setMessage = messageArray.getJSONObject(0);

            //assertTrue("Time is included", setMessage.getLong("$timestamp") >= mTimeZero);

            String distinctId = setMessage.getString("userId");
            assertTrue("Distinct id as expected", "a distinct id".equals(distinctId));

            String token = setMessage.getString("writeKey");
            assertTrue("Token as expected", "a token".equals(token));
        } catch (JSONException e) {
            fail("Data message can't be interpreted as expected: " + mIdentifyMessages);
        }
    }

    public void testEmptyDelivery() {
        ConnectoAPI api = new ConnectoAPI("http://localhost:3003/import", "", 12000) {
            @Override
            public boolean sendData(String dataString, String endpointUrl) {
                fail("Data sent when no data should be sent");
                return true;
            }
        };

        ClientDelivery c = new ClientDelivery();
        try {
            api.deliver(c);
        } catch (IOException e) {
            throw new RuntimeException("Apparently impossible IOException thrown", e);
        }
    }

    public void testLargeDelivery() {
        final List<String> sends = new ArrayList<String>();

        ConnectoAPI api = new ConnectoAPI("http://localhost:3003/import", "", 12000) {
            @Override
            public boolean sendData(String dataString, String endpointUrl) {
                sends.add(dataString);
                return true;
            }
        };

        ClientDelivery c = new ClientDelivery();
        int expectLeftovers = Config.MAX_MESSAGE_SIZE - 1;
        int totalToSend = (Config.MAX_MESSAGE_SIZE * 2) + expectLeftovers;
        for(int i = 0; i < totalToSend; i++) {
            Map<String, Integer> propsMap = new HashMap<String, Integer>();
            propsMap.put("count", i);
            JSONObject props = new JSONObject(propsMap);
            JSONObject message = mBuilder.event("a distinct id", "counted", "track", props);
            c.addMessage(message);
        }

        try {
            api.deliver(c);
        } catch (IOException e) {
            throw new RuntimeException("Apparently impossible IOException", e);
        }

        assertTrue("More than one message", sends.size() == 3);

        try {
            JSONArray firstMessage = new JSONArray(sends.get(0));
            assertTrue("First message has max elements", firstMessage.length() == Config.MAX_MESSAGE_SIZE);

            JSONArray secondMessage = new JSONArray(sends.get(1));
            assertTrue("Second message has max elements", secondMessage.length() == Config.MAX_MESSAGE_SIZE);

            JSONArray thirdMessage = new JSONArray(sends.get(2));
            assertTrue("Third message has all leftover elements", thirdMessage.length() == expectLeftovers);
        } catch (JSONException e) {
            fail("Can't interpret sends appropriately when sending large messages");
        }
    }

    private void checkModifiers(JSONObject built) {
        try {
            JSONObject msg = built;
            assertEquals(msg.getString("userId"), "a distinct id");
            assertEquals(msg.getString("profileId"), "a distinct id");
        } catch (JSONException e) {
            fail(e.toString());
        }
    }

    private void checkIdentifyProps(JSONObject built) {
        try {
            JSONObject msg = built;
            JSONObject props = built.getJSONObject("traits");
            assertEquals(props.getString("prop key"), "prop value");
            assertEquals(props.getString("ratio"), "\u03C0");
        } catch (JSONException e) {
            fail(e.toString());
        }
    }


    private MessageBuilder mBuilder;
    private JSONObject mSampleProps;
    private String mEventsMessages;
    private String mIdentifyMessages;
    private String mIpEventsMessages;
    private String mIpIdentifyMessages;
    private long mTimeZero;
}
