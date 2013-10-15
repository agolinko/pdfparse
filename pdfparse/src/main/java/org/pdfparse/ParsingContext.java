/*
 * Copyright (c) 2013 Anton Golinko
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 */

package org.pdfparse;

import org.pdfparse.utils.ByteBuffer;

import java.io.ByteArrayOutputStream;


public class ParsingContext {
    public final static int EP_THROW_EXCEPTION = 0;
    public final static int EP_TRY_RECOVER = 1;

    public final static int SV_BAD_SYNTAX = 0;
    public final static int SV_WARNING = 10;
    public final static int SV_ERROR = 20;


    public int errorHandlingPolicy = EP_THROW_EXCEPTION;

    public boolean checkSyntaxCompliance = false;
    public boolean tryRecoverErrors = false;
    public boolean ignoreErrors = false;

    public boolean allowScan = true;
    public int headerLookupRange = 100;
    public int eofLookupRange = 100;

    public ByteBuffer tmpBuffer = new ByteBuffer(1024);

    public ParsingGetObject objectCache;
    public boolean useEncryption;
    public byte[] encryptionKey;

    ParsingContext() {

    }

    public void verbosityLog(int severity, String message) {
        System.out.println(message);
    }

}
