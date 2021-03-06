package com.gwel.entities;

import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.Const;
import com.gwel.spacegame.Enums;
import com.gwel.spacegame.MyRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;

public class Planet implements Collidable {	
	public long seed;
	private Body body;
	private final Vector2 position;
	public float mass;
	public float radius;
	public float surfaceLength;
	public Color color;
	public float colorHue;
	public float colorSat;
	public float colorVal;
	int n_sat;
	float[] sat_orbit;
	float[] sat_radius;
	Color[] sat_color;
	ArrayList<Satellite> satellites;
	

	public Planet(Vector2 pos, float rad, long seed) {
		this.seed = seed;
		position = pos.cpy();
		radius = rad;
		mass = (float) Math.PI * rad * rad;
		surfaceLength = MathUtils.PI2 * rad;

		RandomXS128 generator = new RandomXS128(seed);
		colorHue = generator.nextFloat()*360.0f;
		colorSat = generator.nextFloat();
		colorVal = generator.nextFloat()*0.8f;
		color = new Color().fromHsv(colorHue, colorSat, colorVal);
		color.a = 1.0f; // We need to set the alpha component manually, for some reason
		

		// Create satellites configuration, if any (not the actual Satellite instances)
		n_sat = (int) Math.floor(MathUtils.random(Const.PLANET_MAX_SAT+1));
		sat_orbit = new float[n_sat];
		sat_radius = new float[n_sat];
		sat_color = new Color[n_sat];
		for (int i=0; i<n_sat; i++) {
			sat_orbit[i] = MathUtils.random(radius+Satellite.MAX_RADIUS+5.0f, radius*2+Satellite.MAX_RADIUS);
			sat_radius[i] = MathUtils.random(Satellite.MIN_RADIUS, Satellite.MAX_RADIUS);
			sat_color[i] = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		}
		satellites = new ArrayList<Satellite>();
	}
	
	public void initBody(World world) {
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
		fixtureDef.friction = 0.6f;
		fixtureDef.restitution = 0.4f;
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.PLANET);
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
		// rel_pos: relative position of the physic body from the center of gravity
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
		renderer.circleOpt(getPosition(), radius);	
	}

	public void dispose() {
		for (Satellite sat: satellites) {
			sat.dispose();
		}
		body.getWorld().destroyBody(body);
		satellites.clear();
		//System.out.println("Planet disposed");
	}

	@Override
	public float getBoundingRadius() {
		return radius;
	}
}
