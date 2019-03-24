package com.gwel.spacegame;

import java.util.ArrayList;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.mappings.Xbox;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
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
	public String controllerName;
	
	// GAME UNIVERSE VARIABLES
	final static float UNIVERSE_SIZE = 100000.0f;
	final static float GRAVITY_ACTIVE_RADIUS = 800.0f;
	final static int NUMBER_PLANETS = 10000;
	public final static float LOCAL_RADIUS = GRAVITY_ACTIVE_RADIUS;
	final static float exit_radius = GRAVITY_ACTIVE_RADIUS+200.0f;
	public QuadTree Qt;
	ArrayList<ArrayList<String>> godNames;
	
	// Controller Mapping
	public int PAD_XAXIS;
	public int PAD_YAXIS;
	public int PAD_BOOST;
	public int PAD_FIRE;
	
	public Spaceship ship;
	
	
	@Override
	public void create () {
		//gameState = new GameState();
		
		if(Controllers.getControllers().size == 0)
            hasController = false;
        else {
            controller = Controllers.getControllers().first();
            hasController = true;
            controllerName = controller.getName();
            System.out.println("Controller detected : " + controllerName);
            if (Xbox.isXboxController(controller)) {
            	System.out.println("Xbox Controller detected");
            	PAD_XAXIS = Xbox.L_STICK_HORIZONTAL_AXIS;
            	PAD_YAXIS = Xbox.L_STICK_VERTICAL_AXIS;
            	PAD_BOOST = Xbox.A;
            	PAD_FIRE = Xbox.R_TRIGGER;
            } else if (controllerName.toLowerCase().contains("sony")) {
            	// Sony Ps4 controller
            	PAD_XAXIS = Ps4Controller.LSTICK_X;
            	PAD_YAXIS = Ps4Controller.LSTICK_Y;
            	PAD_BOOST = Ps4Controller.CROSS;
            	PAD_FIRE = Ps4Controller.R2;
            }
        }
		
		AABB universe_boundary = new AABB(new Vector2(0, 0), new Vector2(UNIVERSE_SIZE, UNIVERSE_SIZE));
		Qt = new QuadTree(universe_boundary);
		populateUniverse(Qt);
		godNames = new ArrayList<ArrayList<String>>();
		loadGodNames();
		
		Vector2 universeCenter = new Vector2(UNIVERSE_SIZE/2, UNIVERSE_SIZE/2);
		camera = new MyCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.setCenter(universeCenter);
		camera.update();
		renderer = new MyRenderer(camera);
		
		ship = new Spaceship(universeCenter);
		
		setScreen(new ScreenInSpace(this));
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void resize (int width, int height) {
		super.resize(width, height);
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
		RandomXS128 randGenerator = new RandomXS128();
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
				Planet new_planet = new Planet(position, radius, randGenerator.nextLong());
				qt.insert(new_planet);
				i += 1;
			}
		}
		System.out.print("time spent populating (ms): ");
		System.out.println(TimeUtils.millis()-start_time);
	}
	
	void loadGodNames() {
		String[] god_files = {"albanian", "celts", "egyptian", "greek", "hindu", "japanese", "mesopotamian", "norse", "roman"};
		
		long start_time = TimeUtils.millis();
		int num = 0;
		for (String filename: god_files) {
			ArrayList<String> array = new ArrayList<String>();
			FileHandle file = Gdx.files.internal("gods/" + filename + ".txt");
			String content = file.readString();
			String[] lines = content.split("\n");
			for (String line: lines) {
				String name = line.split("\t")[0];
				//name = name.strip();
				if (!name.isEmpty()) {
					num++;
					array.add(name);
				}
			}
			godNames.add(array);
		}
		System.out.print("Parsed " + String.valueOf(num) + " mythological names (ms): ");
		System.out.println(TimeUtils.millis()-start_time);
	}
	
	String getPlanetName(long seed) {
		RandomXS128 generator = new RandomXS128(seed);
		int totNames = 0;
		for (ArrayList<String> array: godNames) {
			totNames += array.size();
		}
		// Create probability weight array
		String name = "";
		float r = generator.nextFloat();
		for (int i=0; i<godNames.size(); i++) {
			int numNames = godNames.get(i).size();
			float prob = (float) numNames/totNames;
			if (r < prob) {
				name = godNames.get(i).get(generator.nextInt(numNames));
				break;
			}
			r -= prob;
		}
		
		
		if (generator.nextFloat() < 0.2) {
			// Add a sci-fi number
			int i = generator.nextInt(1000);
			String str = " ";
			str = str.concat(String.valueOf(i));
			name = name.concat(str);
		}
		
		return name;
	}
}