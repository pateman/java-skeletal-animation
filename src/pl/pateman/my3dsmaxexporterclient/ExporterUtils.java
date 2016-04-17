package pl.pateman.my3dsmaxexporterclient;

/**
 * Created by pateman.
 */
public final class ExporterUtils {
    private ExporterUtils() {

    }

    public static String decodeString(final String string) {
        return string.replaceAll("%20", " ");
    }
}
