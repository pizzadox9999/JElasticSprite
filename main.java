import org.jsfml.graphics.RenderWindow;
import org.jsfml.window.VideoMode;
import org.jsfml.window.event.Event;
import org.jsfml.graphics.Texture;
import java.nio.file.Paths;
import org.jsfml.graphics.Font;
import org.jsfml.graphics.RectangleShape;
import org.jsfml.graphics.Color;
import org.jsfml.window.event.KeyEvent;
import org.jsfml.system.Vector2f;
public class main{
   
  public static void main(String[] args){
    RenderWindow window=new RenderWindow();
    window.create(new VideoMode(800, 600), "Elastic Sprite simple example - texture");
    Texture texture=new Texture();
    try {
      texture.loadFromFile(Paths.get("resources/uv map.jpg"));
      texture.setSmooth(true);
    } catch(Exception e) {
      e.printStackTrace();
    }         
    ElasticSprite sprite=new ElasticSprite(texture);
    sprite.setUseShader(false);
    sprite.setVertexOffset(3, new Vector2f(100, 0) );
    sprite.setVertexOffset(0, new Vector2f(100, 0) );
    while (window.isOpen()){
      for(Event event : window.pollEvents()){
        switch (event.type) {
          case  CLOSED: 
            window.close();
            break;  
        } 
      }
      window.clear();
      window.draw(sprite);
      window.display();
    }
  }
}
    
    
    /*
    import org.jsfml.graphics.RenderWindow;
    import org.jsfml.window.VideoMode;
    import org.jsfml.window.event.Event;
    import org.jsfml.graphics.Texture;
    import java.nio.file.Paths;
    import org.jsfml.graphics.Font;
    import org.jsfml.graphics.RectangleShape;
    import org.jsfml.graphics.Color;
    import org.jsfml.window.event.KeyEvent;
    public class main{
    public main(){
    //prepare sprite
    Texture back=new Texture();
    Texture front=new Texture();
    try {
    front.loadFromFile(Paths.get("resources/Card Back - SFML.png"));
    front.setSmooth(true);
    } catch(Exception e) {
    e.printStackTrace();
    } 
    ElasticSprite sprite=new ElasticSprite(front);
    sprite.resetVertexOffsets();
    //    sprite.move(sprite.getGlobalBounds().width/2, sprite.getGlobalBounds().height/2);
    
    //    sprite.setOrigin(sprite.getGlobalBounds().width/2, sprite.getGlobalBounds().height/2); 
    
    
    RenderWindow window=new RenderWindow();
    window.create(new VideoMode(800, 600), "test");
    window.setFramerateLimit(60);
    
    while (window.isOpen()) { 
    window.clear();
    window.draw(sprite);
    window.display();
    
    
    for (Event event : window.pollEvents()) {
    switch (event.type) {
    case  CLOSED: 
    window.close();
    break;  
    } 
    } 
    } 
    }
    public static void main(String[] args) {
    new main();
    }
    }*/
