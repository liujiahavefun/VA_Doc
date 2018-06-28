package com.lody.virtual.client.hook.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;

import java.io.FileDescriptor;
import java.lang.reflect.Method;

import mirror.RefStaticMethod;
import mirror.android.os.ServiceManager;

/**
 * @author Lody
 */
// IBinder hook asInterface 接口
@SuppressWarnings("unchecked")

/**
 * liujia: hook一个Service Interface的asInterface(IBinder)函数，主要这个函数是静态函数
 * 通过这个asInterface()返回的interface对象，client通过此来调用真正的服务
 * 我们的目的就是
 */
public class BinderInvocationStub extends MethodInvocationStub<IInterface> implements IBinder {

    private static final String TAG = BinderInvocationStub.class.getSimpleName();
    private IBinder mBaseBinder;

    public BinderInvocationStub(RefStaticMethod<IInterface> asInterfaceMethod, IBinder binder) {
        //liujia: 调用 public BinderInvocationStub(IInterface mBaseInterface) 这个构造函数
        this(asInterface(asInterfaceMethod, binder));
    }

    public BinderInvocationStub(Class<?> stubClass, IBinder binder) {
        //liujia: 调用 public BinderInvocationStub(IInterface mBaseInterface) 这个构造函数
        this(asInterface(stubClass, binder));
    }

    public BinderInvocationStub(IInterface baseInterface) {
        super(baseInterface);
        //liujia: getBaseInterface()实现在基类MethodInvocationStub()中，其实就是返回了上面的baseInterface
        mBaseBinder = getBaseInterface() != null ? getBaseInterface().asBinder() : null;
        addMethodProxy(new AsBinder());
    }

    private static IInterface asInterface(RefStaticMethod<IInterface> asInterfaceMethod, IBinder binder) {
        if (asInterfaceMethod == null || binder == null) {
            return null;
        }
        return asInterfaceMethod.call(binder);
    }

    private static IInterface asInterface(Class<?> stubClass, IBinder binder) {
        try {
            if (stubClass == null || binder == null) {
                return null;
            }
            //liujia: 获取class的asInterface这个method，并且invoke，注意invoke的第一个参数是null，表示是static method
            Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
            return (IInterface) asInterface.invoke(null, binder);
        } catch (Exception e) {
            Log.d(TAG, "Could not create stub " + stubClass.getName() + ". Cause: " + e);
            return null;
        }
    }

    // liujia: 增加/替换 了系统ServiceManager中未导出的sCache中的对应项
    // 这样某个service的实现其实就被我们替换了
    // 当某个service需要被调用时，ServiceManager根据name首先查找这个静态的sCache，发现存在，则返回IBinder(就是这个BinderInvocationStub实例)
    // 然后在这个IBinder实例上调用去queryLocalInterface(String descriptor)，其实最终调用了我们的实现
    // 接上，其实调用我们自己实现的queryLocalInterface()返回的是基类MethodInvocationStub中已经被动态代理后的service interface
    public void replaceService(String name) {
        if (mBaseBinder != null) {
            ServiceManager.sCache.get().put(name, this);
        }
    }

    /**
     * liujia: AsBinder这个类，作为对asBinder()这个方法的HOOK类
     * 主要就是实现下面这两个方法，一个是hook哪个函数，另一个是hook的实现(还可以是beforeCall afterCall)
     */
    private final class AsBinder extends MethodProxy {
        @Override
        public String getMethodName() {
            return "asBinder";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return BinderInvocationStub.this;
        }
    }

    public Context getContext() {
        return VirtualCore.get().getContext();
    }

    public IBinder getBaseBinder() {
        return mBaseBinder;
    }

    /**
     * liujia: 下面都是实现IBinder的接口
     */
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return mBaseBinder.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        return mBaseBinder.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return mBaseBinder.isBinderAlive();
    }

    @Override
    public IInterface queryLocalInterface(String descriptor) {
        //liujia: 这里返回的是代理后的接口，而不是原接口了
        return getProxyInterface();
    }

    @Override
    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
        mBaseBinder.dump(fd, args);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @Override
    public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        mBaseBinder.dumpAsync(fd, args);
    }

    @Override
    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return mBaseBinder.transact(code, data, reply, flags);
    }

    @Override
    public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
        mBaseBinder.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
        return mBaseBinder.unlinkToDeath(recipient, flags);
    }
}
