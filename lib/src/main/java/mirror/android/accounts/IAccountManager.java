package mirror.android.accounts;

import android.os.IBinder;
import android.os.IInterface;

import mirror.RefClass;
import mirror.MethodParams;
import mirror.RefStaticMethod;

/**
 * liujia: 对应系统的 core/java/android/accounts/IAccountManager.aidl
 */

public class IAccountManager {
    public static Class<?> TYPE = RefClass.load(IAccountManager.class, "android.accounts.IAccountManager");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(Stub.class, "android.accounts.IAccountManager$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}