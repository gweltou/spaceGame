package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

public interface MovingObject {
	Vector2 getPosition();
	void setPosition(Vector2 pos);
	Vector2 getVelocity();
	float getAngularVelocity();
	float getAngle();
	float getAngleDiff(float refAngle);
	float getMass();
	void push(Vector2 force);
	void render(MyRenderer renderer);
	void dispose();
}
