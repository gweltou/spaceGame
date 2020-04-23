package com.gwel.surfaceEntities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.gwel.entities.Planet;
import com.gwel.spacegame.MyRenderer;


public class ParallaxLayer {
    private final Terrain terrain;
    private final float scale;
    private final XenoTreeManager xtm;
    private final boolean withTrees;
    private final Color color;

    public ParallaxLayer(Planet planet, float scale, boolean withTrees, XenoTreeManager xtm) {
        this.terrain = new Terrain(4, planet.surfaceLength, scale);
        this.scale = scale;
        this.withTrees = withTrees;
        this.xtm = xtm;

        // Color
        float level = (float) (Math.log(scale)/Math.log(0.5));
        float colSat = planet.colorSat * ((float) Math.pow(0.6f, level));
        float colVal = planet.colorVal;
        for (int i=0; i<=level; i++)
            colVal += (1.0f-colVal) * 0.25f;
        color = new Color();
        color.a = 1.0f;
        color.fromHsv(planet.colorHue, colSat, colVal);
    }

    public float getHeight(float position) {
        return terrain.getHeight(position);
    }

    public void render(MyRenderer renderer) {
        Vector2 camCenter = renderer.camera.center.cpy();
        float leftBoundary = camCenter.x + (renderer.camera.sw.x-camCenter.x) / scale;
        float rightBoundary = camCenter.x + (renderer.camera.ne.x-camCenter.x) / scale;
        Vector2[] mesh = terrain.coordsBetween(leftBoundary, rightBoundary);
        // This could be optimized
        TerrainBlock block = new TerrainBlock(new Vector2(leftBoundary, 0f), mesh, color);

        // Trees
        if (withTrees) {
            float[] treeCoords = xtm.treesBetween(leftBoundary, rightBoundary);
            for (float x : treeCoords) {
                block.addTree(xtm.buildTree(x, getHeight(x) - 2f));
            }
        }

        Affine2 transform = new Affine2();
        transform.idt();
        transform.translate(0f, -1.5f/scale);
        transform.scale(scale, scale);
        // Weird translate so the parallax effect works
        transform.translate(-renderer.camera.center.x + renderer.camera.center.x/scale,
                -renderer.camera.center.y + renderer.camera.center.y/scale);

        renderer.pushMatrix(transform);
        block.renderTerrain(renderer);
        block.renderTrees(renderer);
        renderer.popMatrix();
    }
}
