package com.gwel.surfaceEntities;

import java.util.ArrayDeque;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.spacegame.MyCamera;
import com.gwel.spacegame.MyRenderer;


public class TerrainLayer {
	public static final float TERRAIN_BLOCK_SPAWN_RADIUS = 100f;
	private static final float TERRAIN_BLOCK_WIDTH = 100f;
	private World world;
	private float scale;
	private ArrayDeque<TerrainBlock> blocks = new ArrayDeque<TerrainBlock>();
	private HeightArray[] heightArrays;
	private float[] amps;
	private XenoTreeManager xtm;
	Color color;
	
	
	public TerrainLayer(RandomXS128 generator, World w, float surfaceLength, HeightArray[] hArrays, float[] amps, Vector2 position, float scale, XenoTreeManager xtm, boolean walkable, boolean withTrees, Color col) {
		this.world = w;
		this.heightArrays = hArrays;
		this.amps = amps;
		this.scale = scale;
		this.xtm = xtm;
		
		// CREATE 1 INITIAL BLOCK PER LAYER
		TerrainBlock block;
		float[] hsv = new float[3];
		col.toHsv(hsv);
		float level = (float) (Math.log(scale)/Math.log(0.5));
		System.out.println("s " + scale + " l " +level);
		
		float colSat = hsv[1]*((float) Math.pow(0.6f, level));
		float colVal = hsv[2];
		System.out.println(colVal);
		for (int i=0; i<=level; i++)	colVal += (1.0f-colVal)*0.3f;
		color = new Color();
		color.a = 1.0f;
		color.fromHsv(hsv[0], colSat, colVal);
		System.out.println("h " + hsv[0] + " s " +colSat + " v " + colVal);
		block = new TerrainBlock(position, hArrays, amps, position.x, position.x+TERRAIN_BLOCK_WIDTH, color);
		if (walkable)	block.initBody(world);
		blocks.add(block);
	}
	
	public float getHeight(float position) {
		return blocks.getFirst().getHeight(position);
	}
	
	public void updateParallaxPosition(Vector2 travelling) {
		for (TerrainBlock tb: blocks) {
			tb.updateParallaxPosition(travelling.cpy().scl(1.0f-scale));
		}
	}
	
	public void render(MyRenderer renderer, MyCamera camera) {
		for (TerrainBlock tb: blocks) {
			tb.render(renderer, camera.sw.y, scale);
			
		}
		renderer.flush();
	}
	
	public void update(float xCoord) {
		// xCoord is the absolute horizontal position of the player in world coordinates (no wrapping !)
		TerrainBlock leftb = blocks.getFirst();
		TerrainBlock rightb = blocks.getLast();
		
		// Add a terrain block on the left if needed
		if (xCoord - leftb.position.x < TERRAIN_BLOCK_SPAWN_RADIUS) {
			Vector2 blockPos = new Vector2(leftb.position.x-TERRAIN_BLOCK_WIDTH, leftb.position.y);
			TerrainBlock newBlock = new TerrainBlock(blockPos,
					heightArrays, amps,
					leftb.leftBoundary-TERRAIN_BLOCK_WIDTH,
					leftb.rightBoundary-TERRAIN_BLOCK_WIDTH,
					color);
			// Initialize collision layer if needed
			if (leftb.terrainBody != null)	newBlock.initBody(world);
			blocks.addFirst(newBlock);
			leftb = newBlock;
			//System.out.println("Added left");

			// Remove rightmost terrain block if needed
			if (rightb.position.x - xCoord > TERRAIN_BLOCK_SPAWN_RADIUS) {
				rightb.dispose();
				blocks.removeLast();
				rightb = blocks.getLast();
				//System.out.println("Removed right");
			}
		}
		// Add a terrain block on the right if needed
		if (rightb.position.x + TERRAIN_BLOCK_WIDTH - xCoord < TERRAIN_BLOCK_SPAWN_RADIUS) {
			Vector2 blockPos = new Vector2(rightb.position.x+TERRAIN_BLOCK_WIDTH, rightb.position.y);
			TerrainBlock newBlock = new TerrainBlock(blockPos,
					heightArrays, amps,
					rightb.leftBoundary+TERRAIN_BLOCK_WIDTH,
					rightb.rightBoundary+TERRAIN_BLOCK_WIDTH,
					color);
			// Initialize collision layer if needed
			if (leftb.terrainBody != null)	newBlock.initBody(world);
			blocks.addLast(newBlock);
			rightb = newBlock;
			//System.out.println("Added right");
					
			// Remove leftmost terrain block if needed
			if (xCoord - leftb.position.x + TERRAIN_BLOCK_WIDTH > TERRAIN_BLOCK_SPAWN_RADIUS) {
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
