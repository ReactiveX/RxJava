package rx;

import org.junit.Assert;
import org.junit.Test;

public class NotificationTest {
	
	@Test
	public void testOnNextIntegerNotificationDoesNotEqualNullNotification(){
		final Notification<Integer> integerNotification = Notification.createOnNext(1);
		final Notification<Integer> nullNotification = Notification.createOnNext(null);
		Assert.assertFalse(integerNotification.equals(nullNotification));
	}
	
	@Test
	public void testOnNextNullNotificationDoesNotEqualIntegerNotification(){
		final Notification<Integer> integerNotification = Notification.createOnNext(1);
		final Notification<Integer> nullNotification = Notification.createOnNext(null);
		Assert.assertFalse(nullNotification.equals(integerNotification));
	}
	
	@Test
	public void testOnNextIntegerNotificationsWhenEqual(){
		final Notification<Integer> integerNotification = Notification.createOnNext(1);
		final Notification<Integer> integerNotification2 = Notification.createOnNext(1);
		Assert.assertTrue(integerNotification.equals(integerNotification2));
	}
	
	@Test
	public void testOnNextIntegerNotificationsWhenNotEqual(){
		final Notification<Integer> integerNotification = Notification.createOnNext(1);
		final Notification<Integer> integerNotification2 = Notification.createOnNext(2);
		Assert.assertFalse(integerNotification.equals(integerNotification2));
	}
	
	@Test
	public void testOnErrorIntegerNotificationDoesNotEqualNullNotification(){
		final Notification<Integer> integerNotification = Notification.createOnError(new Exception());
		final Notification<Integer> nullNotification = Notification.createOnError(null);
		Assert.assertFalse(integerNotification.equals(nullNotification));
	}
	
	@Test
	public void testOnErrorNullNotificationDoesNotEqualIntegerNotification(){
		final Notification<Integer> integerNotification = Notification.createOnError(new Exception());
		final Notification<Integer> nullNotification = Notification.createOnError(null);
		Assert.assertFalse(nullNotification.equals(integerNotification));
	}

	@Test
	public void testOnErrorIntegerNotificationsWhenEqual(){
		final Exception exception = new Exception();
		final Notification<Integer> onErrorNotification = Notification.createOnError(exception);
		final Notification<Integer> onErrorNotification2 = Notification.createOnError(exception);
		Assert.assertTrue(onErrorNotification.equals(onErrorNotification2));
	}
	
	@Test
	public void testOnErrorIntegerNotificationWhenNotEqual(){
		final Notification<Integer> onErrorNotification = Notification.createOnError(new Exception());
		final Notification<Integer> onErrorNotification2 = Notification.createOnError(new Exception());
		Assert.assertFalse(onErrorNotification.equals(onErrorNotification2));
	}
}
