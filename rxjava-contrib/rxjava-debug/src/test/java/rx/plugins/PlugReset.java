package rx.plugins;

public class PlugReset {
    public static void reset() {
        RxJavaPlugins.getInstance().reset();
    }
}
