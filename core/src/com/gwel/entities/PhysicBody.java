package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.MyRenderer;
import com.gwel.spacegame.utils;


public abstract class PhysicBody implements MovingObject, Collidable {
	protected BodyDef bodyDef;
	protected Body body;
	protected Vector2 position;
	protected Vector2 velocity;
	protected float angle;
	protected float angleVel;
	public boolean disposable;

	PhysicBody(Vector2 pos, float angle) {
		position = pos.cpy();
		this.angle = angle;
		velocity = new Vector2();
		angleVel = 0f;
		body = null;
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
	
	public Vector2 getVelocity() {
		if (body != null)
			return body.getLinearVelocity().cpy();
		return velocity;
	}
	
	public float getAngularVelocity() {
		if (body != null)
			return body.getAngularVelocity();
		return angleVel;
	}
	
	public float getAngle() {
		if (body != null)
			return body.getAngle();
		return angle;
	}
	
	public void setAngle(float angle) {
		this.angle = angle;
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
	
	public void initBody(World world) {
		disposable = false;
		
		bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(getPosition());
		bodyDef.angle = getAngle();
		bodyDef.linearVelocity.set(getVelocity());
		bodyDef.angularVelocity = getAngularVelocity();
		
		body = world.createBody(bodyDef);
		body.setUserData(this);
	}
	
	public void dispose() {
		angle = getAngle();
		angleVel = getAngularVelocity();
		position = getPosition();
		velocity = getVelocity();
		if (body != null) {
			body.getWorld().destroyBody(body);
			body = null;
		}
		disposable = true;
	}
}
