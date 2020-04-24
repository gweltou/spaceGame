package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.entities.Planet;

import java.util.Arrays;


public class RockLayer {
    private final DelaunayRock[] rocks;
    private final float[] xCoords;
    private final float surfaceLength;
    private final Terrain terrain;

    public RockLayer(Planet planet, Terrain terrain) {
        this.surfaceLength = planet.surfaceLength;
        this.terrain = terrain;
        float transparency = Math.min(1f, MathUtils.random(0.6f, 4f));
        float rockDensity = Math.max(0.0f, MathUtils.random(-0.1f, 0.15f));
        int numRocks = (int) (rockDensity * surfaceLength);

        long time0 = TimeUtils.millis();

        xCoords = new float[numRocks];
        rocks = new DelaunayRock[numRocks];
        for (int i=0; i<numRocks; i++) {
            xCoords[i] = MathUtils.random(surfaceLength);
            float radius = MathUtils.random(3f, 8f);
            int nPoints = (int) Math.ceil(2*radius);
            float hue = planet.colorHue + MathUtils.random(-16f, 16f);
            rocks[i] = new DelaunayRock(new Vector2(), nPoints, radius, hue, transparency);
        }
        Arrays.sort(xCoords);

        long elapsed = TimeUtils.millis()-time0;

        System.out.print("RockLayer created with " + rocks.length + " rocks in " + elapsed + " ms");
        System.out.println(" (" + rockDensity + " density)");
    }

    public DelaunayRock[] rocksBetween(float leftCoord, float rightCoord) {
        DelaunayRock[] selectedRocks;
        float offset = 0.0f;

        while (leftCoord < 0.0f) {
            leftCoord += surfaceLength;
            rightCoord += surfaceLength;
            offset -= surfaceLength;
        }
        while (leftCoord >= surfaceLength) {
            leftCoord -= surfaceLength;
            rightCoord -= surfaceLength;
            offset += surfaceLength;
        }

        if (rightCoord > surfaceLength) {
            // Add coordinates from leftCoord to surfaceLength an from 0 to rightCoord-surfaceLength
            int leftIndex = findLeftIndex(leftCoord);
            int rightIndex = findLeftIndex(rightCoord-surfaceLength);
            selectedRocks = new DelaunayRock[rocks.length - leftIndex + rightIndex];
            int i = 0;
            for (; i<rocks.length-leftIndex; i++) {
                DelaunayRock r = rocks[leftIndex+i];
                float x = xCoords[leftIndex+i] + offset;
                r.setPosition(new Vector2(x, terrain.getHeight(x)-2f));
                selectedRocks[i] = r;
            }
            for (int j=0; j<rightIndex; j++) {
                DelaunayRock r = rocks[j];
                float x = xCoords[j] + offset + surfaceLength;
                r.setPosition(new Vector2(x, terrain.getHeight(x)-2f));
                selectedRocks[i+j] = r;
            }
        } else {
            int leftIndex = findLeftIndex(leftCoord);
            int rightIndex = findLeftIndex(rightCoord);
            selectedRocks = new DelaunayRock[rightIndex - leftIndex];
            for (int i=0; i<selectedRocks.length; i++) {
                DelaunayRock r = rocks[leftIndex+i];
                float x = xCoords[leftIndex+i] + offset;
                r.setPosition(new Vector2(x, terrain.getHeight(x)-2f));
                selectedRocks[i] = r;
            }
        }

        return selectedRocks;
    }

    int findLeftIndex(float x) {
        int left = 0;
        int right = xCoords.length - 1;
        int mid;
        while (left <= right) {
            mid = (int) Math.floor((left + right) / 2f);
            if (xCoords[mid] < x)
                left = mid + 1;
            else if (xCoords[mid] > x)
                right = mid - 1;
        }
        return left;
    }
}
