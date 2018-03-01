package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class DandelionTokenFilter extends TokenFilter {

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	public DandelionTokenFilter(TokenStream in){
		super(in);
	}

	public final boolean incrementToken() throws IOException {

	    while (input.incrementToken()) {
            if(!typeAtt.type().equals("")){
                termAtt.setEmpty().append(typeAtt.type());
                typeAtt.setType(TypeAttribute.DEFAULT_TYPE);
                return true;
            }
	    }
	    return false;
	}
}
