package com.lody.virtual.client.hook.secondary;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * @author Lody
 */

abstract class StubBinder implements IBinder {
	private ClassLoader mClassLoader;
	private IBinder mBase;
	private IInterface mInterface;

	StubBinder(ClassLoader classLoader, IBinder base) {
		this.mClassLoader = classLoader;
		this.mBase = base;
	}

	//liujia: 这个需要子类实现，用于动态代理，实现自定义逻辑
	public abstract InvocationHandler createHandler(Class<?> interfaceClass, IInterface iInterface);

	@Override
	public String getInterfaceDescriptor() throws RemoteException {
		return mBase.getInterfaceDescriptor();
	}

	@Override
	public boolean pingBinder() {
		return mBase.pingBinder();
	}

	@Override
	public boolean isBinderAlive() {
		return mBase.isBinderAlive();
	}


	/**
	 * Anti the Proguard.
	 *
	 * Search the AidlClass.Stub.asInterface(IBinder) method by the StackTrace.
	 *
	 * liujia: 搜索栈里的类似 AidlClass.Stub.asInterface(IBinder)这种函数，然后对动态代理
	 */
	@Override
	public IInterface queryLocalInterface(String descriptor) {
		if (mInterface == null) {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			if (stackTrace == null || stackTrace.length <= 1) {
				return null;
			}
			Class<?> aidlType = null;
			IInterface targetInterface = null;

			//liujia: 这个stackTrace正常循环的话，是从栈顶到栈底还是从栈底到栈顶啊？
			for (StackTraceElement element : stackTrace) {
				if (element.isNativeMethod()) {
					continue;
				}
				try {
                    Method method = mClassLoader.loadClass(element.getClassName()).getDeclaredMethod(element.getMethodName(), IBinder.class);
                    if ((method.getModifiers() & Modifier.STATIC) != 0) {
                        method.setAccessible(true);
                        Class<?> returnType = method.getReturnType();
                        //liujia: returnType是interface且是IInterface的子类
                        if (returnType.isInterface() && IInterface.class.isAssignableFrom(returnType)) {
                            aidlType = returnType;
                            targetInterface = (IInterface) method.invoke(null, mBase); //liujia: asInterface(IBinder)是静态函数，所以第一个参数是null
							//liujia: 这里如果找到也不退出循环，看样子是要找到最底层的满足条件的
                        }
                    }
                } catch (Exception e) {
                    // go to the next cycle
                }
			}
			if (aidlType == null || targetInterface == null) {
                return null;
            }
            //liujia: 动态代理，注意返回的是动态代理后的interface
			InvocationHandler handler = createHandler(aidlType, targetInterface);
			mInterface = (IInterface) Proxy.newProxyInstance(mClassLoader, new Class[]{aidlType}, handler);
		}
		return mInterface;
	}

	@Override
	public void dump(FileDescriptor fd, String[] args) throws RemoteException {
		mBase.dump(fd, args);
	}

	@Override
	public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
		mBase.dumpAsync(fd, args);
	}

	@Override
	public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		return mBase.transact(code, data, reply, flags);
	}

	@Override
	public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
		mBase.linkToDeath(recipient, flags);
	}

	@Override
	public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		return mBase.unlinkToDeath(recipient, flags);
	}
}
