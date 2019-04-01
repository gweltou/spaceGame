package com.gwel.spacegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class MyRenderer {
	public static final String VERT_SHADER =  
			"attribute vec2 a_position;\n" +
			"attribute vec4 a_color;\n" +			
			"uniform mat4 u_projTrans;\n" + 
			"varying vec4 vColor;\n" +			
			"void main() {\n" +  
			"	vColor = a_color;\n" +
			"	gl_Position =  u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
			"}";
	
	public static final String FRAG_SHADER = 
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
			"varying vec4 vColor;\n" + 			
			"void main() {\n" +  
			"	gl_FragColor = vColor;\n" + 
			"}";
	
	protected static ShaderProgram createMeshShader() {
		ShaderProgram.pedantic = false;
		ShaderProgram shader = new ShaderProgram(VERT_SHADER, FRAG_SHADER);
		String log = shader.getLog();
		if (!shader.isCompiled())
			throw new GdxRuntimeException(log);		
		if (log!=null && log.length()!=0)
			System.out.println("Shader Log: "+log);
		return shader;
	}
	
	//Position attribute - (x, y) 
	public static final int POSITION_COMPONENTS = 2;
	
	//Color attribute - (r, g, b, a)
	public static final int COLOR_COMPONENTS = 4;

	//Total number of components for all attributes
	public static final int NUM_COMPONENTS = POSITION_COMPONENTS + COLOR_COMPONENTS;

	//The maximum number of triangles our mesh will hold
	public static final int MAX_TRIS = 2048;

	//The maximum number of vertices our mesh will hold
	//public static final int MAX_VERTS = MAX_TRIS * 3;

	//The array which holds all the data, interleaved like so:
	//    x, y, r, g, b, a
	//    x, y, r, g, b, a, 
	//    x, y, r, g, b, a, 
	//    ... etc ...
	private float[] verts_triangle = new float[3*MAX_TRIS * NUM_COMPONENTS];
	private float[] verts_trianglestrip = new float[2*MAX_TRIS * NUM_COMPONENTS];

	//The index position
	private int idx_triangle;
	private int idx_trianglestrip;
	
	public MyCamera camera;
	private Mesh meshTriangles;
	private Mesh meshTrianglestrip;
	private ShaderProgram shader;
	private Color col;
	private Matrix4 projMatrix;
	
	
	public MyRenderer(MyCamera camera) {
		this.camera = camera;
		idx_triangle = 0;
		idx_trianglestrip = 0;
		meshTriangles = new Mesh(true, 3*MAX_TRIS, 0, 
				new VertexAttribute(Usage.Position, POSITION_COMPONENTS, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, COLOR_COMPONENTS, "a_color"));
		meshTrianglestrip = new Mesh(true, 2*MAX_TRIS, 0, 
				new VertexAttribute(Usage.Position, POSITION_COMPONENTS, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, COLOR_COMPONENTS, "a_color"));
		shader = createMeshShader();
		col = new Color(1.0f, 1.0f, 1.0f, 1.0f);
		System.out.println("Renderer created");
	}

	public void setColor(float i, float j, float k, float l) {
		this.col.set(i, j, k, l);
	}
	
	public void setProjectionMatrix(Matrix4 proj) {
		projMatrix = proj;
	}
	
	public void setColor(Color color) {
		this.col.set(color);
	}
	
	public void triangle(Vector2 p1, Vector2 p2, Vector2 p3) {
		triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, col);
	}
	
	public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
		triangle(x1, y1, x2, y2, x3, y3, col);
	}
	
	public void triangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
		//we don't want to hit any index out of bounds exception...
		//so we need to flush the batch if we can't store any more verts
		if (idx_triangle==verts_triangle.length)
			flush();

		//now we push the vertex data into our array
		//we are assuming (0, 0) is lower left, and Y is up

		//bottom left vertex
		verts_triangle[idx_triangle++] = x1; 			//Position(x, y) 
		verts_triangle[idx_triangle++] = y1;
		verts_triangle[idx_triangle++] = color.r; 	//Color(r, g, b, a)
		verts_triangle[idx_triangle++] = color.g;
		verts_triangle[idx_triangle++] = color.b;
		verts_triangle[idx_triangle++] = color.a;

		//top left vertex
		verts_triangle[idx_triangle++] = x2; 			//Position(x, y) 
		verts_triangle[idx_triangle++] = y2 ;
		verts_triangle[idx_triangle++] = color.r; 	//Color(r, g, b, a)
		verts_triangle[idx_triangle++] = color.g;
		verts_triangle[idx_triangle++] = color.b;
		verts_triangle[idx_triangle++] = color.a;

		//bottom right vertex
		verts_triangle[idx_triangle++] = x3;	 //Position(x, y) 
		verts_triangle[idx_triangle++] = y3;
		verts_triangle[idx_triangle++] = color.r;		 //Color(r, g, b, a)
		verts_triangle[idx_triangle++] = color.g;
		verts_triangle[idx_triangle++] = color.b;
		verts_triangle[idx_triangle++] = color.a;
	}
	
	public void triangleStrip(double x1, double y1, double x2, double y2) {
		triangleStrip(x1, y1, x2, y2, col);
	}
	
	private void triangleStrip(double x1, double y1, double x2, double y2, Color color) {
		if (idx_trianglestrip==verts_trianglestrip.length)
			flush();

		verts_trianglestrip[idx_trianglestrip++] = (float) x1; 			//Position(x, y) 
		verts_trianglestrip[idx_trianglestrip++] = (float) y1;
		verts_trianglestrip[idx_trianglestrip++] = color.r; 	//Color(r, g, b, a)
		verts_trianglestrip[idx_trianglestrip++] = color.g;
		verts_trianglestrip[idx_trianglestrip++] = color.b;
		verts_trianglestrip[idx_trianglestrip++] = color.a;

		verts_trianglestrip[idx_trianglestrip++] = (float) x2; 			//Position(x, y) 
		verts_trianglestrip[idx_trianglestrip++] = (float) y2 ;
		verts_trianglestrip[idx_trianglestrip++] = color.r; 	//Color(r, g, b, a)
		verts_trianglestrip[idx_trianglestrip++] = color.g;
		verts_trianglestrip[idx_trianglestrip++] = color.b;
		verts_trianglestrip[idx_trianglestrip++] = color.a;
	}
	
	public void flush() {
		//if we've not already flushed
		if (idx_triangle>0 || idx_trianglestrip>0) {
			//no need for depth...
			Gdx.gl.glDepthMask(false);

			//enable blending, for alpha
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			shader.begin();
			shader.setUniformMatrix("u_projTrans", projMatrix);
			if (idx_triangle>0) {
				//number of vertices we need to render
				int vertexCount = (idx_triangle/NUM_COMPONENTS);
				meshTriangles.setVertices(verts_triangle);
				meshTriangles.render(shader, GL20.GL_TRIANGLES, 0, vertexCount);
				//reset index to zero
				idx_triangle = 0;
			}
			if (idx_trianglestrip>0) {
				System.out.println(idx_trianglestrip);
				int vertexCount = (idx_trianglestrip/NUM_COMPONENTS);
				meshTrianglestrip.setVertices(verts_trianglestrip);
				meshTrianglestrip.render(shader, GL20.GL_TRIANGLE_STRIP, 0, vertexCount);
				System.out.println("end");
				idx_trianglestrip = 0;
			}
			shader.end();

			//re-enable depth to reset states to their default
			Gdx.gl.glDepthMask(true);
		}
	}
	
	public void dispose() {
		meshTriangles.dispose();
		meshTrianglestrip.dispose();
	}

	/** Calls circle(Vector2, float, int)} by estimating the number of segments needed for a smooth circle. */
	public void circle (Vector2 pos, float radius) {
		circle(pos, radius, Math.max(1,  ((int) Math.sqrt(radius*camera.PPU))<<2 ));
	}

	/** Draws a circle using {@link ShapeType#Line} or {@link ShapeType#Filled}. */
	public void circle (Vector2 pos, final float radius, final int segments) {
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
					double newX = cx*cos - cy*sin;
					double newY = cx*sin + cy*cos;
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
					double newX = cx*cos - cy*sin;
					double newY = cx*sin + cy*cos;
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
					double newX = cx*cos - cy*sin;
					double newY = cx*sin + cy*cos;
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
					double newX = x*cos - y*sin;
					double newY = x*sin + y*cos;
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
					double newX = cx*cos - cy*sin;
					double newY = cx*sin + cy*cos;
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
					double newX = cx*cos - cy*sin;
					double newY = cx*sin + cy*cos;
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
				double newX = cx*cos - cy*sin;
				double newY = cx*sin + cy*cos;
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
				double newX = cx*cos - cy*sin;
				double newY = cx*sin + cy*cos;
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
			double newX = 0;
			double newY = 0;
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

	public void line(float x, float y, float x2, float y2) {
		// TODO Auto-generated method stub

	}

	public void line(Vector2 position, Vector2 pposition) {
		// TODO Auto-generated method stub
		
	}
}
