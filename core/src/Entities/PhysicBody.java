package Entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.MyRenderer;

public abstract class PhysicBody {
	public Vector2 position;
	Vector2 pposition;  // Previous position
	public Vector2 speed;
	public Vector2 accel;
	public float mass;
	protected World world;
	protected BodyDef bodyDef;
	protected Body body;

	PhysicBody(World world, Vector2 pos) {
		this.world = world;
		
		bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(pos);
		//this.body = world.createBody(bodyDef);
		//System.out.println(body);
	}

	public Vector2 getPosition() {
		return this.body.getPosition();
	}
	
	public Vector2 getSpeed() {
		return this.body.getLinearVelocity();
	}
	
	public float getAngle() {
		return this.body.getAngle();
	}
	
	public void push(Vector2 force) {
		body.applyForceToCenter(force, true);
	}

	public void update() {
		speed.add(accel);
		accel.set(0.0f, 0.0f);
		pposition.set(position);
		position.add(speed);
	}

	public abstract void render(MyRenderer renderer);
}
