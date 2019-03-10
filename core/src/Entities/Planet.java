package Entities;

import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.MyRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class Planet {
	public static final float MIN_RADIUS = 10.0f;
	public static final float MAX_RADIUS = 100.0f;
	static final int MAX_SAT = 5;

	private World world;
	private Body body;
	public Vector2 position;
	public float radius;
	float mass;
	private Color color;
	int n_sat;
	float[] sat_orbit;
	float[] sat_rad;
	Color[] sat_col;
	

	public Planet(World world, Vector2 pos, float rad) {
		this.world = world;
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(pos);
		body = world.createBody(bodyDef);
		CircleShape circle = new CircleShape();
		circle.setRadius(rad);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = circle;
		fixtureDef.density = 1.0f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		Fixture fixture = body.createFixture(fixtureDef);
		circle.dispose();
		
		position = pos;
		radius = rad;
		color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		mass = radius*radius*0.1f;

		// Create satellites configuration, if any (not the actual Satellite instances)
		n_sat = (int) Math.floor(MathUtils.random(MAX_SAT+1));
		sat_orbit = new float[n_sat];
		sat_rad = new float[n_sat];
		sat_col = new Color[n_sat];
		for (int i=0; i<n_sat; i++) {
			sat_orbit[i] = MathUtils.random(radius+5, radius*2);
			sat_rad[i] = MathUtils.random(Satellite.MIN_RADIUS, Satellite.MAX_RADIUS);
			sat_col[i] = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		}
	}

	public Vector2 getGravityForce(Vector2 sat_pos) {
		// rel_pos: relative position of the physic body from the gravity center
		Vector2 rel_pos = position.cpy().sub(sat_pos);
		float f = 0.01f * this.mass / rel_pos.len2();
		float mag = rel_pos.len();
		return new Vector2(f*rel_pos.x/mag, f*rel_pos.y/mag);
	}

	public ArrayList<Satellite> activateSatellites() {
		// Awaken satellites for animation and physics
		ArrayList<Satellite> satellites = new ArrayList<Satellite>();
		for (int i=0; i<sat_orbit.length; i++) {
			float angle = MathUtils.random(0, MathUtils.PI2);
			// Sets the absolute world position of the satellite
			Vector2 pos = new Vector2(sat_orbit[i] * MathUtils.cos(angle),
									sat_orbit[i] * MathUtils.sin(angle));
			Satellite sat = new Satellite(world, this, pos, sat_rad[i], sat_col[i]);
			sat.position.set(position.cpy().add(pos));
			// Add a tangential force so it orbits the planet
			float R = sat_orbit[i];
			float G = getGravityForce(new Vector2(sat_orbit[i], 0.0f)).x;
			float tangential_force = (float) Math.sqrt(R*G-G*G);
			Vector2 tangential = new Vector2(0.0f, tangential_force*sat.mass);
			tangential.rotateRad(angle);
			sat.push(tangential);
		}
		return satellites;
	}

	public void update() {
		
	}


	public void render(MyRenderer renderer) {
		// Atmosphere
		/*
	    for (int i=0; i<5; i++) {
	      fill(255, 255, 255, 60-10*i);
	      ellipse(pos.x, pos.y, (pow(2,i)+2*radius)*cam.PPU, (pow(2,i)+2*radius)*cam.PPU);
	    }*/
		//System.out.println("rendering planet");
		renderer.setColor(color);
		renderer.circle(body.getPosition(), radius);	
	}

	public Vector2 getPosition() {
		return body.getPosition();
	}
}
