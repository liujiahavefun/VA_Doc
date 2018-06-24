package mirror;

import java.lang.reflect.Field;

/**
 * liujia: 与RefInt不同之处在于，get/set不用穿object参数了，且调用this.field.get()/set()时，第一个参数穿null
 * 表明调用的是类的静态成员
 */
public class RefStaticInt {
    private Field field;

    public RefStaticInt(Class<?> cls, Field field) throws NoSuchFieldException {
        this.field = cls.getDeclaredField(field.getName());
        this.field.setAccessible(true);
    }

    public int get() {
        try {
            return this.field.getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }

    public void set(int value) {
        try {
            this.field.setInt(null, value);
        } catch (Exception e) {
            //Ignore
        }
    }
}