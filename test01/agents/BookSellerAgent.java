/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package project01.trading;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;
import java.sql.*;
import net.sf.clipsrules.jni.*;

/*
import java.sql.Connection;
import java.sql.DriverManager;
*/

public class BookSellerAgent extends Agent {
	// The catalogue of books for sale (maps the title of a book to its price)
	//private Hashtable catalogue;
	// The GUI by means of which the user can add books in the catalogue
	//private BookSellerGui myGui;

	//connection to database
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs;

    //CLIPS
    Environment clips;

	// Put agent initializations here
	public void setup() {
	
        try{
            connection = DriverManager.getConnection( "jdbc:sqlite:seller01catalog.db" );
            connection.setAutoCommit(false);
            if ( connection != null ){
                System.out.println("Connected succesfully!");
            }

            stmt = connection.createStatement();
            rs = stmt.executeQuery( "SELECT * FROM MAIN;" );

            while ( rs.next() ) {
                int partnumber = rs.getInt("partnumber");
                String  name = rs.getString("name");
                float price  = rs.getFloat("price");
                String category = rs.getString("category");
                int existences = rs.getInt("existences");

                System.out.println( "PARTNUMBER = " + partnumber );
                System.out.println( "NAME = " + name );
                System.out.println( "PRICE = " + price );
                System.out.println( "CATEGORY = " + category );
                System.out.println( "EXISTENCES = " + existences );
                System.out.println();
            }
        }
        catch ( Exception ex ) {
            System.err.println( ex.getClass().getName() + ": " + ex.getMessage() );
            System.out.println("Error in conection");
        };

		// Create the catalogue
		//catalogue = new Hashtable();

		// Create and show the GUI 
		//myGui = new BookSellerGui(this);
		//myGui.showGui();

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
		
	}

	// Put agent clean-up operations here
	public void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		//myGui.dispose();
		// Printout a dismissal message
		System.out.println("Seller-agent "+getAID().getName()+" terminating.");
		
		//close connection with database
		
		/*rs.close();
        stmt.close();
        connection.close();*/
	}

	/**
     This is invoked by the GUI when the user adds a new book for sale
	 
	public void updateCatalogue(final String name, final int price) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(name, price);
				System.out.println(name+" inserted into catalogue. Price = "+price);
			}
		} );
	}*/

	/**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by Book-seller agents to serve incoming requests 
	   for offer from buyer agents.
	   If the requested book is in the local catalogue the seller agent replies 
	   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	   sent back.
	 */

	public class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
            clips = new Environment();

			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String product = msg.getContent();
				ACLMessage reply = msg.createReply();

				try{
                    ResultSet rs = stmt.executeQuery( "SELECT partnumber,name,price,category,existences FROM MAIN where name='"+product+"';" );
                    while ( rs.next() ) {
                        int partnumber = rs.getInt("partnumber");
                        String  name = rs.getString("name");
                        float price  = rs.getFloat("price");
                        String category = rs.getString("category");
                        int existences = rs.getInt("existences");

                        System.out.println( "NAME = " + name );
                        System.out.println( "PRICE = " + price );
                        System.out.println( "CATEGORY = " + category );
                        System.out.println( "EXISTENCES = " + existences );
                        System.out.println();

                        try{
                            clips.eval("(clear)");
                            //clips.eval ("(printout t \"carga de reglas aqui\" clrf)");
                            clips.build("(deftemplate purchase (slot request))");
                            clips.build("(defrule promo01 \"regla promo\" (purchase (request \"iPhone7\")) => (printout t \"item has a 5% discount\" crlf))");
                            clips.build("(defrule promo02 \"regla promo\" (purchase (request \"Amplifier\")) => (printout t \"item has a 10% discount\" crlf))");
                            clips.build("(defrule promo03 \"regla promo\" (purchase (request \"Memory USB\")) => (printout t \"item has a 2x1, get one free!\" crlf))");
                            clips.assertString("(purchase (request \"" + product + "\"))");
                            clips.eval("(facts)");
                            clips.eval("(rules)");
                            //clips.eval("(reset)");
                            clips.run();

//                             if(desc == true){price = price - (price * 1/10);}
//                             else{desc = false;}
                        }
                        catch (Exception e){ e.printStackTrace(); }

                        // The requested product is available for sale. Reply with affirmative
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(String.valueOf(price));
                    }
				}
				catch( Exception ex ){
                    System.err.println( ex.getClass().getName() + ": " + ex.getMessage() );
                    //System.out.println("not-found");
                    // The requested product is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Book-seller agents to serve incoming 
	   offer acceptances (i.e. purchase orders) from buyer agents.
	   The seller agent removes the purchased book from its catalogue 
	   and replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	public class PurchaseOrdersServer extends CyclicBehaviour {
	private AID[] supplierAgents;
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String product = msg.getContent();
				ACLMessage reply = msg.createReply();

				try{
                    ResultSet rs = stmt.executeQuery( "SELECT name,price,category,existences FROM MAIN WHERE name='"+product+"';" );
                    while ( rs.next() ) {
                        String  name = rs.getString("name");
                        float price  = rs.getFloat("price");
                        String category = rs.getString("category");
                        int existences = rs.getInt("existences");

                        if(existences > 0){
                        reply.setPerformative(ACLMessage.INFORM);
                        System.out.println(product+" sold to agent "+msg.getSender().getName());
                        //Modifies database, existences=-1
                        int rsi = stmt.executeUpdate("UPDATE MAIN SET existences = existences-1 WHERE name='"+product+"';");
                        /*while(rs.next()){
                            if(rs.getString("name")==product){
                                rs.updateInt("existences",existences-1);
                                rs.updateRow();
                            }
                        }*/
                        }
                        else{
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("not-available");
                            //int rsi = stmt.executeUpdate("UPDATE MAIN SET existences = existences+4 WHERE name='"+product+"';");
                            //supplier();
                        }
                    }
				}
				catch( Exception ex ){
                    System.err.println( ex.getClass().getName() + ": " + ex.getMessage() );
                    // The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}

// 		public void supplier(){
//             // Update the list of agents
//             DFAgentDescription template = new DFAgentDescription();
//             ServiceDescription sd = new ServiceDescription();
//             sd.setType("book-supplier");
//             template.addServices(sd);
//             try {
//                 DFAgentDescription[] result = DFService.search(myAgent, template);
//                 System.out.println("Contacted the supplier agent:");
//                 supplierAgents = new AID[result.length];
//             }
//             catch (FIPAException fe) {fe.printStackTrace();}
//             // Perform the request
//             ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
//             cfp.addReceiver(supplierAgents);
//             cfp.setContent(product);
//             cfp.setConversationId("supplier");
//             cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
//             myAgent.send(cfp);
// 		}
	}  // End of inner class
}
