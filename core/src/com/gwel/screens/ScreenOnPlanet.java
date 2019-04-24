package com.gwel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.gwel.entities.Planet;
import com.gwel.spacegame.SpaceGame;
import com.gwel.spacegame.utils;

public class ScreenOnPlanet implements Screen {
	final SpaceGame game;
	Planet planet;
	String planetName;
	
	public ScreenOnPlanet(final SpaceGame game, Planet p) {
		this.game = game;
		planet = p;
		planetName = game.getPlanetName(planet.seed);
		System.out.println("Switched to planet Screen");
		System.out.println("Welcome to " + planetName);
		
		Vector2 dPos = game.ship.getPosition().sub(planet.getPosition());
		float cameraRotate = utils.wrapAngleAroundZero(MathUtils.PI*0.5f-dPos.angleRad());
		game.camera.rotateTo(cameraRotate);
	}
	
	@Override
	public void dispose() {
		game.camera.rotateTo(0.0f);
		System.out.println("Planet screen disposed");
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
		handleInput();
		
		//  Camera update
		game.camera.glideTo(game.ship.getPosition());
		game.camera.zoomTo(200.0f);
		game.camera.update();

		game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
		Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),  Gdx.graphics.getHeight());
		game.batch.setProjectionMatrix(normalProjection);
		Gdx.gl.glClearColor(0.2f, 1f, 0.9f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		game.ship.render(game.renderer);
		planet.render(game.renderer);
		game.renderer.flush();
		game.batch.begin();
		game.font.draw(game.batch, planetName, 20, Gdx.graphics.getHeight()-game.font.getXHeight());
		game.batch.end();
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
	
	void handleInput() {
		float x_axis = 0.0f;
		float y_axis = 0.0f;
		
		if (game.hasController) {
			PovDirection pov = game.controller.getPov(0);
			if (pov == PovDirection.north) {
				game.camera.zoom(1.04f);
				game.camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				game.camera.zoom(0.95f);
				game.camera.autozoom = false;
			}
			if (game.controller.getButton(game.PAD_BOOST)) {
				game.takeOff();
			}
			
			x_axis = game.controller.getAxis(game.PAD_XAXIS);
			y_axis = game.PAD_YDIR * game.controller.getAxis(game.PAD_YAXIS);
		}
		
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			y_axis += 1.0f;
			game.takeOff();
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN)) {
			y_axis -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			x_axis -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			x_axis += 1.0f;
		}
		
		if (Gdx.input.isKeyPressed(Keys.A)) {
			game.camera.zoom(1.04f);
			game.camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			game.camera.zoom(0.95f);
			game.camera.autozoom = false;
		}
	}
}
