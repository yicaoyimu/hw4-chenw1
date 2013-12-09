package edu.cmu.lti.f13.hw4.hw4_chenw1.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_chenw1.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_chenw1.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_chenw1.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	private class QueryAndAnswer {
	  public String query;
	  public String correctAnswer;
	  public double correctAnswerScore;
	  public ArrayList<String> answer;
	  public ArrayList<Double> score;
	  public int rank;
	  public void addAnswer(Document a) { answer.add(a.getText().toLowerCase()); }
	  public QueryAndAnswer() { answer = new ArrayList<String>(); score = new ArrayList<Double>(); }
	}

	private ArrayList<QueryAndAnswer> qAndAList;
  private Hashtable<Integer, QueryAndAnswer> t;
	
	public void initialize() throws ResourceInitializationException {
	  t = new Hashtable<Integer, QueryAndAnswer>();
	}

	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
		
		
		if (it.hasNext()) {
		  Document doc = (Document) it.next();
		  QueryAndAnswer val;
		  
		  if(t.containsKey(doc.getQueryID()))
		    val = t.get(doc.getQueryID());
		  else
		    val = new QueryAndAnswer();

		  if(doc.getRelevanceValue() == 99)
		    val.query = doc.getText().toLowerCase();
		  else if(doc.getRelevanceValue() == 1)
		    val.correctAnswer = doc.getText().toLowerCase();
		  else
		    val.addAnswer(doc);

		  t.put(doc.getQueryID(), val);
		}
	}

	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
				
    ArrayList<QueryAndAnswer> qAndA = new ArrayList<QueryAndAnswer>();
    Enumeration<QueryAndAnswer> enumeration = t.elements();

    while(enumeration.hasMoreElements())
    {
      QueryAndAnswer temp = enumeration.nextElement();
      qAndA.add(temp);
    }
    
    // compute score
    for(int i = 0; i < qAndA.size(); i++) {
      QueryAndAnswer q = qAndA.get(i);
      for(int j = 0; j < q.answer.size(); j++)
        q.score.add(computeCosineSimilarity(q.query, q.answer.get(j)));
      // q.correctAnswerScore = computeCosineSimilarity(q.query, q.correctAnswer);
      q.correctAnswerScore = computeJaccardSimilarity(q.query, q.correctAnswer);
      // q.correctAnswerScore = computeDiceSimilarity(q.query, q.correctAnswer);

    } 
    
    // compute rank
    for(int i = 0; i < qAndA.size(); i++) {
      QueryAndAnswer q = qAndA.get(i);
      double correctAnswerScore = q.correctAnswerScore;
      
      int rank = 1;
      for(int j = 0; j < q.score.size(); j++)
        if(q.score.get(j) > correctAnswerScore) rank++;
      q.rank = rank;
    }
    
    qAndAList = qAndA;

    for(int i = 0; i < qAndA.size(); i++)
    {
      QueryAndAnswer q = qAndA.get(i);
      System.out.println(q.query + " " + q.rank);
      System.out.println(q.correctAnswerScore + " " + q.correctAnswer);
      for(int j = 0; j < q.answer.size(); j++)
        System.out.println(q.score.get(j) + " " + q.answer.get(j));
      
    }
		double metric_mrr = compute_mrr();
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	private double computeDiceSimilarity(String query, String answer)
	{
    // build query dictionary
    Pattern token = Pattern.compile("[\\W]*\\b(\\w\\w+)\\b[\\W]*");
    Hashtable<String, Integer> dict = new Hashtable<String, Integer>();
    Hashtable<String, Integer> secondDict = new Hashtable<String, Integer>();

    Matcher matchQuery = token.matcher(query);
    int pos = 0;
    while(matchQuery.find(pos))
    {
      String toPut = matchQuery.group(1);
      dict.put(matchQuery.group(1), 0);
      pos = matchQuery.end();
    }

    int overlap = 0;
    
    Matcher matchAnswer = token.matcher(answer);
    pos = 0;
    while(matchAnswer.find(pos))
    {
      String toput = matchAnswer.group(1);
      if(dict.containsKey(matchAnswer.group(1))) overlap++;
      secondDict.put(toput, 0);
      pos = matchAnswer.end();
    }

    return (double)(2 * overlap) / (double)(dict.size() + secondDict.size());
	}
	
	private double computeJaccardSimilarity(String query, String answer)
	{
    // build query dictionary
    Pattern token = Pattern.compile("[\\W]*\\b(\\w\\w+)\\b[\\W]*");
    Hashtable<String, Integer> dict = new Hashtable<String, Integer>();
    Matcher matchQuery = token.matcher(query);
    int pos = 0;
    while(matchQuery.find(pos))
    {
      String toPut = matchQuery.group(1);
      dict.put(matchQuery.group(1), 0);
      pos = matchQuery.end();
    }

    int overlap = 0;
    int total = dict.size();
    
    Matcher matchAnswer = token.matcher(answer);
    pos = 0;
    while(matchAnswer.find(pos))
    {
      if(dict.containsKey(matchAnswer.group(1))) overlap++;
      else total++;
      pos = matchAnswer.end();
    }

    return (double)overlap / (double)total;
	}
	
  private double computeCosineSimilarity(String query, String answer) {
    
    // build query dictionary
    Pattern token = Pattern.compile("[\\W]*\\b(\\w\\w+)\\b[\\W]*");
    ArrayList<String> q = new ArrayList<String> ();
    ArrayList<String> a = new ArrayList<String> ();
    Hashtable<String, Integer> dict = new Hashtable<String, Integer>();
    Matcher matchQuery = token.matcher(query);
    int pos = 0;
    while(matchQuery.find(pos))
    {
      String toPut = matchQuery.group(1);
      if(toPut.compareTo("a") == 0 ||
         toPut.compareTo("the") == 0 ||
         toPut.compareTo("an") == 0 ||
         toPut.compareTo("is") == 0) { pos = matchQuery.end(); continue; }
      dict.put(matchQuery.group(1), 0);
      q.add(matchQuery.group(1));
      pos = matchQuery.end();
    }

    Matcher matchAnswer = token.matcher(answer);
    pos = 0;
    while(matchAnswer.find(pos))
    {
      dict.put(matchAnswer.group(1), 0);
      a.add(matchAnswer.group(1));
      pos = matchAnswer.end();
    }
    
    // count word frequency
    Hashtable <String, Integer> queryCount = new Hashtable<String, Integer> (dict);
    for(int i = 0; i < q.size(); i++)
      queryCount.put(q.get(i), queryCount.get(q.get(i)) + 1);

    Hashtable <String, Integer> answerCount = new Hashtable<String, Integer> (dict);
    for(int i = 0; i < a.size(); i++)
      answerCount.put(a.get(i), answerCount.get(a.get(i)) + 1);

    // build text vector
    ArrayList<Integer> queryVector = new ArrayList<Integer>();
    ArrayList<Integer> answerVector = new ArrayList<Integer>();
    Enumeration<String> enumeration = dict.keys();
    while(enumeration.hasMoreElements())
    {
      String key = enumeration.nextElement();
      if(queryCount.containsKey(key))
        queryVector.add(queryCount.get(key));
      else
        queryVector.add(0);
      
      if(answerCount.containsKey(key))
        answerVector.add(answerCount.get(key));
      else
        answerVector.add(0);
    }
    
    // calculate cosine similarity
    double dotProduct = 0.0;
    double queryNorm = 0.0;
    double docNorm = 0.0;
    for(int i = 0; i < answerVector.size(); i++)
    {
      dotProduct += answerVector.get(i) * queryVector.get(i);
      queryNorm += queryVector.get(i) * queryVector.get(i);
      docNorm += answerVector.get(i) * answerVector.get(i);
    }
    
    double ret = dotProduct / Math.sqrt(docNorm * queryNorm);
    return ret;
  }

	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;

		for(int i = 0; i < qAndAList.size(); i++)
		  metric_mrr += 1/((double)qAndAList.get(i).rank);
		return metric_mrr / ((double) qAndAList.size());
	}

}
