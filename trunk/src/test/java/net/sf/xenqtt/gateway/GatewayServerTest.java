package net.sf.xenqtt.gateway;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttChannelImpl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class GatewayServerTest {

	volatile boolean failCreateSession;
	volatile MqttChannel newSessionChannel;
	volatile ConnectMessage newSessionMsg;

	volatile MqttChannel addedSessionChannel;
	volatile ConnectMessage addedSessionMsg;

	volatile @Mock GatewaySession session;

	volatile Selector selector;

	Thread serverThread;
	volatile TestServer server;
	volatile Exception ex;

	@Before
	public void setup() throws IOException {

		MockitoAnnotations.initMocks(this);
		selector = Selector.open();
		server = new TestServer();

		serverThread = new Thread() {
			@Override
			public void run() {

				try {
					server.run(23416);
				} catch (IOException e) {
					ex = e;
				}
			}
		};

		serverThread.start();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				addedSessionChannel = (MqttChannel) invocation.getArguments()[0];
				addedSessionMsg = (ConnectMessage) invocation.getArguments()[1];
				selector.wakeup();

				return null;
			}
		}).when(session).addClient(any(MqttChannel.class), any(ConnectMessage.class));
	}

	@Test
	public void testStop() throws Exception {

		server.stop();
		serverThread.join();
	}

	@Test
	public void testNewClient_SessionCreationFails() throws Exception {

		failCreateSession = true;

		MqttChannel clientChannel = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel, true, connectMessage);

		assertFalse(clientChannel.isOpen());

		clientChannel.close();
		server.stop();
	}

	@Test
	public void testNewClient_SessionAdditionFails() throws Exception {

		doThrow(new RuntimeException()).when(session).addClient(any(MqttChannel.class), any(ConnectMessage.class));

		MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel1, true, connectMessage1);

		MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage2 = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel2, false, connectMessage2);

		assertTrue(clientChannel1.isOpen());
		assertFalse(clientChannel2.isOpen());

		clientChannel1.close();
		clientChannel2.close();
		server.stop();
	}

	@Test
	public void testNewClient_NewSession_NoOtherSessions() throws Exception {

		MqttChannel clientChannel = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel, true, connectMessage);

		clientChannel.close();
		server.stop();

		assertNotSame(connectMessage, newSessionMsg);
		assertEquals(connectMessage, newSessionMsg);
		assertNull(addedSessionChannel);
		assertNull(addedSessionMsg);
	}

	@Test
	public void testNewClient_NewSession_OtherSessions() throws Exception {

		// establish first session with client id 123
		MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel1, true, connectMessage1);

		assertNotSame(connectMessage1, newSessionMsg);
		assertEquals(connectMessage1, newSessionMsg);

		MqttChannel firstChannel = newSessionChannel;

		// establish first session with client id 456
		newSessionChannel = null;
		newSessionMsg = null;

		MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage2 = new ConnectMessage("456", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel2, true, connectMessage2);

		assertNotEquals(firstChannel, newSessionChannel);
		assertNotEquals(connectMessage1, connectMessage2);

		assertNotSame(connectMessage2, newSessionMsg);
		assertEquals(connectMessage2, newSessionMsg);

		// clean up
		clientChannel1.close();
		clientChannel2.close();

		server.stop();
		assertNull(addedSessionChannel);
		assertNull(addedSessionMsg);
	}

	@Test
	public void testNewClient_ExistingSession() throws Exception {

		// establish first session with client id 123
		MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel1, true, connectMessage1);

		assertNotSame(connectMessage1, newSessionMsg);
		assertEquals(connectMessage1, newSessionMsg);

		MqttChannel firstChannel = newSessionChannel;

		// establish another session with client id 456
		newSessionChannel = null;
		newSessionMsg = null;

		MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
		ConnectMessage connectMessage2 = new ConnectMessage("123", false, 0, "abc", "123");

		waitForConnectToSession(clientChannel2, false, connectMessage2);

		assertNotEquals(firstChannel, addedSessionChannel);
		assertNotSame(connectMessage1, connectMessage2);
		assertEquals(connectMessage1, connectMessage2);

		assertNotSame(connectMessage2, addedSessionMsg);
		assertEquals(connectMessage2, addedSessionMsg);

		// clean up
		clientChannel1.close();
		clientChannel2.close();

		server.stop();
		assertNull(newSessionChannel);
		assertNull(newSessionMsg);
	}

	private void waitForConnectToSession(MqttChannel clientChannel, boolean newSession, ConnectMessage connectMessage) throws Exception {

		clientChannel.send(connectMessage);

		while ((newSession && newSessionChannel == null) || (!newSession && addedSessionChannel == null)) {
			selector.select();
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
			while (iter.hasNext()) {

				SelectionKey key = iter.next();
				MqttChannel channel = (MqttChannel) key.attachment();

				if (key.isConnectable()) {
					channel.finishConnect();
				}
				if (key.isWritable()) {
					channel.write();
				}
				if (key.isReadable()) {
					if (!channel.read()) {
						channel.close();
						return;
					}
				}
				iter.remove();
			}
		}
	}

	private class TestServer extends GatewayServer {

		public TestServer() throws IOException {
			super(null);
		}

		@Override
		GatewaySession createSession(MqttChannel channel, ConnectMessage message) throws IOException {

			if (failCreateSession) {
				throw new RuntimeException();
			}

			newSessionChannel = channel;
			newSessionMsg = message;
			selector.wakeup();

			return session;
		}
	}
}
