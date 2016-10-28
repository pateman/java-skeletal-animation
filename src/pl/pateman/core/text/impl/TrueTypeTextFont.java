package pl.pateman.core.text.impl;

import pl.pateman.core.text.TextFont;
import pl.pateman.core.text.TextGlyph;
import pl.pateman.core.texture.Texture;
import pl.pateman.core.texture.TextureLoader;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates a font texture from a TrueType font.
 *
 * Created by pateman.
 */
public final class TrueTypeTextFont implements TextFont {
    private final Map<Character, TextGlyph> fontGlyphs;
    private Texture fontTexture;

    public TrueTypeTextFont(final String fontName, final int fontSize, final String additionalChars) {
        this.fontGlyphs = new HashMap<>(DEFAULT_FONT_CHARSET.length());

        final Font font = new Font(fontName, Font.PLAIN, fontSize);
        this.createTextFont(font, additionalChars == null ? "" : additionalChars);
    }

    public TrueTypeTextFont(final InputStream fontInputStream, final int fontSize, final String additionalChars) {
        this.fontGlyphs = new HashMap<>(DEFAULT_FONT_CHARSET.length());

        try {
            final Font font = Font.createFont(Font.TRUETYPE_FONT, fontInputStream).deriveFont(Font.PLAIN, fontSize);
            this.createTextFont(font, additionalChars == null ? "" : additionalChars);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException("Unable to read font", e);
        }
    }

    private BufferedImage createCharacter(final Font font, final char character) {
        BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        //  Retrieve the font metrics.
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(font);
        final FontMetrics fontMetrics = graphics.getFontMetrics();
        graphics.dispose();

        final int charWidth = fontMetrics.charWidth(character);
        final int charHeight = fontMetrics.getHeight();

        if (charWidth == 0) {
            return null;
        }

        //  Now that we have the metrics, create the actual image of the character.
        bufferedImage = new BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB);
        graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setPaint(Color.WHITE);
        graphics.drawString(String.valueOf(character), 0, fontMetrics.getAscent());
        graphics.dispose();

        return bufferedImage;
    }

    private void createTextFont(final Font font, final String additionalChars) {
        //  Prepare the charset.
        final String charset = DEFAULT_FONT_CHARSET + additionalChars;

        final Map<Character, BufferedImage> characterBufferedImageMap = new LinkedHashMap<>(charset.length());
        int textureWidth = 0;
        int textureHeight = 0;

        //  Loop through the charset's characters and build an image of each character in the set.
        for (int i = 0; i < charset.length(); i++) {
            final char character = charset.charAt(i);

            //  Generate an image for the character.
            final BufferedImage characterImg = this.createCharacter(font, character);
            if (characterImg == null) {
                System.out.printf("The font '%s' does not contain the character '%s'\n", font.getFontName(), character);
                continue;
            }

            //  Calculate the texture image's dimensions.
            textureWidth += characterImg.getWidth();
            textureHeight = Math.max(textureHeight, characterImg.getHeight());

            //  Put the character in the map.
            characterBufferedImageMap.put(character, characterImg);
        }

        //  Create the texture's image.
        BufferedImage texImg = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D texImgGraphics = texImg.createGraphics();

        //  Iterate through the characters' images and create a glyph for each of them.
        int x = 0, charWidth, charHeight;
        for (final Map.Entry<Character, BufferedImage> entry : characterBufferedImageMap.entrySet()) {
            final BufferedImage bufferedImage = entry.getValue();
            charWidth = bufferedImage.getWidth();
            charHeight = bufferedImage.getHeight();

            //  Additionally, render the character's image on the overall texture.
            texImgGraphics.drawImage(bufferedImage, x, 0, null);

            final TextGlyph glyph = new TextGlyph(charWidth, charHeight, x, textureHeight - charHeight);
            this.fontGlyphs.put(entry.getKey(), glyph);

            x += charWidth;
        }

        //  Transform the texture so that its origin is at the bottom left.
        final AffineTransform affineTransform = AffineTransform.getScaleInstance(1.0f, -1.0f);
        affineTransform.translate(0.0f, -textureHeight);
        texImg = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(texImg, null);

        //  For debugging only.
//        final File file = new File("D:\\" + font.getName() + "_generated.png");
//        try {
//            ImageIO.write(texImg, "png", file);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //  Create OpenGL's texture from the texture image.
        this.fontTexture = new TextureLoader().load(texImg);

        texImgGraphics.dispose();
    }

    @Override
    public Map<Character, TextGlyph> getGlyphs() {
        return this.fontGlyphs;
    }

    @Override
    public Texture getFontTexture() {
        return this.fontTexture;
    }
}
