package com.lody.virtual.client.hook.base;

/**
 * @author Lody
 */

/**
 * liujia： 实现了基类MethodProxy中的getMethodName()函数....
 */
public class StaticMethodProxy extends MethodProxy {

	private String mName;

	public StaticMethodProxy(String name) {
		this.mName = name;
	}

	@Override
	public String getMethodName() {
		return mName;
	}
}
