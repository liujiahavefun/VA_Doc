package com.lody.virtual.client.hook.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.VDeviceInfo;

import java.lang.reflect.Method;

/**
 * @author Lody
 * liujia: 这个对应一个要hook的method
 * 参考一下MethodInvocationStub的addMethodProxy函数
 */
// 方法 Hook 基类，使用动态代理
public abstract class MethodProxy {

    private boolean enable = true;
    private LogInvocation.Condition mInvocationLoggingCondition = LogInvocation.Condition.NEVER; // Inherit

    public MethodProxy() {
        LogInvocation loggingAnnotation = getClass().getAnnotation(LogInvocation.class);
        if (loggingAnnotation != null) {
            this.mInvocationLoggingCondition = loggingAnnotation.value();
        }
    }

    /**
     * liujia: 这个要由基类实现，返回要hook的method名字
     */
    public abstract String getMethodName();

    /**
     * liujia: 下面这一堆函数，需要一个一个看到底做了啥。。。。
     */
    public static String getHostPkg() {
        return VirtualCore.get().getHostPkg();
    }

    protected static Context getHostContext() {
        return VirtualCore.get().getContext();
    }

    protected static boolean isAppProcess() {
        return VirtualCore.get().isVAppProcess();
    }

    protected static boolean isServerProcess() {
        return VirtualCore.get().isServerProcess();
    }

    protected static boolean isMainProcess() {
        return VirtualCore.get().isMainProcess();
    }

    protected static int getVUid() {
        return VClientImpl.get().getVUid();
    }

    protected static int getAppUserId() {
        return VUserHandle.getUserId(getVUid());
    }

    protected static int getBaseVUid() {
        return VClientImpl.get().getBaseVUid();
    }

    protected static int getRealUid() {
        return VirtualCore.get().myUid();
    }

    protected static VDeviceInfo getDeviceInfo() {
        return VClientImpl.get().getDeviceInfo();
    }

    public static boolean isVisiblePackage(ApplicationInfo info) {
        return getHostPkg().equals(info.packageName)
                || ComponentUtils.isSystemApp(info)
                || VirtualCore.get().isOutsidePackageVisible(info.packageName);
    }

    public boolean isAppPkg(String pkg) {
        return VirtualCore.get().isAppInstalled(pkg);
    }

    protected PackageManager getPM() {
        return VirtualCore.getPM();
    }

    /**
     * liujia: hook后，要重写beforeCall call afterCall，重写其中一个也行，用自己的实现替换系统的
     */
    public boolean beforeCall(Object who, Method method, Object... args) {
        return true;
    }

    public Object call(Object who, Method method, Object... args) throws Throwable {
        return method.invoke(who, args);
    }

    public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
        return result;
    }

    /**
     * liujia: hook开关，返回false表明不hook，返回true表明要hook
     */
    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * liujia: 获取和设置其日志的配置
     */
    public LogInvocation.Condition getInvocationLoggingCondition() {
        return mInvocationLoggingCondition;
    }

    public void setInvocationloggingCondition(LogInvocation.Condition invocationLoggingCondition) {
        mInvocationLoggingCondition = invocationLoggingCondition;
    }

    @Override
    public String toString() {
        return "Method : " + getMethodName();
    }
}
