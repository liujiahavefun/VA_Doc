package com.lody.virtual.client.hook.base;

import java.lang.reflect.Method;

/**
 * @author Lody
 */

/**
 * liujia： 对一个方法的hook，替换其返回值为我们给定的result对象
 */
public class ResultStaticMethodProxy extends StaticMethodProxy {

	Object mResult;

	public ResultStaticMethodProxy(String name, Object result) {
		super(name);
		mResult = result;
	}

	public Object getResult() {
		return mResult;
	}

	@Override
	public Object call(Object who, Method method, Object... args) throws Throwable {
		return mResult;
	}
}
