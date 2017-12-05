/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package dodola.hotfix;

import java.io.File;

import android.app.Application;
import android.content.Context;

import dodola.hotfixlib.HotFix;

/**
 * Created by sunpengfei on 15/11/4.
 * <p>
 * 1、dexClassLoader加载hackdex.jar(dodola.hackdex.AntilazyLoad)
 * 2、hack_dex中的.dex文件，动态添加到BaseDexClassLoader对象间接引用的dexElements数组中
 */
public class HotfixApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //
        File dexPath = new File(getDir("dex", Context.MODE_PRIVATE), "hackdex_dex.jar");
        // assets下的hackdex_dex.jar文件拷贝到/data/data/packagename/dex/hackdex_dex.jar 路径下
        Utils.prepareDex(this.getApplicationContext(), dexPath, "hackdex_dex.jar");
        // 将hack_dex 动态添加到BaseDexClassLoader对象间接引用的dexElements数组中
        HotFix.patch(this, dexPath.getAbsolutePath(), "dodola.hackdex.AntilazyLoad");
        // 加载dodola.hackdex.AntilazyLoad
        try {
            this.getClassLoader().loadClass("dodola.hackdex.AntilazyLoad");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
