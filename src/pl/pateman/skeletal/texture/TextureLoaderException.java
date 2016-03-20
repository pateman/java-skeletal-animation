package pl.pateman.skeletal.texture;

/**
 * Created by pateman.
 */
public final class TextureLoaderException extends Exception {
    public TextureLoaderException(String msg) {
        super(msg);
    }

    public TextureLoaderException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
