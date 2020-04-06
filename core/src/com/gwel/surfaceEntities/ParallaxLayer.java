package com.gwel.surfaceEntities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;


public class ParallaxLayer {
    private final Terrain terrain;
    private final float scale;
    private final XenoTreeManager xtm;
    private final boolean withTrees;
    private final Color color;

    public ParallaxLayer(Terrain terrain, float scale, XenoTreeManager xtm, boolean withTrees, Color col) {
        this.terrain = terrain;
        this.scale = scale;
        this.xtm = xtm;
        this.withTrees = withTrees;

        // Color
        float[] hsv = new float[3];
        col.toHsv(hsv);
        float level = (float) (Math.log(scale)/Math.log(0.5));
        float colSat = hsv[1]*((float) Math.pow(0.6f, level));
        float colVal = hsv[2];
        for (int i=0; i<=level; i++)
            colVal += (1.0f-colVal)*0.25f;
        color = new Color();
        color.a = 1.0f;
        color.fromHsv(hsv[0], colSat, colVal);
    }

    public float getHeight(float position) {
        return terrain.getHeight(position);
    }

    public void render(MyRenderer renderer) {
        Vector2 camCenter = renderer.camera.center.cpy();
        float leftBoundary = camCenter.x + (renderer.camera.sw.x-camCenter.x) / scale;
        float rightBoundary = camCenter.x + (renderer.camera.ne.x-camCenter.x) / scale;
        Vector2[] mesh = terrain.coordsBetween(leftBoundary, rightBoundary);
        TerrainBlock block = new TerrainBlock(new Vector2(leftBoundary, 0f), mesh, color);

        // Trees
        if (withTrees) {
            float[] treeCoords = xtm.getCoordsBetween(leftBoundary, rightBoundary);
            for (float c : treeCoords) {
                block.addTree(xtm.buildTree(c, getHeight(c) - 2f));
            }
        }

        Affine2 transform = new Affine2();
        transform.idt();
        //transform.translate(0f, 10f);
        transform.scale(scale, scale);
        // Weird translate so the parallax effect works
        transform.translate(-renderer.camera.center.x + renderer.camera.center.x/scale,
                -renderer.camera.center.y + renderer.camera.center.y/scale);

        renderer.pushMatrix(transform);
        block.render(renderer);
        renderer.popMatrix();
    }
}
