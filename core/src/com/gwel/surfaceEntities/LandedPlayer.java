package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.entities.PhysicBody;
import com.gwel.spacegame.Enums;
import com.gwel.spacegame.MyRenderer;

public class LandedPlayer extends PhysicBody {
	private float[][] triangles;
	private Affine2 transform;
	private Vector2 p1_tmp = new Vector2();
	private Vector2 p2_tmp = new Vector2();
	private Vector2 p3_tmp = new Vector2();
	
	public LandedPlayer(Vector2 pos) {
		super(pos, 0.0f);
		
		disposable = false;
		
		// Set shape triangles
		triangles = new float[1][10];
		float[] triangle = {-0.5f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f, 1f, 1f, 1f, 1f};
		triangles[0] = triangle;
		transform = new Affine2();
	}

	@Override
	public void initBody(World world) {
		//super.initBody(world);
		
		bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(getPosition());
		//bodyDef.angle = getAngle();
		bodyDef.fixedRotation = true;
		//bodyDef.linearVelocity.set(getVelocity());
		
		body = world.createBody(bodyDef);
		body.setUserData(this);
		
		PolygonShape boxShape = new PolygonShape();
		boxShape.set(triangles[0]);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = boxShape;
		fixtureDef.density = 1.0f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.1f;
		//fixtureDef.filter.categoryBits = 0x0002;
		Fixture fixture = body.createFixture(fixtureDef);
		//fixture.setUserData(Enums.SHIP);
		boxShape.dispose();
	}

	@Override
	public float getBoundingRadius() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void move(Vector2 dir) {
		body.applyLinearImpulse(dir.limit2(0.1f), body.getWorldCenter(), true);
	}

	@Override
	public void render(MyRenderer renderer) {
		transform.idt();
		transform.translate(getPosition());
		for (float[] triangle: triangles) {
			p1_tmp.set(triangle[0], triangle[1]);
			transform.applyTo(p1_tmp);
			p2_tmp.set(triangle[2], triangle[3]);
			transform.applyTo(p2_tmp);
			p3_tmp.set(triangle[4], triangle[5]);
			transform.applyTo(p3_tmp);
			renderer.setColor(triangle[6], triangle[7], triangle[8], triangle[9]);
			renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
		}
	}
}
