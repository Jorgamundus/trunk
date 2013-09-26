/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.integration;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.List;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.mockbroker.MockBroker;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AsyncMqttClientIT {

	// FIXME [jim] - trailing slashes in topics should just be removed
	// FIXME [jim] - make sure a/# subscripts to just "a" as well as all subtopics
	// FIXME [jim] - duplicate // separators are just treated as one
	// FIXME [jim] - an empty string is a valid topic
	// FIXME [jim] - the client should send a pingreq if it hasn't heard from the server in ping timeout. this could happen if the client is just publishing qos
	// 0 messages.
	// FIXME [jim] - what is the correct behavior when you subscribe to a topic multiple times??

	String badCredentialsUri = "tcp://q.m2m.io:1883";
	String validBrokerUri = "tcp://test.mosquitto.org:1883";

	@Mock AsyncClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;
	@Captor ArgumentCaptor<Subscription[]> subscriptionCaptor;;
	@Captor ArgumentCaptor<PublishMessage> messageCaptor;;

	MockBroker mockBroker;
	AsyncMqttClient client;
	AsyncMqttClient client2;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void after() {

		if (client != null) {
			client.shutdown();
		}
		if (client2 != null) {
			client2.shutdown();
		}
		if (mockBroker != null) {
			mockBroker.shutdown(5000);
		}
	}

	@Test
	public void testConstructor_InvalidScheme() throws Exception {

		try {
			client = new AsyncMqttClient("ftp://foo:1883", listener, reconnectionStrategy, 5, 0, 5);
			fail("expected exception");
		} catch (MqttException e) {
			assertEquals("Invalid broker URI (scheme must be 'tcp'): ftp://foo:1883", e.getMessage());
		}

		verifyZeroInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConstructor_InvalidHost() throws Exception {

		Throwable thrown = null;
		try {
			client = new AsyncMqttClient("tcp://foo:1883", listener, reconnectionStrategy, 5, 0, 5);
			fail("expected exception");
		} catch (MqttException e) {
			thrown = e.getCause();
			assertEquals(UnresolvedAddressException.class, thrown.getClass());
		}

		verify(listener, timeout(5000)).disconnected(any(AsyncMqttClient.class), same(thrown), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testConstructor_InvalidPort() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1234", listener, reconnectionStrategy, 5, 0, 5);

		verify(listener, timeout(100000)).disconnected(eq(client), any(ConnectException.class), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnectDisconnect_NoCredentialsNoWill() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient1", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_BadCredentials() throws Exception {

		client = new AsyncMqttClient(badCredentialsUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient2", true, 90, "not_a_user", "not_a_password");

		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.BAD_CREDENTIALS);
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_Accepted() throws Exception {

		if (mockBroker == null) {
			mockBroker = new MockBroker(null, 15, 0, true);
			mockBroker.init();
		}

		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient2", true, 90, "user1", "password1");

		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testConnect_Will_NoRetain_Subscribed() throws Exception {

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient3", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient4", true, 90, "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic1", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_NoRetain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient5", true, 90, "my/will/topic2", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient6", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic2", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));
		// verify no will message
		Thread.sleep(1000);
		verify(listener2, never()).publishReceived(same(client2), any(PublishMessage.class));
		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient7", true, 90, "my/will/topic3", "it died dude", QoS.AT_LEAST_ONCE, true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient8", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic3", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic3", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		// remove the message
		client2.publish(new PublishMessage("my/will/topic3", QoS.AT_LEAST_ONCE, new byte[0], true));
		verify(listener2, timeout(5000)).published(same(client2), isA(PublishMessage.class));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_Subscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient10", true, 90, "my/will/topic4", "it died dude", QoS.AT_LEAST_ONCE, true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient9", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic4", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic4", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		// remove the message
		client2.publish(new PublishMessage("my/will/topic4", QoS.AT_LEAST_ONCE, new byte[0], true));
		verify(listener2, timeout(5000)).published(same(client2), isA(PublishMessage.class));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_CredentialsAndWill_Accepted() throws Exception {

		if (mockBroker == null) {
			mockBroker = new MockBroker(null, 15, 0, true);
			mockBroker.init();
		}

		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient3", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient4", true, 90, "user1", "password1", "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic1", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testSubscribeUnsubscribe_Array() throws Exception {

		// connect client
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient11", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic2", QoS.AT_MOST_ONCE) };
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		client.subscribe(requestedSubscriptions);
		verify(listener, timeout(5000)).subscribed(same(client), same(requestedSubscriptions), aryEq(grantedSubscriptions), eq(true));

		// test unsubscribing
		client.unsubscribe(new String[] { "my/topic1", "my/topic2" });
		verify(listener, timeout(5000)).unsubscribed(same(client), aryEq(new String[] { "my/topic1", "my/topic2" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testSubscribeUnsubscribe_List() throws Exception {

		// connect client
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient12", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic3", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic4", QoS.AT_MOST_ONCE) };
		List<Subscription> requestedSubscriptionsList = Arrays.asList(requestedSubscriptions);
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		client.subscribe(requestedSubscriptionsList);
		verify(listener, timeout(5000)).subscribed(same(client), aryEq(requestedSubscriptions), aryEq(grantedSubscriptions), eq(true));

		// test unsubscribing
		client.unsubscribe(Arrays.asList(new String[] { "my/topic3", "my/topic4" }));
		verify(listener, timeout(5000)).unsubscribed(same(client), aryEq(new String[] { "my/topic3", "my/topic4" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testPublish_Qos1_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient13", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic5", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect a client and generate the messages
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient14", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		for (int i = 0; i < 10; i++) {
			client.publish(new PublishMessage("my/topic5", QoS.AT_LEAST_ONCE, "my message " + i));
		}
		verify(listener, timeout(5000).times(10)).published(same(client), isA(PublishMessage.class));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the messages
		verify(listener2, timeout(5000).times(10)).publishReceived(same(client2), messageCaptor.capture());

		for (PublishMessage message : messageCaptor.getAllValues()) {
			message.ack();
			assertEquals("my/topic5", message.getTopic());
			assertTrue(message.getPayloadString().startsWith("my message "));
			assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
			assertFalse(message.isDuplicate());
			assertFalse(message.isRetain());
		}

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testPublish_Qos1_Retain() throws Exception {

		// connect a client and generate the message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient16", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.publish(new PublishMessage("my/topic6", QoS.AT_LEAST_ONCE, "my message", true));
		verify(listener, timeout(5000)).published(same(client), isA(PublishMessage.class));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the messages
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient15", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic6", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// verify the messages
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());

		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/topic6", message.getTopic());
		assertEquals("my message", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		// remove the message
		client2.publish(new PublishMessage("my/topic6", QoS.AT_LEAST_ONCE, new byte[0], true));
		verify(listener2, timeout(5000)).published(same(client2), isA(PublishMessage.class));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testPublish_Qos0_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient15", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect a client and generate the messages
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient16", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		for (int i = 0; i < 10; i++) {
			client.publish(new PublishMessage("my/topic7", QoS.AT_MOST_ONCE, "my message " + i));
		}

		// verify the messages
		verify(listener2, timeout(5000).times(10)).publishReceived(same(client2), messageCaptor.capture());

		for (PublishMessage message : messageCaptor.getAllValues()) {
			message.ack();
			assertEquals("my/topic7", message.getTopic());
			assertTrue(message.getPayloadString().startsWith("my message "));
			assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
			assertFalse(message.isDuplicate());
			assertFalse(message.isRetain());
		}

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	// this test can take up to 30 seconds to run
	@Test
	public void testPublish_DuplicateMessageReceived() throws Exception {

		// connect and subscribe a client to get the message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient18", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect a client and generate the message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient17", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.publish(new PublishMessage("my/topic7", QoS.AT_LEAST_ONCE, "my message"));
		verify(listener, timeout(5000)).published(same(client), isA(PublishMessage.class));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the messages
		verify(listener2, timeout(30000).times(2)).publishReceived(same(client2), messageCaptor.capture());

		PublishMessage message = messageCaptor.getAllValues().get(0);
		assertEquals("my/topic7", message.getTopic());
		assertEquals("my message", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		message = messageCaptor.getAllValues().get(1);
		message.ack();
		assertEquals("my/topic7", message.getTopic());
		assertEquals("my message", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertTrue(message.isDuplicate());
		assertFalse(message.isRetain());

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testClose() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient19", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testDisconnect() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient20", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testConnectionLost_FirstReconnectSucceeds() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnectionLost_NotFirstReconnectSucceeds() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnectionLost_AllReconnectsFail() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testSendWhileReconnectionInProgress() throws Exception {

		fail("not implemented");
	}
}