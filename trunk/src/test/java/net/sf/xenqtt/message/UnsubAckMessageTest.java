package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class UnsubAckMessageTest {

	ByteBuffer buf = ByteBuffer.wrap(new byte[] { (byte) 0xb0, 0x02, 0x00, 0x01 });

	UnsubAckMessage msg;

	@Test
	public void testCtor_Receive() {

		msg = new UnsubAckMessage(buf);
		assertMsg();
	}

	@Test
	public void testCtor_Send() {
		msg = new UnsubAckMessage(1);
		assertMsg();
	}

	@Test
	public void testMessageIds() throws Exception {

		for (int i = 0; i < 0xffff; i++) {
			buf = ByteBuffer.wrap(new byte[] { (byte) 0xb0, 0x02, (byte) (i >> 8), (byte) (i & 0xff) });
			msg = new UnsubAckMessage(i);
			assertEquals(buf, msg.getBuffer());
			assertEquals(i, msg.getMessageId());
		}

		for (int i = 0; i < 0xffff; i++) {
			buf = ByteBuffer.wrap(new byte[] { (byte) 0xb0, 0x02, (byte) (i >> 8), (byte) (i & 0xff) });
			msg = new UnsubAckMessage(buf);
			assertEquals(i, msg.getMessageId());
		}
	}

	private void assertMsg() {

		assertEquals(buf, msg.getBuffer());

		assertEquals(MessageType.UNSUBACK, msg.getMessageType());
		assertFalse(msg.isDuplicate());
		assertEquals(QoS.AT_MOST_ONCE, msg.getQoS());
		assertFalse(msg.isRetain());
		assertEquals(2, msg.getRemainingLength());

		assertEquals(1, msg.getMessageId());
	}
}
