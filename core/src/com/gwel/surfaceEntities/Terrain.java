package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;


public class Terrain {
    private ArrayList<Vector2> coords = new ArrayList<Vector2>();
    private final float surfaceLength;
    private final float step;

    public Terrain(int nLayers, float surfaceLength, float scale) {
        this.surfaceLength = surfaceLength;
        HeightArray[] hArrays = new HeightArray[nLayers];
        float vpu = MathUtils.randomTriangular(2.0f, 4.0f) * scale ;
        float amp = MathUtils.randomTriangular(0.01f, 0.02f) / scale;
        for (int i=0; i<nLayers; i++) {
            vpu /= Math.pow(MathUtils.randomTriangular(1.2f, 3.0f), i);
            amp *= Math.pow(MathUtils.randomTriangular(2.0f, 8.0f), 1);
            hArrays[i] = new HeightArray(surfaceLength, vpu, amp);
        }

        // Use layer with max resolution as a grid to calculate every height coordinate
        step = 1 / hArrays[0].vpu;
        for (float x = 0.0f; x < surfaceLength; x += step) {
            float y = 0.0f;
            for (HeightArray ha: hArrays)
                y += ha.getHeight(x);
            coords.add(new Vector2(x, y));
        }
        //System.out.println("Terrain created with " + coords.size() + " values");
        //System.out.println("Scale : " + scale);
    }

    public float getHeight(float x) {
        // x : a horizontal coordinate on planet surface (can be greater than planet length)
        // Calculate a decimal index (between 0 and coords.size()) from x
        float floatIdx = coords.size() * (x%surfaceLength) / surfaceLength;
        // Wrap around if index is negative
        while (floatIdx < 0.0f)
            floatIdx += coords.size();
        int il = MathUtils.floor(floatIdx); // % coords.size();
        float lVal = coords.get(il).y;
        float rVal = coords.get((il + 1) % coords.size()).y;
        float wr = floatIdx - il;
        float wl = 1.0f - wr;
        // interpolate between the 2 indices
        return lVal*wl + rVal*wr;
    }

    public Vector2[] coordsBetween(float leftCoord, float rightCoord) {
        // Returns an array starting offseted to start at position <= 0.0
        float offset = -leftCoord;

        while (leftCoord > surfaceLength) {
            leftCoord -= surfaceLength;
            rightCoord -= surfaceLength;
            offset += surfaceLength;
        }
        while (leftCoord < 0) {
            leftCoord += surfaceLength;
            rightCoord += surfaceLength;
            offset -= surfaceLength;
        }

        int leftIndex = MathUtils.floor(leftCoord / step);
        float pre_leftCoord = leftIndex * step;
        Vector2[] mesh = new Vector2[MathUtils.ceil((rightCoord-pre_leftCoord)/step) + 1];
        for (int i=0; i<mesh.length; i++) {
            mesh[i] = coords.get(leftIndex).cpy().add(offset, 0);
            leftIndex += 1;
            if (leftIndex >= coords.size()) {
                leftIndex = 0;
                offset += surfaceLength;
            }
        }
        return mesh;
    }
}
