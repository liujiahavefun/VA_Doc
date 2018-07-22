package com.lody.virtual.client.hook.base;

import android.os.IBinder;
import android.os.IInterface;

import mirror.RefStaticMethod;
import mirror.android.os.ServiceManager;

/**
 * @author Paulo Costa
 *
 * @see MethodInvocationProxy
 */

/**
 * liujia: 这个类应该是hook service的总入口之一，com.lody.virtual.client.hook.proxies下面的,要细查之...
 * com.lody.virtual.client.hook.proxies下面使我们要hook的系统服务，hook的姿势有两种
 * 1）public class AccountManagerStub extends BinderInvocationProxy 直接继承BinderInvocationProxy
 * 对应的构造函数一般类似于：
   	public AccountManagerStub() {
 		super(IAccountManager.Stub.asInterface, Context.ACCOUNT_SERVICE);
 	}
 	即我们对通常系统Service的的接口内部的静态Stub类的静态函数asInterface进行hook，替换为我们自己的实现
 *
 * 2）直接继承MethodInvocationProxy，通常是下面这种方式，不过这种形式不多，就那么几种
    public class DisplayStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>>
    这种方式对应的构造函数类似：
 	public DisplayStub() {
 		super(new MethodInvocationStub<IInterface>(DisplayManagerGlobal.mDm.get(DisplayManagerGlobal.getInstance.call())));
 	}
 	这种方式通常是这个系统服务不是从ServiceManager去获取(这种从ServiceManager获取返回的通常是IBinder),
 	而是存在一个全局的单例可以获取这个IInterface对象，这样我们直接拿到这个IInterface实例然后在这个实例上动态代理为我们自己的实现就好了
 	这样，其它代码也是通过去拿这个全局对象，调用的就是我们代理后的代码了
 	这种服务通常不会调用ServiceManager去获取，所以不用通过BinderInvocationProxy去搞了
 *
 * 这里说一下hook的步骤
 * 1）首先是继承BinderInvocationProxy或者MethodInvocationProxy<MethodInvocationStub<IInterface>>
 * 2）然后在Inject()里面，对要代理的对象(要么是IBinder，要么是个全局的单例，都是通过mirror拿到)，设置动态代理
 * 动态代理实际是在MethodInvocationStub中，注意这里对整个接口做了代理
 * 3）然后在onBindMethods()中，添加对已经代理了的接口(对象)的哪个方法的hook代码，即如果要更改原有逻辑，在这里添加hook代码
 * 注意onBindMethods()实在MethodInvocationProxy的构造函数中调用的。
 *
 */
public abstract class BinderInvocationProxy extends MethodInvocationProxy<BinderInvocationStub> {

	protected String mServiceName;

	//liujia: 下面这三种构造函数对应几种形式
	// 1: 我们已经拿到真正的IInterface(服务接口)了
	// 2: 对于RefStaticMethod<IInterface>，我们要拿 ServiceManager.getService.call(serviceName))返回的IBinder作为参数
	//    去调用这个RefStaticMethod<IInterface>，才能得到真正的IInterface
	// 3: 只能拿到stub class，要通过反射拿到其asInterface函数，像上面那样调用并传入IBinder对象
	//    最终拿到真正的IInterface
	public BinderInvocationProxy(IInterface stub, String serviceName) {
		this(new BinderInvocationStub(stub), serviceName);
	}

	public BinderInvocationProxy(RefStaticMethod<IInterface> asInterfaceMethod, String serviceName) {
		this(new BinderInvocationStub(asInterfaceMethod, ServiceManager.getService.call(serviceName)), serviceName);
	}

	public BinderInvocationProxy(Class<?> stubClass, String serviceName) {
		this(new BinderInvocationStub(stubClass, ServiceManager.getService.call(serviceName)), serviceName);
	}

	// liujia: 上面三个构造函数最终都调这个构造函数
	public BinderInvocationProxy(BinderInvocationStub hookDelegate, String serviceName) {
		super(hookDelegate);
		this.mServiceName = serviceName;
	}

	@Override
	public void inject() throws Throwable {
		// liujia: 参考BinderInvocationStub.replaceService(name)的注释
		getInvocationStub().replaceService(mServiceName);
	}

	@Override
	public boolean isEnvBad() {
		// liujia: inject成功后，ServiceManager.getService(name)返回的应该是我们自己的BinderInvocationStub实例(也是一个IBinder)
		IBinder binder = ServiceManager.getService.call(mServiceName);
		return binder != null && getInvocationStub() != binder;
	}
}
