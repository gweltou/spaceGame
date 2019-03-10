package Entities;

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
	private Body body;

	public Satellite(World world, Planet parent, Vector2 pos, float rad, Color col) {
		super(world, pos);
		//body = world.createBody(bodyDef);
		CircleShape circle = new CircleShape();
		circle.setRadius(rad);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = circle;
		fixtureDef.density = 1.0f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		//Fixture fixture = body.createFixture(fixtureDef);
		circle.dispose();
		
		this.parent = parent;
		color = col;
	}
	
	public void update() {
		boolean wake = false;
		body.applyForceToCenter(parent.getGravityForce(body.getPosition()), wake);
	}
	
	public void render(MyRenderer renderer) {
		//Vector2 pos = cam.world_to_camera(this.position);
		//System.out.println("Rendering satellite");
		
		renderer.setColor(color);
		renderer.circle(position, radius);
	}
}