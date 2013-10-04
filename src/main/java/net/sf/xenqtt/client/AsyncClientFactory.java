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

/**
 * Used to create multiple "sibling" {@link AsyncMqttClient async clients} that share an {@link Executor}, broker URI, etc.
 */
public interface AsyncClientFactory {

	/**
	 * Stops this factory. Closes all open connections to the broker. Blocks until shutdown is complete. Any other methods called after this have unpredictable
	 * results.
	 */
	void shutdown();

	/**
	 * Creates an asynchronous {@link MqttClient client}. You may only use this method if the factory was constructed to create asynchronous clients.
	 * 
	 * @param asyncClientListener
	 *            Handles events from this client's channel. Use {@link AsyncClientListener#NULL_LISTENER} if you don't want to receive messages or be notified
	 *            of events.
	 * 
	 * @return A new asynchronous {@link MqttClient client}
	 * 
	 * @throws IllegalStateException
	 *             If this factory was constructed to create synchronous clients and not asynchronous clients.
	 */
	MqttClient newAsyncClient(AsyncClientListener asyncClientListener) throws IllegalStateException;
}