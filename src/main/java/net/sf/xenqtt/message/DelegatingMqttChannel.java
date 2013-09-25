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
package net.sf.xenqtt.message;

import java.nio.channels.Selector;
import java.util.List;

/**
 * Wrapper that delegates directly to another {@link MqttChannel} implementation. This is used to allow changing the delegate to easily support things like
 * client reconnection.
 */
final class DelegatingMqttChannel implements MqttChannel {

	/**
	 * The channel to delegate to
	 */
	MqttChannel delegate;

	DelegatingMqttChannel(MqttChannel delegate) {
		this.delegate = delegate;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#deregister()
	 */
	@Override
	public void deregister() {

		delegate.deregister();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#register(java.nio.channels.Selector, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public boolean register(Selector selector, MessageHandler handler) {

		return delegate.register(selector, handler);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#finishConnect()
	 */
	@Override
	public boolean finishConnect() {

		return delegate.finishConnect();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#read(long)
	 */
	@Override
	public boolean read(long now) {

		return delegate.read(now);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#send(net.sf.xenqtt.message.MqttMessage, net.sf.xenqtt.message.BlockingCommand)
	 */
	@Override
	public boolean send(MqttMessage message, BlockingCommand<MqttMessage> blockingCommand) {

		return delegate.send(message, blockingCommand);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#write(long)
	 */
	@Override
	public boolean write(long now) {

		return delegate.write(now);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#close()
	 */
	@Override
	public void close() {

		delegate.close();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isOpen()
	 */
	@Override
	public boolean isOpen() {

		return delegate.isOpen();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnected()
	 */
	@Override
	public boolean isConnected() {

		return delegate.isConnected();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnectionPending()
	 */
	@Override
	public boolean isConnectionPending() {

		return delegate.isConnectionPending();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#houseKeeping(long)
	 */
	@Override
	public long houseKeeping(long now) {

		return delegate.houseKeeping(now);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#sendQueueDepth()
	 */
	@Override
	public int sendQueueDepth() {

		return delegate.sendQueueDepth();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#inFlightMessageCount()
	 */
	@Override
	public int inFlightMessageCount() {

		return delegate.inFlightMessageCount();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#cancelBlockingCommands()
	 */
	@Override
	public void cancelBlockingCommands() {

		delegate.cancelBlockingCommands();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getUnsentMessages()
	 */
	@Override
	public List<MqttMessage> getUnsentMessages() {

		return delegate.getUnsentMessages();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getRemoteAddress()
	 */
	@Override
	public String getRemoteAddress() {

		return delegate.getRemoteAddress();
	}
}