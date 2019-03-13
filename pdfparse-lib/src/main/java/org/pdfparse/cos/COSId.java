package org.pdfparse.cos;

import org.pdfparse.exception.EParseError;
import org.pdfparse.parser.PDFParser;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.Token;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class COSId extends IdGenPair implements COSObject {
    public COSId(int id, int gen) {
        super(id,gen);
    }

    @Override
    public void parse(PDFRawData src, PDFParser pdfFile) throws EParseError {
        if (!tryReadId(src, this, Token.OBJ)) {
            throw new EParseError("Failed to read object header");
        }
    }

    @Override
    public void produce(OutputStream dst, PDFParser pdfFile) throws IOException {
        String s = String.format("%d %d obj", id, gen);
        dst.write(s.getBytes(Charset.defaultCharset()));
    }

    @Override
    public String toString() {
        return String.format("%d %d obj", id, gen);
    }
}
