package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gwel.entities.Spaceship;
import com.gwel.spacegame.MyRenderer;


public class LandedShip {
    private final Affine2 transform = new Affine2();
    private final Vector2 position = new Vector2();
    private float angle;
    private final Vector2 p1_tmp = new Vector2();
    private final Vector2 p2_tmp = new Vector2();
    private final Vector2 p3_tmp = new Vector2();


    public Vector2 getPosition() {
        return position.cpy();
    }

    public void setPosition(Vector2 pos) {
        position.set(pos);
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float a) {
        angle = a;
    }

    public void render(MyRenderer renderer) {
        transform.idt();
        transform.translate(getPosition());
        transform.rotateRad(getAngle() + MathUtils.PI/2);
        transform.scale(4f, 4f);
        renderer.pushMatrix(transform);
        for (float[] triangle : Spaceship.triangles) {
            p1_tmp.set(triangle[0], triangle[1]);
            p2_tmp.set(triangle[2], triangle[3]);
            p3_tmp.set(triangle[4], triangle[5]);
            renderer.setColor(triangle[6], triangle[7], triangle[8], triangle[9]);
            renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
        }
        renderer.popMatrix();
    }
}
