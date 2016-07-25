/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.bkromhout.minerva.data;

import javax.annotation.Nonnull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Hashes an input stream as it is read using the SHA-256 hashing algorithm.
 * <p>
 * All of the code in this class has been extracted and derived from the Guava library, and I lay no claim to it. I
 * chose to extract this functionality so that I don't have to include the whole library in my app, since it's a huge
 * dependency.
 * <p>
 * Guava's license file is located <a href="https://github.com/google/guava/blob/master/COPYING">here</a>, and the
 * appropriate license header has been retained in this file, citing all years which were cited in the original files
 * this code was derived from at the time of derivation.
 * @see SHA256HashFunction
 */
class HashingInputStream extends FilterInputStream {
    private final SHA256HashFunction.MessageDigestHasher hasher;

    /**
     * Constructs a new {@code FilterInputStream} with the specified input stream as source.
     * <p>
     * <p><strong>Warning:</strong> passing a null source creates an invalid {@code FilterInputStream}, that fails on
     * every method that is not overridden. Subclasses should check for null in their constructors.
     * @param in the input stream to filter reads on.
     */
    HashingInputStream(InputStream in) {
        super(in);
        hasher = new SHA256HashFunction().newHasher();
    }

    /**
     * Reads the next byte of data from the underlying input stream and updates the hasher with the byte read.
     */
    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            hasher.putByte((byte) b);
        }
        return b;
    }

    /**
     * Reads the specified bytes of data from the underlying input stream and updates the hasher with the bytes read.
     */
    @Override
    public int read(@Nonnull byte[] bytes, int off, int len) throws IOException {
        int numOfBytesRead = in.read(bytes, off, len);
        if (numOfBytesRead != -1) hasher.putBytes(bytes, off, numOfBytesRead);
        return numOfBytesRead;
    }

    /**
     * mark() is not supported for HashingInputStream
     * @return {@code false} always
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * mark() is not supported for HashingInputStream
     */
    @Override
    public void mark(int readlimit) {}

    /**
     * reset() is not supported for HashingInputStream.
     * @throws IOException this operation is not supported
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("reset not supported");
    }

    /**
     * Returns the {@link com.bkromhout.minerva.data.SHA256HashFunction.BytesHashCode} based on the data read from this
     * stream. The result is unspecified if this method is called more than once on the same instance.
     */
    SHA256HashFunction.BytesHashCode hash() {
        return hasher.hash();
    }
}
