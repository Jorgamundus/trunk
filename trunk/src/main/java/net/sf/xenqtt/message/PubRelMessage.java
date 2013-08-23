package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PUBREL message is the response either from a publisher to a PUBREC message from the server, or from the server to a PUBREC message from a subscriber. It is
 * the third message in the QoS 2 protocol flow.
 */
public final class PubRelMessage extends IdentifiableMqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PubRelMessage(ByteBuffer buffer) {
		super(buffer, 2);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubRelMessage(int messageId) {
		super(MessageType.PUBREL, false, QoS.AT_LEAST_ONCE, false, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}
}