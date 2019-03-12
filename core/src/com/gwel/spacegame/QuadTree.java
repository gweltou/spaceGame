package com.gwel.spacegame;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;

import Entities.Planet;

public class QuadTree {
	AABB boundary;

	Planet element;

	QuadTree nw;
	QuadTree ne;
	QuadTree sw;
	QuadTree se;

	QuadTree(AABB boundary) {
		this.boundary = boundary;
		this.element = null;
		this.nw = null;
		this.ne = null;
		this.sw = null;
		this.se = null;
	}

	boolean insert(Planet p) {
		// Ignore objects that do not belong in this quad tree
		if (!boundary.containsPoint(p.getPosition()))
			return false;

		// If there is space in this quad tree and if doesn't have subdivisions, add the object here
		if (element == null && nw == null) {
			this.element = p;
			return true;
		}

		if (nw == null)
			subdivide();

		if (nw.insert(p))
			return true;
		if (ne.insert(p))
			return true;
		if (sw.insert(p))
			return true;
		if (se.insert(p))
			return true;

		return false;
	}

	void subdivide() {
		// create four children that fully divide this quad into four quads of equal area
		Vector2 center = boundary.sw.cpy().lerp(boundary.ne, 0.5f);
		Vector2 top = new Vector2(center.x, boundary.ne.y);
		Vector2 bottom = new Vector2(center.x, boundary.sw.y);
		Vector2 left = new Vector2(boundary.sw.x, center.y);
		Vector2 right = new Vector2(boundary.ne.x, center.y);

		this.nw = new QuadTree(new AABB(left, top));
		this.ne = new QuadTree(new AABB(center, boundary.ne));
		this.sw = new QuadTree(new AABB(boundary.sw, center));
		this.se = new QuadTree(new AABB(bottom, right));
	}

	ArrayList<Planet> query(AABB range) {
		ArrayList<Planet> inrange = new ArrayList<Planet>();
		
		if (boundary.intersectsAABB(range) == false)
			return inrange;

		if (element != null && range.containsPoint(element.getPosition()))
			inrange.add(element);

		if (nw == null)
			return inrange;

		inrange.addAll(nw.query(range));
		inrange.addAll(ne.query(range));
		inrange.addAll(sw.query(range));
		inrange.addAll(se.query(range));

		return inrange;
	}
}
