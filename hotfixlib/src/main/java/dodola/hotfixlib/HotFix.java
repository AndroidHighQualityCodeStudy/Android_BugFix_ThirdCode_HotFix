/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package dodola.hotfixlib;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import android.annotation.TargetApi;
import android.content.Context;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/* compiled from: ProGuard */
public final class HotFix {


    /**
     * 将patch.dex 动态添加到BaseDexClassLoader对象间接引用的dexElements数组中
     *
     * @param context
     * @param patchDexFile   dex文件路径
     * @param patchClassName 要加载的类
     */
    public static void patch(Context context, String patchDexFile, String patchClassName) {
        // dex文件存在
        if (patchDexFile != null && new File(patchDexFile).exists()) {
            try {
                // 阿里设备
                if (hasLexClassLoader()) {
                    injectInAliyunOs(context, patchDexFile, patchClassName);
                }
                // 非阿里设备
                else if (hasDexClassLoader()) {
                    injectAboveEqualApiLevel14(context, patchDexFile, patchClassName);
                } else {
                    injectBelowApiLevel14(context, patchDexFile, patchClassName);
                }
            } catch (Throwable th) {
            }
        }
    }

    /**
     * 阿里os中LexClassLoader的存在情况
     *
     * @return
     */
    private static boolean hasLexClassLoader() {
        try {
            Class.forName("dalvik.system.LexClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 判断BaseDexClassLoader的存在情况
     *
     * @return
     */
    private static boolean hasDexClassLoader() {
        try {
            Class.forName("dalvik.system.BaseDexClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void injectInAliyunOs(Context context, String patchDexFile, String patchClassName)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchFieldException {
        PathClassLoader obj = (PathClassLoader) context.getClassLoader();
        String replaceAll = new File(patchDexFile).getName().replaceAll("\\.[a-zA-Z0-9]+", ".lex");
        Class cls = Class.forName("dalvik.system.LexClassLoader");
        Object newInstance =
                cls.getConstructor(new Class[]{String.class, String.class, String.class, ClassLoader.class}).newInstance(
                        new Object[]{context.getDir("dex", 0).getAbsolutePath() + File.separator + replaceAll,
                                context.getDir("dex", 0).getAbsolutePath(), patchDexFile, obj});
        cls.getMethod("loadClass", new Class[]{String.class}).invoke(newInstance, new Object[]{patchClassName});
        setField(obj, PathClassLoader.class, "mPaths",
                appendArray(getField(obj, PathClassLoader.class, "mPaths"), getField(newInstance, cls, "mRawDexPath")));
        setField(obj, PathClassLoader.class, "mFiles",
                combineArray(getField(obj, PathClassLoader.class, "mFiles"), getField(newInstance, cls, "mFiles")));
        setField(obj, PathClassLoader.class, "mZips",
                combineArray(getField(obj, PathClassLoader.class, "mZips"), getField(newInstance, cls, "mZips")));
        setField(obj, PathClassLoader.class, "mLexs",
                combineArray(getField(obj, PathClassLoader.class, "mLexs"), getField(newInstance, cls, "mDexs")));
    }

    @TargetApi(14)
    private static void injectBelowApiLevel14(Context context, String str, String str2)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        PathClassLoader obj = (PathClassLoader) context.getClassLoader();
        DexClassLoader dexClassLoader =
                new DexClassLoader(str, context.getDir("dex", 0).getAbsolutePath(), str, context.getClassLoader());
        dexClassLoader.loadClass(str2);
        setField(obj, PathClassLoader.class, "mPaths",
                appendArray(getField(obj, PathClassLoader.class, "mPaths"), getField(dexClassLoader, DexClassLoader.class,
                        "mRawDexPath")
                ));
        setField(obj, PathClassLoader.class, "mFiles",
                combineArray(getField(obj, PathClassLoader.class, "mFiles"), getField(dexClassLoader, DexClassLoader.class,
                        "mFiles")
                ));
        setField(obj, PathClassLoader.class, "mZips",
                combineArray(getField(obj, PathClassLoader.class, "mZips"), getField(dexClassLoader, DexClassLoader.class,
                        "mZips")));
        setField(obj, PathClassLoader.class, "mDexs",
                combineArray(getField(obj, PathClassLoader.class, "mDexs"), getField(dexClassLoader, DexClassLoader.class,
                        "mDexs")));
        obj.loadClass(str2);
    }

    /**
     * 从dex文件中加载对应的类
     *
     * @param context
     * @param str     dex文件路径
     * @param str2    要加载的类
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static void injectAboveEqualApiLevel14(Context context, String str, String str2)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        // PathClassLoader
        PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
        // ----------合并数组--------
        // 合并数组
        Object a = combineArray(
                // PathClassLoader中的dexElements数组
                getDexElements(getPathList(pathClassLoader)),
                // DexClassLoader 加载本地dex文件后,dexElements数组
                getDexElements(getPathList(new DexClassLoader(str, context.getDir("dex", 0).getAbsolutePath(), str, context.getClassLoader())))
        );
        //--------dexElements数组赋值---------
        // 获取pathList
        Object a2 = getPathList(pathClassLoader);
        setField(a2, a2.getClass(), "dexElements", a);
        //--------加载str2类---------
        pathClassLoader.loadClass(str2);
    }

    /**
     * 获取dalvik.system.BaseDexClassLoader中的pathList
     *
     * @param obj PathClassLoader
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Object getPathList(Object obj) throws ClassNotFoundException, NoSuchFieldException,
            IllegalAccessException {
        return getField(obj, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    /**
     * 获取dexClassloader中的dexElements
     *
     * @param obj
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Object getDexElements(Object obj) throws NoSuchFieldException, IllegalAccessException {
        return getField(obj, obj.getClass(), "dexElements");
    }

    /**
     * 获取 cls的str方法
     *
     * @param obj
     * @param cls
     * @param str
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Object getField(Object obj, Class cls, String str)
            throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField.get(obj);
    }

    /**
     * 将 obj2数据赋值给str方法
     *
     * @param obj
     * @param cls  对应的类
     * @param str  方法名称
     * @param obj2 要设置的数据
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static void setField(Object obj, Class cls, String str, Object obj2)
            throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        declaredField.set(obj, obj2);
    }

    /**
     * 数组合并
     *
     * @param obj  原数组
     * @param obj2 新增内容的数组
     * @return
     */
    private static Object combineArray(Object obj, Object obj2) {
        //
        Class componentType = obj2.getClass().getComponentType();
        // 新增内容的数组 长度
        int length = Array.getLength(obj2);
        // "原数组"长度 + "新增内容的数组"长度
        int length2 = Array.getLength(obj) + length;
        // 创建一个新的数组(长度为："原数组"长度 + "新增内容的数组"长度 )
        Object newInstance = Array.newInstance(componentType, length2);
        //
        for (int i = 0; i < length2; i++) {
            // 将"新增内容的数组"中元素放到前边
            if (i < length) {
                Array.set(newInstance, i, Array.get(obj2, i));
            }
            // "原数组"内容放到后边
            else {
                Array.set(newInstance, i, Array.get(obj, i - length));
            }
        }
        return newInstance;
    }

    private static Object appendArray(Object obj, Object obj2) {
        Class componentType = obj.getClass().getComponentType();
        int length = Array.getLength(obj);
        Object newInstance = Array.newInstance(componentType, length + 1);
        Array.set(newInstance, 0, obj2);
        for (int i = 1; i < length + 1; i++) {
            Array.set(newInstance, i, Array.get(obj, i - 1));
        }
        return newInstance;
    }
}