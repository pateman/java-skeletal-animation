package pl.pateman.core.text;

import pl.pateman.core.texture.Texture;

import java.util.Map;

/**
 * Created by pateman.
 */
public interface TextFont {
    /**
     * The default set of characters a font should consist of.
     */
    String DEFAULT_FONT_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789,.-;:()+$@! '_";

    /**
     * Returns a map of font's glyphs. A glyph holds properties of a single character.
     *
     * @return {@code Map<Character, TextGlyph>}.
     */
    Map<Character, TextGlyph> getGlyphs();

    /**
     * Returns the font's OpenGL-ready texture. Every {@code TextFont} needs to create an OpenGL-compatible texture
     * that can be used for rendering texts.
     *
     * @return {@code Texture}.
     */
    Texture getFontTexture();

    /**
     * Returns the height of the font.
     *
     * @return {@code int}.
     */
    int getFontHeight();

    /**
     * Determines whether the font has been loaded or not.
     *
     * @return {@code true} if it has, {@code false} otherwise.
     */
    default boolean isLoaded() {
        return this.getFontTexture() != null && this.getFontTexture().isLoaded() && this.getGlyphs() != null &&
                !this.getGlyphs().isEmpty();
    }
}
