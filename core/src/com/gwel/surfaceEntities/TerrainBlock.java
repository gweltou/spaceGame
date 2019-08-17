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
	public Body terrainBody;
	private Vector2[] mesh;
	public Vector2 position = new Vector2();
	public final Color color = new Color();
	public HeightArray[] heightArrays;
	public float[] amps;
	private ArrayList<XenoTree> trees = new ArrayList<XenoTree>();
	
	public TerrainBlock(Vector2 position, Vector2[] mesh, Color color) {
		this.position.set(position);
		this.color.set(color);
		this.mesh = mesh;
	}
	
	public void initBody(World world) {
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
	
	public void updateParallaxPosition(Vector2 travelling) {
		// That'a a shitty way to update the TerrainBlock's position according to the camera translation
		// so it looks like it's on a different depth
		position.add(travelling);
	}
	
	public void addTree(XenoTree tree) {
		trees.add(tree);
	}
	
	public void render(MyRenderer renderer, float scale) {
		for (XenoTree tree: trees) {
			renderer.setColor(color);
			tree.render(renderer);
		}
		
		renderer.setColor(color);
		for (Vector2 point: mesh) {
			renderer.triangleStrip(position.x+point.x, position.y-50f, position.x+point.x, position.y+point.y);
			//renderer.flush(); // This doesn't work for some reason
		}
	}
	
	public void dispose() {
		for (XenoTree tree: trees)
			tree.dispose();
		if (terrainBody != null)	terrainBody.getWorld().destroyBody(terrainBody);
	}
}
