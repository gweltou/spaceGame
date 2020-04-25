package com.gwel.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gwel.spacegame.Enums;
import com.gwel.spacegame.MyRenderer;

public class Satellite extends PhysicBody {
	public static final float MIN_RADIUS = 1.0f;
	public static final float MAX_RADIUS = 5.0f;
	public Planet parent;
	public float radius;
	private final Color color;
	public boolean freeFlying;

	public Satellite(World world, Planet parent, Vector2 pos, float rad, Color col) {
		super();

		setPosition(pos);
		radius = rad;
		initBody(world);
		body.setUserData(this);
		//body.setAwake(true);
		CircleShape circle = new CircleShape();
		circle.setRadius(rad);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = circle;
		fixtureDef.density = 0.6f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SATELLITE);
		circle.dispose();
		freeFlying = false;
		
		this.parent = parent;
		color = col;
	}
	
	public void detach() {
		parent.satellites.remove(this);
		freeFlying = true;
	}
	
	public void render(MyRenderer renderer) {
		//System.out.println("Rendering satellite");
		renderer.setColor(color);
		renderer.circleOpt(getPosition(), radius);
	}

	@Override
	public float getBoundingRadius() {
		return radius;
	}
}