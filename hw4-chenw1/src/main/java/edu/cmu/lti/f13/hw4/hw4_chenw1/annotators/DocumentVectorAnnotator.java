package edu.cmu.lti.f13.hw4.hw4_chenw1.annotators;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_chenw1.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_chenw1.typesystems.Token;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {
  private Pattern token = Pattern.compile("[\\W]*\\b(\\w\\w+)\\b[\\W]*");

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		
		FSList list = new FSList(jcas);
		ArrayList<Token> l = new ArrayList<Token>();
		
    Matcher matcher = token.matcher(docText);
    int pos = 0;
    while(matcher.find(pos))
    {
      Token annotation = new Token(jcas);
      annotation.setText(matcher.group(1));
      annotation.addToIndexes();
      l.add(annotation);
      pos = matcher.end();
    }
		
    list = FSCollectionFactory.createFSList(jcas, l);
    doc.setTokenList(list);

	}

}
