package mirror;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static mirror.RefStaticMethod.getProtoType; //liujia: 把类的静态函数作为静态函数import....

/**
 * liujia: 与RefConstructor类似，不过获取的是普通的method，能call这个函数
 * 此类的实现，尤其是有MethodParams的注解下的实现，有不解之处，需仔细研究....
 * 注意，类模板参数T，是call()的返回值类型，即函数返回值的类型
 */

// 方法映射
@SuppressWarnings("unchecked")
public class RefMethod<T> {
    private Method method;

    public RefMethod(Class<?> cls, Field field) throws NoSuchMethodException {
        // 是否是隐藏类，隐藏类使用 String 描述类型
        if (field.isAnnotationPresent(MethodParams.class)) {
            Class<?>[] types = field.getAnnotation(MethodParams.class).value();
            //liujia: 这个for循环是干什么的？为什么要判断是否同一个classLoader，并且如果是的话，要拿到realClass并替换掉types[i]
            for (int i = 0; i < types.length; i++) {
                Class<?> clazz = types[i];
                //liujia: 为何要判断是否同一个classLoader？
                if (clazz.getClassLoader() == getClass().getClassLoader()) {
                    try {
                        //liujia：这个realClass和之前的types[i]有何区别？
                        Class.forName(clazz.getName());
                        Class<?> realClass = (Class<?>) clazz.getField("TYPE").get(null);
                        types[i] = realClass;
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            this.method = cls.getDeclaredMethod(field.getName(), types);
            this.method.setAccessible(true);
        } else if (field.isAnnotationPresent(MethodReflectParams.class)) {
            String[] typeNames = field.getAnnotation(MethodReflectParams.class).value();
            Class<?>[] types = new Class<?>[typeNames.length];
            for (int i = 0; i < typeNames.length; i++) {
                //liujia： 如果是基础类型int long double void等，则直接用，否则Class.forName()加载此类
                Class<?> type = getProtoType(typeNames[i]);
                if (type == null) {
                    try {
                        type = Class.forName(typeNames[i]);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                types[i] = type;
            }
            this.method = cls.getDeclaredMethod(field.getName(), types);
            this.method.setAccessible(true);
        }
        else {
            //liujia: 无参数的方法，直接遍历class的所有方法，名字对上就ok
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals(field.getName())) {
                    this.method = method;
                    this.method.setAccessible(true);
                    break;
                }
            }
        }
        if (this.method == null) {
            throw new NoSuchMethodException(field.getName());
        }
    }

    public T call(Object receiver, Object... args) {
        try {
            return (T) this.method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            } else {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public T callWithException(Object receiver, Object... args) throws Throwable {
        try {
            return (T) this.method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw e;
        }
    }

    public Class<?>[] paramList() {
        return method.getParameterTypes();
    }
}