package org.jline.terminal.impl.jna.linux;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface LibNotify extends Library {

    /**
     * Initialized libnotify. This must be called before any other functions.
     * 
     * @param appName The name of the application initializing libnotify.
     * @return TRUE if successful, or FALSE on error.
     */
    int notify_init(String appName);

    /**
     * Uninitialized libnotify.
     * This should be called when the program no longer needs libnotify for the rest of its lifecycle, typically just before exitting.
     */
    void notify_uninit();

    /**
     * Creates a new NotifyNotification. The summary text is required, but all other parameters are optional.
     * @param summary      The required summary text.
     * @param body  The optional body text.    [allow-none]
     * @param icon   The optional icon theme icon name or filename.    [allow-none]
     * @return      The new NotifyNotification.
     */
    Pointer notify_notification_new(String summary, String body, String icon);

    /**
     * Tells the notification server to display the notification on the screen.
     * 
     * @param notification      The notification.
     * @param error      The returned error information.
     * @return      TRUE if successful. On error, this will return FALSE and set error .
     */
    int notify_notification_show(Pointer notification, Pointer error);
}
