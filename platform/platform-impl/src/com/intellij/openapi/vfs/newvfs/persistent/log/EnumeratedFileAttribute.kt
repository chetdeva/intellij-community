// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vfs.newvfs.persistent.log

/**
 * EnumeratedFileAttribute stores information about a FileAttribute in a single ULong value and
 * accounts only enumeratedId for equality check
 */
class EnumeratedFileAttribute(val compressedInfo: ULong) {
  constructor(enumeratedId: Int, version: Int, fixedSize: Boolean) : this(compressFields(enumeratedId, version, fixedSize))

  val version: Int get() = compressedInfo.getField(VERSION_OFFSET, VERSION_BITS)
  val enumeratedId: Int get() = compressedInfo.getField(ENUMERATED_ID_OFFSET, ENUMERATED_ID_BITS)
  val fixedSize: Boolean get() = compressedInfo.getField(FIXED_SIZE_OFFSET, FIXED_SIZE_BITS) != 0

  override fun toString(): String {
    return "EnumeratedFileAttribute(enumId=$enumeratedId;version=$version;fixedSize=$fixedSize)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EnumeratedFileAttribute

    return enumeratedId == other.enumeratedId
  }

  override fun hashCode(): Int {
    return enumeratedId
  }

  companion object {
    const val SIZE_BYTES = ULong.SIZE_BYTES

    private const val VERSION_OFFSET = 0
    private const val VERSION_BITS = 32
    private const val ENUMERATED_ID_OFFSET = VERSION_OFFSET + VERSION_BITS
    private const val ENUMERATED_ID_BITS = 31
    private const val FIXED_SIZE_OFFSET = ENUMERATED_ID_OFFSET + ENUMERATED_ID_BITS
    private const val FIXED_SIZE_BITS = 1

    // first `bitsCount` least significant bits are 1, 0 <= bitsCounts _<_ 64
    private fun bitsMask(bitsCount: Int): ULong = (1.toULong() shl bitsCount).dec()

    private fun compressFields(enumeratedId: Int, version: Int, fixedSize: Boolean): ULong =
      0.toULong()
        .setField(enumeratedId, ENUMERATED_ID_OFFSET, ENUMERATED_ID_BITS)
        .setField(version, VERSION_OFFSET, VERSION_BITS)
        .setField(if (fixedSize) 1 else 0, FIXED_SIZE_OFFSET, FIXED_SIZE_BITS)

    private fun ULong.setField(value: Int, offset: Int, bits: Int): ULong {
      val uValue = value.toUInt() // preventing the sign bit extension
      assert(offset + bits <= ULong.SIZE_BITS)
      assert(uValue <= bitsMask(bits))
      return this or (uValue.toULong() shl offset)
    }

    private fun ULong.getField(offset: Int, bits: Int): Int =
      ((this shr offset) and bitsMask(bits))
        .toUInt().toInt() // intermediate conversion matters
  }
}