package com.gwel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.entities.LandedPlayer;
import com.gwel.entities.Planet;
import com.gwel.entities.Spaceship;
import com.gwel.spacegame.MyCamera;
import com.gwel.spacegame.SpaceGame;
import com.gwel.spacegame.utils;

public class ScreenOnPlanet implements Screen {
	final SpaceGame game;
	private MyCamera camera;
	private World world;
	private Planet planet;
	private String strName;
	private GlyphLayout layoutName;
	private LandedPlayer player;
	private boolean landingIntro;
	private float surfaceLength;
	private Spaceship ship;
	
	public ScreenOnPlanet(final SpaceGame game, Planet p) {
		this.game = game;
		world = new World(new Vector2(0.0f, -10.0f), true);
		camera = new MyCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		planet = p;
		strName = game.getPlanetName(planet.seed);
		layoutName = new GlyphLayout();
		layoutName.setText(game.font, strName);
		System.out.println("Switched to planet Screen");
		System.out.println("Welcome to " + strName);		
		
		surfaceLength = MathUtils.PI2 * planet.radius;
		float landingPointAngle = game.ship.getPosition().sub(planet.getPosition()).angleRad();
		float landingHPos = surfaceLength * landingPointAngle/MathUtils.PI2;
		System.out.println("Surface Length :" + surfaceLength);
		System.out.println("Landing angle :" + landingPointAngle);
		
		// Regenerating ship
		game.ship.hitpoints = game.ship.MAX_HITPOINTS;
		game.ship.ammunition = game.ship.MAX_AMMUNITION;
		
		Vector2 dPos = game.ship.getPosition().sub(planet.getPosition());
		float cameraRotate = utils.wrapAngleAroundZero(MathUtils.PI*0.5f-dPos.angleRad());
		game.camera.rotateTo(cameraRotate);
		
		ship = new Spaceship(new Vector2(landingHPos, 1.0f));
		ship.setAngle(game.ship.getAngle());
		player = new LandedPlayer(new Vector2(landingHPos, 1.0f));
		player.initBody(world);
		
		landingIntro = false;
	}
	
	@Override
	public void dispose() {
		game.camera.rotateTo(0.0f);
		player.dispose();
		ship.dispose();
		world.dispose();
		System.out.println("Planet screen disposed");
	}

	@Override
	public void hide() {}

	@Override
	public void pause() {}

	@Override
	public void render(float arg0) {
		handleInput();
		
		if (landingIntro) {
			//  Camera update
			game.camera.glideTo(game.ship.getPosition());
			game.camera.zoomTo(200.0f);
			game.camera.update();
	
			game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
			Gdx.gl.glClearColor(0.4f, 1f, 1f, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			game.ship.render(game.renderer);
			planet.render(game.renderer);
			player.render(game.renderer);
			game.renderer.flush();
			
			Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			game.batch.setProjectionMatrix(normalProjection);
			game.batch.begin();
			game.font.draw(game.batch, strName, (Gdx.graphics.getWidth()-layoutName.width)/2, Gdx.graphics.getHeight()-game.font.getXHeight());
			game.batch.end();
		} else {
			camera.glideTo(player.getPosition());
			camera.update();
			game.renderer.setProjectionMatrix(new Matrix4().set(camera.affine));
			Gdx.gl.glClearColor(0.4f, 1f, 1f, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			ship.render(game.renderer);
			player.render(game.renderer);
			game.renderer.flush();
		}
		
		world.step(1.0f/60f, 8, 3);
	}

	@Override
	public void resize(int width, int height) {
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
