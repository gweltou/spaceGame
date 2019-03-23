package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

public interface MovingObject {
	public Vector2 getPosition();
	public Vector2 getSpeed();
	public float getAngularSpeed();
	public float getAngle();
	public float getAngleDiff(float refAngle);
	public float getMass();
	public void push(Vector2 force);
	public void render(MyRenderer renderer);
	void dispose();
}
