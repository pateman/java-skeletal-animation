package pl.pateman.core.text;

/**
 * Created by pateman.
 */
public final class TextGlyph {
    private final int w;
    private final int h;
    private final int x;
    private final int y;

    public TextGlyph(int w, int h, int x, int y) {
        this.w = w;
        this.h = h;
        this.x = x;
        this.y = y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
