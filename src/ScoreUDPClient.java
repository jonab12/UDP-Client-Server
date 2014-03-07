
// ScoreUDPClient.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/*
  A test rig for ScoreUDPServer with a GUI interface
  The client can send a name/score, and ask for the 
  current high scores. 

  Unlike the TCP version of the client, there is no
  long-term link to close when the window is terminated.

  The application makes use of the implicit GUI thread 
  to send messages to the server, and uses the application
  thread to receive them. 

  There is a minor synchronization issue since the two threads
  may try to update the message text area, jtaMesgs, at the same
  time. We ignore that problem here.
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class ScoreUDPClient extends JFrame implements ActionListener
{
  private static final int SERVER_PORT = 1234;     // server details
  private static final String SERVER_HOST = "localhost";

  private static final int BUFSIZE = 1024;   // max size of a message

  private DatagramSocket sock;
  private InetAddress serverAddr;

  private JTextArea jtaMesgs;
  private JTextField jtfName, jtfScore;
  private JButton jbGetScores;


  public ScoreUDPClient()
  {
     super( "High Score UDP Client" );

     initializeGUI();

     try {   // try to create the client's socket
       sock = new DatagramSocket();
     }
     catch( SocketException se ) {
       se.printStackTrace();
       System.exit(1);
     }

     try {  // try to turn the server's name into an internet address
       serverAddr = InetAddress.getByName(SERVER_HOST);
     }
     catch( UnknownHostException uhe) {
       uhe.printStackTrace();
       System.exit(1);
     }

     setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
     setSize(300,450);
     setResizable(false);    // fixed size display
     setVisible(true);

     waitForPackets();
  } // end of ScoreUDPClient();


  private void initializeGUI()
  // text area in center, and controls in south
  {
    Container c = getContentPane();
    c.setLayout( new BorderLayout() );

    jtaMesgs = new JTextArea(7, 7);
    jtaMesgs.setEditable(false);
    JScrollPane jsp = new JScrollPane( jtaMesgs);
    c.add( jsp, "Center");

    JLabel jlName = new JLabel("Name: ");
    jtfName = new JTextField(10);

    JLabel jlScore = new JLabel("Score: ");
    jtfScore = new JTextField(5);
    jtfScore.addActionListener(this);    // pressing enter triggers sending of name/score

    jbGetScores = new JButton("Get Scores");
    jbGetScores.addActionListener(this);

    JPanel p1 = new JPanel( new FlowLayout() );
    p1.add(jlName); p1.add(jtfName);
    p1.add(jlScore); p1.add(jtfScore);

    JPanel p2 = new JPanel( new FlowLayout() );
    p2.add(jbGetScores);

    JPanel p = new JPanel();
    p.setLayout( new BoxLayout(p, BoxLayout.Y_AXIS));
    p.add(p1); p.add(p2);

    c.add(p, "South");

  }  // end of initializeGUI()


  // --------------------------------------------------------------
  // client processing of user commands; done by GUI thread


   public void actionPerformed(ActionEvent e)
   /* Either a name/score is to be sent or the "Get Scores"
      button has been pressed.
  
      Only output messages are sent to the server from here,
      no input is received. Server responses are dealt with by
      the application thread in waitForPackets().
   */
   {
     if (e.getSource() == jbGetScores) {
       sendMessage(serverAddr, SERVER_PORT, "get");
       jtaMesgs.append("Sent a get command\n");
     }
     else if (e.getSource() == jtfScore)
       sendScore();
   } // end of actionPerformed()



  private void sendScore()
  /* Check if the user has supplied a name and score, then
     send "score name & score &" to server
     Note: we should check that score is an integer, but we don't. */
  {
    String name = jtfName.getText().trim();
    String score = jtfScore.getText().trim();
    // System.out.println("'" + name + "'   '" + score + "'");

    if ((name.equals("")) && (score.equals("")))
      JOptionPane.showMessageDialog( null, 
           "No name and score entered", "Send Score Error", 
			JOptionPane.ERROR_MESSAGE);
    else if (name.equals(""))
      JOptionPane.showMessageDialog( null, 
           "No name entered", "Send Score Error", 
			JOptionPane.ERROR_MESSAGE);
    else if (score.equals(""))
      JOptionPane.showMessageDialog( null, 
           "No score entered", "Send Score Error", 
			JOptionPane.ERROR_MESSAGE);
    else {
      sendMessage(serverAddr, SERVER_PORT, 
						"score " + name + " & " + score + " &");
      jtaMesgs.append("Sent " + name + " & " + score + "\n");
    }
  }  // end of sendScore()



  private void sendMessage(InetAddress serverAddr, int serverPort, String mesg)
  // send message to the socket at the specified address and port
  {
    byte mesgData[] = mesg.getBytes();   // convert message to byte[] form
    try {
      DatagramPacket sendPacket =
          new DatagramPacket( mesgData, mesgData.length, serverAddr, serverPort);
      sock.send( sendPacket );
    }
    catch(IOException ioe)
    {  System.out.println(ioe);  }
  }  // end of sendMessage()



  // ------------------------------------------------------------
  // processing of server responses; done by the application thread
  // very similar to the server processing of client messages


  private void waitForPackets()
  /* Repeatedly receive a packet, process it. 
     No messages are sent to the server from here. Output is
     left to the GUI thread. */
  {
    DatagramPacket receivePacket;
    byte data[];

    try {
      while (true) {
        // set up an empty packet
        data = new byte[BUFSIZE];
        receivePacket = new DatagramPacket(data, data.length);

        System.out.println("Waiting for a packet...");
        sock.receive( receivePacket );

        processServer(receivePacket);
      }
    }
    catch(IOException ioe)
    {  System.out.println(ioe);  }
  }  // end of waitForPackets()



   private void processServer(DatagramPacket receivePacket)
   // extract server details from the received packet
   {
     InetAddress serverAddr = receivePacket.getAddress();
     int serverPort = receivePacket.getPort();
     String serverMesg = new String( receivePacket.getData(), 0,
                           receivePacket.getLength() );

     System.out.println("Server packet from " + serverAddr +
                                          ", " + serverPort );
     System.out.println("Server mesg: " + serverMesg);
        
     showResponse(serverMesg);
   }  // end of processServer()



   private void showResponse(String mesg)
   /* The only server response is to a client's get command.
      The response should be "HIGH$$ n1 & s1 & .... nN & sN & "
   */
   {
     if ((mesg.length() >= 7) &&     // "HIGH$$ "
         (mesg.substring(0,6).equals("HIGH$$")))
       showHigh( mesg.substring(6).trim() );    
		    // remove HIGH$$ keyword and surrounding spaces
     else    // should not happen
       jtaMesgs.append( mesg + "\n");
   }  // end of showResponse()



  private void showHigh(String mesg)
  // Parse the high scores and display in a nice way
  {
    StringTokenizer st = new StringTokenizer(mesg, "&");
    String name;
    int i, score;
    i = 1;
    try {
      while (st.hasMoreTokens()) {
        name = st.nextToken().trim();
        score = Integer.parseInt( st.nextToken().trim() );
        jtaMesgs.append("" + i + ". " + name + " : " + score + "\n");
        i++;
      }
      jtaMesgs.append("\n");
    }
    catch(Exception e)
    { 
      jtaMesgs.append("Problem parsing high scores\n");
      System.out.println("Parsing error with high scores: \n" + e);  
    }
  }  // end of showHigh()



  // ------------------------------------

  public static void main(String args[]) 
  {  new ScoreUDPClient();  }

} // end of ScoreUDPClient class

