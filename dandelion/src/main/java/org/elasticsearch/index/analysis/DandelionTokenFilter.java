package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import java.util.LinkedList;

import java.io.IOException;

public class DandelionTokenFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsAtt = addAttribute(OffsetAttribute.class);
	private final LinkedList<String> extraTokens = new LinkedList<String>();
	private State savedState;
	private int previousLength = 0;

	public DandelionTokenFilter(TokenStream in){
		super(in);
	}

	public final boolean incrementToken() throws IOException {
		if (!extraTokens.isEmpty()) {
			return addToken();
	    }

	    if (input.incrementToken()) {
           	extraTokens.add("AHAHA");
		   	extraTokens.add("sono");
		   	extraTokens.add("Enrico");
        	return addToken();
	    }
	    return false;
	}

	private final boolean addToken(){
		String token = extraTokens.remove();
		offsAtt.setOffset(previousLength,previousLength+token.length());
		previousLength+=token.length()+1;
		termAtt.setEmpty().append(token);
		return true;
	}

}
