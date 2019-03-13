package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.spacegame.MyRenderer;

public class Projectile extends PhysicBody {  
	public Projectile(Vector2 pos) {
		super(pos);
	}

	@Override
	public void render(MyRenderer renderer) {
		// TODO Auto-generated method stub
		//renderer.line(position, pposition);
	}
}