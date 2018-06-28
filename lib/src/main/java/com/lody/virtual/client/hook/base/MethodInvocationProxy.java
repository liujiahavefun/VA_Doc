package com.lody.virtual.client.hook.base;

import android.content.Context;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * liujia: 注意这个是抽象类，需要子类实现Inject()函数
 * 参考IInjector接口，对某个类或者Interface进行hook，需要显式的调用Inject()
 * 通常Inject有两种方法，请参考BinderInvocationProxy的注释，一种是对通过ServiceManager获取的IInterface进行hook
 * 一种是对全局单例对象的某个filed进行hook
 * 对应的就是一是对ServiceManager.sCache进行添加和替换(IBinder)，另一种就是直接对那个全局对象的field进行替换(mirror导出后设置为我们自己的)
 *
 * MethodInvocationProxy 需要配合 MethodInvocationStub 一起使用
 * MethodInvocationProxy实际上包了MethodInvocationStub
 */

/**
 * @author Lody
 *         <p>
 *         This class is responsible with:
 *         - Instantiating a {@link MethodInvocationStub.HookInvocationHandler} on {@link #getInvocationStub()} ()}
 *         - Install a bunch of {@link MethodProxy}s, either with a @{@link Inject} annotation or manually
 *           calling {@link #addMethodProxy(MethodProxy)} from {@link #onBindMethods()}
 *         - Install the hooked object on the Runtime via {@link #inject()}
 *         <p>
 *         All {@link MethodInvocationProxy}s (plus a couple of other @{@link IInjector}s are installed by {@link InvocationStubManager}
 * @see Inject
 */
//Method Hook 通过动态代理
public abstract class MethodInvocationProxy<T extends MethodInvocationStub> implements IInjector {

    protected T mInvocationStub;

    public MethodInvocationProxy(T invocationStub) {
        this.mInvocationStub = invocationStub;
        onBindMethods();
        afterHookApply(invocationStub);

        LogInvocation loggingAnnotation = getClass().getAnnotation(LogInvocation.class);
        if (loggingAnnotation != null) {
            invocationStub.setInvocationLoggingCondition(loggingAnnotation.value());
        }
    }

    /**
     * liujia: 这里注释一下外面是如果通过onBindMethods()和addMethodProxy()来hook的
     * 通常的hook，有两种方法
     * 1）通过Inject(MethodPrxies.class)注解的方式
        @Inject(MethodProxies.class)
        public class NotificationManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>>
        这种方式的话，MethodProxies这个同包名下面的类里，实现了很多继承了MethodProxy的静态类
        如下面onBindMethods()方法里，会获取MethodProxies内部的所有静态类，然后这样的每一个静态类都对应一个待hook的方法(也包括了hook后的逻辑)
        然后对这样的每一个内部类通过addMethodProxy(Class<?> hookType)，我们将其构造出来并且 mInvocationStub.addMethodProxy(methodProxy);
        注意：子类如果重新onBindMethod()，必须调用super.onBindMethod()才行

        2）子类重写onBindMethod()，然后手动的添加addMethodProxy
        如 addMethodProxy(new ReplaceCallingPkgMethodProxy("enqueueToast"));
        这样最终调用了下面的addMethodProxy(MethodProxy methodProxy)
        这种方法通常是在子类内部定义了继承了MethodProxy的内部类，并且手动的构造这个类然后调用addMethodProxy
        或者是直接使用ReplaceCallingPkgMethodProxy等辅助类来hook某个函数
     *
     *  注意：我们hook的所有系统服务及hook后的逻辑都在com.lody.virtual.client.hook.proxies下面
     */
    protected void onBindMethods() {
        if (mInvocationStub == null) {
            return;
        }
        Class<? extends MethodInvocationProxy> clazz = getClass();
        Inject inject = clazz.getAnnotation(Inject.class);
        if (inject != null) {
            Class<?> proxiesClass = inject.value();
            Class<?>[] innerClasses = proxiesClass.getDeclaredClasses(); //liujia: 获取内部类
            // 遍历内部类
            for (Class<?> innerClass : innerClasses) {
                //liujia: 如果内部类不是抽象的，并且此MethodProxy和此内部类有继承关系(isAssignableFrom参考文档)，并没有SkipInject注解
                //则调用addMethodProxy去hook此内部类
                //尤其注意的是，初次看到这里颇为令人难以理解，为何这里一是要去addMethodProxy(见下)的参数是内部类，二是addMethodProxy内部逻辑里，构造函数返回的是MethodProxy类型
                //这个要结合具体实现去看，就是去看看@Inject注解是怎么用的，通常都是这样 @Inject(MethodProxies.class)
                if (!Modifier.isAbstract(innerClass.getModifiers())
                        && MethodProxy.class.isAssignableFrom(innerClass)
                        && innerClass.getAnnotation(SkipInject.class) == null) {
                    addMethodProxy(innerClass);
                }
            }
        }
    }

    private void addMethodProxy(Class<?> hookType) {
        try {
            Constructor<?> constructor = hookType.getDeclaredConstructors()[0];
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            MethodProxy methodProxy;
            if (constructor.getParameterTypes().length == 0) {
                methodProxy = (MethodProxy) constructor.newInstance();
            } else {
                methodProxy = (MethodProxy) constructor.newInstance(this);
            }
            mInvocationStub.addMethodProxy(methodProxy);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to instance Hook : " + hookType + " : " + e.getMessage());
        }
    }

    public MethodProxy addMethodProxy(MethodProxy methodProxy) {
        return mInvocationStub.addMethodProxy(methodProxy);
    }

    protected void afterHookApply(T delegate) {
    }

    @Override
    public abstract void inject() throws Throwable;

    public Context getContext() {
        return VirtualCore.get().getContext();
    }

    public T getInvocationStub() {
        return mInvocationStub;
    }
}
