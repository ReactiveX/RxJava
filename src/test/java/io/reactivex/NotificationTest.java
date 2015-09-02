/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

import java.util.Optional;

import org.junit.*;

public class NotificationTest {
	
	@Test(expected = NullPointerException.class)
	public void testOnNextIntegerNotificationDoesNotEqualNullNotification(){
		final Try<Optional<Integer>> integerNotification = Notification.next(1);
		final Try<Optional<Integer>> nullNotification = Notification.next(null);
		Assert.assertFalse(integerNotification.equals(nullNotification));
	}
	
	@Test(expected = NullPointerException.class)
	public void testOnNextNullNotificationDoesNotEqualIntegerNotification(){
		final Try<Optional<Integer>> integerNotification = Notification.next(1);
		final Try<Optional<Integer>> nullNotification = Notification.next(null);
		Assert.assertFalse(nullNotification.equals(integerNotification));
	}
	
	@Test
	public void testOnNextIntegerNotificationsWhenEqual(){
		final Try<Optional<Integer>> integerNotification = Notification.next(1);
		final Try<Optional<Integer>> integerNotification2 = Notification.next(1);
		Assert.assertTrue(integerNotification.equals(integerNotification2));
	}
	
	@Test
	public void testOnNextIntegerNotificationsWhenNotEqual(){
		final Try<Optional<Integer>> integerNotification = Notification.next(1);
		final Try<Optional<Integer>> integerNotification2 = Notification.next(2);
		Assert.assertFalse(integerNotification.equals(integerNotification2));
	}
	
	@Test
	public void testOnErrorIntegerNotificationDoesNotEqualNullNotification(){
		final Try<Optional<Integer>> integerNotification = Notification.error(new Exception());
		final Try<Optional<Integer>> nullNotification = Notification.error(null);
		Assert.assertFalse(integerNotification.equals(nullNotification));
	}
	
	@Test
	public void testOnErrorNullNotificationDoesNotEqualIntegerNotification(){
		final Try<Optional<Integer>> integerNotification = Notification.error(new Exception());
		final Try<Optional<Integer>> nullNotification = Notification.error(null);
		Assert.assertFalse(nullNotification.equals(integerNotification));
	}

	@Test
	public void testOnErrorIntegerNotificationsWhenEqual(){
		final Exception exception = new Exception();
		final Try<Optional<Integer>> onErrorNotification = Notification.error(exception);
		final Try<Optional<Integer>> onErrorNotification2 = Notification.error(exception);
		Assert.assertTrue(onErrorNotification.equals(onErrorNotification2));
	}
	
	@Test
	public void testOnErrorIntegerNotificationWhenNotEqual(){
		final Try<Optional<Integer>> onErrorNotification = Notification.error(new Exception());
		final Try<Optional<Integer>> onErrorNotification2 = Notification.error(new Exception());
		Assert.assertFalse(onErrorNotification.equals(onErrorNotification2));
	}
}