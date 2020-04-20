package com.gwel.spacegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

public class HUD implements Disposable {
    private static SpaceGame game;
    private final BitmapFont dialogFont;
    private final BitmapFont planetNameFont;
    private static final int BOX_PADDING = 10;
    private static final int BOX_MARGIN = 10;
    private final float fontHeight;
    private static final int align = Align.left;
    private static float dialog_box_width;
    private float dialog_box_height;
    private String dialogText;
    private final BitmapFontCache planetName;
    private boolean showPlanetName;
    private long planetNameHideTime;
    private long dialogHideTime;

    public HUD(SpaceGame game) {
        HUD.game = game;
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("Amaranth-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.color = new Color(1f, 1f, 1f, 1.0f);
        dialogFont = fontGenerator.generateFont(parameter);
        fontGenerator.dispose();
        planetNameFont = new BitmapFont(Gdx.files.internal("greeknames.fnt"));

        fontHeight = dialogFont.getCapHeight() - dialogFont.getDescent() * 2;
        dialog_box_width = Gdx.graphics.getWidth() - 2*BOX_MARGIN;
        dialog_box_height = fontHeight + 2*BOX_PADDING;
        dialogText = "";

        showPlanetName = false;
        planetName = planetNameFont.newFontCache();
    }

    public void dialog(String text) {
        dialogText = text;
    }

    public void tempDialog(String text) {
        dialogText = text;
        dialogHideTime = TimeUtils.millis() + 100 * text.length();
    }

    public void showPlanetName(String name) {
        GlyphLayout layout = new GlyphLayout(planetNameFont, name, Color.WHITE, 0f, Align.center, false);
        planetName.setText(layout,
                    Gdx.graphics.getWidth()/2f,
                    Gdx.graphics.getHeight() - planetNameFont.getCapHeight());
        showPlanetName = true;
        planetNameHideTime = TimeUtils.millis() + 6000;
    }

    public void update() {
        long now = TimeUtils.millis();
        if (showPlanetName && now > planetNameHideTime) {
            showPlanetName = false;
        }
        if (!dialogText.isEmpty() && now > dialogHideTime) {
            dialogText = "";
        }
    }

    public void render() {
        // Display dialog box
        if (!dialogText.isEmpty()) {
            game.renderer.setProjectionMatrix(MyCamera.normal);

            // Draw transparent container box
            game.renderer.setColor(1f, 1f, 1f, 0.1f);
            game.renderer.triangle( BOX_MARGIN, BOX_MARGIN,
                                    BOX_MARGIN, dialog_box_height+BOX_MARGIN,
                                    dialog_box_width+BOX_MARGIN, BOX_MARGIN);
            game.renderer.triangle( dialog_box_width+BOX_MARGIN, BOX_MARGIN,
                                    BOX_MARGIN, dialog_box_height+BOX_MARGIN,
                                    dialog_box_width+BOX_MARGIN, dialog_box_height+BOX_MARGIN);
            game.renderer.flush();
        }

        //game.batch.setProjectionMatrix(MyCamera.normal);
        game.batch.begin();

        // Display dialog
        if (!dialogText.isEmpty()) {
            float x = BOX_MARGIN + BOX_PADDING;
            float y = BOX_MARGIN + dialog_box_height - BOX_PADDING;
            float targetWidth = dialog_box_width - 2*BOX_PADDING;
            GlyphLayout gl = dialogFont.draw(game.batch, dialogText, x, y, targetWidth, align, true);
            dialog_box_height = gl.height + 2*BOX_PADDING;
        }

        // Display planet's name
        if (showPlanetName) {
            planetName.draw(game.batch);
        }

        game.batch.end();
    }

    @Override
    public void dispose() {
        dialogFont.dispose();
    }
}