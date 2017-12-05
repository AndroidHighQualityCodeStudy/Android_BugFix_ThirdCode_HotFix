/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package dodola.hotfix;

/**
 * Created by sunpengfei on 15/11/4.
 */
public class LoadBugClass {
    /**
     * 返回BugClass错误日志打印
     *
     * @return
     */
    public String getBugString() {
        BugClass bugClass = new BugClass();
        return bugClass.bug();
    }
}
