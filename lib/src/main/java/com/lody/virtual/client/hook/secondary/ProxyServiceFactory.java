package com.lody.virtual.client.hook.secondary;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 */

public class ProxyServiceFactory {

	private static final String TAG = ProxyServiceFactory.class.getSimpleName();

	private static Map<String, ServiceFetcher> sHookSecondaryServiceMap = new HashMap<>();

	static {
		//liujia: google登录？搜不到，猜的...
		sHookSecondaryServiceMap.put("com.google.android.auth.IAuthManagerService", new ServiceFetcher() {
			@Override
			public IBinder getService(final Context context, ClassLoader classLoader, IBinder binder) {
				//liujia: StubBinder就是一个IBinder...
				return new StubBinder(classLoader, binder) {
					@Override
					public InvocationHandler createHandler(Class<?> interfaceClass, final IInterface base) {
						return new InvocationHandler() {
							@Override
							public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
								try {
									//InvocationHandler.invoke的三个参数分别是：
									//proxy: 指代我们所代理的那个真实对象
									//method: 指代的是我们所要调用真实对象的某个方法的Method对象
									//args:指代的是调用真实对象某个方法时接受的参数
									//这里调用方法不是用原来的真实对象，而是用传入的base
									//但是参考StubBinder的代码
									//InvocationHandler handler = createHandler(aidlType, targetInterface);
									//这里的base就是targetInterface，即就是代理前的接口
									//所以这么做目的是？
									return method.invoke(base, args);
								} catch (InvocationTargetException e) {
									if (e.getCause() != null) {
										throw e.getCause();
									}
									throw e;
								}
							}
						};
					}
				};
			}
		});

		//liujia: 似乎是google的应用内支付的接口
		sHookSecondaryServiceMap.put("com.android.vending.billing.IInAppBillingService", new ServiceFetcher() {
			@Override
			public IBinder getService(final Context context, ClassLoader classLoader, IBinder binder) {
				return new StubBinder(classLoader, binder) {
					@Override
					public InvocationHandler createHandler(Class<?> interfaceClass, final IInterface base) {
						return new InvocationHandler() {
							@Override
							public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
								try {
									return method.invoke(base, args);
								} catch (InvocationTargetException e) {
									if (e.getCause() != null) {
										throw e.getCause();
									}
									throw e;
								}
							}
						};
					}
				};
			}
		});

		//liujia: 参考这个： https://blog.csdn.net/andrio/article/details/80408249
		// 似乎是为了禁止google的弹框
		sHookSecondaryServiceMap.put("com.google.android.gms.common.internal.IGmsServiceBroker", new ServiceFetcher() {
			@Override
			public IBinder getService(final Context context, ClassLoader classLoader, IBinder binder) {
				return new StubBinder(classLoader, binder) {
					@Override
					public InvocationHandler createHandler(Class<?> interfaceClass, final IInterface base) {
						return new InvocationHandler() {
							@Override
							public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
								try {
									return method.invoke(base, args);
								} catch (InvocationTargetException e) {
									if (e.getCause() != null) {
										throw e.getCause();
									}
									throw e;
								}
							}
						};
					}
				};
			}
		});
	}

	//liujia: 参考IVClient.aidl中的getProxyService，主要是实现这个接口。。。。
	public static IBinder getProxyService(Context context, ComponentName component, IBinder binder) {
		if (context == null || binder == null) {
			return null;
		}
		try {
			String description = binder.getInterfaceDescriptor();
			ServiceFetcher fetcher = sHookSecondaryServiceMap.get(description);
			if (fetcher != null) {
				IBinder res = fetcher.getService(context, context.getClassLoader(), binder);
				if (res != null) {
					return res;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private interface ServiceFetcher {
		IBinder getService(Context context, ClassLoader classLoader, IBinder binder);
	}
}
