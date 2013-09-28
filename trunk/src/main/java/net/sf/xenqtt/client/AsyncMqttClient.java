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
package net.sf.xenqtt.client;

import java.util.concurrent.Executor;

import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttMessage;

/**
 * An {@link MqttClient} that handles interactions with the MQTT broker in an asynchronous fashion.
 */
public final class AsyncMqttClient extends AbstractMqttClient {

	/**
	 * Constructs an instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles events from this client. Use {@link AsyncClientListener#NULL_LISTENER} if you don't want to receive messages or be notified of events.
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. The minimum allowable value for this setting is 2
	 */
	public AsyncMqttClient(String brokerUri, AsyncClientListener listener, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize,
			int connectTimeoutSeconds, int messageResendIntervalSeconds) {
		super(XenqttUtil.validateNotEmpty("brokerUri", brokerUri), //
				XenqttUtil.validateNotNull("listener", listener), //
				XenqttUtil.validateNotNull("reconnectionStrategy", reconnectionStrategy), //
				XenqttUtil.validateGreaterThan("messageHandlerThreadPoolSize", messageHandlerThreadPoolSize, 0), //
				XenqttUtil.validateGreaterThanOrEqualTo("connectTimeoutSeconds", connectTimeoutSeconds, 0), //
				XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 2));
	}

	/**
	 * Constructs an instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles events from this client. Use {@link AsyncClientListener#NULL_LISTENER} if you don't want to receive messages or be notified of events.
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. The minimum allowable value for this setting is 2
	 */
	public AsyncMqttClient(String brokerUri, AsyncClientListener listener, ReconnectionStrategy reconnectionStrategy, Executor executor,
			int connectTimeoutSeconds, int messageResendIntervalSeconds) {
		super(XenqttUtil.validateNotEmpty("brokerUri", brokerUri), //
				XenqttUtil.validateNotNull("listener", listener), //
				XenqttUtil.validateNotNull("reconnectionStrategy", reconnectionStrategy), //
				XenqttUtil.validateNotNull("executor", executor), //
				XenqttUtil.validateGreaterThanOrEqualTo("connectTimeoutSeconds", connectTimeoutSeconds, 0), //
				XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 2));
	}
}