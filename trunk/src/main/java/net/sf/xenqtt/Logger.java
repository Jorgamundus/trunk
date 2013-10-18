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
package net.sf.xenqtt;

/**
 * Specifies a type that manages the logging of disparate events that are generated by components within Xenqtt.
 */
interface Logger {

	/**
	 * Initialize the logger.
	 */
	void init();

	/**
	 * Shut down the logger
	 */
	void shutdown();

	/**
	 * Log an event at a particular logging level.
	 * 
	 * @param levelFlag
	 *            The flag that specifies which level to log at
	 * @param message
	 *            The message to log
	 * @param parameters
	 *            A variable argument array of parameters used to replace string format specifiers included in the {@code message}
	 */
	void log(int levelFlag, String message, Object... parameters);

	/**
	 * Log an event at a particular logging level.
	 * 
	 * @param levelFlag
	 *            The flag that specifies which level to log at
	 * @param t
	 *            A {@link Throwable} that is associated with the event being logged
	 * @param message
	 *            The message to log
	 * @param parameters
	 *            A variable argument array of parameters used to replace string format specifiers included in the {@code message}
	 */
	void log(int levelFlag, Throwable t, String message, Object... parameters);

}