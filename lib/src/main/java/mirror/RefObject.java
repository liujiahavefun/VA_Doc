package mirror;

import java.lang.reflect.Field;

/**
 * liujia: 与RefBoolean类似，不过这个是模板类，对应的Object类型通过模板参数T传进来
 *
 */

// Field 映射
@SuppressWarnings("unchecked")
public class RefObject<T> {

    // framework 层对应的 Field
    private Field field;

    public RefObject(Class<?> cls, Field field) throws NoSuchFieldException {
        // 获取 framework 中同名字段的 field
        this.field = cls.getDeclaredField(field.getName());
        this.field.setAccessible(true);
    }

    // 获取变量值
    public T get(Object object) {
        try {
            return (T) this.field.get(object);
        } catch (Exception e) {
            return null;
        }
    }
    // 赋值
    public void set(Object obj, T value) {
        try {
            this.field.set(obj, value);
        } catch (Exception e) {
            //Ignore
        }
    }
}