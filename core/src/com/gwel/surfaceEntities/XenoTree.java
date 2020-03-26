package com.gwel.surfaceEntities;

import java.util.ArrayList;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.gwel.spacegame.MyRenderer;

public class XenoTree {
	public static int RANK_LIMIT = 14;
	public static int FLOWER_LIMIT = 1024;
	public static int LEAF_LIMIT = 1024;
	public static int SEG_LIMIT = 512;
	
	public TreeParam tp;
	public TreeParamMod tpm;
	public FlowerParam fp;
	public LeafParam lp;
	private ArrayList<TreeSegment> segs;
	public int nFlowers;
	public int nLeaves;
	private int odd;
	public int maxRank = 0;
	private WindManager wm;
	private Affine2 transform = new Affine2();
	private World world;
	
/*
	public XenoTree(World world, float x, float y, float w, RandomXS128 generator) {
		this.world = world;
		tp = new TreeParam(generator);
		tpm = new TreeParamMod(generator);
		fp = new FlowerParam(generator);
		lp = new LeafParam(generator);
		odd = Math.round(generator.nextFloat());
		segs = new ArrayList<TreeSegment>();
		segs.addAll(branch(x, y, 0.0f, w, tp, tpm, true, 0, 0, generator));
		
		wm = new WindManager();
	}
 */
	
	XenoTree(World world, float x, float y, float w, TreeParam tp, TreeParamMod tpm, FlowerParam fp, LeafParam lp, WindManager wm, RandomXS128 generator) {
		this.world = world;
		this.tp = tp;
		this.tpm = tpm;
		this.fp = fp;
		this.lp = lp;
		this.wm = wm;
		odd = Math.round(generator.nextFloat());
		segs = new ArrayList<TreeSegment>();
		segs.addAll(branch(x, y, 0.0f, w, tp, tpm, true, 0, 0, generator));
		/*println();
	    println("nSegs", segs.size());
	    println("nFlowers", nFlowers);
	    println("maxRank", maxRank);*/
	}

	public void initBody(World world) {
		for (TreeSegment segment: segs) {
			segment.initBody(world);
		}
	}

	void applyForce(Vector2 f) {
		for (TreeSegment seg: segs) {
			seg.body.applyForceToCenter(f.scl(seg.volume/100), false);
		}
	}

	public void render(MyRenderer renderer) {
		//System.out.println("rendering tree");
		// Draw trunk segments
		for (TreeSegment seg: segs) {
			seg.render(renderer);
			/*
	      for (RevoluteJoint joint: seg.joints) {
	        joint.setMotorSpeed(-2*joint.getJointAngle()-0.5*joint.getJointSpeed());
	      }*/
		}
		// Draw leaves
		for (TreeSegment seg: segs) {
			for (TreeLeaf leaf : seg.leaves) {
				transform.idt();
				transform.translate(seg.getPosition());
				transform.rotateRad(-seg.getAngle());
				renderer.pushMatrix(transform);
				leaf.render(renderer);
				renderer.popMatrix();
			}
		}
		// Draw flowers
		for (TreeSegment seg: segs) {
			for (TreeFlower flo : seg.flowers) {
				transform.idt();
				transform.translate(seg.getPosition());
				transform.rotateRad(-seg.getAngle());
				renderer.pushMatrix(transform);
				flo.render(renderer);
				renderer.popMatrix();
			}
		}
	}

	public void dispose() {
		for (int i=segs.size()-1; i>=0; i--) {
			segs.get(i).dispose();
		}
	}

	ArrayList<TreeSegment> branch(float x, float y, float angle, float base, TreeParam tp, TreeParamMod tpm, boolean root, int rank, int level, RandomXS128 generator) {
		ArrayList<TreeSegment> segs = new ArrayList<TreeSegment>();
		if (base < 0.1f || segs.size() >= SEG_LIMIT)
			return segs;
		maxRank = Math.max(maxRank, rank);
		float tall = base * tp.wToH;
		TreeSegment s = new TreeSegment(x, y, angle, base, base*tp.wCoeff, tall, root, rank, level);

		// add Flowers
		if (rank >= tp.floRank && nFlowers < FLOWER_LIMIT) {
			int nFlo = (int) Math.floor(generator.nextFloat()*2f);
			for (int i=0; i<nFlo; i++) {
				s.flowers.add(new TreeFlower(s.r1, tall, fp, wm));
				nFlowers++;
			}
		}
		// Add Leaves
		if ((rank >= tp.leafRank || level >= 2) && nLeaves < LEAF_LIMIT) {
			int n = (int) Math.floor(0.1*generator.nextFloat()*s.surface);
			for (int i=0; i<n; i++) {
				s.leaves.add(new TreeLeaf(s, lp, wm));
				nLeaves++;
			}
		}
		segs.add(s);

		if (rank < RANK_LIMIT) {
			// Grow further
			float nextX = x - tall*MathUtils.sin(angle);
			float nextY = y + tall*MathUtils.cos(angle);
			TreeParam newTp = tp.copy();
			newTp.applyMod(tpm);
			// ZigZag modifier
			if (rank%2 == odd)
				angle += tp.zigZag;
			else
				angle -= tp.zigZag;
			if (angle > MathUtils.PI) angle -= MathUtils.PI2;
			if (angle < -MathUtils.PI) angle += MathUtils.PI2;
			angle -= angle*tp.heliotropism;
			float nextAngle = angle + generator.nextFloat()*2*tp.angleChaos - tp.angleChaos;

			segs.addAll(branch(nextX, nextY, nextAngle, base*tp.wCoeff, newTp, tpm, false, rank+1, level, generator));

			// Add joint if there's a least 2 segments
			/*
			if (segs.size()>1) {
				RevoluteJointDef rjd = new RevoluteJointDef();
				Body b1 = s.body;
				Body b2 = segs.get(1).body;
				//rjd.bodyA = b1;
				//rjd.bodyB = b2;
				rjd.initialize(b1, b2, new Vector2(nextX, nextY));	// TODO: should be set manually
				rjd.enableLimit = true;
				rjd.collideConnected = false;
				rjd.lowerAngle = -0.12f;
				rjd.upperAngle = 0.12f;
				rjd.enableMotor = true;
				rjd.motorSpeed = 0.0f;
				rjd.maxMotorTorque = s.volume*tp.stiffCoeff;
				segs.get(1).joints.add((RevoluteJoint) world.createJoint(rjd));
			}

			 */

			// branch out
			if (generator.nextFloat() < tp.branchProb) {
				ArrayList<TreeSegment> newBranch;
				if (generator.nextFloat() < 0.5f) {
					angle += tp.branchAngle + generator.nextFloat()*2*tp.angleChaos - tp.angleChaos;
					newBranch = branch(nextX, nextY, angle, base*tp.wCoeff, newTp, tpm, false, rank+1, level+1, generator);
				} else {
					angle -= tp.branchAngle + generator.nextFloat()*2*tp.angleChaos - tp.angleChaos;
					newBranch = branch(nextX, nextY, angle-tp.branchAngle, base*tp.wCoeff, newTp, tpm, false, rank+1, level+1, generator);
				}
				/*
				if (!newBranch.isEmpty()) {
					RevoluteJointDef rjd = new RevoluteJointDef();
					Body b1 = s.body;
					Body b2 = newBranch.get(0).body;
					rjd.initialize(b1, b2, new Vector2(nextX, nextY));	// TODO: should be set manually
					rjd.collideConnected = false;
					rjd.enableLimit = true;
					rjd.lowerAngle = -0.12f;
					rjd.upperAngle = 0.12f;
					rjd.enableMotor = true;
					rjd.motorSpeed = 0.0f;
					rjd.maxMotorTorque = s.volume*tp.stiffCoeff;
					newBranch.get(0).joints.add((RevoluteJoint) world.createJoint(rjd));
				}

				 */
				segs.addAll(newBranch);
			}
		}
		return segs;
	}
}

class TreeParam {
	public float wCoeff;
	public float wToH;
	public float stiffCoeff;
	public float branchAngle;
	public float branchProb;
	public float angleChaos;
	public float zigZag;
	public float heliotropism;
	public int floRank;
	public int leafRank;

	TreeParam(RandomXS128 generator) {
		wCoeff = generator.nextFloat() * 0.14f + 0.82f;
		wToH = generator.nextFloat() * 0.8f + 1.2f;
		stiffCoeff = generator.nextFloat() * 1000f;
		branchAngle = generator.nextFloat() + 0.2f;
		branchProb = generator.nextFloat() * 0.15f + 0.25f;
		angleChaos = generator.nextFloat() * 0.32f;
		zigZag = 0.0f;
		if (generator.nextFloat() < 0.3f)
			zigZag = generator.nextFloat() * 0.5f;
		heliotropism = generator.nextFloat() * 0.8f - 0.4f;
		floRank = (int) Math.floor(generator.nextFloat() * (XenoTree.RANK_LIMIT-2f) + 2f);
		leafRank = (int) Math.floor(generator.nextFloat() * (XenoTree.RANK_LIMIT-2f) + 2f);

		//println();
		//println("wCoeff", wCoeff);
		//println("wToH", wToH);
		//println("stiffCoeff", stiffCoeff);
		//println("branchAngle", branchAngle);
		//println("branchProb", branchProb);
		//println("angleChaos", angleChaos);
		//println("zigZag", zigZag);
		//println("floRank", floRank);
	}

	TreeParam(float wc, float wh, float sc, float ba, float bp, float ac, float zz, float ht, int fr, int lr) {
		wCoeff = wc;
		wToH = wh;
		stiffCoeff = sc;
		branchAngle = ba;
		branchProb = bp;
		angleChaos = ac;
		zigZag = zz;
		heliotropism = ht;
		floRank = fr;
		leafRank = lr;
	}

	TreeParam copy() {
		return new TreeParam(wCoeff, wToH, stiffCoeff, branchAngle, branchProb, angleChaos, zigZag, heliotropism, floRank, leafRank);
	}

	void applyMod(TreeParamMod mod) {
		wCoeff *= mod.wCoeffMod;
		wToH *= mod.wToHMod;
		branchAngle *= mod.bAngleMod;
		branchProb *= mod.bProbMod;
		angleChaos *= mod.aChaosMod;
		zigZag *= mod.zzMod;
	}
}


class TreeParamMod {
	public float wCoeffMod;
	public float wToHMod;
	public float bAngleMod;
	public float bProbMod;
	public float aChaosMod;
	public float zzMod;
	
	TreeParamMod(RandomXS128 generator) {
		wCoeffMod = generator.nextFloat()*0.1f + 0.86f;
		wToHMod = generator.nextFloat()*0.28f + 0.92f;
		bAngleMod = generator.nextFloat()*0.4f + 0.8f;
		bProbMod = generator.nextFloat()*0.35f + 0.8f;
		aChaosMod = generator.nextFloat()*0.5f + 0.7f;
		zzMod = generator.nextFloat()*0.6f + 0.6f;
	}
}