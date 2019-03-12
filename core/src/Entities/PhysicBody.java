package Entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.MyRenderer;

public abstract class PhysicBody {
	//protected World world;
	protected BodyDef bodyDef;
	protected Body body;

	PhysicBody(Vector2 pos) {
		//this.world = world;
		bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(pos);
	}

	public Vector2 getPosition() {
		return body.getPosition().cpy();
	}
	
	public Vector2 getSpeed() {
		return body.getLinearVelocity().cpy();
	}
	
	public float getAngle() {
		return body.getAngle();
	}
	
	public float getMass() {
		return body.getMass();
	}
	
	public void push(Vector2 force) {
		body.applyForceToCenter(force, true);
	}

	public abstract void render(MyRenderer renderer);
	
	public void dispose() {
		body.getWorld().destroyBody(body);
	}
}
