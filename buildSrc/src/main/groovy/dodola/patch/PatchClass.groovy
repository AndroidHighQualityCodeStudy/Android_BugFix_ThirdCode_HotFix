package dodola.patch;

import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtMethod
import javassist.CtNewConstructor
import javassist.CtNewMethod

/**
 * dodola.hotfi想构造方法中植入代码(植入的代码为"System.out.println(dodola.hackdex.AntilazyLoad.class);")
 */
public class PatchClass {
    /**
     * 构造方法中植入代码(植入的代码为"System.out.println(dodola.hackdex.AntilazyLoad.class);")
     * @param buildDir 是项目的build class目录,就是要注入的.class所在地
     * @param lib 这个是hackdex的目录,就是AntilazyLoad类的class文件所在地
     */
    public static void process(String buildDir, String lib) {
        // 打印AntilazyLoad所在目录
        println(lib)
        //
        ClassPool classes = ClassPool.getDefault()
        classes.appendClassPath(buildDir)
        classes.appendClassPath(lib)

        //---------------------------------------------------------------
        //找到BugClass.class
        CtClass c = classes.getCtClass("dodola.hotfix.BugClass")
        if (c.isFrozen()) {
            c.defrost()
        }
        // BugClass.class构造方法中注入代码
        println("====BugClass.class构造方法中注入代码====")
        def constructor = c.getConstructors()[0];
        constructor.insertBefore("System.out.println(dodola.hackdex.AntilazyLoad.class);")
        // 输出文件
        c.writeFile(buildDir)

        //---------------------------------------------------------------
        // 找到LoadBugClass.class
        CtClass c1 = classes.getCtClass("dodola.hotfix.LoadBugClass")
        if (c1.isFrozen()) {
            c1.defrost()
        }
        // LoadBugClass.class构造方法中注入代码
        println("====LoadBugClass.class构造方法中注入代码====")
        def constructor1 = c1.getConstructors()[0];
        constructor1.insertBefore("System.out.println(dodola.hackdex.AntilazyLoad.class);")
        // 输出文件
        c1.writeFile(buildDir)


    }

    static void growl(String title, String message) {
        def proc = ["osascript", "-e", "display notification \"${message}\" with title \"${title}\""].execute()
        if (proc.waitFor() != 0) {
            println "[WARNING] ${proc.err.text.trim()}"
        }
    }
}
