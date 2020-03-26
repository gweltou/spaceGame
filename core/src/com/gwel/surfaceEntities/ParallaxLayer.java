package com.gwel.surfaceEntities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

import java.util.ArrayList;
import java.util.Collections;

public class ParallaxLayer {
    private final RandomXS128 generator;
    private final HeightArray[] heightArrays;
    private final float[] amps;
    private final float scale;
    private final XenoTreeManager xtm;
    private final boolean withTrees;
    private final Color color;

    public ParallaxLayer(RandomXS128 generator, HeightArray[] hArrays, float[] amps, Vector2 position, float scale, XenoTreeManager xtm, boolean withTrees, Color col) {
        this.generator = generator;
        this.heightArrays = hArrays;
        this.amps = amps;
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
        float h = 0.0f;
        for (int j=0; j<heightArrays.length; j++)
            h += amps[j] * heightArrays[j].getHeight(position);
        return h;
    }

    private Vector2[] createBlockMesh(float leftCoord, float rightCoord) {
        // CALCULATE MESH VERTICES HEIGHT COORDINATE

        ArrayList<Float> xCoords = new ArrayList<Float>();
        xCoords.add(leftCoord-0.01f);	// Add leftmost boundary coordinate
        for (HeightArray ha: heightArrays) {
            // Calculate a decimal index (between 0 and values.length) of leftCoord in height Array
            float normalizedIdx = ha.values.length * leftCoord / ha.span;
            normalizedIdx = normalizedIdx % ha.values.length;
            // Wrap around if index is negative
            if (normalizedIdx < 0.0f)	normalizedIdx += ha.values.length;

            float stepLength = ha.span / (ha.values.length * scale);	// length between each value (in game units)
            float stepToNextIndex = ha.vpu * (MathUtils.ceil(normalizedIdx) - normalizedIdx);	// length to next integer (in game units)
            float nextLength = leftCoord + stepToNextIndex;
            while (nextLength < rightCoord) {
                xCoords.add(nextLength);
                nextLength += stepLength;
            }
        }
        xCoords.add(rightCoord+0.01f);	// add rightmost boundary coordinate
        Collections.sort(xCoords);

        // Remove duplicate coords
        ArrayList<Float> tmpArray = new ArrayList<Float>();
        float lastItem = xCoords.get(0);
        tmpArray.add(lastItem);
        for (int i=1; i<xCoords.size(); i++) {
            float nextItem = xCoords.get(i);
            if (Math.abs(lastItem-nextItem) > 0.01f)
                tmpArray.add(nextItem);
            lastItem = nextItem;
        }
        xCoords = tmpArray;

        // Now convert every horizontal coordinate to a height value
        Vector2[] mesh = new Vector2[xCoords.size()];
        int i = 0;
        for (float xCoord: xCoords) {
            mesh[i++] = new Vector2(xCoord-leftCoord, getHeight(xCoord));
        }

        return mesh;
    }

    public void render(MyRenderer renderer) {
        Vector2 camCenter = renderer.camera.center.cpy();
        float leftBoundary = camCenter.x + (renderer.camera.sw.x-camCenter.x) / scale;
        float rightBoundary = camCenter.x + (renderer.camera.ne.x-camCenter.x) / scale;
        Vector2[] mesh = createBlockMesh(leftBoundary, rightBoundary);
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
