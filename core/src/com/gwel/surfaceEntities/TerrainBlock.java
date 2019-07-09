package com.gwel.surfaceEntities;

import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
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
	private Vector2[] coords;
	public Vector2 position = new Vector2();
	public final Color color = new Color();
	public float leftBoundary, rightBoundary;
	public HeightArray[] heightArrays;
	public float[] amps;
	private float tmpFloat;
	private ArrayList<XenoTree> trees = new ArrayList<XenoTree>();
	
	public TerrainBlock(Vector2 position, HeightArray[] heightArrays, float[] amps, float leftCoord, float rightCoord, Color color) {
		this.position.set(position);
		this.color.set(color);
		leftBoundary = leftCoord;
		rightBoundary = rightCoord;
		this.heightArrays = heightArrays;
		this.amps = amps.clone();
		//System.out.println(leftCoord + " " + rightCoord);
		
		ArrayList<Float> xCoords = new ArrayList<Float>();
		xCoords.add(leftCoord-0.01f);	// Add leftmost boundary coordinate
		for (HeightArray ha: heightArrays) {
			// Calculate a decimal index (between 0 and values.length) of leftCoord
			float normalizedIdx = ha.values.length * leftCoord / ha.span;
			normalizedIdx = normalizedIdx % ha.values.length;
			// Wrap around if index is negative
			if (normalizedIdx < 0.0f)	normalizedIdx += ha.values.length;
			float stepLength = ha.span / ha.values.length;	// length between each value (in game units)
			float stepToNextIndex = ha.vpu * (MathUtils.ceil(normalizedIdx) - normalizedIdx);	// length to next integer (in game units)
			float nextLength = leftCoord + stepToNextIndex;
			while (nextLength < rightCoord) {
				xCoords.add(nextLength);
				nextLength += stepLength;
			}
		}
		xCoords.add(rightCoord+0.01f);	// add rightmost boundary coordinate
		Collections.sort(xCoords);
		
		// Remove duplicate coords
		ArrayList<Float> tmpArray = new ArrayList<Float>();
		float lastItem = xCoords.get(0);
		tmpArray.add(lastItem);
		for (int i=1; i<xCoords.size(); i++) {
			tmpFloat = xCoords.get(i);
			if (Math.abs(lastItem-tmpFloat) > 0.01f)	tmpArray.add(tmpFloat);
			lastItem = tmpFloat;
		}
		xCoords = tmpArray;
		
		// Now convert every horizontal coordinate to a height value
		coords = new Vector2[xCoords.size()];
		int i = 0;
		for (float xCoord: xCoords) {
			coords[i++] = new Vector2(xCoord-leftCoord, getHeight(xCoord));
		}
		//System.out.println(xCoords);
	}
	
	public float getHeight(float xPos) {
		tmpFloat = 0.0f;
		for (int j=0; j<heightArrays.length; j++)
			tmpFloat += amps[j] * heightArrays[j].getHeight(xPos);
		return tmpFloat;
	}
	
	public void initBody(World world) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(position);
		terrainBody = world.createBody(bodyDef);
		ChainShape solidTerrainShape = new ChainShape();
		solidTerrainShape.createChain(coords);
		FixtureDef fd = new FixtureDef();
		fd.shape = solidTerrainShape; 
		fd.friction = 0.3f;
		fd.restitution = 0.5f;
		terrainBody.createFixture(fd);
		solidTerrainShape.dispose();
	}
	
	public void updateParallaxPosition(Vector2 travelling) {
		// That'a a shitty to update the TerrainBlock's position according to the camera translation
		// so it looks like it's on a different depth
		position.add(travelling);
	}
	
	public void addTree(XenoTree tree) {
		trees.add(tree);
	}
	
	public void render(MyRenderer renderer, float screenBottom, float scale) {
		for (XenoTree tree: trees) {
			tree.render(renderer);
		}
		for (Vector2 point: coords) {
			renderer.setColor(color);
			renderer.triangleStrip(position.x+point.x, screenBottom, position.x+point.x, position.y+point.y);
			//renderer.flush(); // This doesn't work for some reason
		}
	}
	
	public void dispose() {
		for (XenoTree tree: trees)
			tree.dispose();
		if (terrainBody != null)	terrainBody.getWorld().destroyBody(terrainBody);
	}
}
