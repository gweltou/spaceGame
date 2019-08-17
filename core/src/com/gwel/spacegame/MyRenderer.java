package com.gwel.spacegame;

import java.util.ArrayDeque;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;


public class MyRenderer implements Disposable {
	public static final String VERT_SHADER =  
			"attribute vec2 a_position;\n" +
			"attribute vec4 a_color;\n" +			
			"uniform mat4 u_projModelView;\n" + 
			"varying vec4 vColor;\n" +			
			"void main() {\n" +  
			"	vColor = a_color;\n" +
			"	gl_Position =  u_projModelView * vec4(a_position.xy, 0.0, 1.0);\n" +
			"}";
	
	public static final String FRAG_SHADER = 
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
			"varying vec4 vColor;\n" + 			
			"void main() {\n" +  
			"	gl_FragColor = vColor;\n" + 
			"}";
	
	protected static ShaderProgram createShaderProgram(String vertexShader, String fragShader) {
		ShaderProgram.pedantic = false;
		ShaderProgram shader = new ShaderProgram(vertexShader, fragShader);
		String log = shader.getLog();
		if (!shader.isCompiled())
			throw new GdxRuntimeException(log);		
		if (log!=null && log.length()!=0)
			System.out.println("Shader Log: "+log);
		return shader;
	}
	
	//The maximum number of triangles our mesh will hold
	public static final int MAX_TRIS = 2048;

	//The maximum number of vertices our mesh will hold
	//public static final int MAX_VERTS = MAX_TRIS * 3;

	//The array which holds all the data, interleaved like so:
	//    x, y, r, g, b, a
	//    x, y, r, g, b, a, 
	//    x, y, r, g, b, a, 
	//    ... etc ...
	private float[] verts_triangle = new float[3*MAX_TRIS * (2+4)];	// POSITION_ATTRIBUTE + COLOR_ATTRIBUTE
	private float[] verts_trianglestrip = new float[2*MAX_TRIS * (2+4)];	// POSITION_ATTRIBUTE + COLOR_ATTRIBUTE

	//The index positions
	private int iTriangle;
	private int iTrianglestrip;
	
	public MyCamera camera;
	private Mesh meshTriangles;
	private Mesh meshTrianglestrip;
	private final ShaderProgram shader;
	private final Color color = new Color(1, 1, 1, 1);
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();
	private boolean matrixDirty = false;
	private final ArrayDeque<Affine2> matrixStack = new ArrayDeque<Affine2>();
	private final Vector2 tmpv1 = new Vector2();
	private final Vector2 tmpv2 = new Vector2();
	private final Vector2 tmpv3 = new Vector2();
	
	
	public MyRenderer(MyCamera camera) {
		this.camera = camera;
		iTriangle = 0;
		iTrianglestrip = 0;
		meshTriangles = new Mesh(true, 3*MAX_TRIS, 0, 
				new VertexAttribute(Usage.Position, 2, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, 4, "a_color"));
		meshTrianglestrip = new Mesh(true, 2*MAX_TRIS, 0, 
				new VertexAttribute(Usage.Position, 2, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, 4, "a_color"));
		
		shader = createShaderProgram(VERT_SHADER, FRAG_SHADER);
		
		System.out.println("Renderer created");
	}

	public void setCamera(MyCamera cam) {
		camera = cam;
	}
	
	public void setColor(float r, float g, float b, float a) {
		this.color.set(r, g, b, a);
	}
	
	public void setColor(Color color) {
		this.color.set(color);
	}
	
	public void setProjectionMatrix(Matrix4 matrix) {
		projectionMatrix.set(matrix);
		matrixDirty = true;
	}
	
	public void setTransformMatrix(Matrix4 matrix) {
		transformMatrix.set(matrix);
		matrixDirty = true;
	}
	
	/** Sets the transformation matrix to identity. */
	public void identity() {
		transformMatrix.idt();
		matrixDirty = true;
	}

	/** Multiplies the current transformation matrix by a translation matrix. */
	public void translate (float x, float y, float z) {
		transformMatrix.translate(x, y, z);
		matrixDirty = true;
	}

	/** Multiplies the current transformation matrix by a rotation matrix. */
	public void rotate (float axisX, float axisY, float axisZ, float degrees) {
		transformMatrix.rotate(axisX, axisY, axisZ, degrees);
		matrixDirty = true;
	}

	/** Multiplies the current transformation matrix by a scale matrix. */
	public void scale (float scaleX, float scaleY, float scaleZ) {
		transformMatrix.scale(scaleX, scaleY, scaleZ);
		matrixDirty = true;
	}
	
	public void pushMatrix(Affine2 matrix) {
		Affine2 cpy = new Affine2();
		cpy.set(matrix);
		if (matrixStack.isEmpty()) {
			matrixStack.push(cpy);
		} else {
			matrixStack.push(cpy.mul(matrixStack.getFirst()));
		}
		//System.out.println("push");
	}
	
	public void popMatrix() {
		matrixStack.pop();
		//System.out.println("pop");
	}
	
	public void triangle(Vector2 p1, Vector2 p2, Vector2 p3) {
		if (matrixStack.isEmpty()) {
			triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
		} else {
			tmpv1.set(p1);
			tmpv2.set(p2);
			tmpv3.set(p3);
			matrixStack.getFirst().applyTo(tmpv1);
			matrixStack.getFirst().applyTo(tmpv2);
			matrixStack.getFirst().applyTo(tmpv3);
			triangle(tmpv1.x, tmpv1.y, tmpv2.x, tmpv2.y, tmpv3.x, tmpv3.y);
		}
	}
	
	public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
		//we don't want to hit any index out of bounds exception...
		//so we need to flush the batch if we can't store any more verts
		if (iTriangle==verts_triangle.length)
			flush();

		//now we push the vertex data into our array
		//we are assuming (0, 0) is lower left, and Y is up

		//bottom left vertex
		verts_triangle[iTriangle++] = x1; 			//Position(x, y) 
		verts_triangle[iTriangle++] = y1;
		verts_triangle[iTriangle++] = color.r; 	//Color(r, g, b, a)
		verts_triangle[iTriangle++] = color.g;
		verts_triangle[iTriangle++] = color.b;
		verts_triangle[iTriangle++] = color.a;

		//top left vertex
		verts_triangle[iTriangle++] = x2; 			//Position(x, y) 
		verts_triangle[iTriangle++] = y2 ;
		verts_triangle[iTriangle++] = color.r; 	//Color(r, g, b, a)
		verts_triangle[iTriangle++] = color.g;
		verts_triangle[iTriangle++] = color.b;
		verts_triangle[iTriangle++] = color.a;

		//bottom right vertex
		verts_triangle[iTriangle++] = x3;	 //Position(x, y) 
		verts_triangle[iTriangle++] = y3;
		verts_triangle[iTriangle++] = color.r;		 //Color(r, g, b, a)
		verts_triangle[iTriangle++] = color.g;
		verts_triangle[iTriangle++] = color.b;
		verts_triangle[iTriangle++] = color.a;
	}
	
	public void triangleStrip(double x1, double y1, double x2, double y2) {
		triangleStrip(x1, y1, x2, y2, color);
	}
	
	private void triangleStrip(double x1, double y1, double x2, double y2, Color color) {
		if (iTrianglestrip==verts_trianglestrip.length)
			flush();

		verts_trianglestrip[iTrianglestrip++] = (float) x1; 			//Position(x, y) 
		verts_trianglestrip[iTrianglestrip++] = (float) y1;
		verts_trianglestrip[iTrianglestrip++] = color.r; 	//Color(r, g, b, a)
		verts_trianglestrip[iTrianglestrip++] = color.g;
		verts_trianglestrip[iTrianglestrip++] = color.b;
		verts_trianglestrip[iTrianglestrip++] = color.a;

		verts_trianglestrip[iTrianglestrip++] = (float) x2; 			//Position(x, y) 
		verts_trianglestrip[iTrianglestrip++] = (float) y2 ;
		verts_trianglestrip[iTrianglestrip++] = color.r; 	//Color(r, g, b, a)
		verts_trianglestrip[iTrianglestrip++] = color.g;
		verts_trianglestrip[iTrianglestrip++] = color.b;
		verts_trianglestrip[iTrianglestrip++] = color.a;
	}
	
	public void texturedSquare(float xPos, float yPos, float width, float height, Vector2 uv0, Vector2 uv1, Vector2 uv2) {	}
	
	
	/** Calls circleOpt(Vector2, float, int)} by estimating the number of segments needed for a smooth circle. */
	public void circleOpt (Vector2 pos, float radius) {
		circleOpt(pos, radius, Math.max(1,  ((int) Math.sqrt(radius*camera.PPU))<<2 ));
	}

	/** Calls circle(Vector2, float, int)} by estimating the number of segments needed for a smooth circle. */
	public void circle (Vector2 pos, float radius) {
		circle(pos.x, pos.y, radius, Math.max(1,  ((int) Math.sqrt(radius*camera.PPU))<<2 ));
	}
	
	public void circle (float x, float y, float radius, int segments) {
		if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.");
		float angle = 2 * MathUtils.PI / segments;
		float cos = MathUtils.cos(angle);
		float sin = MathUtils.sin(angle);
		float cx = radius, cy = 0;
		float newX, newY;
		
		segments--;
		for (int i = 0; i < segments; i++) {
			newX = cos * cx - sin * cy;
			newY = sin * cx + cos * cy;
			triangle(x, y, x + cx, y + cy, x + newX, y + newY);
			cx = newX;
			cy = newY;
		}
		// Ensure the last segment is identical to the first.
		newX = radius;
		newY = 0;
		triangle(x, y, x + cx, y + cy, x + newX, y + newY);
	}
	
	/* 
	 * Optimized circle (only visible quarters are drawn)
	 */
	public void circleOpt (Vector2 pos, final float radius, final int segments) {
		if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.");
		//float colorBits = color.toFloatBits();
		
		//   6  |  7  |  8
		// -----|-----|-----
		//   4  | V.P |  5 
		// -----|-----|-----
		//   1  |  2  |  3
		
		//System.out.println(segments);
		float angle = 2 * MathUtils.PI / segments;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double newX, newY;
		Vector2 rotated = pos.cpy();
		rotated.sub(camera.center);
		rotated.rotate(-camera.angle);
		rotated.add(camera.center);
		if (rotated.x < camera.sw.x) {
			// Circle center is outside the viewport, on the left
			if (rotated.y < camera.sw.y) {
				// Case 1
				// Circle center is outside the viewport, on the bottom-left corner
				double cx = radius;
				double cy = 0;
				// Draw a quarter circle only
				for (int i=0; i<segments/4; i++) {
					newX = cx*cos - cy*sin;
					newY = cx*sin + cy*cos;
					//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
					triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
					cx = newX;
					cy = newY;
				}
			} else if (rotated.y > camera.ne.y) {
				// Case 6
				// Circle center is outside the viewport, on the top-left corner
				double cx = 0;
				double cy = -radius;
				// Draw a quarter circle only
				for (int i=0; i<segments/4; i++) {
					newX = cx*cos - cy*sin;
					newY = cx*sin + cy*cos;
					//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
					triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
					cx = newX;
					cy = newY;
				}
			} else {
				// Case 4
				// Circle center is outside the viewport, on the left side
				double cx = 0;
				double cy = -radius;
				// Draw a half circle only
				for (int i=0; i<segments/2; i++) {
					newX = cx*cos - cy*sin;
					newY = cx*sin + cy*cos;
					//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
					triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
					cx = newX;
					cy = newY;
				}
			}
		} else if (rotated.x > camera.ne.x) {
			// Circle center is outside the viewport, on the right
			if (rotated.y < camera.sw.y) {
				// Case 3
				// Circle center is outside the viewport, on the bottom-right corner
				double x = 0;
				double y = radius;
				// Draw a quarter circle only
				for (int i=0; i<segments/4; i++) {
					newX = x*cos - y*sin;
					newY = x*sin + y*cos;
					//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
					triangle(pos.x, pos.y,(float) x+pos.x,(float) y+pos.y,(float) newX+pos.x,(float) newY+pos.y);
					x = newX;
					y = newY;
				}
			} else if (rotated.y > camera.ne.y) {
				// Case 8
				// Circle center is outside the viewport, on the top-right corner
				double cx = -radius;
				double cy = 0;
				// Draw a quarter circle only
				for (int i=0; i<segments/4; i++) {
					newX = cx*cos - cy*sin;
					newY = cx*sin + cy*cos;
					//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
					triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
					cx = newX;
					cy = newY;
				}
			} else {
				// Case 5
				// Circle center is outside the viewport, on the right side
				double cx = 0;
				double cy = radius;
				// Draw a half circle only
				for (int i=0; i<segments/2; i++) {
					newX = cx*cos - cy*sin;
					newY = cx*sin + cy*cos;
					//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
					triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
					cx = newX;
					cy = newY;
				}
			}
		} else if (rotated.y < camera.sw.y) {
			// Case 2
			// Circle center is outside the viewport, under it
			double cx = radius;
			double cy = 0;
			// Draw a half circle only
			for (int i=0; i<segments/2; i++) {
				newX = cx*cos - cy*sin;
				newY = cx*sin + cy*cos;
				//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
				triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
				cx = newX;
				cy = newY;
			}
		} else if (rotated.y > camera.ne.y) {
			// Case 7
			// Circle center is outside the viewport, above it
			double cx = -radius;
			double cy = 0;
			// Draw a half circle only
			for (int i=0; i<segments/2; i++) {
				newX = cx*cos - cy*sin;
				newY = cx*sin + cy*cos;
				//triangleStrip(x+pos.x, camera.sw.y, newX+pos.x, newY+pos.y);
				triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
				cx = newX;
				cy = newY;
			}
		} else {
			// Case 0
			// Circle center is in the viewport
			double cx = radius;
			double cy = 0;
			newX = 0;
			newY = 0;
			for (int i = 0; i < segments-1; i++) {
				newX = cos*cx - sin*cy;
				newY = sin*cx + cos*cy;
				triangle(pos.x, pos.y,(float) cx+pos.x,(float) cy+pos.y,(float) newX+pos.x,(float) newY+pos.y);
				cx = newX;
				cy = newY;
			}
			// Ensure the last segment is identical to the first.
			triangle(pos.x, pos.y, (float) cx+pos.x,(float) cy+pos.y,(float) pos.x+radius, pos.y);
		}
	}
	
	public void flush() {
		//if we've not already flushed
		if (iTriangle>0 || iTrianglestrip>0) {
			//no need for depth...
			Gdx.gl.glDepthMask(false);

			//enable blending, for alpha
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			if (matrixDirty) {
				combinedMatrix.set(projectionMatrix);
				Matrix4.mul(combinedMatrix.val, transformMatrix.val);
				matrixDirty = false;
			}
			shader.begin();
			shader.setUniformMatrix("u_projModelView", combinedMatrix);
			
			if (iTriangle>0) {
				//number of vertices we need to render
				int vertexCount = (iTriangle/6); // Number of triangles divided by number of components
				meshTriangles.setVertices(verts_triangle);
				meshTriangles.render(shader, GL20.GL_TRIANGLES, 0, vertexCount);
				//reset index to zero
				iTriangle = 0;
			}
			if (iTrianglestrip>0) {
				int vertexCount = (iTrianglestrip/6); // Number of triangles divided by number of components
				meshTrianglestrip.setVertices(verts_trianglestrip);
				meshTrianglestrip.render(shader, GL20.GL_TRIANGLE_STRIP, 0, vertexCount);
				iTrianglestrip = 0;
			}
			shader.end();

			//re-enable depth to reset states to their default
			Gdx.gl.glDepthMask(true);
		}
	}
	
	public void dispose() {
		meshTriangles.dispose();
		meshTrianglestrip.dispose();
		shader.dispose();
	}
}
