package com.gwel.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

public class ShipTail {
	int bufferSize;
	Vector2[][] table;
	int t_index;
	boolean t_full;
	Vector2 last_pos;
	PhysicBody ship;
	Vector2 local_pos;
	float radius;
	private Color baseColor, color;
	private float dr, dg, db, da;
	Vector2 tmp;
	Affine2 rotate;

	public ShipTail(PhysicBody ship, Vector2 pos, float width, int buffer, Color col1, Color col2) {
		bufferSize = buffer;
		baseColor = col2;
		dr = (col1.r-col2.r) / bufferSize;
		dg = (col1.g-col2.g) / bufferSize;
		db = (col1.b-col2.b) / bufferSize;
		da = 1.0f / bufferSize;
		this.table = new Vector2[bufferSize][2];
		this.t_index = 0;
		this.t_full = false;
		this.ship = ship;
		local_pos = pos.cpy();
		radius = width/2.0f;
		tmp = new Vector2();
		rotate = new Affine2();
	}

	public void update() {
		Vector2[] points = new Vector2[2];
		rotate.idt();
		rotate.rotateRad(ship.getAngle() + MathUtils.PI/2);
		tmp.set(local_pos);
		rotate.applyTo(tmp);
		tmp.add(ship.getPosition());
		Vector2 tmp2 = ship.getVelocity();
		tmp2.set(-tmp2.y, tmp2.x).nor().scl(radius);
		points[0] = new Vector2(tmp).add(tmp2);
		points[1] = new Vector2(tmp).sub(tmp2);
		table[t_index] = points;
		t_index++;
		if (t_index == bufferSize) {
			this.t_index = 0;
			this.t_full = true;
		}
	}

	public void render(MyRenderer renderer) {
		Vector2 p1 = new Vector2();
		Vector2 p2 = new Vector2();
		Vector2 p3 = new Vector2();
		Vector2 p4 = new Vector2();
		color = baseColor.cpy();
		color.a = 0.0f;
		if (t_full) {
			for (int i=0; i<bufferSize-1; i++) {
				color.add(dr, dg, db, da);
				p1.set(table[(i+t_index)%bufferSize][0]);
				p2.set(table[(i+t_index)%bufferSize][1]);
				p3.set(table[(i+1+t_index)%bufferSize][0]);
				p4.set(table[(i+1+t_index)%bufferSize][1]);
				renderer.setColor(color);
				renderer.triangle(p1, p2, p3);
				renderer.triangle(p3, p2, p4);
			}
		} else {
			color.a = (bufferSize-t_index)*da;
			for (int i=0; i<t_index-1; i++) {
				color.add(dr, dg, db, da);
				p1.set(table[i][0]);
				p2.set(table[i][1]);
				p3.set(table[i+1][0]);
				p4.set(table[i+1][1]);
				renderer.setColor(color);
				renderer.triangle(p1, p2, p3);
				renderer.triangle(p3, p2, p4);			
			}
		}
		/*
		if (last_pos != null) {
			Vector2 p1 = last_pos;
			Vector2 p2 = ship.position;
			renderer.line(p1.x, p1.y, p2.x, p2.y);
		}*/
	}
}