import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ListLayers {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.minecraft.client.render.RenderLayer");
        System.out.println("Methods returning RenderLayer:");
        for (Method m : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(clazz)) {
                System.out.println(m.getName());
            }
        }
    }
}
