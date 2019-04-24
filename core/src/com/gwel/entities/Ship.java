package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;

public interface Ship {
	public void addHit();
	public void push(Vector2 scl);
	public void push(Vector2 scl, Vector2 point);
	public void hit(float damage);
}
