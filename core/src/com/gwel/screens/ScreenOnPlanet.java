package com.gwel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.gwel.entities.Planet;
import com.gwel.spacegame.SpaceGame;

public class ScreenOnPlanet implements Screen {
	final SpaceGame game;
	Planet planet;
	
	public ScreenOnPlanet(final SpaceGame game, Planet p) {
		this.game = game;
		planet = p;
		System.out.println("Switched to planet Screen");
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(float arg0) {
	//  Camera update
		game.camera.glideTo(game.ship.getPosition());
		game.camera.zoomTo(200.0f);
		game.camera.update();

		game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
		Gdx.gl.glClearColor(0.6f, 0.6f, 0.6f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		game.ship.render(game.renderer);
		game.renderer.flush();
	}

	@Override
	public void resize(int arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
		
	}
}
