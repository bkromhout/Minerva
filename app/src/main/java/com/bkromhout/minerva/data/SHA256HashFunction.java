/*
 * Copyright (C) 2011, 2012 The Guava Authors
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

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * A hash function which uses the SHA-256 hashing algorithm.
 * <p>
 * All of the code in this class has been extracted and derived from the Guava library, and I lay no claim to it. I
 * chose to extract this functionality so that I don't have to include the whole library in my app, since it's a huge
 * dependency.
 * <p>
 * Guava's license file is located <a href="https://github.com/google/guava/blob/master/COPYING">here</a>, and the
 * appropriate license header has been retained in this file, citing all years which were cited in the original files
 * this code was derived from at the time of derivation.
 * @see HashingInputStream
 */
@SuppressLint("DefaultLocale")
class SHA256HashFunction implements Serializable {
    private final MessageDigest digest = getMessageDigest("SHA-256");
    private final int bytes;
    private final boolean supportsClone;

    SHA256HashFunction() {
        this.bytes = digest.getDigestLength();
        this.supportsClone = supportsClone();
    }

    private SHA256HashFunction(int bytes) {
        int maxLength = digest.getDigestLength();
        if (!(bytes >= 4 && bytes <= maxLength))
            throw new IllegalArgumentException(String.format("bytes (%d) must be >= 4 and < %d.", bytes, maxLength));
        this.bytes = bytes;
        this.supportsClone = supportsClone();
    }

    /**
     * Checks whether or not the {@link #digest} supports cloning.
     * @return True if so, false otherwise.
     */
    private boolean supportsClone() {
        try {
            digest.clone();
            return true;
        } catch (CloneNotSupportedException e) {
            return false;
        }
    }

    /**
     * Begins a new hash code computation by returning an initialized, stateful {@link MessageDigestHasher} instance
     * that is ready to receive data.
     */
    MessageDigestHasher newHasher() {
        if (supportsClone) {
            try {
                return new SHA256HashFunction.MessageDigestHasher((MessageDigest) digest.clone(), bytes);
            } catch (CloneNotSupportedException e) {
                // falls through
            }
        }
        return new SHA256HashFunction.MessageDigestHasher(getMessageDigest(digest.getAlgorithm()), bytes);
    }

    /**
     * Shortcut for {@code newHasher().putInt(input).hash()}; returns the hash code for the given {@code int} value,
     * interpreted in little-endian byte order. The implementation <i>might</i> perform better than its longhand
     * equivalent, but should not perform worse.
     */
    BytesHashCode hashInt(int input) {
        return newHasher().putInt(input).hash();
    }

    /**
     * Shortcut for {@code newHasher().putLong(input).hash()}; returns the hash code for the given {@code long} value,
     * interpreted in little-endian byte order. The implementation <i>might</i> perform better than its longhand
     * equivalent, but should not perform worse.
     */
    BytesHashCode hashLong(long input) {
        return newHasher().putLong(input).hash();
    }

    /**
     * Shortcut for {@code newHasher().putBytes(input).hash()}. The implementation <i>might</i> perform better than its
     * longhand equivalent, but should not perform worse.
     */
    BytesHashCode hashBytes(byte[] input) {
        return newHasher().putBytes(input).hash();
    }

    /**
     * Shortcut for {@code newHasher().putBytes(input, off, len).hash()}. The implementation <i>might</i> perform better
     * than its longhand equivalent, but should not perform worse.
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > bytes.length} or {@code len < 0}
     */
    BytesHashCode hashBytes(byte[] input, int off, int len) {
        return newHasher().putBytes(input, off, len).hash();
    }

    /**
     * Shortcut for {@code newHasher().putUnencodedChars(input).hash()}. The implementation <i>might</i> perform better
     * than its longhand equivalent, but should not perform worse. Note that no character encoding is performed; the low
     * byte and high byte of each {@code char} are hashed directly (in that order).
     */
    BytesHashCode hashUnencodedChars(CharSequence input) {
        return newHasher().putUnencodedChars(input).hash();
    }

    /**
     * Shortcut for {@code newHasher().putString(input, charset).hash()}. Characters are encoded using the given {@link
     * Charset}. The implementation <i>might</i> perform better than its longhand equivalent, but should not perform
     * worse.
     */
    BytesHashCode hashString(CharSequence input, Charset charset) {
        return newHasher().putString(input, charset).hash();
    }

    /**
     * Returns the number of bits (a multiple of 32) that each hash code produced by this hash function has.
     */
    int bits() {
        return bytes * Byte.SIZE;
    }

    @Override
    public String toString() {
        return "SHA256HashFunction";
    }

    /**
     * Get the message digest algorithm specified by {@code algorithmName}, or throw an exception if we can't.
     * @param algorithmName Name of the algorithm to use for the message digest.
     * @return Instance of a message digest using the specified algorithm
     */
    private static MessageDigest getMessageDigest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Hasher that updates a message digest.
     */
    static class MessageDigestHasher {
        private final ByteBuffer scratch = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        private final MessageDigest digest;
        private final int bytes;
        private boolean done;

        private MessageDigestHasher(MessageDigest digest, int bytes) {
            this.digest = digest;
            this.bytes = bytes;
        }

        /**
         * Updates this hasher with the given byte.
         */
        protected void update(byte b) {
            checkNotDone();
            digest.update(b);
        }

        /**
         * Updates this hasher with the given bytes.
         */
        protected void update(byte[] b) {
            checkNotDone();
            digest.update(b);
        }

        /**
         * Updates this hasher with {@code len} bytes starting at {@code off} in the given buffer.
         */
        protected void update(byte[] b, int off, int len) {
            checkNotDone();
            digest.update(b, off, len);
        }

        /**
         * Updates the hasher with the given number of bytes from the buffer.
         */
        private MessageDigestHasher update(int bytes) {
            try {
                update(scratch.array(), 0, bytes);
            } finally {
                scratch.clear();
            }
            return this;
        }

        private void checkNotDone() {
            if (done) throw new IllegalStateException("Cannot re-use after calling hash().");
        }

        /**
         * Computes a hash code based on the data that have been provided to this hasher. The result is unspecified if
         * this method is called more than once on the same instance.
         */
        BytesHashCode hash() {
            checkNotDone();
            done = true;
            return (bytes == digest.getDigestLength())
                    ? new BytesHashCode(digest.digest())
                    : new BytesHashCode(Arrays.copyOf(digest.digest(), bytes));
        }

        /**
         * Puts a byte into this hasher.
         * @param b A byte.
         * @return This instance.
         */
        MessageDigestHasher putByte(byte b) {
            update(b);
            return this;
        }

        /**
         * Puts an array of bytes into this hasher.
         * @param bytes A byte array.
         * @return This instance.
         */
        MessageDigestHasher putBytes(byte[] bytes) {
            if (bytes == null) throw new IllegalArgumentException("bytes must not be null");
            update(bytes);
            return this;
        }

        /**
         * Puts a chunk of an array of bytes into this sink. {@code bytes[off]} is the first byte written, {@code
         * bytes[off + len - 1]} is the last.
         * @param bytes a byte array
         * @param off   the start offset in the array
         * @param len   the number of bytes to write
         * @return This instance.
         * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > bytes.length} or {@code len < 0}
         */
        MessageDigestHasher putBytes(byte[] bytes, int off, int len) {
            if (off < 0 || (off + len) < off || (off + len) > bytes.length) throw new IndexOutOfBoundsException(
                    String.format("Bad args; start=%d, end=%d, size=%d", off, off + len, bytes.length));
            update(bytes, off, len);
            return this;
        }

        /**
         * Puts a short into this hasher.
         * @param s A short.
         * @return This instance.
         */
        MessageDigestHasher putShort(short s) {
            scratch.putShort(s);
            return update(Short.SIZE / Byte.SIZE);
        }

        /**
         * Puts an int into this hasher.
         * @param i An int.
         * @return This instance.
         */
        MessageDigestHasher putInt(int i) {
            scratch.putInt(i);
            return update(Integer.SIZE / Byte.SIZE);
        }

        /**
         * Puts a long into this hasher.
         * @param l A long.
         * @return This instance.
         */
        MessageDigestHasher putLong(long l) {
            scratch.putLong(l);
            return update(Long.SIZE / Byte.SIZE);
        }

        /**
         * Puts a char into this hasher.
         * @param c A char.
         * @return This instance.
         */
        MessageDigestHasher putChar(char c) {
            scratch.putChar(c);
            return update(Character.SIZE / Byte.SIZE);
        }

        /**
         * Equivalent to {@code putByte(b ? (byte) 1 : (byte) 0)}.
         */
        final MessageDigestHasher putBoolean(boolean b) {
            return putByte(b ? (byte) 1 : (byte) 0);
        }

        /**
         * Equivalent to {@code putLong(Double.doubleToRawLongBits(d))}.
         */
        final MessageDigestHasher putDouble(double d) {
            return putLong(Double.doubleToRawLongBits(d));
        }

        /**
         * Equivalent to {@code putInt(Float.floatToRawIntBits(f))}.
         */
        final MessageDigestHasher putFloat(float f) {
            return putInt(Float.floatToRawIntBits(f));
        }

        /**
         * Equivalent to processing each {@code char} value in the {@code CharSequence}, in order. The input must not be
         * updated while this method is in progress.
         */
        MessageDigestHasher putUnencodedChars(CharSequence charSequence) {
            for (int i = 0, len = charSequence.length(); i < len; i++) {
                putChar(charSequence.charAt(i));
            }
            return this;
        }

        /**
         * Equivalent to {@code putBytes(charSequence.toString().getBytes(charset))}.
         */
        MessageDigestHasher putString(CharSequence charSequence, Charset charset) {
            return putBytes(charSequence.toString().getBytes(charset));
        }
    }

    /**
     * An immutable hash code of arbitrary length, backed by bytes.
     */
    static class BytesHashCode {
        final byte[] bytes;

        BytesHashCode(byte[] bytes) {
            if (bytes == null) throw new IllegalArgumentException("bytes must not be null.");
            this.bytes = bytes;
        }

        /**
         * Returns the number of bits in this hash code; a positive multiple of 8.
         */
        int bits() {
            return bytes.length * 8;
        }

        /**
         * Returns the value of this hash code as a byte array. The caller may modify the byte array; changes to it will
         * <i>not</i> be reflected in this {@code HashCode} object or any other arrays returned by this method.
         */
        byte[] asBytes() {
            return bytes.clone();
        }

        /**
         * Returns the first four bytes of {@linkplain #asBytes() this hashcode's bytes}, converted to an {@code int}
         * value in little-endian order.
         * @throws IllegalStateException if {@code bits() < 32}
         */
        int asInt() {
            if (bytes.length < 4) throw new IllegalStateException(
                    String.format("HashCode#asInt() requires >= 4 bytes (it only has %d bytes).", bytes.length));
            return (bytes[0] & 0xFF)
                    | ((bytes[1] & 0xFF) << 8)
                    | ((bytes[2] & 0xFF) << 16)
                    | ((bytes[3] & 0xFF) << 24);
        }

        /**
         * Returns the first eight bytes of {@linkplain #asBytes() this hashcode's bytes}, converted to a {@code long}
         * value in little-endian order.
         * @throws IllegalStateException if {@code bits() < 64}
         */
        long asLong() {
            if (bytes.length < 8) throw new IllegalStateException(
                    String.format("HashCode#asLong() requires >= 8 bytes (it only has %d bytes).", bytes.length));
            return padToLong();
        }

        /**
         * If this hashcode has enough bits, returns {@code asLong()}, otherwise returns a {@code long} value with
         * {@code asBytes()} as the least-significant bytes and {@code 0x00} as the remaining most-significant bytes.
         */
        long padToLong() {
            long retVal = (bytes[0] & 0xFF);
            for (int i = 1; i < Math.min(bytes.length, 8); i++) {
                retVal |= (bytes[i] & 0xFFL) << (i * 8);
            }
            return retVal;
        }

        /**
         * Copies bytes from this hash code into {@code dest}.
         * @param dest      the byte array into which the hash code will be written
         * @param offset    the start offset in the data
         * @param maxLength the maximum number of bytes to write
         */
        void writeBytesToImpl(byte[] dest, int offset, int maxLength) {
            System.arraycopy(bytes, 0, dest, offset, maxLength);
        }

        /**
         * Returns whether this {@link BytesHashCode} and that {@link BytesHashCode} have the same value, given that
         * they have the same number of bits.
         */
        boolean equalsSameBits(BytesHashCode that) {
            // We don't use MessageDigest.isEqual() here because its contract does not guarantee constant-time
            // evaluation (no short-circuiting).
            if (this.bytes.length != that.asBytes().length) {
                return false;
            }

            boolean areEqual = true;
            for (int i = 0; i < this.bytes.length; i++) {
                areEqual &= (this.bytes[i] == that.asBytes()[i]);
            }
            return areEqual;
        }
    }

    /**
     * Serialized form of {@link SHA256HashFunction}.
     */
    private static final class SerializedForm implements Serializable {
        private final int bytes;

        private SerializedForm(int bytes) {
            this.bytes = bytes;
        }

        private Object readResolve() {
            return new SHA256HashFunction(bytes);
        }

        private static final long serialVersionUID = 0;
    }

    Object writeReplace() {
        return new SHA256HashFunction.SerializedForm(bytes);
    }
}
