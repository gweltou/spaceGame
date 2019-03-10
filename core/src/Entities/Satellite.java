package Entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.gwel.spacegame.MyRenderer;

public class Satellite extends PhysicBody {
	public Planet parent;
	float orbit;
	public float radius;
	private Color col;

	public Satellite(Planet parent, float orb) {
		super();
		this.parent = parent;
		orbit = orb;
		radius = MathUtils.random(1,5);
		col = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1.0f);
		mass = radius*radius*0.1f;
	}
	
	public void detach() {
		System.out.println(parent.satellites.size());
		parent.satellites.remove(this);
		System.out.println(parent.satellites.size());
	}
	
	public void render(MyRenderer renderer) {
		//Vector2 pos = cam.world_to_camera(this.position);
		//System.out.println("Rendering satellite");
		
		renderer.setColor(col);
		renderer.circle(position.x, position.y, radius);
	}
}