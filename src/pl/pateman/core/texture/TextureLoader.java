package pl.pateman.core.texture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import pl.pateman.core.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Created by pateman.
 */
public final class TextureLoader {
    private static final ColorModel glAlphaColorModel = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            new int[]{8, 8, 8, 8},
            true,
            false,
            ComponentColorModel.TRANSLUCENT,
            DataBuffer.TYPE_BYTE
    );

    private static final ColorModel glColorModel = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            new int[]{8, 8, 8, 0},
            false,
            false,
            ComponentColorModel.OPAQUE,
            DataBuffer.TYPE_BYTE
    );

    public TextureLoader() {
        //  http://stackoverflow.com/a/20141447/759049
        ImageIO.setUseCache(false);
    }

    private TextureInformation loadTexture(final BufferedImage bufferedImage) {
        int texWidth = 2;
        int texHeight = 2;

        //  Find the closest power of 2 for the width and height of the produced texture.
        while (texWidth < bufferedImage.getWidth()) {
            texWidth *= 2;
        }
        while (texHeight < bufferedImage.getHeight()) {
            texHeight *= 2;
        }

        //  Create a raster that can be used by OpenGL as a source for a texture.
        WritableRaster raster;
        BufferedImage texImage;
        if (bufferedImage.getColorModel().hasAlpha()) {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
            texImage = new BufferedImage(glAlphaColorModel, raster, false, new Hashtable());
        } else {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 3, null);
            texImage = new BufferedImage(glColorModel, raster, false, new Hashtable());
        }

        //  Copy the source image into the produced image.
        Graphics g = texImage.getGraphics();
        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
        g.fillRect(0, 0, texWidth, texHeight);
        g.drawImage(bufferedImage, 0, 0, null);

        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();
        final TextureInformation textureInformation = new TextureInformation(data, texWidth, texHeight, bufferedImage.
                getColorModel().hasAlpha());
        g.dispose();

        return textureInformation;
    }

    private TextureInformation loadTextureInformation(final InputStream is) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            BufferedImage bufferedImage = ImageIO.read(bis);
            return this.loadTexture(bufferedImage);
        }
    }

    private Texture load(final InputStream is, int textureUnit) throws IOException {
        if (is == null || textureUnit < 0) {
            throw new IllegalArgumentException();
        }

        final TextureInformation textureInformation = this.loadTextureInformation(is);
        return this.createTexture(textureUnit, textureInformation);
    }

    private Texture createTexture(int textureUnit, final TextureInformation textureInformation) {
        final Texture texture = new Texture(textureInformation);
        texture.setUnit(textureUnit);

        //  Pass texture data to OpenGL.
        Texture.bindTexture(textureUnit, texture.getHandle());

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,
                textureInformation.hasAlphaChannel() ? GL11.GL_RGBA8 : GL11.GL_RGB, textureInformation.getWidth(),
                textureInformation.getHeight(), 0,
                textureInformation.hasAlphaChannel() ? GL11.GL_RGBA : GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE, textureInformation.getImageBuffer());
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        Texture.bindTexture(textureUnit, 0);
        return texture;
    }

    public Texture load(String resourcePath) throws TextureLoaderException {
        return this.load(resourcePath, 0);
    }

    public Texture load(String resourcePath, int textureUnit) throws TextureLoaderException {
        try (InputStream is = Utils.getResourceStream(resourcePath)) {
            return this.load(is, textureUnit);
        } catch (IOException e) {
            throw new TextureLoaderException("Unable to load texture", e);
        }
    }

    public Texture load(final byte[] textureBytes) throws TextureLoaderException {
        return this.load(textureBytes, 0);
    }

    public Texture load(final byte[] textureBytes, int textureUnit) throws TextureLoaderException {
        try (InputStream is = new ByteArrayInputStream(textureBytes)) {
            return this.load(is, textureUnit);
        } catch (IOException e) {
            throw new TextureLoaderException("Unable to load texture", e);
        }
    }

    public Texture load(final BufferedImage bufferedImage) {
        return this.load(bufferedImage, 0);
    }

    public Texture load(final BufferedImage bufferedImage, int textureUnit) {
        final TextureInformation textureInformation = this.loadTexture(bufferedImage);
        return this.createTexture(textureUnit, textureInformation);
    }
}
