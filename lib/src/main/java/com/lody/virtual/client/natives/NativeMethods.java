package com.lody.virtual.client.natives;

import android.hardware.Camera;
import android.media.AudioRecord;
import android.os.Build;

import java.lang.reflect.Method;

import dalvik.system.DexFile;

/**
 * @author Lody
 */
public class NativeMethods {

    public static int gCameraMethodType;
    public static Method gCameraNativeSetup;

    public static Method gOpenDexFileNative;

    public static Method gAudioRecordNativeCheckPermission;

    public static void init() {
        String methodName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? "openDexFileNative" : "openDexFile";
        for (Method method : DexFile.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                gOpenDexFileNative = method;
                break;
            }
        }
        if (gOpenDexFileNative == null) {
            throw new RuntimeException("Unable to find method : " + methodName);
        }
        gOpenDexFileNative.setAccessible(true);

        /**
         * liujia:
         * private void final int native_setup(Object camera_this,int cameraId, int halVersion, String packageName); 等
         * 这个方法在JNI层通过Binder通信请求Server端的ICameraService去初始化摄像头。在Server端ICameraService会检查Client端传过来的包名，
         * 然后去PackageManagerService那边请求该包名对应的应用是否声明了Camera相关的权限，如果有，则打开摄像头，如果没有在界面上就会提示去设置里面打开相应的权限。
         * 我们的问题就出在这里，当我们的插件调起初始化Camera类时，Camera类获取的包名是插件的包名，而插件没有安装，所以在Server端进行权限校验时就会失败，最终导致摄像头无法打开
         */
        gCameraMethodType = -1;
        try {
            gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class, String.class);
            gCameraMethodType = 1;
        } catch (NoSuchMethodException e) {
            // ignore
        }
        if (gCameraNativeSetup == null) {
            try {
                gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class, int.class, String.class);
                gCameraMethodType = 2;
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        // HuaWei common
        if (gCameraNativeSetup == null) {
            try {
                gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class, int.class, String.class, boolean.class);
                gCameraMethodType = 3;
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        // HUAWEI MediaPad X1 7.0
        if (gCameraNativeSetup == null) {
            try {
                gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class, String.class, boolean.class);
                gCameraMethodType = 4;
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        if (gCameraNativeSetup != null) {
            gCameraNativeSetup.setAccessible(true);
        }

        for (Method mth : AudioRecord.class.getDeclaredMethods()) {
            if (mth.getName().equals("native_check_permission")
                    && mth.getParameterTypes().length == 1
                    && mth.getParameterTypes()[0] == String.class) {
                gAudioRecordNativeCheckPermission = mth;
                mth.setAccessible(true);
                break;
            }
        }
    }

}
