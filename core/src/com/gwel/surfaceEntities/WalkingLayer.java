package com.gwel.surfaceEntities;

import java.util.ArrayDeque;
import java.util.LinkedList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gwel.entities.Planet;
import com.gwel.spacegame.MyRenderer;


public class WalkingLayer implements Disposable {
	public static final float TERRAIN_BLOCK_SPAWN_RADIUS = 100f;
	public static final float TERRAIN_BLOCK_WIDTH = 100f;
	private final World world;
	private final Planet planet;
	private final ArrayDeque<TerrainBlock> blocks = new ArrayDeque<>();
	private final Terrain terrain;
	private final XenoTreeManager xtm;
	private final InhabitantLayer inm;
	private final boolean withTrees;
	private float leftBoundary, rightBoundary;
	private final Color color;
	
	
	public WalkingLayer(World w, Planet planet, Vector2 position, float scale, boolean withTrees, XenoTreeManager xtm) {
		this.world = w;
		this.planet = planet;
		this.terrain = new Terrain(4, planet.surfaceLength, scale);
		this.withTrees = withTrees;
		this.xtm = xtm;
		this.inm = new InhabitantLayer(world, planet, terrain);

		leftBoundary = position.x;
		rightBoundary = leftBoundary + TERRAIN_BLOCK_WIDTH;
		System.out.println("From WalkingLayer:");
		System.out.print("    leftBoundary " + leftBoundary);
		System.out.println(" rightBoundary" + rightBoundary);
		
		// CREATE 1 INITIAL BLOCK
		float level = (float) (Math.log(scale)/Math.log(0.5));
		float colSat = planet.colorSat*((float) Math.pow(0.6f, level));
		float colVal = planet.colorVal;
		for (int i=0; i<=level; i++)
			colVal += (1.0f-colVal)*0.25f;
		color = new Color();
		color.a = 1.0f;
		color.fromHsv(planet.colorHue, colSat, colVal);

		Vector2[] mesh = terrain.coordsBetween(leftBoundary, rightBoundary);
		TerrainBlock block = new TerrainBlock(position, mesh, color);
		block.initBody(world);
		if (withTrees) {
			float[] treeCoords = xtm.treesBetween(leftBoundary, rightBoundary);
			for (float c : treeCoords) {
				block.addTree(xtm.buildTree(c, getHeight(c) - 2f));
			}
		}

		// Add people
		inm.addBetween(leftBoundary, rightBoundary);

		blocks.add(block);
	}
	
	public float getHeight(float position) {
		return terrain.getHeight(position);
	}

	public LinkedList<Inhabitant> getInhabitants() {
		return inm.alive;
	}

	public void update(float xCoord) {
		TerrainBlock leftb = blocks.getFirst();
		TerrainBlock rightb = blocks.getLast();
		
		// xCoord is the absolute horizontal position of the player in world coordinates (no wrapping !)
		// Add a terrain block on the left if needed
		if (xCoord - leftBoundary < TERRAIN_BLOCK_SPAWN_RADIUS) {
			leftBoundary -= TERRAIN_BLOCK_WIDTH;
			Vector2 blockPos = new Vector2(leftBoundary, leftb.position.y);
			Vector2[] mesh = terrain.coordsBetween(leftBoundary, leftBoundary+TERRAIN_BLOCK_WIDTH);
			TerrainBlock newBlock = new TerrainBlock(blockPos, mesh, color);			
			// Initialize collision layer
			newBlock.initBody(world);
			blocks.addFirst(newBlock);
			leftb = newBlock;

			// Add trees if needed
			if (withTrees) {
				float[] treeCoords = xtm.treesBetween(leftBoundary, leftBoundary+TERRAIN_BLOCK_WIDTH);
				for (float treeXpos: treeCoords) {
					float treeYpos = getHeight(treeXpos)-2f;
					newBlock.addTree(xtm.buildTree(treeXpos, treeYpos));
				}
			}
			// Add NPC
			inm.addBetween(leftBoundary, leftBoundary+TERRAIN_BLOCK_WIDTH);

			// Remove rightmost terrain block if needed
			if (rightb.position.x - xCoord > TERRAIN_BLOCK_SPAWN_RADIUS) {
				rightBoundary -= TERRAIN_BLOCK_WIDTH;
				rightb.dispose(planet.surfaceLength);
				blocks.removeLast();
				rightb = blocks.getLast();
				// Remove NPC
				inm.removeBetween(rightBoundary, rightBoundary+TERRAIN_BLOCK_WIDTH);
			}
		}
		// Add a terrain block on the right if needed
		if (rightBoundary - xCoord < TERRAIN_BLOCK_SPAWN_RADIUS) {
			Vector2 blockPos = new Vector2(rightBoundary, rightb.position.y);
			rightBoundary += TERRAIN_BLOCK_WIDTH;
			Vector2[] mesh = terrain.coordsBetween(blockPos.x, rightBoundary);
			TerrainBlock newBlock = new TerrainBlock(blockPos, mesh, color);
			// Initialize collision layer
			newBlock.initBody(world);
			blocks.addLast(newBlock);

			// Add trees if needed
			if (withTrees) {
				float[] treeCoords = xtm.treesBetween(rightBoundary-TERRAIN_BLOCK_WIDTH, rightBoundary);
				for (float treeXpos: treeCoords) {
					float treeYpos = getHeight(treeXpos)-2f;
					newBlock.addTree(xtm.buildTree(treeXpos, treeYpos));
				}
			}
			// Add NPC
			inm.addBetween(rightBoundary-TERRAIN_BLOCK_WIDTH, rightBoundary);

			// Remove leftmost terrain block if needed
			if (xCoord - leftb.position.x + TERRAIN_BLOCK_WIDTH > TERRAIN_BLOCK_SPAWN_RADIUS) {
				leftBoundary += TERRAIN_BLOCK_WIDTH;
				leftb.dispose(planet.surfaceLength);
				blocks.removeFirst();
				// Remove NPC
				inm.removeBetween(leftBoundary-TERRAIN_BLOCK_WIDTH, leftBoundary);
			}
		}
	}

	public void render(MyRenderer renderer) {
		for (TerrainBlock tb : blocks) {
			tb.renderTerrain(renderer);
		}
		for (TerrainBlock tb : blocks) {
			tb.renderTrees(renderer);
		}
		inm.render(renderer);
	}

	public void dispose() {
		for (TerrainBlock block: blocks) {
			// We need to pass surfaceLength to reset inhabitants positions
			block.dispose(planet.surfaceLength);
		}
		inm.dispose();
	}
}
