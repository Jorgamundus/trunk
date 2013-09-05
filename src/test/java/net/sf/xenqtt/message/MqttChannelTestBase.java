package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import net.sf.xenqtt.mock.MockMessageHandler;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class MqttChannelTestBase<C extends AbstractMqttChannel, B extends AbstractMqttChannel> {

	ServerSocketChannel ssc;
	Selector selector;
	int port;

	C clientChannel;
	B brokerChannel;

	MockMessageHandler clientHandler = new MockMessageHandler();
	MockMessageHandler brokerHandler = new MockMessageHandler();

	long now = System.currentTimeMillis();

	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		ServerSocket serverSocket = ssc.socket();
		serverSocket.bind(new InetSocketAddress(0));
		port = serverSocket.getLocalPort();

		selector = Selector.open();

		ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	/**
	 * reads and writes until the specified number of client and broker messages are received or until a channel is closed
	 * 
	 * @return null if the requested message counts were received. Otherwise, the connection that was closed (read op hit end of stream)
	 */
	MqttChannel readWrite(int clientMessageCount, int brokerMessageCount) throws Exception {

		clientHandler.clearMessages();
		brokerHandler.clearMessages();

		while (brokerHandler.messageCount() < brokerMessageCount || clientHandler.messageCount() < clientMessageCount) {

			selector.select();
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				MqttChannel channel = (MqttChannel) key.attachment();
				if (key.isReadable()) {
					if (!channel.read(now)) {
						return channel;
					}
				}
				if (key.isWritable()) {
					if (!channel.write(now)) {
						return channel;
					}
				}
				iter.remove();
			}
		}

		return null;
	}

	/**
	 * Closes both client and broker connections
	 */
	void closeConnection() {

		clientChannel.close();
		clientHandler.assertChannelClosedCount(1);
		assertFalse(clientChannel.isConnected());
		assertFalse(clientChannel.isOpen());
		brokerChannel.close();
		brokerHandler.assertChannelClosedCount(1);
		assertFalse(brokerChannel.isConnected());
		assertFalse(brokerChannel.isOpen());
	}

	/**
	 * Establishes a socket connection. Creates the client and/or broker channels if they do not exist
	 */
	void establishConnection() throws Exception {

		if (clientChannel == null) {
			clientChannel = newClientChannel();
		}

		assertTrue(clientChannel.isOpen());
		assertTrue(clientChannel.isConnectionPending());

		clientHandler.assertChannelOpenedCount(0);
		brokerHandler.assertChannelOpenedCount(0);

		assertEquals(2, selector.keys().size());

		boolean clientConnected = false;
		while (brokerChannel == null || !clientConnected) {
			selector.select();

			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				if (key.isAcceptable()) {
					assertSame(ssc, key.channel());
					assertTrue(key.isAcceptable());
					SocketChannel brokerSocketChannel = ssc.accept();
					brokerChannel = newBrokerChannel(brokerSocketChannel);
					assertFalse(brokerChannel.isConnectionPending());
					assertTrue(brokerChannel.isOpen());
					brokerHandler.assertChannelOpenedCount(1);
					key.cancel();
					ssc.close();
				} else if (key.isConnectable()) {
					assertSame(clientChannel, key.attachment());
					assertFalse(key.channel().isBlocking());
					assertTrue(key.channel().isRegistered());
					clientHandler.assertChannelOpenedCount(0);
					assertTrue(clientChannel.isConnectionPending());
					assertTrue(clientChannel.finishConnect());
					assertFalse(clientChannel.isConnectionPending());
					clientHandler.assertChannelOpenedCount(1);
					assertTrue(clientChannel.isOpen());
					clientConnected = true;
				}
				iter.remove();
			}
		}

		assertFalse(clientChannel.isConnected());
		assertFalse(brokerChannel.isConnected());

		clientHandler.assertChannelOpenedCount(1);
		brokerHandler.assertChannelOpenedCount(1);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);
	}

	/**
	 * Sends a {@link ConnectMessage} from the client and a {@link ConnAckMessage} reply and waits for both to complete
	 */
	void connect() throws Exception {

		ConnectMessage connMsg = new ConnectMessage("abc", false, 10000);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);

		clientChannel.send(now, connMsg);
		assertNull(readWrite(0, 1));
		brokerHandler.assertMessages(connMsg);

		brokerChannel.send(now, ackMsg);
		assertNull(readWrite(1, 0));
		clientHandler.assertMessages(ackMsg);

		assertTrue(clientChannel.isConnected());
		assertTrue(brokerChannel.isConnected());
	}

	/**
	 * Sends a {@link DisconnectMessage} from the client to the broker
	 */
	void disconnect() throws Exception {

		clientChannel.send(now, new DisconnectMessage());
		readWrite(0, 1);

		assertFalse(clientChannel.isConnected());
		assertFalse(brokerChannel.isConnected());
		assertFalse(clientChannel.isOpen());
		assertFalse(brokerChannel.isOpen());
	}

	/**
	 * @return a new broker channel
	 */
	abstract B newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception;

	/**
	 * @return A new client channel
	 */
	abstract C newClientChannel() throws Exception;

	final class TestChannel extends AbstractMqttChannel {

		private final MockMessageHandler messageHandler;
		Exception exceptionToThrow;
		long lastReceived;
		boolean connectedCalled;
		boolean disconnectedCalled;
		long pingIntervalMillis;

		public TestChannel(SocketChannel channel, MockMessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
			super(channel, handler, selector, messageResendIntervalMillis);
			this.messageHandler = handler;
		}

		public TestChannel(String host, int port, MockMessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
			super(host, port, handler, selector, messageResendIntervalMillis);
			this.messageHandler = handler;
		}

		@Override
		void connected(long pingIntervalMillis) {
			connectedCalled = true;
			this.pingIntervalMillis = pingIntervalMillis;
		}

		@Override
		void disconnected() {
			disconnectedCalled = true;
		}

		@Override
		void pingReq(long now, PingReqMessage message) throws Exception {
			messageHandler.pingReq(this, message);
		}

		@Override
		void pingResp(long now, PingRespMessage message) throws Exception {
			messageHandler.pingResp(this, message);
		}

		@Override
		long keepAlive(long now, long lastMessageReceived) throws Exception {

			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			lastReceived = lastMessageReceived;

			return 25000;
		}
	}
}