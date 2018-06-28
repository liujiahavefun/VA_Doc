package com.lody.virtual.client.hook.base;

import java.lang.reflect.Method;

/**
 * liujia： 对一个方法的hook，替换uid啥的....
 * 需要结合具体使用来看
 */
public class ReplaceUidMethodProxy extends StaticMethodProxy {

        private final int index;
        public ReplaceUidMethodProxy(String name, int index) {
            super(name);
            this.index = index;
        }

    @Override
    public boolean beforeCall(Object who, Method method, Object... args) {
        int uid = (int) args[index];
        if (uid == getVUid() || uid == getBaseVUid()) {
            args[index] = getRealUid();
        }
        return super.beforeCall(who, method, args);
    }
}