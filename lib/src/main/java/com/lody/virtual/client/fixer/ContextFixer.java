package com.lody.virtual.client.fixer;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.DropBoxManager;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.ReflectException;

import mirror.android.app.ContextImpl;
import mirror.android.app.ContextImplKitkat;
import mirror.android.content.ContentResolverJBMR2;

/**
 * @author Lody
 */
public class ContextFixer {

    private static final String TAG = ContextFixer.class.getSimpleName();

    /**
     * Fuck AppOps
     *
     * @param context Context
     */
    public static void fixContext(Context context) {
        //liujia: why do this? it raise exception when context is null?
        try {
            context.getPackageName();
        } catch (Throwable e) {
            return;
        }

        //liujia: try inject IGraphicsStats
        InvocationStubManager.getInstance().checkEnv(GraphicsStatsStub.class);

        //liujia: what's this? try to get the most "base" context?
        int deep = 0;
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
            deep++;
            if (deep >= 10) {
                return;
            }
        }

        //liujia: 为啥用设置context(ContextImpl类型对象)的mPackageManager对象为null ?
        ContextImpl.mPackageManager.set(context, null);
        try {
            context.getPackageManager();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (!VirtualCore.get().isVAppProcess()) {
            return;
        }

        DropBoxManager dm = (DropBoxManager) context.getSystemService(Context.DROPBOX_SERVICE);
        BinderInvocationStub boxBinder = InvocationStubManager.getInstance().getInvocationStub(DropBoxManagerStub.class);
        if (boxBinder != null) {
            try {
                Reflect.on(dm).set("mService", boxBinder.getProxyInterface());
            } catch (ReflectException e) {
                e.printStackTrace();
            }
        }
        String hostPkg = VirtualCore.get().getHostPkg();
        ContextImpl.mBasePackageName.set(context, hostPkg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContextImplKitkat.mOpPackageName.set(context, hostPkg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ContentResolverJBMR2.mPackageName.set(context.getContentResolver(), hostPkg);
        }
    }
}
