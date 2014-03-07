
// HighScores.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th


import java.io.*;
// import java.net.*;
import java.util.*;



public class HighScores
// Maintains the collection of high scores
{
  private ScoreInfo scores[];
  private int numScores;       // number of scores in the array

  private static final int MAX_SCORES = 10;
  private static String SCORE_FN = "scores.txt";

  public HighScores()
  {
    scores = new ScoreInfo[MAX_SCORES];
    numScores = 0;
    loadScores();
  }


  public String toString()
  // The returned string is "HIGH$$ name1 & score1 & .... nameN & scoreN & "
  {
    String details = "HIGH$$ ";
    for(int i = 0; i < numScores; i++)
      details += scores[i].getName() + " & " + scores[i].getScore() + " & ";
    System.out.println("details: " + details);
    return details;
  }


  public void addScore(String line)
  // The line should be "name & score &"
  {
     StringTokenizer st = new StringTokenizer(line, "&");
     try {
       String name = st.nextToken().trim();
       int score = Integer.parseInt( st.nextToken().trim() );
       addScore(name, score);
       saveScores();    // save after each update
     }
     catch(Exception e)
     {  System.out.println("Problem parsing new score:\n" + e);  }
  }  // end of addScore()



  public void addScore(String name, int scr)
  {
    int i = 0;

    while ((i < numScores) && (scores[i].getScore() >= scr))
      i++;

    if (i == MAX_SCORES) {   // array is full and new score is smaller than existing ones
      System.out.println("Score too small to be added to full array");
      return;      // do not add new score
    }

    if (numScores == MAX_SCORES)     // full array, so 'remove' current smallest
      numScores--;
      
    // move smaller scores to the right in the array
    for(int j = numScores-1; j >= i; j--)
      scores[j+1] = scores[j];

    // add in new score
    scores[i] = new ScoreInfo(name, scr);

    numScores++;
  }  // end of addScore()



  private void loadScores()
  /* We assume a score consists of "name & score &" on a line
     The scores are read in from SCORE_FN.
  */
  {
    String line;
    try {
      BufferedReader in = 
			new BufferedReader(new FileReader(SCORE_FN));
      while ((line = in.readLine()) != null)
        addScore(line);
      in.close();
    }
    catch(IOException e)
    {  System.out.println(e);  }
  }  // end of loadScores()


  public void saveScores()
  /* We write out "name & score &" to each line of the file
     The scores are written to SCORE_FN.
  */
  {
    String line;
    try {
      PrintWriter out = new PrintWriter(
			new BufferedWriter( new FileWriter(SCORE_FN) ), true);
      for (int i=0; i < numScores; i++) {
         line = scores[i].getName() + " & " + scores[i].getScore() + " &";
         out.println(line);
      }
      out.close();
    }
    catch(IOException e)
    {  System.out.println(e);  }
  }  // end of saveScores()


}  // end of HighScores class



// --------------------------------------------------------------

class ScoreInfo
// Maintains the name and score details
{
  private String name;
  private int score;

  public ScoreInfo(String n, int s)
  {  name = n;  score = s;  }

  public String getName()
  {  return name;  }

  public int getScore()
  {  return score;  }

}  // end of ScoreInfo

