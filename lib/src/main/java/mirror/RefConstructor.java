package mirror;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * liujia: 我猜目的是把类的隐藏的构造函数变为公开的
 * 典型用法：
 public class UserInfo {
    public static Class<?> TYPE = RefClass.load(UserInfo.class, "android.content.pm.UserInfo");
    public static RefStaticInt FLAG_PRIMARY;

    @MethodParams({int.class, String.class, int.class})
    public static RefConstructor<Object> ctor;
 }
 *
 * RefConstructor的构造函数也是接受一个class和一个field，和其它RefXXX是一样的，class是表明是要拿哪个类的构造函数，field的目的在于其上的注解，根据注解传入的参数，决定拿哪个构造函数
 * 首先看起field的注解是否有MethodParams，有的话动态的获取注解的值，然后从类中找到对应的构造函数并赋给ctor
 * 如果是MethodReflectParams，则用反射将字符串转为Class，然后想MethodParams一样获取其构造函数
 * 如果都不是，则获取默认构造函数，即无参数的那个构造函数
 * 最后将构造函数变为public的
 * 两个newInstance则返回T的实例对象
 */

public class RefConstructor<T> {
    private Constructor<?> ctor;

    public RefConstructor(Class<?> cls, Field field) throws NoSuchMethodException {
        if (field.isAnnotationPresent(MethodParams.class)) {
            Class<?>[] types = field.getAnnotation(MethodParams.class).value();
            ctor = cls.getDeclaredConstructor(types);
        } else if (field.isAnnotationPresent(MethodReflectParams.class)) {
            String[] values = field.getAnnotation(MethodReflectParams.class).value();
            Class[] parameterTypes = new Class[values.length];
            int N = 0;
            while (N < values.length) {
                try {
                    parameterTypes[N] = Class.forName(values[N]);
                    N++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ctor = cls.getDeclaredConstructor(parameterTypes);
        } else {
            ctor = cls.getDeclaredConstructor();
        }
        if (ctor != null && !ctor.isAccessible()) {
            ctor.setAccessible(true);
        }
    }

    public T newInstance() {
        try {
            return (T) ctor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public T newInstance(Object... params) {
        try {
            return (T) ctor.newInstance(params);
        } catch (Exception e) {
            return null;
        }
    }
}