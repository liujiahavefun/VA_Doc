package com.lody.virtual.client.ipc;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.ServiceCache;
import com.lody.virtual.server.interfaces.IServiceFetcher;

/**
 * @author Lody
 */

/**
 * liujia: 封装了获取远程服务的过程，参考ServiceFetcher服务
 */
public class ServiceManagerNative {

    public static final String PACKAGE = "package";
    public static final String ACTIVITY = "activity";
    public static final String USER = "user";
    public static final String APP = "app";
    public static final String ACCOUNT = "account";
    public static final String JOB = "job";
    public static final String NOTIFICATION = "notification";
    public static final String VS = "vs";
    public static final String DEVICE = "device";

    public static final String SERVICE_DEF_AUTH = "virtual.service.BinderProvider";
    private static final String TAG = ServiceManagerNative.class.getSimpleName();
    public static String SERVICE_CP_AUTH = "virtual.service.BinderProvider";

     // 通过 ContentProvider 传递一个
    private static IServiceFetcher sFetcher;

    // liujia: 通过provider的方式获取ServiceFetcher服务
    private static IServiceFetcher getServiceFetcher() {
        // liujia: lazy initialize! 注意参考linkBinderDied()设置的死亡代理为何没有重连
        // 这样，当去获取服务的时候，如果没初始化或者已经死亡了，则重连并获取服务对象
        if (sFetcher == null || !sFetcher.asBinder().isBinderAlive()) {
            synchronized (ServiceManagerNative.class) {
                Context context = VirtualCore.get().getContext();
                Bundle response = new ProviderCall.Builder(context, SERVICE_CP_AUTH).methodName("@").call();
                if (response != null) {
                    IBinder binder = BundleCompat.getBinder(response, "_VA_|_binder_");
                    linkBinderDied(binder);
                    sFetcher = IServiceFetcher.Stub.asInterface(binder);
                }
            }
        }
        return sFetcher;
    }

    public static void ensureServerStarted() {
        new ProviderCall.Builder(VirtualCore.get().getContext(), SERVICE_CP_AUTH).methodName("ensure_created").call();
    }

    public static void clearServerFetcher() {
        sFetcher = null;
    }

    // liujia: 为此IBinder对象设置"死亡代理"，当远程服务挂了(通信失败)，系统调用此死亡代理
    // 通常的处理是重启服务，但这里仅仅是unlinToDeath(取消死亡代理)
    private static void linkBinderDied(final IBinder binder) {
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                binder.unlinkToDeath(this, 0);
            }
        };
        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // liujia: 通过ServiceFetcher返回服务的IBinder句柄，即获取远程服务
    public static IBinder getService(String name) {
        // 如果是本地服务，直接本地返回   liujia: 如果当前是服务进程，则直接冲ServiceCache中直接获取就好了
        if (VirtualCore.get().isServerProcess()) {
            return ServiceCache.getService(name);
        }

        // 通过 ServiceFetcher 的句柄找到远程 Service 的句柄   //liujia: 通过ServiceFetcher获取远程服务
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                return fetcher.getService(name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        VLog.e(TAG, "GetService(%s) return null.", name);
        return null;
    }

    public static void addService(String name, IBinder service) {
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                fetcher.addService(name, service);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    public static void removeService(String name) {
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                fetcher.removeService(name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
