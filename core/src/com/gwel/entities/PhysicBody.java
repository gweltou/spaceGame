package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.MyRenderer;
import com.gwel.spacegame.utils;


public abstract class PhysicBody implements MovingObject, Collidable {
	protected BodyDef bodyDef;
	protected Body body;
	public boolean disposable;
	protected Vector2 position;
	protected Vector2 velocity;
	protected float angle;
	protected float angleVel;

	PhysicBody(Vector2 pos) {
		position = pos.cpy(); 
		
		bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(pos);
		disposable = false;
	}

	public Vector2 getPosition() {
		if (body != null)
			return body.getPosition().cpy();
		return position.cpy();
	}
	
	public void setPosition(Vector2 pos) {
		position = pos.cpy();
	}
	
	public Vector2 getSpeed() {
		if (body != null)
			return body.getLinearVelocity().cpy();
		return null;
	}
	
	public float getAngularSpeed() {
		if (body != null)
			return body.getAngularVelocity();
		return 0;
	}
	
	public float getAngle() {
		if (body != null)
			return body.getAngle();
		return angle;
	}
	
	public float getAngleDiff(float refAngle) {
		if (body != null)
			return utils.wrapAngleAroundZero(refAngle-body.getAngle());
		return utils.wrapAngleAroundZero(refAngle-angle);
	}
	
	public float getMass() {
		if (body != null)
			return body.getMass();
		return 0;
	}
	
	public void push(Vector2 force) {
		body.applyForceToCenter(force, true);
	}

	public abstract void render(MyRenderer renderer);
	
	public void dispose() {
		angle = getAngle();
		angleVel = getAngularSpeed();
		position = getPosition();
		velocity = getSpeed();
		if (body != null) {
			body.getWorld().destroyBody(body);
			body = null;
		}
		disposable = true;
		System.out.println("Body disposed");
	}
}
