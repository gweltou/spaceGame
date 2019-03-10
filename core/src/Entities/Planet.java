package Entities;

import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class Planet {
	public static final float MIN_RADIUS = 10.0f;
	public static final float MAX_RADIUS = 100.0f;
	static final int MAX_SAT = 5;

	public Vector2 position;
	public float radius;
	float mass;
	Color col;
	int n_sat;
	ArrayList<Satellite> satellites;

	public Planet(Vector2 pos, float rad) {
		this.position = pos;
		this.radius = rad;
		this.col = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		this.mass = radius*radius*0.1f;

		// Create satellites, if any
		n_sat = (int) Math.floor(MathUtils.random(MAX_SAT+1));
		this.satellites = new ArrayList<Satellite>();
		for (int i=0; i<n_sat; i++) {
			float orbit_radius = MathUtils.random(radius+5, radius*2);
			this.satellites.add(new Satellite(this, orbit_radius));
		}
	}

	public Vector2 getGravityForce(Vector2 rel_pos) {
		// rel_pos: relative position of the physic body from the gravity center
		float f = 0.01f * this.mass / rel_pos.len2();
		float mag = rel_pos.len();
		return new Vector2(f*rel_pos.x/mag, f*rel_pos.y/mag);
	}

	public ArrayList<Satellite> activateSatellites() {
		// Awaken satellites for animation and physics
		for (Satellite sat : satellites) {
			float angle = MathUtils.random(0, MathUtils.PI2);
			// Sets the absolute world position of the satellite
			Vector2 pos = new Vector2(sat.orbit*MathUtils.cos(angle), sat.orbit*MathUtils.sin(angle));
			sat.position.set(position.cpy().add(pos));
			// Add a tangential force so it orbits the planet
			float R = sat.orbit;
			float G = getGravityForce(new Vector2(sat.orbit, 0.0f)).x;
			float tangential_force = (float) Math.sqrt(R*G-G*G);
			Vector2 tangential = new Vector2(0.0f, tangential_force*sat.mass);
			tangential.rotateRad(angle);
			sat.push(tangential);
		}
		return satellites;
	}

	public void update() {
		for (Satellite sat : satellites) {
			// Apply gravity force
			sat.push(getGravityForce(position.cpy().sub(sat.position)).scl(sat.mass));
			sat.update();
		}
	}


	public void render(MyRenderer renderer) {
		//Vector2 pos = cam.world_to_camera(this.position);

		// Atmosphere
		/*
	    for (int i=0; i<5; i++) {
	      fill(255, 255, 255, 60-10*i);
	      ellipse(pos.x, pos.y, (pow(2,i)+2*radius)*cam.PPU, (pow(2,i)+2*radius)*cam.PPU);
	    }*/

		renderer.setColor(col);
		renderer.circle(position.x, position.y, radius);
		//System.out.println("rendering planet");
		// Render satellites
		for (Satellite sat : satellites) {
			sat.render(renderer);
		}
	}
	
}
