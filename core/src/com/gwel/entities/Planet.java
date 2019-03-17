package com.gwel.entities;

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
	
	private int seed;
	private Body body;
	public Vector2 position;
	private float mass;
	public float radius;
	private Color color;
	int n_sat;
	float[] sat_orbit;
	float[] sat_radius;
	Color[] sat_color;
	ArrayList<Satellite> satellites;
	

	public Planet(Vector2 pos, float rad, int seed) {
		position = pos.cpy();
		radius = rad;
		this.seed = seed;
		color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		mass = (float) Math.PI * rad * rad;
		// Create satellites configuration, if any (not the actual Satellite instances)
		n_sat = (int) Math.floor(MathUtils.random(MAX_SAT+1));
		sat_orbit = new float[n_sat];
		sat_radius = new float[n_sat];
		sat_color = new Color[n_sat];
		for (int i=0; i<n_sat; i++) {
			sat_orbit[i] = MathUtils.random(radius+Satellite.MAX_RADIUS+2.0f, radius*2);
			sat_radius[i] = MathUtils.random(Satellite.MIN_RADIUS, Satellite.MAX_RADIUS);
			sat_color[i] = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		}
		satellites = new ArrayList<Satellite>();
	}
	
	public void activate(World world) {
		// Register planet to Box2D for physics
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(position);
		body = world.createBody(bodyDef);
		body.setUserData(this);
		CircleShape circle = new CircleShape();
		circle.setRadius(radius);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = circle;
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData("Planet");
		circle.dispose();
		/*
		CircleShape landingZone = new CircleShape();
		landingZone.setRadius(radius + 2);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = landingZone;
		fixtureDef.isSensor = true;
		fixtureDef.filter.maskBits = 0x0002; // Collides with ship only
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData("Landing");
		landingZone.dispose();
		*/
	}

	public Vector2 getGravityAccel(Vector2 pos) {
		// rel_pos: relative position of the physic body from the gravity center
		Vector2 rel_pos = getPosition().sub(pos);
		float f =  1.0f * mass / rel_pos.len2();
		float mag = rel_pos.len();
		return new Vector2(f*rel_pos.x/mag, f*rel_pos.y/mag);
	}

	public ArrayList<Satellite> activateSatellites(World world) {
		// Awaken satellites for physics
		for (int i=0; i<sat_orbit.length; i++) {
			//System.out.println("Activating satellite");
			float angle = MathUtils.random(0, MathUtils.PI2);
			// Sets the absolute world position of the satellite
			Vector2 pos = new Vector2(sat_orbit[i] * MathUtils.cos(angle),
									sat_orbit[i] * MathUtils.sin(angle));
			pos.add(getPosition());
			Satellite sat = new Satellite(world, this, pos, sat_radius[i], sat_color[i]);
			// Add a tangential force so it orbits the planet
			float R = sat_orbit[i];
			float G = getGravityAccel(sat.getPosition()).len();
			float tangential_force = (float) Math.sqrt(R*G-G*G);
			Vector2 tangential = new Vector2(0.0f, tangential_force);
			tangential.rotateRad(angle);
			sat.body.setLinearVelocity(tangential);
			//sat.push(tangential);
			satellites.add(sat);
		}
		return satellites;
	}
	
	public Vector2 getPosition() {
		return position.cpy();
	}
	
	public void update() {
		for (Satellite sat: satellites) {
			//System.out.println("Gravity pushing satellite");
			//sat.body.applyLinearImpulse(getGravityForce(sat), sat.getPosition(), true);
			sat.push(getGravityAccel(sat.getPosition()).scl(sat.getMass()));
		}
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
		renderer.circle(getPosition(), radius);	
	}

	public void dispose() {
		for (Satellite sat: satellites) {
			sat.dispose();
		}
		body.getWorld().destroyBody(body);
		satellites.clear();
		//System.out.println("Planet disposed");
	}
}
