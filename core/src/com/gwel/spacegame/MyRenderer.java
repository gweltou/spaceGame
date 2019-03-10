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
	public static final int MAX_VERTS = MAX_TRIS * 3;

	//The array which holds all the data, interleaved like so:
	//    x, y, r, g, b, a
	//    x, y, r, g, b, a, 
	//    x, y, r, g, b, a, 
	//    ... etc ...
	private float[] verts = new float[MAX_VERTS * NUM_COMPONENTS];

	//The index position
	private int idx;
	
	private MyCamera camera;
	//private ShapeRenderer shapeRenderer;
	private Mesh mesh;
	private ShaderProgram shader;
	private Color col;
	
	
	public MyRenderer(MyCamera camera) {
		this.camera = camera;
		//shapeRenderer = new ShapeRenderer();
		idx = 0;
		mesh = new Mesh(true, MAX_VERTS, 0, 
				new VertexAttribute(Usage.Position, POSITION_COMPONENTS, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, COLOR_COMPONENTS, "a_color"));
		shader = createMeshShader();
		col = new Color(1.0f, 1.0f, 1.0f, 1.0f);
		System.out.println("Renderer created");
	}

	public void setColor(float i, float j, float k, float l) {
		this.col.set(i, j, k, l);
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
		//System.out.println("Rendering triangle");
		//we don't want to hit any index out of bounds exception...
		//so we need to flush the batch if we can't store any more verts
		if (idx==verts.length)
			flush();

		//now we push the vertex data into our array
		//we are assuming (0, 0) is lower left, and Y is up

		//bottom left vertex
		verts[idx++] = x1; 			//Position(x, y) 
		verts[idx++] = y1;
		verts[idx++] = color.r; 	//Color(r, g, b, a)
		verts[idx++] = color.g;
		verts[idx++] = color.b;
		verts[idx++] = color.a;

		//top left vertex
		verts[idx++] = x2; 			//Position(x, y) 
		verts[idx++] = y2 ;
		verts[idx++] = color.r; 	//Color(r, g, b, a)
		verts[idx++] = color.g;
		verts[idx++] = color.b;
		verts[idx++] = color.a;

		//bottom right vertex
		verts[idx++] = x3;	 //Position(x, y) 
		verts[idx++] = y3;
		verts[idx++] = color.r;		 //Color(r, g, b, a)
		verts[idx++] = color.g;
		verts[idx++] = color.b;
		verts[idx++] = color.a;
	}
	
	void flush() {
		//System.out.println("Renderer flushing");
		//if we've already flushed
		if (idx==0)
			return;

		//sends our vertex data to the mesh
		mesh.setVertices(verts);

		//no need for depth...
		Gdx.gl.glDepthMask(false);

		//enable blending, for alpha
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		//number of vertices we need to render
		int vertexCount = (idx/NUM_COMPONENTS);

		//start the shader before setting any uniforms
		shader.begin();

		//update the projection matrix so our triangles are rendered in 2D
		shader.setUniformMatrix("u_projTrans", new Matrix4().set(camera.affine));
		//shader.setUniformMatrix("u_projTrans", new Matrix4().idt()); // XXX
		
		//render the mesh
		mesh.render(shader, GL20.GL_TRIANGLES, 0, vertexCount);

		shader.end();

		//re-enable depth to reset states to their default
		Gdx.gl.glDepthMask(true);

		//reset index to zero
		idx = 0;
	}
	
	public void dispose() {
		mesh.dispose();
	}

	/** Calls circle(Vector2, float, int)} by estimating the number of segments needed for a smooth circle. */
	public void circle (Vector2 pos, float radius) {
		circle(pos, radius, Math.max(1, (int)(6 * (float)Math.cbrt(radius*camera.PPU))));
	}

	/** Draws a circle using {@link ShapeType#Line} or {@link ShapeType#Filled}. */
	public void circle (Vector2 pos, float radius, int segments) {
		//System.out.println("Rendering circle");
		if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.");
		//float colorBits = color.toFloatBits();
		float angle = 2 * MathUtils.PI / segments;
		float cos = MathUtils.cos(angle);
		float sin = MathUtils.sin(angle);
		float cx = radius, cy = 0;
		Vector2 p1 = new Vector2();
		Vector2 p2 = new Vector2();
		segments--;
		for (int i = 0; i < segments; i++) {
			p1.set(pos.x + cx, pos.y + cy);
			float temp = cx;
			cx = cos * cx - sin * cy;
			cy = sin * temp + cos * cy;
			p2.set(pos.x + cx, pos.y + cy);
			triangle(pos, p1, p2);
		}
		// Ensure the last segment is identical to the first.
		
		p1.set(pos.x + cx, pos.y + cy);
		p2.set(pos.x + radius, pos.y);
		triangle(pos, p1, p2);
	}

	public void line(float x, float y, float x2, float y2) {
		// TODO Auto-generated method stub

	}

	public void line(Vector2 position, Vector2 pposition) {
		// TODO Auto-generated method stub
		
	}
}
