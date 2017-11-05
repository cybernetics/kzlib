/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2011 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.soywiz.kzlib

class Deflater : ZStream {

	private var finished = false

	constructor() : super()

	constructor(level: Int, nowrap: Boolean) : this(level, MAX_WBITS, nowrap)

	constructor(level: Int, bits: Int = MAX_WBITS, nowrap: Boolean = false) : super() {
		val ret = init(level, bits, nowrap)
		if (ret != Z_OK)
			throw GZIPException(ret.toString() + ": " + msg)
	}

	constructor(level: Int, bits: Int, memlevel: Int, wrapperType: JZlib.WrapperType) : super() {
		val ret = init(level, bits, memlevel, wrapperType)
		if (ret != Z_OK)
			throw GZIPException(ret.toString() + ": " + msg)
	}

	constructor(level: Int, bits: Int, memlevel: Int) : super() {
		val ret = init(level, bits, memlevel)
		if (ret != Z_OK)
			throw GZIPException(ret.toString() + ": " + msg)
	}

	fun init(level: Int, nowrap: Boolean): Int {
		return init(level, MAX_WBITS, nowrap)
	}

	fun init(level: Int, bits: Int, memlevel: Int, wrapperType: JZlib.WrapperType): Int {
		var bits = bits
		if (bits < 9 || bits > 15) return Z_STREAM_ERROR
		when (wrapperType) {
			JZlib.W_NONE -> bits *= -1
			JZlib.W_GZIP -> bits += 16
			JZlib.W_ANY -> return Z_STREAM_ERROR
			JZlib.W_ZLIB -> Unit
			else -> Unit
		}
		return init(level, bits, memlevel)
	}

	fun init(level: Int, bits: Int, memlevel: Int): Int {
		finished = false
		dstate = Deflate(this)
		return dstate!!.deflateInit(level, bits, memlevel)
	}

	fun init(level: Int, bits: Int = MAX_WBITS, nowrap: Boolean = false): Int {
		finished = false
		dstate = Deflate(this)
		return dstate!!.deflateInit(level, if (nowrap) -bits else bits)
	}

	override fun deflate(flush: Int): Int {
		if (dstate == null) return Z_STREAM_ERROR
		val ret = dstate!!.deflate(flush)
		if (ret == Z_STREAM_END)
			finished = true
		return ret
	}

	override fun end(): Int {
		finished = true
		if (dstate == null) return Z_STREAM_ERROR
		val ret = dstate!!.deflateEnd()
		dstate = null
		free()
		return ret
	}

	fun params(level: Int, strategy: Int): Int {
		return if (dstate == null) Z_STREAM_ERROR else dstate!!.deflateParams(level, strategy)
	}

	fun setDictionary(dictionary: ByteArray, dictIndex: Int, dictLength: Int): Int {
		return if (dstate == null) Z_STREAM_ERROR else dstate!!.deflateSetDictionary(dictionary, dictIndex, dictLength)
	}

	override fun finished(): Boolean {
		return finished
	}

	fun copy(src: Deflater): Int {
		this.finished = src.finished
		return Deflate.deflateCopy(this, src)
	}

	companion object {

		private val MAX_WBITS = 15        // 32K LZ77 window
		private val DEF_WBITS = MAX_WBITS

		private val Z_NO_FLUSH = 0
		private val Z_PARTIAL_FLUSH = 1
		private val Z_SYNC_FLUSH = 2
		private val Z_FULL_FLUSH = 3
		private val Z_FINISH = 4

		private val MAX_MEM_LEVEL = 9

		private val Z_OK = 0
		private val Z_STREAM_END = 1
		private val Z_NEED_DICT = 2
		private val Z_ERRNO = -1
		private val Z_STREAM_ERROR = -2
		private val Z_DATA_ERROR = -3
		private val Z_MEM_ERROR = -4
		private val Z_BUF_ERROR = -5
		private val Z_VERSION_ERROR = -6
	}
}
