package com.gwel.entities;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

public class ShipTail {
	int SIZE = 512;
	Vector2[][] table;
	int t_index;
	boolean t_full;
	Vector2 last_pos;
	Spaceship ship;
	private float alpha;
	private float alpha_step = 1.0f / SIZE;
	Vector2 local_pos;
	float radius;
	Vector2 tmp;
	Affine2 rotate;

	public ShipTail(Spaceship ship, Vector2 pos, float width) {
		this.table = new Vector2[SIZE][2];
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
		if (t_index == SIZE) {
			this.t_index = 0;
			this.t_full = true;
		}
	}

	public void render(MyRenderer renderer) {
		Vector2 p1 = new Vector2();
		Vector2 p2 = new Vector2();
		Vector2 p3 = new Vector2();
		Vector2 p4 = new Vector2();
		alpha = 0.0f;
		if (t_full) {
			for (int i=0; i<SIZE-1; i++) {
				alpha += alpha_step;
				p1.set(table[(i+t_index)%SIZE][0]);
				p2.set(table[(i+t_index)%SIZE][1]);
				p3.set(table[(i+1+t_index)%SIZE][0]);
				p4.set(table[(i+1+t_index)%SIZE][1]);
				renderer.setColor(0.5f, 1.0f-alpha, 0, alpha);
				renderer.triangle(p1, p2, p3);
				renderer.triangle(p3, p2, p4);
			}
		} else {
			alpha += (SIZE-t_index)*alpha_step;
			for (int i=0; i<t_index-1; i++) {
				alpha += alpha_step;
				p1.set(table[i][0]);
				p2.set(table[i][1]);
				p3.set(table[i+1][0]);
				p4.set(table[i+1][1]);
				renderer.setColor(0.5f, 1.0f-alpha, 0, alpha);
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