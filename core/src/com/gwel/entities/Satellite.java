package com.gwel.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gwel.spacegame.MyRenderer;

public class Satellite extends PhysicBody {
	public static final float MIN_RADIUS = 1.0f;
	public static final float MAX_RADIUS = 5.0f;
	public Planet parent;
	public float radius;
	private Color color;
	public boolean detached;

	public Satellite(World world, Planet parent, Vector2 pos, float rad, Color col) {
		super(pos);
		radius = rad;
		//bodyDef.linearVelocity.set(vel);
		body = world.createBody(bodyDef);
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
		circle.dispose();
		detached = false;
		
		this.parent = parent;
		color = col;
	}
	
	public void detach() {
		parent.satellites.remove(this);
		detached = true;
	}
	
	public void render(MyRenderer renderer) {
		//System.out.println("Rendering satellite");
		renderer.setColor(color);
		renderer.circle(getPosition(), radius);
	}
}