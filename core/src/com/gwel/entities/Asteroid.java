package com.gwel.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ShortArray;
import com.gwel.spacegame.Const;
import com.gwel.spacegame.MyRenderer;

import java.util.Arrays;


public class Asteroid extends PhysicBody implements Pool.Poolable {
    private final float[] convexHull;
    private final float[] triangles;
    private final Affine2 transform = new Affine2();
    private final Vector2 p1_tmp = new Vector2();
    private final Vector2 p2_tmp = new Vector2();
    private final Vector2 p3_tmp = new Vector2();
    private final float radius;

    public Asteroid() {
        super();

        // Generate random points cloud
        radius = MathUtils.random(Const.ASTEROID_MIN_RADIUS, Const.ASTEROID_MAX_RADIUS);
        int nPoints = (int) Math.ceil(2*radius);
        float[] points = pointCloud(nPoints, radius);

        // Compute convex hull
        float[] rawConvexHull = new ConvexHull().computePolygon(points, false).toArray();
        // Remove last point (same as first) and limit to 8 points (Box2d limit for polyShape)
        convexHull = Arrays.copyOf(rawConvexHull, Math.min(rawConvexHull.length-2, 16));

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
            Color col = new Color().fromHsv(320f, 0.4f, MathUtils.random(0.4f, 0.5f));
            triangles[i++] = col.r;
            triangles[i++] = col.g;
            triangles[i++] = col.b;
        }
    }

    private float[] pointCloud(int n, float radius) {
        // All points in a circle
        float[] points = new float[2*n];
        for (int i=0; i<points.length;) {
            float a = MathUtils.random(MathUtils.PI2);
            points[i++] = MathUtils.random(radius) * MathUtils.cos(a);
            points[i++] = MathUtils.random(radius) * MathUtils.sin(a);
        }
        return points;
    }

    public void initBody(World world) {
        super.initBody(world);

        PolygonShape shape = new PolygonShape();
        shape.set(convexHull);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.6f;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.6f;
        //fixtureDef.filter.categoryBits = 0x0002;
        Fixture fixture = body.createFixture(fixtureDef);
        //fixture.setUserData(Enums.SHIP);
        shape.dispose();
    }

    public void render(MyRenderer renderer) {
        transform.idt();
        transform.translate(getPosition());
        transform.rotateRad(getAngle());
        renderer.pushMatrix(transform);
        for (int i=0; i< triangles.length;) {
            p1_tmp.set(triangles[i], triangles[i+1]);
            i += 2;
            p2_tmp.set(triangles[i], triangles[i+1]);
            i += 2;
            p3_tmp.set(triangles[i], triangles[i+1]);
            i += 2;
            renderer.setColor(triangles[i], triangles[i+1], triangles[i+2], 1f);
            i += 3;
            renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
        }
        renderer.popMatrix();
    }

    @Override
    public float getBoundingRadius() {
        return radius;
    }

    @Override
    public void reset() {
        dispose();
    }
}