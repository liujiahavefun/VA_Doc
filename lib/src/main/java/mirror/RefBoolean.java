package mirror;

import java.lang.reflect.Field;

/**
 * liujia: 代表一个Class的内部的Boolean类型的field
 * 使用时，即在RefBoolean实例上调用get，传入Class的实例object，获取其对应filed的boolean类型的值，无论这个field是公开还是私有的
 * 当然，也可以set这个field的值，无论其公开还是私有
 *
 * 不再对其它类似的RefDouble RefFloat RefInt RefLong做详细说明，都类似这个，下面仅作简介
 */

public class RefBoolean {
    private Field field;

    public RefBoolean(Class<?> cls, Field field) throws NoSuchFieldException {
            this.field = cls.getDeclaredField(field.getName());
            this.field.setAccessible(true);
    }

    public boolean get(Object object) {
        try {
            return this.field.getBoolean(object);
        } catch (Exception e) {
            return false;
        }
    }

    public void set(Object obj, boolean value) {
        try {
            this.field.setBoolean(obj, value);
        } catch (Exception e) {
            //Ignore
        }
    }
}