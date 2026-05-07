import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class ClassDumper {
    public static void main(String[] args) throws Exception {
        // We will pass the jar path as argument
        if (args.length == 0) {
            System.out.println("No jar provided");
            return;
        }
        File f = new File(args[0]);
        URL[] urls = { f.toURI().toURL() };
        URLClassLoader cl = new URLClassLoader(urls);
        
        dumpClass(cl, "net.minecraft.client.render.RenderTickCounter");
        dumpClass(cl, "net.minecraft.client.gui.DrawContext");
        dumpClass(cl, "net.minecraft.client.gui.ParentElement");
        dumpClass(cl, "net.minecraft.client.option.KeyBinding");
    }

    private static void dumpClass(ClassLoader cl, String className) {
        try {
            Class<?> clazz = Class.forName(className, false, cl);
            System.out.println("Class: " + className);
            for (Method m : clazz.getDeclaredMethods()) {
                System.out.println("  Method: " + m.toString());
            }
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                System.out.println("  Constructor: " + c.toString());
            }
        } catch (Exception e) {
            System.out.println("Could not find class " + className);
        }
    }
}
