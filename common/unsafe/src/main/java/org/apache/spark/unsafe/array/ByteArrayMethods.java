/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.unsafe.array;

import org.apache.spark.unsafe.Platform;

public class ByteArrayMethods {

  private ByteArrayMethods() {
    // Private constructor, since this class only contains static methods.
  }

  /** Returns the next number greater or equal num that is power of 2. */
  public static long nextPowerOf2(long num) {
    final long highBit = Long.highestOneBit(num);
    return (highBit == num) ? num : highBit << 1;
  }

  public static int roundNumberOfBytesToNearestWord(int numBytes) {
    int remainder = numBytes & 0x07;  // This is equivalent to `numBytes % 8`
    if (remainder == 0) {
      return numBytes;
    } else {
      return numBytes + (8 - remainder);
    }
  }

  /**
   * Optimized byte array equality check for byte arrays.
   * @return true if the arrays are equal, false otherwise
   */
  public static boolean arrayEquals(final Object leftBase, long leftOffset,
      final Object rightBase, long rightOffset, final long length) {
    // for the case that equals will fail in first few bytes itself, the overhead
    // of JNI call is too high
    /*
    if (leftBase == null && rightBase == null &&
        length >= Native.MIN_JNI_SIZE && Native.isLoaded()) {
      return Native.arrayEquals(leftOffset, rightOffset, length);
    }
    */
    long endOffset = leftOffset + length;
    // try to align at least one side
    if ((rightOffset & 0x7) != 0 && (leftOffset & 0x7) != 0) { // mod 8
      final long alignedOffset = Math.min(((leftOffset + 7) >>> 3) << 3, endOffset);
      if (Platform.unaligned()) {
        if (leftOffset <= (alignedOffset - 4)) {
          if (Platform.getInt(leftBase, leftOffset) !=
              Platform.getInt(rightBase, rightOffset)) {
            return false;
          }
          leftOffset += 4;
          rightOffset += 4;
        }
      }
      while (leftOffset < alignedOffset) {
        if (Platform.getByte(leftBase, leftOffset) !=
            Platform.getByte(rightBase, rightOffset)) {
          return false;
        }
        leftOffset++;
        rightOffset++;
      }
    }
    // for architectures that support unaligned accesses, chew it up 8 bytes at a time
    if (Platform.unaligned() || (((leftOffset & 0x7) == 0) && ((rightOffset & 0x7) == 0))) {
      endOffset -= 8;
      while (leftOffset <= endOffset) {
        if (Platform.getLong(leftBase, leftOffset) !=
            Platform.getLong(rightBase, rightOffset)) {
          return false;
        }
        leftOffset += 8;
        rightOffset += 8;
      }
      endOffset += 4;
      if (leftOffset <= endOffset) {
        if (Platform.getInt(leftBase, leftOffset) !=
            Platform.getInt(rightBase, rightOffset)) {
          return false;
        }
        leftOffset += 4;
        rightOffset += 4;
      }
      endOffset += 4;
    }
    // this will finish off the unaligned comparisons, or do the entire aligned
    // comparison whichever is needed.
    while (leftOffset < endOffset) {
      if (Platform.getByte(leftBase, leftOffset) !=
          Platform.getByte(rightBase, rightOffset)) {
        return false;
      }
      leftOffset++;
      rightOffset++;
    }
    return true;
  }
}
