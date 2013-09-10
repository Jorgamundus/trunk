package net.sf.xenqtt.client;

import net.sf.xenqtt.message.QoS;

/**
 * An MQTT topic subscription
 */
public final class Subscription {

	private final String topic;
	private final QoS qos;

	/**
	 * @param topic
	 *            The topic being subscribed to. This can include wildcards:
	 *            <ul>
	 *            <li>'+': Matches a single level in the topic. foo/+ would match foo/bar but not foo/a/b or foo/a/b/c. foo/+/+/c would match foo/a/b/c and
	 *            foo/d/g/c but not foo/a/c</li>
	 *            <li>'#': Matches the rest of the topic. Must be the last character in the topic. foo/# would match foo/bar, foo/a/b/c, etc</li>
	 *            </ul>
	 * @param qos
	 *            The QoS for the topic. When a subscription request is sent to the broker this is the requested qos. When the subscription acknowledgment is
	 *            received from the broker this is the granted qos.
	 */
	public Subscription(String topic, QoS qos) {
		this.topic = topic;
		this.qos = qos;
	}

	/**
	 * @return The topic being subscribed to. This can include wildcards:
	 *         <ul>
	 *         <li>'+': Matches a single level in the topic. foo/+ would match foo/bar but not foo/a/b or foo/a/b/c. foo/+/+/c would match foo/a/b/c and
	 *         foo/d/g/c but not foo/a/c</li>
	 *         <li>'#': Matches the rest of the topic. Must be the last character in the topic. foo/# would match foo/bar, foo/a/b/c, etc</li>
	 *         </ul>
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * @return The QoS for the topic. When a subscription request is sent to the broker this is the requested qos. When the subscription acknowledgment is
	 *         received from the broker this is the granted qos.
	 */
	public QoS getQos() {
		return qos;
	}
}