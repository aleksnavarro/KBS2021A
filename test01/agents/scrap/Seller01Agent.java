import jade.core.Agent;
import jade.core.behaviours.Behaviour;

import java.sql.*;

public class Seller01Agent extends Agent {
  
  protected void setup() {
    addBehaviour(new TellBehaviour());
    addBehaviour(new AskBehaviour());
  } 

  private class TellBehaviour extends Behaviour {
    boolean tellDone = false;
    
    public void loaddb( String args[] ) {
      Connection c = null;
      
      try {
         Class.forName("org.sqlite.JDBC");
         c = DriverManager.getConnection("jdbc:sqlite:seller01catalog.db");
      } catch ( Exception e ) {
         System.err.println( e.getClass().getName() + ": " + e.getMessage() );
         System.exit(0);
      }
      System.out.println("Opened database successfully");
   }

    public void action() {
       System.out.println("Tell is executed");
    
       /*carga la base*/

       tellDone = true;
    } 
    
    public boolean done() {
      if (tellDone)
        return true;
      else
        return false;
    }
   
  }    // END of inner class ...Behaviour

  private class AskBehaviour extends Behaviour {
    boolean askDone = false;

    public void action() {
       System.out.println("Ask is executed");
     /*
        clips.eval("(clear)");
        clips.load("./clips/Seller01Rules.clp");
        clips.eval("(reset)");
        clips.run();
    */
       askDone = true;
    } 
    
    public boolean done() {
      if (askDone)
        return true;
      else
        return false;
    }
   
    public int onEnd() {
      myAgent.doDelete();
      return super.onEnd();
    } 
  } 
}
