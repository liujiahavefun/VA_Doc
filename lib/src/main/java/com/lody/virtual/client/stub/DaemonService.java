package com.lody.virtual.client.stub;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;


/**
 * @author Lody
 *
 */

/**
 * liujia: 为了保活VAService进程。。。
 * 现在后台服务很容易被杀(Android8.0以后后台服务职能在后台存在5s)，而前台则不受影响
 */
public class DaemonService extends Service {

    private static final int NOTIFY_ID = 1001;

	public static void startup(Context context) {
		context.startService(new Intent(context, DaemonService.class));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		startup(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
        startService(new Intent(this, InnerService.class));
        startForeground(NOTIFY_ID, new Notification());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//liujia: START_STICKY是service被kill掉后自动重新创建
		return START_STICKY;
	}

	public static final class InnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(NOTIFY_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
	}


}
