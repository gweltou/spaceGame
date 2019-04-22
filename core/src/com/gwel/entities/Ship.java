package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;

public interface Ship {
	public void addDamage(int dmg);
	public void push(Vector2 scl);
	public void hit(float damage);
}
