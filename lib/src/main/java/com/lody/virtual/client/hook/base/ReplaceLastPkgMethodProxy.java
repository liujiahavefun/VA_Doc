package com.lody.virtual.client.hook.base;

import java.lang.reflect.Method;

import com.lody.virtual.client.hook.utils.MethodParameterUtils;

/**
 * @author Lody
 */

// 一个 Hook for 替换最新的 Pkg ？
/**
 * liujia： 对一个方法的hook，就是将方法参数中的最后一个String，替换为宿主的包名
 */
public class ReplaceLastPkgMethodProxy extends StaticMethodProxy {

	public ReplaceLastPkgMethodProxy(String name) {
		super(name);
	}

	@Override
	public boolean beforeCall(Object who, Method method, Object... args) {
		MethodParameterUtils.replaceLastAppPkg(args);
		return super.beforeCall(who, method, args);
	}
}
