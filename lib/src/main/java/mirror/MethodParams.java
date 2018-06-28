package mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * liujia: @interface表示的不是interface，而是注解类。JDK1.5后加入
 * 这个注解类，就是定义一个可用的注解，包括这个注解用在什么地方，是类还是方法还是property，还是方法入参等待
 * @Retention(RetentionPolicy.RUNTIME)  注解会在class字节码文件中存在，在运行时可以通过反射获取到
 * @Target:注解的作用目标，如下：
 *      @Target(ElementType.TYPE)           //接口、类、枚举、注解
 *      @Target(ElementType.FIELD)          //字段、枚举的常量
 *　　　 @Target(ElementType.METHOD)         //方法
 *　　　 @Target(ElementType.PARAMETER)      //方法参数
 *　　　 @Target(ElementType.CONSTRUCTOR)    //构造函数
 *　　　 @Target(ElementType.LOCAL_VARIABLE) //局部变量
 *　　　 @Target(ElementType.ANNOTATION_TYPE)//注解
 *　　　 @Target(ElementType.PACKAGE)        ///包
 *
 * 所以，下面这个注解类名字叫MethodParams，是runtime的，即注解值可以在运行时通过反射获取
 * 并且这个注解类用于类的字段，和枚举常量
 *
 * 下面展示一个用法的例子，通过RefStaticMethod<T>和RefClass.load，我们把系统的某个Stub类(不是通用的，而是某一个Stub)的隐藏方法asInterface，变为我们mirror中对应的Stub类的public方法
 * 此时这个隐藏方法asInterface，变为公开的field也叫asInterface，可以通过asInterface.get(obj)获取真正的方法，然后根据注解类，获取调用参数，最后运行时调用
 * 相当于运行时调用了系统的隐藏方法
   public static class Stub {
        public static Class<?> TYPE = RefClass.load(Stub.class, "android.app.backup.IBackupManager$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
   }
 *
 * 注解里面的Class<?>[] 表明使用时我们要这么用 @MethodParams({int.class, String.class...})，传递任意多个参数，这些参数都是方法参数的类型(class)
 * 获取注解就是 clazz.getAnnotation(MethodParams.class).value()
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodParams {
    Class<?>[] value();
}