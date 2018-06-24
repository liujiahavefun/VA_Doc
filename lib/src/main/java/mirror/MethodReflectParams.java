package mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * liujia: 同MethodParams注解，不过这里的参数不是通过类似{int.class, String.class}
 * 而是通过{"java.lang.String"，。。。}这种字符串来传递的，而后通过反射获取类型
 * 目的因为有些参数是内部类实例，无法在编译时获取内部类的class，只能通过反射运行时获取
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodReflectParams {
    String[] value();
}