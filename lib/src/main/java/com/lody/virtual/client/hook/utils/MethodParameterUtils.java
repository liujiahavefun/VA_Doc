package com.lody.virtual.client.hook.utils;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.ArrayUtils;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Lody
 *
 */
public class MethodParameterUtils {

	/**
	 * liujia: 找到参数里第一个String类型的参数，并且替换为宿主Apk(即VirtualApp这个程序)的包名
	 */
	public static String replaceFirstAppPkg(Object[] args) {
		if (args == null) {
			return null;
		}
		int index = ArrayUtils.indexOfFirst(args, String.class); //liujia: 找到Object[]中的第一个String类型的对象，并返回index
		if (index != -1) {
			String pkg = (String) args[index];
			args[index] = VirtualCore.get().getHostPkg();
			return pkg;
		}
		return null;
	}

	// 看上去将包名替换成了 Host
	/**
	 * liujia: 找到参数里最后一个String类型的参数，并且替换为宿主的包名
	 */
	public static String replaceLastAppPkg(Object[] args) {
		int index = ArrayUtils.indexOfLast(args, String.class);
		if (index != -1) {
			String pkg = (String) args[index];
			args[index] = VirtualCore.get().getHostPkg();
			return pkg;
		}
		return null;
	}

	/**
	 * liujia: 找到参数里所有的String类型的参数，并且替换为宿主的包名，最多替换sequence个
	 */
	public static String replaceSequenceAppPkg(Object[] args, int sequence) {
		int index = ArrayUtils.indexOf(args, String.class, sequence);
		if (index != -1) {
			String pkg = (String) args[index];
			args[index] = VirtualCore.get().getHostPkg();
			return pkg;
		}
		return null;
	}

	/**
	 * liujia: 见下面的具体实现
	 */
	public static Class<?>[] getAllInterface(Class clazz){
		HashSet<Class<?>> classes = new HashSet<>();
		getAllInterfaces(clazz,classes);
		Class<?>[] result=new Class[classes.size()];
		classes.toArray(result);
		return result;
	}


	/**
	 * liujia: 获取clazz implement和extends的接口集合，这里要注意不止获得了实现的interface，同时也获取了自己的基类(如果基类不是Object的话)
	 */
	public static void getAllInterfaces(Class clazz, HashSet<Class<?>> interfaceCollection) {
		Class<?>[] classes = clazz.getInterfaces();
		if (classes.length != 0) {
			interfaceCollection.addAll(Arrays.asList(classes));
		}
		if (clazz.getSuperclass() != Object.class) {
			getAllInterfaces(clazz.getSuperclass(), interfaceCollection);
		}
	}
}
