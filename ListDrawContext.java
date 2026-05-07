import java.lang.reflect.*;
import java.net.*;
import java.io.*;
public class ListDrawContext {
    public static void main(String[] args) throws Exception {
        URL url = new java.io.File(args[0]).toURI().toURL();
        URLClassLoader cl = new URLClassLoader(new URL[]{url}, null);
        Class<?> dc = Class.forName("net.minecraft.client.gui.DrawContext", false, cl);
        for (Method m : dc.getDeclaredMethods()) {
            if (m.getName().contains("draw") || m.getName().contains("fill")) {
                System.out.println(m);
            }
        }
        cl.close();
    }
}
