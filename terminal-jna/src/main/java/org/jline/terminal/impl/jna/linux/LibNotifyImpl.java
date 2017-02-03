package org.jline.terminal.impl.jna.linux;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * https://wiki.archlinux.org/index.php/Desktop_notifications
 */
public class LibNotifyImpl {
    private LibNotify libNotify;
    
    public LibNotifyImpl(){
        this("JLine terminal");
    }
    
    public LibNotifyImpl(String appName){
        libNotify = (LibNotify) Native.loadLibrary("libnotify.so.4", LibNotify.class);
        if (libNotify.notify_init(appName) == 0) {
            throw new IllegalStateException("notify_init failed");
        }
    }
    
    public void close(){
        libNotify.notify_uninit();
    }

    public void showNotification(String appName, String message, String pathIcon) {
        Pointer notification = libNotify.notify_notification_new(appName, message, pathIcon);
        libNotify.notify_notification_show(notification, null);
    }

}
