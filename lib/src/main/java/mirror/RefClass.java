package mirror;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * liujia: RefClass主要用mirror中镜像系统framework的相关类
 * 典型用法：
    public class IAccountManager {
        public static Class<?> TYPE = RefClass.load(IAccountManager.class, "android.accounts.IAccountManager");

        public static class Stub {
            public static Class<?> TYPE = RefClass.load(Stub.class, "android.accounts.IAccountManager$Stub");
            @MethodParams({IBinder.class})
            public static RefStaticMethod<IInterface> asInterface;
        }
    }
 * IAccountManager是系统的接口，我们这里用镜像中的类来代替系统的实现，主要就是要实现IAccountManager中的Stub类的asInterface函数
 * asInterface()负责将IBinder对象转换为对外的interface，这里我们只要替换了asInterface()实现，就能保证外面调用获取的系统服务接口是我们自己的实现
 *
 * 下面分析RefClass实现，首先定了静态的一个映射表REF_TYPES，内置了RefXXX和其对应的构造函数，注意所有的RefXXX类的构造函数参数都是(Class.class，Field.class)
 * 即都接受一个class和一个filed
 * 然后就是load()的实现，主要看下面那个Class<?> load(Class<?> mappingClass, String className)
 * 第一个参数是我们实现的mapping类，后面是系统的真实的类
 * 首先遍历mapping类的所有静态成员，如果其类型是刚才那个静态映射表内的，其实就是其类型是RefXXX，得到其构造函数，然后将这个静态成员初始化(即调用构造函数构造它)
 * 最后的效果就是，如上面的典型用法，IAccountManager被加载后，其静态函数load()被调用，即RefClass.load(Stub.class, "android.accounts.IAccountManager$Stub")
 * 然后内部的RefXXX这样的静态Field对象就同时也被初始化了，这样就可以方便的拿到系统类的一些隐藏field或者method，将其变为public的
 */

public final class RefClass {

    private static HashMap<Class<?>,Constructor<?>> REF_TYPES = new HashMap<Class<?>, Constructor<?>>();
    static {
        try {
            REF_TYPES.put(RefObject.class, RefObject.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefMethod.class, RefMethod.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefInt.class, RefInt.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefLong.class, RefLong.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefFloat.class, RefFloat.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefDouble.class, RefDouble.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefBoolean.class, RefBoolean.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefStaticObject.class, RefStaticObject.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefStaticInt.class, RefStaticInt.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefStaticMethod.class, RefStaticMethod.class.getConstructor(Class.class, Field.class));
            REF_TYPES.put(RefConstructor.class, RefConstructor.class.getConstructor(Class.class, Field.class));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Class<?> load(Class<?> mappingClass, String className) {
        try {
            return load(mappingClass, Class.forName(className));
        } catch (Exception e) {
            return null;
        }
    }


    public static Class load(Class mappingClass, Class<?> realClass) {
        // 获取 Mirror 类的所有字段
        Field[] fields = mappingClass.getDeclaredFields();
        for (Field field : fields) {
            try {
                // 必须是 static 变量
                // liujia: 判断一个field(method?)是否是static成员的方法
                if (Modifier.isStatic(field.getModifiers())) {
                    // 从预设的 Map 中找到 RefXXXX 的构造器
                    Constructor<?> constructor = REF_TYPES.get(field.getType());
                    if (constructor != null) {
                        // 赋值
                        // liujia: RefXXX这些类的构造函数的参数都是两个，一个是类，一个是field
                        field.set(null, constructor.newInstance(realClass, field));
                    }
                }
            }
            catch (Exception e) {
                // Ignore
            }
        }
        return realClass;
    }

}