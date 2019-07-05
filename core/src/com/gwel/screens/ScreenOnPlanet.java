package com.gwel.screens;

import java.util.ArrayDeque;

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
import com.gwel.entities.LandedPlayer;
import com.gwel.entities.Planet;
import com.gwel.entities.Spaceship;
import com.gwel.spacegame.MyCamera;
import com.gwel.spacegame.SpaceGame;
import com.gwel.spacegame.utils;
import com.gwel.surfaceEntities.HeightArray;
import com.gwel.surfaceEntities.TerrainBlock;

public class ScreenOnPlanet implements Screen {
	private final static float TERRAIN_BLOCK_WIDTH = 100.0f;	// in game units
	private final static int NUM_PARALLAX_LAYERS = 6;
	private final static float TERRAIN_BLOCK_SPAWN_RADIUS = 100.0f;	// in game units
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
	private boolean showShip = false;
	
	// Terrain data
	private final ArrayDeque<TerrainBlock>[] parallaxLayers;
	HeightArray primaryHeightArray;
	TerrainBlock parallaxBlock1;
	TerrainBlock parallaxBlock2;
	
	public ScreenOnPlanet(final SpaceGame game, Planet p) {
		this.game = game;
		world = new World(new Vector2(0.0f, -10.0f), true);
		camera = new MyCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.zoomTo(100.0f);
		planet = p;
		strName = game.getPlanetName(planet.seed);
		layoutName = new GlyphLayout();
		layoutName.setText(game.font, strName);
		System.out.println("Switched to planet Screen");
		System.out.println("Welcome to " + strName);		
		
		surfaceLength = MathUtils.PI2 * planet.radius;
		float landingPointAngle = game.ship.getPosition().sub(planet.getPosition()).angleRad();
		float landingHPos = surfaceLength * landingPointAngle/MathUtils.PI2;
		
		
		// GENERATE TERRAIN DATA
		parallaxLayers = new ArrayDeque[NUM_PARALLAX_LAYERS];
		game.generator.setSeed(planet.seed);
		HeightArray[] hArrays = {
				new HeightArray(game.generator, 200.0f, 0.1f),
				new HeightArray(game.generator, 10.0f, 1f),
				new HeightArray(game.generator, 20.0f, 3f)};
		float[] amps = {10.0f, 0.4f, 0.15f};
		
		Vector2 blockPos = new Vector2(landingHPos-50, 0.0f);
		TerrainBlock terrainBlock;
		terrainBlock = new TerrainBlock(blockPos, hArrays, amps, blockPos.x, blockPos.x+100, planet.color);
		terrainBlock.initBody(world);
		ArrayDeque<TerrainBlock> terrainLayer = new ArrayDeque<TerrainBlock>();
		terrainLayer.add(terrainBlock);
		parallaxLayers[0] = terrainLayer;
		Color col = new Color();
		col.a = 1.0f;
		float colVal = planet.colorVal;
		for (int i=1; i<parallaxLayers.length; i++) {
			for (int j=0; j<amps.length; j++)	amps[j] = game.generator.nextFloat() * (float) i;
			float colSat = planet.colorSat*((float) Math.pow(0.6f, i));
			colVal += (1.0f-colVal)*((float) Math.pow(0.4f, i));
			col.fromHsv(planet.colorHue, colSat, colVal);
			terrainBlock = new TerrainBlock(blockPos.add(0.0f, 1f), hArrays, amps, blockPos.x, blockPos.x+TERRAIN_BLOCK_WIDTH, col);
			terrainLayer = new ArrayDeque<TerrainBlock>();
			terrainLayer.add(terrainBlock);
			parallaxLayers[i] = terrainLayer;
		}
		
		// Regenerating ship
		game.ship.hitpoints = game.ship.MAX_HITPOINTS;
		game.ship.ammunition = game.ship.MAX_AMMUNITION;
		
		Vector2 dPos = game.ship.getPosition().sub(planet.getPosition());
		float cameraRotate = utils.wrapAngleAroundZero(MathUtils.PI*0.5f-dPos.angleRad());
		game.camera.rotateTo(cameraRotate);
		
		// Create a mock-up ship to draw
		ship = new Spaceship(new Vector2(landingHPos, 5.0f));
		ship.setAngle(game.ship.getAngle()-landingPointAngle);
		//ship.initBody(world);
		
		player = new LandedPlayer(new Vector2(landingHPos, 5.0f));
		player.initBody(world);
		
		landingIntro = false;
	}
	
	@Override
	public void dispose() {
		game.camera.rotateTo(0.0f);
		player.dispose();
		ship.dispose();
		for (ArrayDeque<TerrainBlock> layer: parallaxLayers) {
			for (TerrainBlock block: layer)	block.dispose();
		}
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
			camera.glideTo(player.getPosition().add(0f, 100.0f/camera.PPU));
			camera.update();
			
			// Update terrain blocks
			for (ArrayDeque<TerrainBlock> pl: parallaxLayers) {
				TerrainBlock leftb = pl.getFirst();
				TerrainBlock rightb = pl.getLast();
				// Add a terrain block on the left if needed
				if (player.getPosition().x - leftb.position.x < TERRAIN_BLOCK_SPAWN_RADIUS) {
					Vector2 blockPos = new Vector2(leftb.position.x-TERRAIN_BLOCK_WIDTH, leftb.position.y);
					TerrainBlock newBlock = new TerrainBlock(blockPos,
							leftb.heightArrays, leftb.amps,
							leftb.leftBoundary-TERRAIN_BLOCK_WIDTH,
							leftb.rightBoundary-TERRAIN_BLOCK_WIDTH,
							leftb.color);
					// Initialize collision layer if needed
					if (leftb.terrainBody != null)	newBlock.initBody(world);
					pl.addFirst(newBlock);
					leftb = newBlock;
					//System.out.println("Added left");
							
					// Remove rightmost terrain block if needed
					if (rightb.position.x - player.getPosition().x > TERRAIN_BLOCK_SPAWN_RADIUS) {
						rightb.dispose();
						pl.removeLast();
						rightb = pl.getLast();
						//System.out.println("Removed right");
					}
				}
				// Add a terrain block on the right if needed
				if (rightb.position.x + TERRAIN_BLOCK_WIDTH - player.getPosition().x < TERRAIN_BLOCK_SPAWN_RADIUS) {
					Vector2 blockPos = new Vector2(rightb.position.x+TERRAIN_BLOCK_WIDTH, rightb.position.y);
					TerrainBlock newBlock = new TerrainBlock(blockPos,
							rightb.heightArrays, rightb.amps,
							rightb.leftBoundary+TERRAIN_BLOCK_WIDTH,
							rightb.rightBoundary+TERRAIN_BLOCK_WIDTH,
							rightb.color);
					// Initialize collision layer if needed
					if (leftb.terrainBody != null)	newBlock.initBody(world);
					pl.addLast(newBlock);
					rightb = newBlock;
					//System.out.println("Added right");
							
					// Remove leftmost terrain block if needed
					if (player.getPosition().x - leftb.position.x + TERRAIN_BLOCK_WIDTH > TERRAIN_BLOCK_SPAWN_RADIUS) {
						leftb.dispose();
						pl.removeFirst();
						leftb = pl.getFirst();
						//System.out.println("Removed left");
					}
				}
			}
			
			// UPDATE SHIP POSITION
			// so it wraps around the planet
			float xPlayerPos = player.getPosition().x % surfaceLength;
			if (xPlayerPos < 0.0f)	xPlayerPos += surfaceLength;
			float xShipPos = ship.getPosition().x % surfaceLength;
			if (xShipPos < 0.0f)	xShipPos += surfaceLength;
			float dPos = xPlayerPos - xShipPos;
			if (dPos > surfaceLength/2)	dPos -= surfaceLength;
//			System.out.println("");
//			System.out.println("xPlayerPos " + xPlayerPos);
//			System.out.println("xShipPos " + xShipPos);
//			System.out.println("dPos " + dPos);
			if (showShip) {
				if (dPos < -TERRAIN_BLOCK_SPAWN_RADIUS || dPos > TERRAIN_BLOCK_SPAWN_RADIUS) {
					ship.dispose();
					System.out.println(ship.getPosition());
					showShip = false;
				}
			} else {
				// Check if ship should be displayed
				if (dPos > -TERRAIN_BLOCK_SPAWN_RADIUS && dPos < TERRAIN_BLOCK_SPAWN_RADIUS) {
					// Spawn and display ship
					System.out.println("ship spawned");
					ship.setPosition(new Vector2(player.getPosition().x - dPos, ship.getPosition().y));
					ship.initBody(world);
					showShip = true;
				}
			}
			
			game.renderer.setProjectionMatrix(new Matrix4().set(camera.affine));
			Gdx.gl.glClearColor(0.4f, 1f, 1f, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			
			
			// Draw terrain
			for (int i=parallaxLayers.length-1; i>0; i--) {
				for (TerrainBlock tb: parallaxLayers[i]) {
					tb.updateParallaxPosition(camera.getTravelling(), 1.0f/(2<<i));
					tb.render(game.renderer, camera.sw.y);
					game.renderer.flush();
				}
			}
			for (TerrainBlock tb: parallaxLayers[0]) {
				tb.render(game.renderer, camera.sw.y);
				game.renderer.flush();
			}
			
			game.renderer.flush();
			ship.render(game.renderer);
			player.render(game.renderer);
			game.renderer.flush();
			
			// DISPLAY PLANET NAME
			Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			game.batch.setProjectionMatrix(normalProjection);
			game.batch.begin();
			game.font.draw(game.batch, strName, (Gdx.graphics.getWidth()-layoutName.width)/2, Gdx.graphics.getHeight()-game.font.getXHeight());
			game.batch.end();
		}
		
		world.step(1.0f/60f, 8, 3);
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
			 if (Math.abs(player.getPosition().x-ship.getPosition().x) < 1.0f)	game.takeOff();
		}
	}
}
