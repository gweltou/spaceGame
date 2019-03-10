package com.gwel.spacegame;

import com.badlogic.gdx.math.Vector2;

public class AABB {
	Vector2 sw;
	Vector2 ne;

	AABB(Vector2 sw, Vector2 ne) {
		this.sw = sw;
		this.ne = ne;
	}

	boolean containsPoint(Vector2 p) {
		// North and East directions are POSITIVE !
		return (p.x >= sw.x && p.x < ne.x && p.y < ne.y && p.y >= sw.y);
	}

	boolean intersectsAABB(AABB other) {
		// North and East directions are POSITIVE !
		if (sw.x > other.ne.x || other.sw.x > ne.x || ne.y < other.sw.y || other.ne.y < sw.y)
			return false;
		return true;
	}
}
