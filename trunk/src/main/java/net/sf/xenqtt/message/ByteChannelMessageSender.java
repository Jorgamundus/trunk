package net.sf.xenqtt.message;

import java.nio.channels.WritableByteChannel;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Writes {@link MqttMessage}s to a {@link WritableByteChannel}.
 */
public class ByteChannelMessageSender implements MessageSender {

	private final WritableByteChannel channel;
	private final Queue<MqttMessage> pending = new ArrayDeque<MqttMessage>();

	private MqttMessage currentMessage;

	/**
	 * @param channel
	 *            Channel to write messages to
	 */
	public ByteChannelMessageSender(WritableByteChannel channel) {
		this.channel = channel;
	}

	/**
	 * @see net.sf.xenqtt.message.MessageSender#enqueue(net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public boolean enqueue(MqttMessage message) {

		if (currentMessage == null) {
			currentMessage = message;
			// FIXME [jim] - maybe just call send? single threaded after all
			return true;
		}

		pending.offer(message);
		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.MessageSender#send()
	 */
	@Override
	public boolean send() {
		// FIXME [jim] - write messages
		return false;
	}
}
