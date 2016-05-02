package pl.pateman.core.texture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by pateman.
 */
public final class TextureInformation {
    private final ByteBuffer imageBuffer;
    private final int width;
    private final int height;
    private final int textureSize;
    private final boolean alphaChannel;

    TextureInformation(final byte[] image, int w, int h, boolean hasAlpha) {
        this.imageBuffer = ByteBuffer.allocateDirect(image.length);
        this.width = w;
        this.height = h;
        this.textureSize = image.length;
        this.alphaChannel = hasAlpha;

        this.imageBuffer.order(ByteOrder.nativeOrder());
        this.imageBuffer.put(image, 0, image.length);
        this.imageBuffer.flip();
    }

    public ByteBuffer getImageBuffer() {
        return imageBuffer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTextureSize() {
        return textureSize;
    }

    public boolean hasAlphaChannel() {
        return alphaChannel;
    }
}
