package pl.pateman.skeletal.texture;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import pl.pateman.skeletal.Clearable;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Created by pateman.
 */
public class Texture implements Clearable {
    private int unit;
    private final int handle;
    private boolean bound;
    private TextureInformation textureInformation;
    private TextureWrapping textureWrappingT;
    private TextureWrapping textureWrappingS;
    private TextureFiltering textureFiltering;

    public Texture() {
        this.unit = -1;
        this.handle = glGenTextures();
        this.textureWrappingT = TextureWrapping.REPEAT;
        this.textureWrappingS = TextureWrapping.REPEAT;
        this.textureFiltering = TextureFiltering.BILINEAR;
        this.bound = false;
    }

    public Texture(final TextureInformation textureInformation) {
        this();

        this.textureInformation = textureInformation;
        if (this.textureInformation == null || this.textureInformation.getTextureSize() == 0) {
            throw new IllegalArgumentException();
        }
    }

    public static final void bindTexture(int texUnit, int texHandle) {
        glActiveTexture(GL_TEXTURE0 + texUnit);
        glBindTexture(GL_TEXTURE_2D, texHandle);
    }

    public void bind() {
        if (!this.isLoaded()) {
            throw new IllegalStateException("Texture needs to be loaded and have a unit assigned");
        }

        Texture.bindTexture(this.unit, this.handle);

        switch (this.textureFiltering) {
            case BILINEAR:
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 1.0f);
                break;
            case TRILINEAR:
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 1.0f);
                break;
            case AF2X:
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 2.0f);
                break;
            case AF4X:
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 4.0f);
                break;
            case AF8X:
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 8.0f);
                break;
            case AF16X:
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);
                break;
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, this.getTextureWrappingS().getGlValue());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, this.getTextureWrappingT().getGlValue());

        this.bound = true;
    }

    public void unbind() {
        if (this.bound) {
            Texture.bindTexture(this.unit, 0);

            this.bound = false;
        }
    }

    public boolean isBound() {
        return bound;
    }

    public boolean isLoaded() {
        return this.unit > -1 && this.textureInformation != null && this.textureInformation.getTextureSize() > 0;
    }

    @Override
    public void clear() {
        this.unbind();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
        glDeleteTextures(this.handle);
    }

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public int getHandle() {
        return handle;
    }

    public TextureInformation getTextureInformation() {
        return textureInformation;
    }

    public TextureWrapping getTextureWrappingT() {
        return textureWrappingT;
    }

    public void setTextureWrappingT(TextureWrapping textureWrappingT) {
        this.textureWrappingT = textureWrappingT;
    }

    public TextureWrapping getTextureWrappingS() {
        return textureWrappingS;
    }

    public void setTextureWrappingS(TextureWrapping textureWrappingS) {
        this.textureWrappingS = textureWrappingS;
    }

    public TextureFiltering getTextureFiltering() {
        return textureFiltering;
    }

    public void setTextureFiltering(TextureFiltering textureFiltering) {
        this.textureFiltering = textureFiltering;
    }
}
