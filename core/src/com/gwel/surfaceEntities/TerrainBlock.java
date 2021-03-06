package com.gwel.surfaceEntities;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.gwel.spacegame.MyRenderer;


public class TerrainBlock {
	/* A block of Terrain, part of a Terrain Layer
	   It has a polygons mesh, a physics body, trees
	   position is in absolute planet coordinates
	 */
	public Body terrainBody;
	private final Vector2[] mesh;
	public Vector2 position = new Vector2();
	public final Color color = new Color();
	private final ArrayList<XenoTree> trees = new ArrayList<>();
	private DelaunayRock[] rocks;
	
	public TerrainBlock(Vector2 position, Vector2[] mesh, Color color) {
		this.position.set(position);
		this.color.set(color);
		this.mesh = mesh;
	}
	
	public void initBody(World world) {
		/*
			initBody is called from the outside (from WalkingLayer)
		 */
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(position);
		terrainBody = world.createBody(bodyDef);
		ChainShape solidTerrainShape = new ChainShape();
		solidTerrainShape.createChain(mesh);
		FixtureDef fd = new FixtureDef();
		fd.shape = solidTerrainShape; 
		fd.friction = 0.3f;
		fd.restitution = 0.5f;
		terrainBody.createFixture(fd);
		solidTerrainShape.dispose();
	}
	
	public void addTree(XenoTree tree) {
		trees.add(tree);
	}

	public void addRocks(DelaunayRock[] rocks) {
		this.rocks = rocks;
	}

	public void renderTerrain(MyRenderer renderer) {
		renderer.setColor(color);
		for (int i=0; i<mesh.length-1; i++) {
			renderer.triangle(	new Vector2(position.x+mesh[i].x, -50f),
					new Vector2(position.x+mesh[i].x, position.y+mesh[i].y),
					new Vector2(position.x+mesh[i+1].x, position.y+mesh[i+1].y));
			renderer.triangle(	new Vector2(position.x+mesh[i+1].x, position.y+mesh[i+1].y),
					new Vector2(position.x+mesh[i+1].x, -50f),
					new Vector2(position.x+mesh[i].x, -50f));
		}
	}

	public void renderRocks(MyRenderer renderer) {
		for (DelaunayRock rock: rocks) {
			rock.render(renderer);
		}
	}

	public void renderTrees(MyRenderer renderer) {
		for (XenoTree tree: trees) {
			renderer.setColor(color);
			tree.render(renderer);
		}
	}
	
	public void dispose(float surfaceLength) {
		for (XenoTree tree : trees)
			tree.dispose();

		if (terrainBody != null)	terrainBody.getWorld().destroyBody(terrainBody);
	}
}
