package com.gwel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.entities.Planet;
import com.gwel.entities.Spaceship;
import com.gwel.spacegame.MyCamera;
import com.gwel.spacegame.SpaceGame;
import com.gwel.spacegame.utils;
import com.gwel.surfaceEntities.XenoTreeManager;
import com.gwel.surfaceEntities.HeightArray;
import com.gwel.surfaceEntities.LandedPlayer;
import com.gwel.surfaceEntities.TerrainLayer;

public class ScreenOnPlanet implements Screen {
	private final static int NUM_PARALLAX_LAYERS = 6;
	final SpaceGame game;
	private MyCamera camera;
	private World world;
	private Planet planet;
	private String strName;
	private GlyphLayout layoutName;
	private LandedPlayer player;
	private boolean landingIntro;
	private float surfaceLength;
	private float sunHPos;
	private Spaceship ship;
	private boolean showShip = false;
	private final static float SUN_SIZE = 200f;
	private static XenoTreeManager xtm;
	public boolean mustDispose = false;
		
	// Terrain data
	private TerrainLayer[]	parallaxLayers;
	private TerrainLayer	walkingLayer;
	
	
	public ScreenOnPlanet(final SpaceGame game, Planet p) {
		this.game = game;
		world = new World(new Vector2(0.0f, -10.0f), true);
		camera = new MyCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.zoomTo(100.0f);
		game.renderer.setCamera(camera);
		planet = p;
		game.generator.setSeed(planet.seed);
		strName = game.getPlanetName(planet.seed);
		layoutName = new GlyphLayout();
		layoutName.setText(game.font, strName);
		
		System.out.println("Welcome to planet \"" + strName + "\"");
		System.out.println("Universal position : " + planet.getPosition());
		
		surfaceLength = MathUtils.PI2 * planet.radius;
		float landingPointAngle = -game.ship.getPosition().sub(planet.getPosition()).angleRad();
		float landingHPos = surfaceLength * landingPointAngle/MathUtils.PI2;
		sunHPos = surfaceLength * planet.getPosition().angleRad()/MathUtils.PI2;
		System.out.println("Sun local position : " + sunHPos);
		
		// TREE MANAGER
		xtm = new XenoTreeManager(game.generator, world, surfaceLength);
		
		// GENERATE TERRAIN DATA
		HeightArray[] hArrays = {
				new HeightArray(game.generator, surfaceLength, 0.001f, surfaceLength),
				new HeightArray(game.generator, surfaceLength/3, 0.1f, surfaceLength),
				new HeightArray(game.generator, 10.0f, 1f, surfaceLength),
				new HeightArray(game.generator, 10.0f, 3f, surfaceLength)};
		float[] amps = {20f, 8f, 1.0f, 0.15f};
		
		Vector2 blockPos = new Vector2(landingHPos-50f, 0.0f);
		walkingLayer = new TerrainLayer(game.generator, world, hArrays, amps, blockPos, 1f, xtm, true, true, planet.color);
		parallaxLayers = new TerrainLayer[NUM_PARALLAX_LAYERS];
		for (int i=0; i<NUM_PARALLAX_LAYERS; i++) {
			float scale = (float) Math.pow(0.5f, i+1);
			parallaxLayers[i] = new TerrainLayer(game.generator, world, hArrays, amps, blockPos, scale, xtm, false, false, planet.color);
		}
				
		// Regenerating ship
		game.ship.hitpoints = game.ship.MAX_HITPOINTS;
		game.ship.ammunition = game.ship.MAX_AMMUNITION;
		
		Vector2 dPos = game.ship.getPosition().sub(planet.getPosition());
		float cameraRotate = utils.wrapAngleAroundZero(MathUtils.PI*0.5f-dPos.angleRad());
		game.camera.rotateTo(cameraRotate);
		
		// Create a mock-up ship to draw
		float spawningHeight = walkingLayer.getHeight(landingHPos)+2f;
		ship = new Spaceship(new Vector2(landingHPos, spawningHeight));
		ship.setAngle(game.ship.getAngle()-landingPointAngle);
		//ship.initBody(world);
		
		player = new LandedPlayer(new Vector2(landingHPos, spawningHeight));
		player.initBody(world);
		
		landingIntro = false;
	}
	
	@Override
	public void dispose() {
		game.camera.rotateTo(0.0f);
		player.dispose();
		ship.dispose();
		for (TerrainLayer layer: parallaxLayers) {
			layer.dispose();
		}
		world.dispose();
		System.out.println("Planet screen disposed");
	}

	@Override
	public void hide() {}

	@Override
	public void pause() {}

	@Override
	public void render(float timeDelta) {
		handleInput();
		Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
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
			
			game.batch.setProjectionMatrix(normalProjection);
			game.batch.begin();
			game.font.draw(game.batch, strName, (Gdx.graphics.getWidth()-layoutName.width)/2, Gdx.graphics.getHeight()-game.font.getXHeight());
			game.batch.end();
		} else {
			camera.glideTo(player.getPosition().add(0f, 100.0f/camera.PPU));
			camera.update();
			
			
			float playerHPos = player.getPosition().x % surfaceLength;
			if (playerHPos < 0.0f)	playerHPos += surfaceLength;
			
			// CALCULATE SUN RELATIVE POSITION AND SKY COLOR
			float dPos = (playerHPos - sunHPos) / surfaceLength;
			if (dPos > 0.5f)	dPos -= 1.0f;
			else if (dPos < -0.5f) dPos += 1.0f;
			Color skyColor = new Color().fromHsv((planet.colorHue+180.0f)%360.0f, 0.5f*Math.abs(dPos), 1.0f-Math.abs(dPos));
			float sunRise = MathUtils.cos(dPos*MathUtils.PI2);
			Color sunColor = new Color(1.0f, 0.5f*(sunRise+1f), 0.4f*(sunRise+1f), 1.0f);
			Vector2 sunPos = new Vector2((float) Gdx.graphics.getWidth()/2,
					Gdx.graphics.getHeight()*(0.5f + 0.45f*sunRise));
			
			// UPDATE SHIP POSITION
			// so it wraps around the planet
			float shipHPos = ship.getPosition().x % surfaceLength;
			if (shipHPos < 0.0f)	shipHPos += surfaceLength;
			dPos = playerHPos - shipHPos;
			if (dPos > surfaceLength/2)	dPos -= surfaceLength;
			else if (dPos < -surfaceLength/2) dPos += surfaceLength;
//			System.out.println("");
//			System.out.println("playerHPos " + playerHPos);
//			System.out.println("xShipPos " + xShipPos);
//			System.out.println("dPos " + dPos);
			if (showShip) {
				// Remove ship if outside player's range
				if (dPos < -TerrainLayer.TERRAIN_BLOCK_SPAWN_RADIUS || dPos > TerrainLayer.TERRAIN_BLOCK_SPAWN_RADIUS) {
					ship.dispose();
					showShip = false;
				}
			} else {
				// Check if ship should be displayed
				if (dPos > -TerrainLayer.TERRAIN_BLOCK_SPAWN_RADIUS && dPos < TerrainLayer.TERRAIN_BLOCK_SPAWN_RADIUS) {
					// Spawn and display ship
					ship.setPosition(new Vector2(player.getPosition().x - dPos, ship.getPosition().y));
					ship.initBody(world);
					showShip = true;
				}
			}
			
			// UPDATE TERRAIN LAYERS
			for (TerrainLayer layer: parallaxLayers)
				layer.update(player.getPosition().x);
			walkingLayer.update(player.getPosition().x);
			
			Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			
			// DRAW SUN
			game.renderer.setProjectionMatrix(normalProjection);
			game.renderer.setColor(sunColor);
			game.renderer.circle(sunPos.x, sunPos.y, SUN_SIZE, 48);
			game.renderer.flush();
			
			game.renderer.setProjectionMatrix(new Matrix4().set(camera.affine));
			// Draw terrain
			for (int i = parallaxLayers.length-1; i>=0; i--) {
				//parallaxLayers[i].updateParallaxPosition(camera.getTravelling());
				parallaxLayers[i].render(game.renderer);
			}
			walkingLayer.render(game.renderer);
						
			ship.render(game.renderer);
			player.render(game.renderer);
			game.renderer.flush();
			
			// DISPLAY PLANET NAME
			game.batch.setProjectionMatrix(normalProjection);
			game.batch.begin();
			game.font.draw(game.batch, strName, (Gdx.graphics.getWidth()-layoutName.width)/2, Gdx.graphics.getHeight()-game.font.getXHeight());
			game.batch.end();
		}
		
		world.step(timeDelta, 8, 3);
		
		if (mustDispose) {
			game.takeOff();
		}
	}

	@Override
	public void resize(int width, int height) {
		camera.width = width;
		camera.height = height;
		camera.update();
	}

	@Override
	public void resume() {}

	@Override
	public void show() {}
	
	void handleInput() {
		float x_axis = 0.0f;
		float y_axis = 0.0f;
		
		if (game.hasController) {
			PovDirection pov = game.controller.getPov(0);
			if (pov == PovDirection.north) {
				camera.zoomIn();
				camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				camera.zoomOut();
				camera.autozoom = false;
			}
			if (game.controller.getButton(game.PAD_BOOST)) {
				// TAKE OFF
				if (player.getPosition().dst2(ship.getPosition()) < 3.0f)	mustDispose = true;
			}
			
			x_axis = game.controller.getAxis(game.PAD_XAXIS);
			y_axis = game.PAD_YDIR * game.controller.getAxis(game.PAD_YAXIS);
		}
		
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			y_axis += 1.0f;
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
			camera.zoomIn();
			camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			camera.zoomOut();
			camera.autozoom = false;
		}
		
		player.move(new Vector2(x_axis, y_axis));
		if (y_axis > 0.9f) {
			 
		}
	}
}
