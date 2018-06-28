package com.lody.virtual.client.hook.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.fixer.ActivityFixer;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.ActivityClientRecord;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.interfaces.IUiCallback;

import mirror.android.app.ActivityThread;

/**
 * @author Lody
 */

/**
 * liujia: 这个delegate了系统主进程的ActivityThread.currentActivityThread()返回的ActivityThread对象里的mInstrumentation对象
 * 对此对象hook，可以控制activity application的生命周期
 */
public final class AppInstrumentation extends InstrumentationDelegate implements IInjector {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation gDefault;

    private AppInstrumentation(Instrumentation base) {
        super(base);
    }

    public static AppInstrumentation getDefault() {
        if (gDefault == null) {
            synchronized (AppInstrumentation.class) {
                if (gDefault == null) {
                    gDefault = create();
                }
            }
        }
        return gDefault;
    }

    private static AppInstrumentation create() {
        //liujia: VirtualCore.mainThread()返回的是VirtualCore启动时调的ActivityThread.currentActivityThread()返回的ActivityThread实例
        //然后，get()返回的instrumentation实际上是此ActivityThread实例里的私有的(但被mirror里的RefObject导出为public了)mInstrumentation对象
        //注意下面的instanceof判断，在inject()之前，instanceof判断一定不成立，因为返回的对象是Instrumentation类型而不是AppInstrumentation
        //要new 一个AppInstrumentation对象
        Instrumentation instrumentation = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        if (instrumentation instanceof AppInstrumentation) {
            return (AppInstrumentation) instrumentation;
        }
        return new AppInstrumentation(instrumentation);
    }


    @Override
    public void inject() throws Throwable {
        //liujia: 获取原来的instrumentation作为base，然后将自己set进去
        base = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        ActivityThread.mInstrumentation.set(VirtualCore.mainThread(), this);
    }

    @Override
    public boolean isEnvBad() {
        //liujia: 说明inject没成功....
        return !(ActivityThread.mInstrumentation.get(VirtualCore.mainThread()) instanceof AppInstrumentation);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        // liujia: afterActivityCreate...
        VirtualCore.get().getComponentDelegate().beforeActivityCreate(activity);
        // liujia: 调用自己的VActivityManager...
        IBinder token = mirror.android.app.Activity.mToken.get(activity);
        ActivityClientRecord r = VActivityManager.get().getActivityRecord(token);
        // 替换 Activity 对象
        if (r != null) {
            r.activity = activity;
        }
        // liujia: 移花接木？
        ContextFixer.fixContext(activity);
        ActivityFixer.fixActivity(activity);
        ActivityInfo info = null;
        if (r != null) {
            info = r.info;
        }
        // 设置主题和屏幕纵横控制
        if (info != null) {
            if (info.theme != 0) {
                activity.setTheme(info.theme);
            }
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    && info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                activity.setRequestedOrientation(info.screenOrientation);
            }
        }
        // liujia: 做完后，调用基类的实现
        super.callActivityOnCreate(activity, icicle);
        // liujia: afterActivityCreate...
        VirtualCore.get().getComponentDelegate().afterActivityCreate(activity);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        //liujia: 同上，顺序依次是调用beforeActivityXXX 调用VActivityManager 调用基类实现 调用afterActivityXXX
        VirtualCore.get().getComponentDelegate().beforeActivityResume(activity);
        VActivityManager.get().onActivityResumed(activity);
        super.callActivityOnResume(activity);
        VirtualCore.get().getComponentDelegate().afterActivityResume(activity);

        //liujia: 下面的是什么？可能要看看"_VA_|_sender_"和"_VA_|_ui_callback_"这两个东西设置进去的上下文。。。
        Intent intent = activity.getIntent();
        if (intent != null) {
            Bundle bundle = intent.getBundleExtra("_VA_|_sender_");
            if (bundle != null) {
                IBinder callbackToken = BundleCompat.getBinder(bundle, "_VA_|_ui_callback_");
                IUiCallback callback = IUiCallback.Stub.asInterface(callbackToken);
                if (callback != null) {
                    try {
                        callback.onAppOpened(VClientImpl.get().getCurrentPackage(), VUserHandle.myUserId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    @Override
    public void callActivityOnDestroy(Activity activity) {
        VirtualCore.get().getComponentDelegate().beforeActivityDestroy(activity);
        super.callActivityOnDestroy(activity);
        VirtualCore.get().getComponentDelegate().afterActivityDestroy(activity);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        VirtualCore.get().getComponentDelegate().beforeActivityPause(activity);
        super.callActivityOnPause(activity);
        VirtualCore.get().getComponentDelegate().afterActivityPause(activity);
    }


    @Override
    public void callApplicationOnCreate(Application app) {
        super.callApplicationOnCreate(app);
    }
}
