package com.gwel.entities;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import com.gwel.spacegame.MyRenderer;


public class Asteroid extends PhysicBody {
    static private final float MAX_RADIUS = 8f;
    static private final int NUM_POINTS = 16;

    private final float[] triangle;
    private final Affine2 transform = new Affine2();
    private final Vector2 p1_tmp = new Vector2();
    private final Vector2 p2_tmp = new Vector2();
    private final Vector2 p3_tmp = new Vector2();

    public Asteroid(Vector2 pos) {
        super(pos, 0f);
        DelaunayTriangulator dt = new DelaunayTriangulator();

        // Generate random points cloud
        float[] points = new float[2 * NUM_POINTS];
        for (int i = 0; i < points.length;) {
            points[i++] = MathUtils.random(-MAX_RADIUS, MAX_RADIUS);
            points[i++] = MathUtils.random(-MAX_RADIUS, MAX_RADIUS);
        }

        // Triangles is a list of point indices, with each triad making a triangle
        ShortArray triangleIndices = dt.computeTriangles(points, false);

        // Fill triangle vertices array
        int nTriangles = triangleIndices.size / 3;
        triangle = new float[9 * nTriangles];
        int pi; // point index
        int ti = 0; // triangle index
        for (int i=0; i<triangle.length;) {
            pi = triangleIndices.get(ti++);
            triangle[i++] = points[2*pi];
            triangle[i++] = points[2*pi+1];
            pi = triangleIndices.get(ti++);
            triangle[i++] = points[2*pi];
            triangle[i++] = points[2*pi+1];
            pi = triangleIndices.get(ti++);
            triangle[i++] = points[2*pi];
            triangle[i++] = points[2*pi+1];
            triangle[i++] = 0.4f + MathUtils.random(0.4f); // Red
            triangle[i++] = MathUtils.random(0.2f); // Green
            triangle[i++] = 0.2f + MathUtils.random(0.2f); // Blue
        }
    }

    public void render(MyRenderer renderer) {
        transform.idt();
        transform.translate(getPosition());
        transform.rotateRad(getAngle() + MathUtils.PI/2);
        renderer.pushMatrix(transform);
        for (int i=0; i<triangle.length;) {
            p1_tmp.set(triangle[i], triangle[i+1]);
            i += 2;
            p2_tmp.set(triangle[i], triangle[i+1]);
            i += 2;
            p3_tmp.set(triangle[i], triangle[i+1]);
            i += 2;
            renderer.setColor(triangle[i], triangle[i+1], triangle[i+2], 0.5f);
            i += 3;
            renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
        }
        renderer.popMatrix();
    }

    @Override
    public float getBoundingRadius() {
        return 0;
    }
}