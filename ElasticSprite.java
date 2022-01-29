import org.jsfml.graphics.Drawable;
import org.jsfml.graphics.Transformable;
import java.util.ArrayList;
import org.jsfml.graphics.Vertex;
import org.jsfml.system.Vector2f;
import org.jsfml.graphics.Texture;
import org.jsfml.graphics.FloatRect;
import org.jsfml.graphics.PrimitiveType;
import org.jsfml.graphics.Shader;
import org.jsfml.graphics.RenderTarget;
import org.jsfml.graphics.RenderStates;
import org.jsfml.graphics.Color;
import org.jsfml.graphics.Transform;
import java.util.Arrays;
import org.jsfml.graphics.Sprite;
public class ElasticSprite extends Sprite implements Drawable, Transformable{
  private boolean m_requiresVerticesUpdate;
  private Vertex[] m_vertices;
  private float[] m_weights;
  private FloatRect m_actualTextureRect;
  private Vector2f[] m_offsets;
  private Texture m_pTexture;
  private FloatRect m_baseTextureRect;
  private boolean m_useShader;
  private boolean m_usePerspectiveInterpolation;
  private boolean m_textureFlipX;
  private boolean m_textureFlipY;
  private PrimitiveType m_primitiveType=PrimitiveType.QUADS;
  private boolean m_areShadersLoaded=false;
  private Shader m_bilinearShader=new Shader();
  private Shader m_perspectiveShader=new Shader();
  private String m_bilinearFragmentShaderCode=new String(
  "#version 110\n\nuniform bool useTexture;\nuniform sampler2D texture;"+
  "\nuniform int renderTargetHeight;\nuniform vec2 v0;\nuniform vec2 v1;"+
  "\nuniform vec2 v2;\nuniform vec2 v3;\nuniform float textureRectLeftRat"+
  "io;\nuniform float textureRectTopRatio;\nuniform float textureRectWid"+
  "thRatio;\nuniform float textureRectHeightRatio;\nuniform vec4 c0;\nun"+
  "iform vec4 c1;\nuniform vec4 c2;\nuniform vec4 c3;\n\nvec2 linesInter"+
  "section(vec2 aStart, vec2 aEnd, vec2 bStart, vec2 bEnd)\n{\nvec2 a ="+
  " aEnd - aStart;\nvec2 b = bEnd - bStart;\nfloat aAngle = atan(a.y, "+
  "a.x);\nfloat bAngle = atan(b.y, b.x);\nif (abs(aAngle - bAngle) < 0"+
  ".01)\n{\na = mix(aEnd, bEnd, 0.0001) - aStart;\nb = mix(bEnd, aE"+
  "nd, 0.0001) - bStart;\n}\nvec2 c = aStart - bStart;\nfloat alpha ="+
  " ((b.x * c.y) - (b.y * c.x)) / ((b.y * a.x) - (b.x * a.y));\nreturn "+
  "aStart + (a * alpha);\n}\n\nvoid main()\n{\nvec2 p = vec2(gl_FragCoo"+
  "rd.x, (float(renderTargetHeight) - gl_FragCoord.y));\nvec2 o = lines"+
  "Intersection(v0, v3, v1, v2);\nvec2 n = linesIntersection(v1, v0, v2"+
  ", v3);\nvec2 l = linesIntersection(o, p, v0, v1);\nvec2 m = linesIn"+
  "tersection(o, p, v3, v2);\nvec2 j = linesIntersection(n, p, v0, v3);"+
  "\nvec2 k = linesIntersection(n, p, v2, v1);\nvec2 ratioCoord = vec2"+
  "(distance(p, l) / distance(m, l), distance(p, j) / distance(k, j));\n"+
  "vec4 color = mix(mix(c0, c3, ratioCoord.x), mix(c1, c2, ratioCoord.x"+
  "), ratioCoord.y);\nif (useTexture)\n{\nvec2 texCoord = vec2(ratio"+
  "Coord.x * textureRectWidthRatio + textureRectLeftRatio, ratioCoord.y "+
  "* textureRectHeightRatio + textureRectTopRatio);\nvec4 pixel = text"+
  "ure2D(texture, texCoord);\ngl_FragColor = color * pixel;\n}\nelse"+
  "\ngl_FragColor = color;\n}\n"
  ); 
  private String m_perspectiveVertexShaderCode=new String(
  "#version 110\n\nuniform vec4 c0;\nuniform vec4 c1;\nuniform vec4 c2;"+
  "\nuniform vec4 c3;\nuniform float w0;\nuniform float w1;\nuniform floa"+
  "t w2;\nuniform float w3;\n\nvoid main()\n{\nint vertexNumber = 0;\n"+
  "if (gl_Color.r > 0.5)\nvertexNumber = 1;\nelse if (gl_Color.g > 0."+
  "5)\nvertexNumber = 2;\nelse if (gl_Color.b > 0.5)\nvertexNumber "+
  "= 3;\n\nvec4 color;\nfloat weight;\nif (vertexNumber == 0)\n{\n"+
  "color = c0;\nweight = w0;\n}\nelse if (vertexNumber == 1)\n{\n"+
  "color = c1;\nweight = w1;\n}\nelse if (vertexNumber == 2)\n{\n"+
  "color = c2;\nweight = w2;\n}\nelse if (vertexNumber == 3)\n{\n"+
  "color = c3;\nweight = w3;\n}\ngl_Position = gl_ModelViewProjecti"+
  "onMatrix * gl_Vertex;\ngl_TexCoord[0] = gl_TextureMatrix[0] * gl_Mul"+
  "tiTexCoord0;\ngl_TexCoord[0].z = weight;\ngl_FrontColor = color;\n}"+
  "\n");
  private String m_perspectiveFragmentShaderCode=new String(
  "#version 110\n\nuniform bool useTexture;\nuniform sampler2D texture;"+
  "\n\nvoid main()\n{\nvec4 color = gl_Color;\nif (useTexture)\n{\nv"+
  "ec2 texCoord = gl_TexCoord[0].xy / gl_TexCoord[0].z;\ngl_FragColor"+
  "= color * texture2D(texture, texCoord);\n}\nelse\ngl_FragColor = "+
  "color;\n}\n");
  private void loadShader(){
    if(Shader.isAvailable()){
      if(!m_areShadersLoaded){
        try {
          m_bilinearShader.loadFromSource(m_bilinearFragmentShaderCode, Shader.Type.FRAGMENT);
          m_perspectiveShader.loadFromSource(m_perspectiveVertexShaderCode, m_perspectiveFragmentShaderCode);
          m_areShadersLoaded = true;
        } catch(Exception e) {
          e.printStackTrace();
        } 
      }
    }
  }
  private boolean isValidVertexIndex(int vertexIndex){
    return (vertexIndex<4);
  }
  private Vector2f linesIntersection(float aStartX, float aStarty, float aEndX, float aEndY, float bStartX, float bStartY, float bEndX, float bEndY){
    return linesIntersection(new Vector2f(aStartX, aStarty), new Vector2f(aEndX, aEndY), new Vector2f(bStartX, bStartY), new Vector2f(bEndX, bEndY));
  }
  private Vector2f linesIntersection(Vector2f aStart, Vector2f aEnd, Vector2f bStart, Vector2f bEnd){
    Vector2f a=vec_sub(aEnd, aStart);
    Vector2f b=vec_sub(aEnd, bStart);
    Vector2f c=vec_sub(aStart, bStart);
    float alpha=((b.x * c.y) - (b.y * c.x)) / ((b.y * a.x) - (b.x * a.y));
    return vec_add(aStart, vec_mul(a, alpha));
  }
  private float distanceBetweenPoints(Vector2f a, Vector2f b){
    Vector2f c=vec_sub(a, b);
    return (float)Math.sqrt(c.x * c.x + c.y * c.y);
  }
  private Color encodeFloatAsColor(float f){
    return new Color( (int)(f / 256) & 0xFF, (int)(f) & 0xFF, (int)(f * 256) & 0xFF, 0);
  }
  
  
  public ElasticSprite(){
    m_requiresVerticesUpdate=false;
    m_vertices=new Vertex[4];
    Arrays.fill(m_vertices, new Vertex(new Vector2f(1, 1)));
    m_weights=new float[0];
    m_offsets=new Vector2f[4];
    m_pTexture=null; 
    m_baseTextureRect=null; //new FloatRect();
    m_actualTextureRect=null; //new FloatRect();
    m_useShader=Shader.isAvailable();
    m_usePerspectiveInterpolation=false;
    m_textureFlipX=false;
    m_textureFlipY=false;    
    if (m_useShader)
      loadShader();
  }
  public ElasticSprite(Texture texture){
    this();
    setTexture(texture, true);
  }
  ElasticSprite(Texture texture, FloatRect textureRect){
    this();
    setTexture(texture);
    setTextureRect(textureRect);
  }
  public void setTexture(Texture texture){
    setTexture(texture, false);
  }
  public void setTexture(Texture texture, boolean resetTextureRect){
    if (resetTextureRect){
      resetVertexOffsets();
      setTextureRect(new FloatRect(Vector2f.ZERO, new Vector2f(texture.getSize())));
    }
    m_pTexture = texture;
    m_requiresVerticesUpdate = true;
  }
  public void setTexture(){
    m_pTexture=null;
  }                   
  public void setTextureRect(FloatRect textureRect){
    m_baseTextureRect = textureRect;
    m_requiresVerticesUpdate = true;
  }    
  public Texture getTexture(){
    return m_pTexture;
  }

  /* testwise out because i cant override it
  public FloatRect getTextureRect(){
    return m_baseTextureRect;
  }  */

  public void setTextureFlipX(boolean textureFlipX){
    m_textureFlipX = textureFlipX;
    m_requiresVerticesUpdate = true;
  }

  public boolean getTextureFlipX(){
    return m_textureFlipX;
  }

  public void setTextureFlipY(boolean textureFlipY){
    m_textureFlipY = textureFlipY;
    m_requiresVerticesUpdate = true;
  }

  public boolean getTextureFlipY(){
    return m_textureFlipY;
  }

  public boolean setUseShader(boolean useShader){
    m_requiresVerticesUpdate = true;
    return m_useShader = (useShader && Shader.isAvailable());
  }

  public boolean getUseShader(){
    return m_useShader;
  }

  public void activateBilinearInterpolation(){
    m_usePerspectiveInterpolation = false;
    Arrays.fill(m_weights, 0); //m_weights.clear();
    m_requiresVerticesUpdate = true;
  }

  public boolean isActiveBilinearInterpolation(){
    return !m_usePerspectiveInterpolation;
  }

  public void activatePerspectiveInterpolation(){
    m_usePerspectiveInterpolation = true;
    m_weights=Arrays.copyOf(m_weights, 4); //m_weights.resize(4); ???
    m_requiresVerticesUpdate = true;
  }
  
  public boolean isActivePerspectiveInterpolation(){
    return m_usePerspectiveInterpolation;
  }

  public void setVertexOffset(int vertexIndex, Vector2f offset){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    m_offsets[vertexIndex] = offset;
    m_requiresVerticesUpdate = true;
  }

  public Vector2f getVertexOffset(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    return m_offsets[vertexIndex];
  }

  public void setColor(Color color){
    for (int i=0; i<m_vertices.length; i++) {
      m_vertices[i]=new Vertex(m_vertices[i].position, color, m_vertices[i].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
    }
    m_requiresVerticesUpdate = true;
  }

  public void setVertexColor(int vertexIndex, Color color){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    m_vertices[vertexIndex]=new Vertex(m_vertices[vertexIndex].position, color, m_vertices[vertexIndex].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
    //m_vertices[vertexIndex].color = color;
    m_requiresVerticesUpdate = true;
  }

  public Color getColor(){
    //int totalR{ static_cast<unsigned int>(m_vertices[0].color.r) + m_vertices[1].color.r + m_vertices[2].color.r + m_vertices[3].color.r };
    int totalR=m_vertices[0].color.r + m_vertices[1].color.r + m_vertices[2].color.r + m_vertices[3].color.r ;
    //int totalG{ static_cast<unsigned int>(m_vertices[0].color.g) + m_vertices[1].color.g + m_vertices[2].color.g + m_vertices[3].color.g };
    int totalG=m_vertices[0].color.g + m_vertices[1].color.g + m_vertices[2].color.g + m_vertices[3].color.g;
    //int totalB{ static_cast<unsigned int>(m_vertices[0].color.b) + m_vertices[1].color.b + m_vertices[2].color.b + m_vertices[3].color.b };
    int totalB=m_vertices[0].color.b + m_vertices[1].color.b + m_vertices[2].color.b + m_vertices[3].color.b;
    //int totalA{ static_cast<unsigned int>(m_vertices[0].color.a) + m_vertices[1].color.a + m_vertices[2].color.a + m_vertices[3].color.a };
    int totalA=m_vertices[0].color.a + m_vertices[1].color.a + m_vertices[2].color.a + m_vertices[3].color.a;
    return new Color( (int)(totalR / 4), (int)(totalG / 4), (int)(totalB / 4), (int)(totalA / 4) );
  }

  public Color getVertexColor(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    return m_vertices[vertexIndex].color;
  }

  public void resetVertexOffsets(){
    setVertexOffset(0, Vector2f.ZERO);
    setVertexOffset(1, Vector2f.ZERO);
    setVertexOffset(2, Vector2f.ZERO);
    setVertexOffset(3, Vector2f.ZERO);
  }
          
  public Vector2f getVertexLocalPosition(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    return getTransform().transformPoint(vec_add(priv_getVertexBasePosition(vertexIndex), m_offsets[vertexIndex]));
  }
          
  Vector2f getVertexBaseLocalPosition(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    return getTransform().transformPoint(priv_getVertexBasePosition(vertexIndex));
  }
          
  public Vector2f getVertexGlobalPosition(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    return getTransform().transformPoint(vec_add(priv_getVertexBasePosition(vertexIndex), m_offsets[vertexIndex]));
  }
          
  Vector2f getVertexBaseGlobalPosition(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    return getTransform().transformPoint(priv_getVertexBasePosition(vertexIndex));
  }
          
  public FloatRect getLocalBounds() {
    Vector2f topLeft = new Vector2f(m_offsets[0].x, m_offsets[0].y);
    Vector2f bottomRight = new Vector2f(topLeft.x, topLeft.y);
    
    for (int i=1; i < 4; ++i){
      Vector2f vertex=vec_add(priv_getVertexBasePosition(i), m_offsets[i]); //add two vecs
      if (vertex.x < topLeft.x){
        topLeft=new Vector2f(vertex.x, topLeft.y);//topLeft.x = vertex.x;
      }else if (vertex.x > bottomRight.x){
          bottomRight=new Vector2f(vertex.x, bottomRight.y);//         bottomRight.x = vertex.x;
        }
      if (vertex.y < topLeft.y){
        topLeft=new Vector2f(topLeft.x, vertex.y);//       topLeft.y = vertex.y;
      }else if (vertex.y > bottomRight.y){
          bottomRight=new Vector2f(bottomRight.x, vertex.y);//          bottomRight.y = vertex.y;
        }
    }
    return new FloatRect(topLeft, vec_sub(bottomRight, topLeft));
  }
    
  public FloatRect getBaseLocalBounds(){
    return new FloatRect(Vector2f.ZERO, priv_getVertexBasePosition(2));
  }
    
  public FloatRect getGlobalBounds(){
    if (m_requiresVerticesUpdate)
      priv_updateVertices(Transform.IDENTITY);
    
    Vector2f topLeft=new Vector2f(m_vertices[0].position.x, m_vertices[0].position.y);
    Vector2f bottomRight=new Vector2f(topLeft.x, topLeft.y);
    
    for (int i=1; i < 4; ++i){
      Vector2f transformedVertex=new Vector2f(m_vertices[i].position.x, m_vertices[i].position.y);
      if (transformedVertex.x < topLeft.x){
        topLeft=new Vector2f(transformedVertex.x, topLeft.y);//topLeft.x = transformedVertex.x;
      }else if (transformedVertex.x > bottomRight.x){
          bottomRight=new Vector2f(transformedVertex.x, bottomRight.y);//bottomRight.x = transformedVertex.x;
        }
      if (transformedVertex.y < topLeft.y){
        topLeft=new Vector2f(topLeft.x, transformedVertex.y);//     topLeft.y = transformedVertex.y;
      } else if (transformedVertex.y > bottomRight.y){
          bottomRight=new Vector2f(bottomRight.x, transformedVertex.y);      //bottomRight.y = transformedVertex.y;
          }
    }
    return new FloatRect(topLeft, vec_sub(bottomRight, topLeft));
  }
    
  public FloatRect getBaseGlobalBounds(){
    return getTransform().transformRect(getBaseLocalBounds());
  }
    
    
    
    // PRIVATE
    
  public void draw(RenderTarget target, RenderStates states){
    if (m_requiresVerticesUpdate)
      priv_updateVertices(states.transform);
    //states.transform = Transform.IDENTITY;
    states=new RenderStates(states.blendMode, Transform.IDENTITY, states.texture, states.shader); //RenderStates(BlendMode blendMode, Transform transform, ConstTexture texture, ConstShader shader) 
    
    if (!m_useShader){
      states=new RenderStates(states.blendMode, states.transform, m_pTexture, states.shader); //RenderStates(BlendMode blendMode, Transform transform, ConstTexture texture, ConstShader shader) 
      target.draw(m_vertices, m_primitiveType, states);
    } else {
      boolean isTextureAvailable=m_pTexture != null;
      if (m_usePerspectiveInterpolation){
        Color[] colors=new Color[4]; //std::vector<sf::Color> colors(4);
        for (int i=0; i < 4; ++i)
          colors[i] = m_vertices[i].color;
        m_perspectiveShader.setParameter("useTexture", isTextureAvailable?1:0);
        if (isTextureAvailable)
          m_perspectiveShader.setParameter("texture", m_pTexture);
        
        m_perspectiveShader.setParameter("c0", m_vertices[0].color);
        m_perspectiveShader.setParameter("c1", m_vertices[1].color);
        m_perspectiveShader.setParameter("c2", m_vertices[2].color);
        m_perspectiveShader.setParameter("c3", m_vertices[3].color);
        m_perspectiveShader.setParameter("w0", m_weights[0]);
        m_perspectiveShader.setParameter("w1", m_weights[1]);
        m_perspectiveShader.setParameter("w2", m_weights[2]);
        m_perspectiveShader.setParameter("w3", m_weights[3]);
        //m_vertices[0].color = Color.BLACK;
        m_vertices[0]=new Vertex(m_vertices[0].position, Color.BLACK, m_vertices[0].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
        //m_vertices[1].color = Color.RED;
        m_vertices[1]=new Vertex(m_vertices[1].position, Color.RED, m_vertices[1].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
        //m_vertices[2].color = Color.GREEN;
        m_vertices[0]=new Vertex(m_vertices[2].position, Color.GREEN, m_vertices[2].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
        //m_vertices[3].color = Color.BLUE;
        m_vertices[0]=new Vertex(m_vertices[3].position, Color.BLUE, m_vertices[3].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
        states=new RenderStates(states.blendMode, states.transform, states.texture, m_perspectiveShader); //RenderStates(BlendMode blendMode, Transform transform, ConstTexture texture, ConstShader shader) 
        target.draw(m_vertices, m_primitiveType, states);
        for (int i=0; i < 4; ++i){
          m_vertices[i]=new Vertex(m_vertices[i].position, colors[i], m_vertices[i].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
          //m_vertices[i].color = colors[i];
        }
      } else {
        m_bilinearShader.setParameter("useTexture", isTextureAvailable?1:0);
        if (isTextureAvailable){
          m_bilinearShader.setParameter("texture", m_pTexture);
          Vector2f textureSize=new Vector2f(m_pTexture.getSize());
          m_bilinearShader.setParameter("textureRectLeftRatio", m_actualTextureRect.left / textureSize.x);
          m_bilinearShader.setParameter("textureRectTopRatio", m_actualTextureRect.top / textureSize.y);
          m_bilinearShader.setParameter("textureRectWidthRatio", m_actualTextureRect.width / textureSize.x);
          m_bilinearShader.setParameter("textureRectHeightRatio", m_actualTextureRect.height / textureSize.y);
        }
        m_bilinearShader.setParameter("renderTargetHeight", (int)target.getSize().y);
        m_bilinearShader.setParameter("v0", new Vector2f(target.mapCoordsToPixel(m_vertices[0].position)));
        m_bilinearShader.setParameter("v1", new Vector2f(target.mapCoordsToPixel(m_vertices[1].position)));
        m_bilinearShader.setParameter("v2", new Vector2f(target.mapCoordsToPixel(m_vertices[2].position)));
        m_bilinearShader.setParameter("v3", new Vector2f(target.mapCoordsToPixel(m_vertices[3].position)));
        m_bilinearShader.setParameter("c0", m_vertices[0].color);
        m_bilinearShader.setParameter("c1", m_vertices[1].color);
        m_bilinearShader.setParameter("c2", m_vertices[2].color);
        m_bilinearShader.setParameter("c3", m_vertices[3].color);
        //states.shader = bilinearShader;
        new RenderStates(states.blendMode, states.transform, states.texture, m_bilinearShader); //RenderStates(BlendMode blendMode, Transform transform, ConstTexture texture, ConstShader shader) 
        target.draw(m_vertices, m_primitiveType, states);
      }
    }
  }
    
  public void priv_updateVertices(Transform transform){
    m_requiresVerticesUpdate = false;
    
    transform = getTransform();
    
    for (int i=0; i < 4; ++i){
      m_vertices[i]=new Vertex(transform.transformPoint(vec_add(m_offsets[i], priv_getVertexBasePosition(i))), m_vertices[i].color, m_vertices[i].texCoords); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
      //m_vertices[i].position = transform.transformPoint(m_offsets[i] + priv_getVertexBasePosition(i));
    }
    
    m_actualTextureRect = m_baseTextureRect;
    
    if (m_textureFlipX){
      //m_actualTextureRect.left += m_actualTextureRect.width;
      m_actualTextureRect=new FloatRect(m_actualTextureRect.left+m_actualTextureRect.width, m_actualTextureRect.top, m_actualTextureRect.width, m_actualTextureRect.height); //FloatRect(float left, float top, float width, float height) 
      //m_actualTextureRect.width = -m_actualTextureRect.width;                            //dont know if i should sub from it or just *-1 it ???
      m_actualTextureRect=new FloatRect(m_actualTextureRect.left, m_actualTextureRect.top, -m_actualTextureRect.width, m_actualTextureRect.height); //FloatRect(float left, float top, float width, float height) 
    }
    if (m_textureFlipY){
      //m_actualTextureRect.top += m_actualTextureRect.height;
      m_actualTextureRect=new FloatRect(m_actualTextureRect.left, m_actualTextureRect.top+m_actualTextureRect.height, m_actualTextureRect.width, m_actualTextureRect.height); //FloatRect(float left, float top, float width, float height) 
      //m_actualTextureRect.height = -m_actualTextureRect.height;
      m_actualTextureRect=new FloatRect(m_actualTextureRect.left, m_actualTextureRect.top, m_actualTextureRect.width, -m_actualTextureRect.height); //FloatRect(float left, float top, float width, float height)       
    }
    
    if (m_useShader && m_usePerspectiveInterpolation && m_pTexture != null){
      Vector2f intersection=linesIntersection(m_vertices[0].position.x, m_vertices[0].position.y, m_vertices[2].position.x, m_vertices[2].position.y, m_vertices[1].position.x, m_vertices[1].position.y, m_vertices[3].position.x, m_vertices[3].position.y);
      float distanceToIntersection0= distanceBetweenPoints(m_vertices[0].position, intersection) ;
      float distanceToIntersection1= distanceBetweenPoints(m_vertices[1].position, intersection) ;
      float distanceToIntersection2= distanceBetweenPoints(m_vertices[2].position, intersection) ;
      float distanceToIntersection3= distanceBetweenPoints(m_vertices[3].position, intersection) ;
      m_weights[0] = (distanceToIntersection0 + distanceToIntersection2) / distanceToIntersection2;
      m_weights[1] = (distanceToIntersection1 + distanceToIntersection3) / distanceToIntersection3;
      m_weights[2] = (distanceToIntersection2 + distanceToIntersection0) / distanceToIntersection0;
      m_weights[3] = (distanceToIntersection3 + distanceToIntersection1) / distanceToIntersection1;
      
      Vector2f textureSize=new Vector2f(m_pTexture.getSize());
      
      //m_vertices[0].texCoords =  new Vector2f( m_weights[0] * (m_actualTextureRect.left / textureSize.x), m_weights[0] * (m_actualTextureRect.top / textureSize.y) );
      m_vertices[0]=new Vertex(m_vertices[0].position, m_vertices[0].color, new Vector2f( m_weights[0] * (m_actualTextureRect.left / textureSize.x), m_weights[0] * (m_actualTextureRect.top / textureSize.y) )); //Vertex(Vector2f position, Color color, Vector2f texCoords) 
      //m_vertices[1].texCoords =  new Vector2f( m_weights[1] * (m_actualTextureRect.left / textureSize.x), m_weights[1] * ((m_actualTextureRect.top + m_actualTextureRect.height) / textureSize.y) );
      m_vertices[1]=new Vertex(m_vertices[1].position, m_vertices[1].color, new Vector2f( m_weights[1] * (m_actualTextureRect.left / textureSize.x), m_weights[1] * ((m_actualTextureRect.top + m_actualTextureRect.height) / textureSize.y) ));
      //m_vertices[2].texCoords =  new Vector2f( m_weights[2] * ((m_actualTextureRect.left + m_actualTextureRect.width) / textureSize.x), m_weights[2] * ((m_actualTextureRect.top + m_actualTextureRect.height) / textureSize.y) );
      m_vertices[2]=new Vertex(m_vertices[2].position, m_vertices[2].color, new Vector2f( m_weights[2] * ((m_actualTextureRect.left + m_actualTextureRect.width) / textureSize.x), m_weights[2] * ((m_actualTextureRect.top + m_actualTextureRect.height) / textureSize.y) ));
      //m_vertices[3].texCoords =  new Vector2f( m_weights[3] * ((m_actualTextureRect.left + m_actualTextureRect.width) / textureSize.x), m_weights[3] * (m_actualTextureRect.top / textureSize.y) );
      m_vertices[3]=new Vertex(m_vertices[3].position, m_vertices[3].color, new Vector2f( m_weights[3] * ((m_actualTextureRect.left + m_actualTextureRect.width) / textureSize.x), m_weights[3] * (m_actualTextureRect.top / textureSize.y) ));
    } else {
      //m_vertices[0].texCoords = new Vector2f( m_actualTextureRect.left, m_actualTextureRect.top );
      m_vertices[0]=new Vertex(m_vertices[0].position, m_vertices[0].color, new Vector2f( m_actualTextureRect.left, m_actualTextureRect.top ));
      //m_vertices[2].texCoords = new Vector2f( m_actualTextureRect.left + m_actualTextureRect.width, m_actualTextureRect.top + m_actualTextureRect.height );
      m_vertices[2]=new Vertex(m_vertices[2].position, m_vertices[2].color, new Vector2f( m_actualTextureRect.left + m_actualTextureRect.width, m_actualTextureRect.top + m_actualTextureRect.height ));
      //m_vertices[1].texCoords = new Vector2f( m_vertices[0].texCoords.x, m_vertices[2].texCoords.y );
      m_vertices[1]=new Vertex(m_vertices[1].position, m_vertices[1].color, new Vector2f( m_vertices[0].texCoords.x, m_vertices[2].texCoords.y ));
      //m_vertices[3].texCoords = new Vector2f( m_vertices[2].texCoords.x, m_vertices[0].texCoords.y );
      m_vertices[3]=new Vertex(m_vertices[3].position, m_vertices[3].color, new Vector2f( m_vertices[2].texCoords.x, m_vertices[0].texCoords.y ));
    }
  }
    
  public Vector2f priv_getVertexBasePosition(int vertexIndex){
    // must be valid vertex index
    assert(isValidVertexIndex(vertexIndex));
    
    switch (vertexIndex){
      case 1:
        return new Vector2f(0.f, m_baseTextureRect.height);
      case 2:
        return new Vector2f(m_baseTextureRect.width, m_baseTextureRect.height);
      case 3:
        return new Vector2f(m_baseTextureRect.width, 0.f);
      case 0:
      default:
        return Vector2f.ZERO;
    }
  }
    
    
  private Vector2f vec_sub(Vector2f a, Vector2f b){
    return new Vector2f(a.x-b.x, a.y-b.y);
  }
  private Vector2f vec_add(Vector2f a, Vector2f b){
    return new Vector2f(a.x+b.x, a.y+b.y);
  }
  private Vector2f vec_mul(Vector2f a, float f){
    return new Vector2f(a.x*f, a.y*f);
  } 
}
