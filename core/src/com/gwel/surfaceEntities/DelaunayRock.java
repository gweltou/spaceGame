package com.gwel.surfaceEntities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import com.gwel.spacegame.MyRenderer;

public class DelaunayRock {
    private final float[] triangles;
    private final Affine2 transform = new Affine2();
    private final Vector2 p1_tmp = new Vector2();
    private final Vector2 p2_tmp = new Vector2();
    private final Vector2 p3_tmp = new Vector2();
    private final Vector2 position = new Vector2();
    private final float alpha;

    public DelaunayRock(Vector2 pos, int nPoints, float radius, float hue, float alpha) {
        position.set(pos);
        this.alpha = alpha;

        // Generate random points cloud
        float[] points = pointCloud(nPoints, radius);

        // Triangles is a list of point indices, with each triad making a triangle
        ShortArray triangleIndices = new DelaunayTriangulator().computeTriangles(points, false);

        // Fill triangle vertices array
        int nTriangles = triangleIndices.size / 3;
        triangles = new float[9 * nTriangles];
        int pi; // point index
        int ti = 0; // triangle index
        for (int i=0; i< triangles.length;) {
            pi = triangleIndices.get(ti++);
            triangles[i++] = points[2*pi];
            triangles[i++] = points[2*pi+1];
            pi = triangleIndices.get(ti++);
            triangles[i++] = points[2*pi];
            triangles[i++] = points[2*pi+1];
            pi = triangleIndices.get(ti++);
            triangles[i++] = points[2*pi];
            triangles[i++] = points[2*pi+1];
            Color col = new Color().fromHsv(hue, 0.5f, MathUtils.random(0.3f, 0.4f));
            triangles[i++] = col.r;
            triangles[i++] = col.g;
            triangles[i++] = col.b;
        }
    }

    private float[] pointCloud(int n, float radius) {
        // All points in a circle + 2 points for bottom base
        float[] points = new float[2*n];
        points[0] = -radius;
        points[1] = 0f;
        points[2] = radius;
        points[3] = 0f;
        for (int i=4; i<points.length;) {
            float a = MathUtils.random(MathUtils.PI2);
            points[i++] = MathUtils.random(radius) * MathUtils.cos(a);
            points[i++] = MathUtils.random(radius) * MathUtils.sin(a) + radius;
        }
        return points;
    }

    public Vector2 getPosition() {
        return position.cpy();
    }

    public void setPosition(Vector2 pos) {
        position.set(pos);
    }

    public float getAngle() {
        return 0;
    }

    public void render(MyRenderer renderer) {
        transform.idt();
        transform.translate(getPosition());
        renderer.pushMatrix(transform);
        for (int i=0; i< triangles.length;) {
            p1_tmp.set(triangles[i], triangles[i+1]);
            i += 2;
            p2_tmp.set(triangles[i], triangles[i+1]);
            i += 2;
            p3_tmp.set(triangles[i], triangles[i+1]);
            i += 2;
            renderer.setColor(triangles[i], triangles[i+1], triangles[i+2], alpha);
            i += 3;
            renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
        }
        renderer.popMatrix();
    }
}
