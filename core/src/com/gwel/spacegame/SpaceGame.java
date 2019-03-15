package com.gwel.spacegame;

import java.util.ArrayList;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.entities.*;
import com.gwel.screens.ScreenInSpace;
import com.gwel.screens.ScreenOnPlanet;


public class SpaceGame extends Game {
	public MyCamera camera;
	public MyRenderer renderer;
	public Controller controller;
	public boolean hasController;
	public GameState gameState;
	
	// GAME UNIVERSE VARIABLES
	final static float UNIVERSE_SIZE = 100000.0f;
	final static float GRAVITY_ACTIVE_RADIUS = 800.0f;
	final static int NUMBER_PLANETS = 10000;
	public final static float LOCAL_RADIUS = GRAVITY_ACTIVE_RADIUS;
	final static float exit_radius = GRAVITY_ACTIVE_RADIUS+200.0f;
	public QuadTree Qt;
	
	
	@Override
	public void create () {
		//gameState = new GameState();
		
		if(Controllers.getControllers().size == 0)
            hasController = false;
        else {
            controller = Controllers.getControllers().first();
            hasController = true;
        }
		
		AABB universe_boundary = new AABB(new Vector2(0, 0), new Vector2(UNIVERSE_SIZE, UNIVERSE_SIZE));
		Qt = new QuadTree(universe_boundary);
		populateUniverse(Qt);
		
		Vector2 universeCenter = new Vector2(UNIVERSE_SIZE/2, UNIVERSE_SIZE/2);
		camera = new MyCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.setCenter(universeCenter);
		camera.update();
		renderer = new MyRenderer(camera);
		
		setScreen(new ScreenInSpace(this));
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void resize (int width, int height) {
		System.out.println("Resizing camera viewport");
		camera.width = width;
		camera.height = height;
		camera.update();
	}
	
	@Override
	public void dispose () {
		 renderer.dispose();
	}

	void land(Planet p) {
		if (getScreen() != null) {
			getScreen().dispose();
		}
		setScreen(new ScreenOnPlanet(this, p));
	}
	
	void populateUniverse(QuadTree qt) {
		long start_time = TimeUtils.millis();
		int i=0;
		while (i<NUMBER_PLANETS) {
			Vector2 position = new Vector2(MathUtils.random(UNIVERSE_SIZE), MathUtils.random(UNIVERSE_SIZE));
			float radius = MathUtils.random(Planet.MIN_RADIUS, Planet.MAX_RADIUS);

			// Check if other planets are near this position
			float min_dist = 3*Planet.MAX_RADIUS;
			// North and East directions are POSITIVE !
			AABB range = new AABB(position.cpy().sub(min_dist, min_dist),
								  position.cpy().add(min_dist, min_dist));
			ArrayList<Planet> neighbours = qt.query(range);
			boolean empty = true;
			for (Planet p : neighbours) {
				if (position.dst2(p.getPosition()) < min_dist*min_dist) {
					empty = false;
					break;
				}
			}
			if (empty) {
				Planet new_planet = new Planet(position, radius);
				qt.insert(new_planet);
				i += 1;
			}
		}
		System.out.print("time spent populating (ms): ");
		System.out.println(TimeUtils.millis()-start_time);
	}
}