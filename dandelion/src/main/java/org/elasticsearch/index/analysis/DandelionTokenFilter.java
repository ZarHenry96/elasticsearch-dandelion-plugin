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
	private final LinkedList<String> tokens = new LinkedList<String>();
	private State savedState;

	public DandelionTokenFilter(TokenStream in){
		super(in);
	}

	public final boolean incrementToken() throws IOException {
		if (!tokens.isEmpty()) {
			termAtt.setEmpty().append(tokens.remove());
			return true;
	    }

	    if (input.incrementToken()) {

		   	return true;
	    }
	    return false;
	}
}
