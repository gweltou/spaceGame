package com.gwel.spacegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

public class UI implements Disposable {
    private static SpaceGame game;
    private FreeTypeFontGenerator fontGenerator;
    private BitmapFont dialogFont;
    private static final int BOX_PADDING = 10;
    private static final int BOX_MARGIN = 10;
    private final float fontHeight;
    private static float dialog_box_width;
    private float dialog_box_height;
    private String dialog_text;
    private static final int align = Align.left;

    public UI(SpaceGame game) {
        UI.game = game;
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("minotaur.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.color = new Color(1f, 1f, 1f, 1.0f);
        dialogFont = fontGenerator.generateFont(parameter);
        fontGenerator.dispose();
        fontHeight = dialogFont.getCapHeight() - dialogFont.getDescent() * 2;
        dialog_box_width = Gdx.graphics.getWidth() - 2*BOX_MARGIN;
        dialog_box_height = fontHeight + 2*BOX_PADDING;
    }

    public void dialog(String text) {
        dialog_text = text;
    }

    public void update() {

    }

    public void render() {
        if (!dialog_text.isEmpty()) {
            Matrix4 normalProj = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            game.renderer.setProjectionMatrix(normalProj);
            game.renderer.setColor(1f, 1f, 1f, 0.1f);
            game.renderer.triangle( BOX_MARGIN, BOX_MARGIN,
                                    BOX_MARGIN, dialog_box_height+BOX_MARGIN,
                                    dialog_box_width+BOX_MARGIN, BOX_MARGIN);
            game.renderer.triangle( dialog_box_width+BOX_MARGIN, BOX_MARGIN,
                                    BOX_MARGIN, dialog_box_height+BOX_MARGIN,
                                    dialog_box_width+BOX_MARGIN, dialog_box_height+BOX_MARGIN);
            game.renderer.flush();

            float x = BOX_MARGIN + BOX_PADDING;
            float y = BOX_MARGIN + dialog_box_height - BOX_PADDING;
            float targetWidth = dialog_box_width - 2*BOX_PADDING;
            game.batch.setProjectionMatrix(normalProj);
            game.batch.begin();
            GlyphLayout gl = dialogFont.draw(game.batch, dialog_text, x, y, targetWidth, align, true);
            dialog_box_height = gl.height + 2*BOX_PADDING;
            game.batch.end();
        }
    }

    @Override
    public void dispose() {
        dialogFont.dispose();
    }
}