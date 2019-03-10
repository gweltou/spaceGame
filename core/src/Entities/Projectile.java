package Entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

public class Projectile extends PhysicBody {  
	public Projectile(Vector2 position) {
		super();
		this.mass = 0.5f;
		this.position = position;
	}

	@Override
	public void render(MyRenderer renderer) {
		// TODO Auto-generated method stub
		renderer.line(position, pposition);
	}
}