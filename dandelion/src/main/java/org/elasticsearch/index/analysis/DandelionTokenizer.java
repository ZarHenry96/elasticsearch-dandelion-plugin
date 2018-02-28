package org.elasticsearch.index.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;


public final class DandelionTokenizer extends Tokenizer {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    private String auth_token;

    private Boolean read;
    private String inputString;
    private Integer offset;
    private Boolean end;

    public DandelionTokenizer(String auth_token) {
        super();
        this.auth_token = auth_token;
    }

    @Override
    public boolean incrementToken() throws IOException {

        clearAttributes();
        if(!read){
            setInputString();
        }
        if(!end) {
            termAtt.setEmpty().append(inputString);
            typeAtt.setType("stringa");
            offsetAtt.setOffset(offset,offset+inputString.length());
            offset += inputString.length();
            end = true;
            return true;
        }else{
            return false;
        }

    }

    private void setInputString() throws IOException {
        int intValueOfChar;
        while ((intValueOfChar = input.read()) != -1) {
            inputString += (char) intValueOfChar;
        }
        read = true;
    }

    public void end() throws IOException {
        super.end();
        offsetAtt.setOffset(offset, offset);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        read = false;
        inputString = "";
        offset = 0;
        end = false;
    }
}
