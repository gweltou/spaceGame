package com.gwel.spacegame;

import java.util.ArrayList;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.mappings.Xbox;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.entities.*;
import com.gwel.screens.ScreenInSpace;
import com.gwel.screens.ScreenOnPlanet;
import com.gwel.screens.ScreenTraining;


public class SpaceGame extends Game {
	final boolean TRAINING = false; 
	
	public MyRenderer renderer;
	public SpriteBatch batch;
	public Controller controller;
	public boolean hasController;
	public String controllerName;
	public RandomXS128 generator;
	public HUD hud;
	public DialogManager dialogManager;
	
	// GAME UNIVERSE VARIABLES
	private final static float UNIVERSE_SIZE = 100000.0f;
	//final static float exit_radius = GRAVITY_ACTIVE_RADIUS+200.0f;
	public QuadTree quadTree;
	ArrayList<ArrayList<String>> godNames;
	
	// Controller Mapping
	public int PAD_XAXIS;
	public int PAD_YAXIS;
	public float PAD_YDIR = 1f;
	public int PAD_BOOST;
	public int PAD_FIRE;
	
	// GAME STATE VARIABLES
	public Spaceship ship;
	private ScreenInSpace spaceScreen;
	private Planet mustLandOnPlanet;
	
	
	@Override
	public void create () {
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
            	PAD_YDIR = -1.0f;
            	PAD_BOOST = Xbox.A;
            	PAD_FIRE = Xbox.R_TRIGGER;
            } else if (controllerName.toLowerCase().contains("sony")) {
            	// Sony Ps4 controller
            	PAD_XAXIS = Ps4Controller.LSTICK_X;
            	PAD_YAXIS = Ps4Controller.LSTICK_Y;
            	PAD_YDIR = -1.0f;
            	PAD_BOOST = Ps4Controller.CROSS;
            	PAD_FIRE = Ps4Controller.R2;
            }
        }
		
		if (TRAINING) {
			System.out.println("training");
			setScreen(new ScreenTraining(this));
			return;
		}
		
		generator = (RandomXS128) MathUtils.random;
		//generator.setSeed((long) 6.0f);
		godNames = new ArrayList<>();
		loadGodNames();
		AABB universe_boundary = new AABB(new Vector2(-UNIVERSE_SIZE/2, -UNIVERSE_SIZE/2), new Vector2(UNIVERSE_SIZE/2, UNIVERSE_SIZE/2));
		quadTree = new QuadTree(universe_boundary);
		populateUniverse(quadTree);

		hud = new HUD(this);
		dialogManager = new DialogManager();

		renderer = new MyRenderer();
		batch = new SpriteBatch();

		// Game starting place
		mustLandOnPlanet = quadTree.element;	// First planet in QuadTree
		float angle = MathUtils.random(MathUtils.PI2);
		float radius = mustLandOnPlanet.radius;
		Vector2 startPos = mustLandOnPlanet.getPosition();
		startPos.add(radius*MathUtils.cos(angle), radius*MathUtils.sin(angle));
		ship = new Spaceship();
		ship.setPosition(startPos);

		spaceScreen = new ScreenInSpace(this);
	}

	@Override
	public void render() {
		super.render();

		hud.update();

		// Landing on planet
		if (mustLandOnPlanet != null) {
			setScreen(new ScreenOnPlanet(this, mustLandOnPlanet));
			mustLandOnPlanet = null;
		}
	}
	
	@Override
	public void resize (int width, int height) {
		super.resize(width, height);
	}
	
	@Override
	public void dispose () {
		hud.dispose();
		renderer.dispose();
		batch.dispose();
	}

	void land(Planet p) {
		// Don't dispose of ScreenInSpace !
		if (mustLandOnPlanet == null) {
			// This test prevents a change of screen during a Box2D step
			mustLandOnPlanet = p;
		}		
	}
	
	public void takeOff() {
		if (getScreen() != null) {
			getScreen().dispose();
		}
		setScreen(spaceScreen);
	}
	
	void populateUniverse(QuadTree qt) {
		long start_time = TimeUtils.millis();
		int i=0;
		while (i<Const.NUMBER_PLANETS) {
			Vector2 position = new Vector2(generator.nextFloat()*UNIVERSE_SIZE - UNIVERSE_SIZE/2,
					generator.nextFloat()*UNIVERSE_SIZE - UNIVERSE_SIZE/2);
			
			float radius = MathUtils.random(Const.PLANET_MIN_RADIUS, Const.PLANET_MAX_RADIUS);

			// Check if other planets are near this position
			float min_dist = 4*Const.PLANET_MAX_RADIUS;
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
				Planet new_planet = new Planet(position, radius, generator.nextLong());
				qt.insert(new_planet);
				i += 1;
			}
		}
		System.out.print("time spent populating (ms): ");
		System.out.println(TimeUtils.millis()-start_time);
	}
	
	void loadGodNames() {
		String[] god_files = {"albanian", "celts", "egyptian", "greek", "hindu",
				"japanese", "mesopotamian", "norse", "roman", "dedicaces"};
		
		long start_time = TimeUtils.millis();
		int num = 0;
		for (String filename: god_files) {
			ArrayList<String> array = new ArrayList<>();
			FileHandle file = Gdx.files.internal("gods/" + filename + ".txt");
			String content = file.readString();
			String[] lines = content.split("\n");
			for (String line: lines) {
				String name = line.split("\t")[0];
				//name = name.strip();
				if (!name.isEmpty() && !name.startsWith("#")) {
					num++;
					array.add(name);
				}
			}
			godNames.add(array);
		}
		System.out.print("Parsed " + num + " mythological names (ms): ");
		System.out.println(TimeUtils.millis()-start_time);
	}
	
	public String getPlanetName(long seed) {
		generator.setSeed(seed);
		int totNames = 0;
		for (ArrayList<String> array: godNames) {
			totNames += array.size();
		}
		// Create probability weight array
		String name = "";
		float r = generator.nextFloat();
		for (ArrayList<String> godName : godNames) {
			int numNames = godName.size();
			float prob = (float) numNames / totNames;
			if (r < prob) {
				name = godName.get(generator.nextInt(numNames));
				break;
			}
			r -= prob;
		}

		// Add something at the beginning
		if (generator.nextFloat() < 0.1) {
			// Add a sci-fi number to the end
			String str;
			r = generator.nextFloat();
			if (r < 0.6f) {
				String greekChars = "αβγδεζηθλμξπρΣστυφχψΩω";
				char c = greekChars.charAt(generator.nextInt(greekChars.length()));
				str = c + "-";
			} else if (r < 0.8f) {
				str = "World of ";
			} else if (r < 0.9f) {
				str = "New ";
			} else {
				str = "Neo-";
			}
			name = str.concat(name);
		}

		// Add something in the end
		if (generator.nextFloat() < 0.2) {
			String[] romanNums = {"Prime", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV"};
			String str;
			r = generator.nextFloat();
			if (r < 0.1f) {
				str = "'s Colony";
			} else if (r < 0.2f) {
				str  = " Minor";
			} else if (r < 0.3f) {
				str  = " Major";
			} else if (r < 0.6f) {
				// Roman numerals
				str = " " + romanNums[generator.nextInt(romanNums.length)];
			} else {
				// Add a sci-fi number to the end
				int i = generator.nextInt(1000);
				r = generator.nextFloat();
				if (r < 0.2f) {
					str = " " + i;
				} else if (r < 0.4f) {
					str = " " + (char) (generator.nextInt(26) + 'A') + i;
				} else if (r < 0.6f) {
					str = " " + (char) (generator.nextInt(26) + 'A') + "-" + i;
				} else if (r < 0.8f) {
					str = " " +
							(char) (generator.nextInt(26) + 'A') +
							(char) (generator.nextInt(26) + 'A') + i;
				} else {
					str = " " +
							(char) (generator.nextInt(26) + 'A') +
							(char) (generator.nextInt(26) + 'A') + "-" + i;
				}
			}
			name = name.concat(str);
		}
		return name;
	}
}