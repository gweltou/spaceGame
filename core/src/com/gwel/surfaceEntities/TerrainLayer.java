package com.gwel.surfaceEntities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.spacegame.MyRenderer;


public class TerrainLayer {
	public static final float TERRAIN_BLOCK_SPAWN_RADIUS = 100f;
	private static final float TERRAIN_BLOCK_WIDTH = 100f;
	private World world;
	private RandomXS128 generator;
	private float scale;
	private ArrayDeque<TerrainBlock> blocks = new ArrayDeque<TerrainBlock>();
	private HeightArray[] heightArrays;
	private float[] amps;
	private XenoTreeManager xtm;
	private boolean withTrees;
	private boolean walkable;
	private float leftBoundary, rightBoundary;
	private float tmpFloat;
	Color color;
	
	
	public TerrainLayer(RandomXS128 generator, World w, HeightArray[] hArrays, float[] amps, Vector2 position, float scale, XenoTreeManager xtm, boolean walkable, boolean withTrees, Color col) {
		this.world = w;
		this.generator = generator;
		this.heightArrays = hArrays;
		this.amps = amps;
		this.scale = scale;
		this.xtm = xtm;
		this.withTrees = withTrees;
		this.walkable = walkable;
		leftBoundary = position.x;
		rightBoundary = leftBoundary + TERRAIN_BLOCK_WIDTH;
		System.out.println("From TerrainLayer.TerrainLayer:");
		System.out.print("    leftBoundary " + leftBoundary);
		System.out.println(" rightBoundary" + rightBoundary);
		
		// CREATE 1 INITIAL BLOCK PER LAYER
		float[] hsv = new float[3];
		col.toHsv(hsv);
		float level = (float) (Math.log(scale)/Math.log(0.5));
		float colSat = hsv[1]*((float) Math.pow(0.6f, level));
		float colVal = hsv[2];
		for (int i=0; i<=level; i++)
			colVal += (1.0f-colVal)*0.25f;
		color = new Color();
		color.a = 1.0f;
		color.fromHsv(hsv[0], colSat, colVal);
		Vector2[] mesh = createBlockMesh(position.x, position.x+TERRAIN_BLOCK_WIDTH);
		TerrainBlock block = new TerrainBlock(position, mesh, color);
		if (walkable)	block.initBody(world);
		if (withTrees) {
			float[] treeCoords = xtm.getCoordsBetween(leftBoundary, rightBoundary);
			for (float c : treeCoords) {
				block.addTree(xtm.buildTree(c, getHeight(c) - 2f));
			}
		}
		blocks.add(block);
	}
	
	public float getHeight(float position) {
		tmpFloat = 0.0f;
		for (int j=0; j<heightArrays.length; j++)
			tmpFloat += amps[j] * heightArrays[j].getHeight(position);
		return tmpFloat;
	}
	
	public void updateParallaxPosition(Vector2 travelling) {
		for (TerrainBlock tb: blocks) {
			tb.updateParallaxPosition(travelling.cpy().scl(1.0f-scale));
		}
	}

	public void render(MyRenderer renderer) {
		Affine2 transform = new Affine2();
		transform.idt();
		transform.scale(scale, scale);
		transform.translate(leftBoundary, 0f);

		renderer.pushMatrix(transform);

		for (TerrainBlock tb: blocks) {
			tb.render(renderer);
		}
		renderer.popMatrix();
		//renderer.flush();
	}

	private Vector2[] createBlockMesh(float leftCoord, float rightCoord) {		
		// CALCULATE MESH VERTICES HEIGHT COORDINATE
		
		ArrayList<Float> xCoords = new ArrayList<Float>();
		xCoords.add(leftCoord-0.01f);	// Add leftmost boundary coordinate
		for (HeightArray ha: heightArrays) {
			// Calculate a decimal index (between 0 and values.length) of leftCoord in height Array
			float normalizedIdx = ha.values.length * leftCoord / ha.span;
			normalizedIdx = normalizedIdx % ha.values.length;
			// Wrap around if index is negative
			if (normalizedIdx < 0.0f)	normalizedIdx += ha.values.length;

			float stepLength = ha.span / (ha.values.length * scale);	// length between each value (in game units)
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
		Vector2[] mesh = new Vector2[xCoords.size()];
		int i = 0;
		for (float xCoord: xCoords) {
			mesh[i++] = new Vector2(xCoord-leftCoord, getHeight(xCoord));
		}
		
		return mesh;
	}
	
	public void update(float xCoord) {
		TerrainBlock leftb = blocks.getFirst();
		TerrainBlock rightb = blocks.getLast();
		
		// xCoord is the absolute horizontal position of the player in world coordinates (no wrapping !)
		// Add a terrain block on the left if needed
		if (xCoord - leftb.position.x < TERRAIN_BLOCK_SPAWN_RADIUS) {
			leftBoundary -= TERRAIN_BLOCK_WIDTH;
			Vector2 blockPos = new Vector2(leftBoundary, leftb.position.y);
			Vector2[] mesh = createBlockMesh(leftBoundary, leftBoundary+TERRAIN_BLOCK_WIDTH);
			TerrainBlock newBlock = new TerrainBlock(blockPos, mesh, color);			
			// Initialize collision layer if needed
			if (walkable)	newBlock.initBody(world);
			// Add trees if needed
			if (withTrees) {
				float[] treeCoords = xtm.getCoordsBetween(leftBoundary, leftBoundary+TERRAIN_BLOCK_WIDTH);
				for (float treeXpos: treeCoords) {
					float treeYpos = getHeight(treeXpos)-2f;
					newBlock.addTree(xtm.buildTree(treeXpos, treeYpos));
					System.out.println("Tree added at :" + treeXpos);
				}
			}
			blocks.addFirst(newBlock);
			leftb = newBlock;
			//System.out.println("Added left");

			// Remove rightmost terrain block if needed
			if (rightb.position.x - xCoord > TERRAIN_BLOCK_SPAWN_RADIUS) {
				rightBoundary -= TERRAIN_BLOCK_WIDTH;
				rightb.dispose();
				blocks.removeLast();
				rightb = blocks.getLast();
				//System.out.println("Removed right");
			}
		}
		// Add a terrain block on the right if needed
		if (rightb.position.x + TERRAIN_BLOCK_WIDTH - xCoord < TERRAIN_BLOCK_SPAWN_RADIUS) {
			rightBoundary += TERRAIN_BLOCK_WIDTH;
			Vector2 blockPos = new Vector2(rightb.position.x+TERRAIN_BLOCK_WIDTH, rightb.position.y);
			Vector2[] mesh = createBlockMesh(rightBoundary-TERRAIN_BLOCK_WIDTH, rightBoundary);
			TerrainBlock newBlock = new TerrainBlock(blockPos, mesh, color);
			// Initialize collision layer if needed
			if (walkable)	newBlock.initBody(world);
			// Add trees if needed
			if (withTrees) {
				float[] treeCoords = xtm.getCoordsBetween(rightBoundary-TERRAIN_BLOCK_WIDTH, rightBoundary);
				for (float treeXpos: treeCoords) {
					float treeYpos = getHeight(treeXpos)-2f;
					newBlock.addTree(xtm.buildTree(treeXpos, treeYpos));
					System.out.println("Tree added at :" + treeXpos);
				}
			}
			blocks.addLast(newBlock);
			rightb = newBlock;
			//System.out.println("Added right");
					
			// Remove leftmost terrain block if needed
			if (xCoord - leftb.position.x + TERRAIN_BLOCK_WIDTH > TERRAIN_BLOCK_SPAWN_RADIUS) {
				leftBoundary += TERRAIN_BLOCK_WIDTH;
				leftb.dispose();
				blocks.removeFirst();
				leftb = blocks.getFirst();
				//System.out.println("Removed left");
			}
		}
	}

	public void dispose() {
		for (TerrainBlock block: blocks) {
			block.dispose();
		}
	}
}
