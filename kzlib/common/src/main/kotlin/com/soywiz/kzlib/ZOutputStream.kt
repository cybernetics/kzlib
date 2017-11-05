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

package com.soywiz.kzlib

/**
 * ZOutputStream
 *
 * deprecated  use DeflaterOutputStream or InflaterInputStream
 */
//@Deprecated
class ZOutputStream : FilterOutputStream {

	protected var bufsize = 512
	var flushMode = JZlib.Z_NO_FLUSH
	protected var buf = ByteArray(bufsize)
	protected var compress: Boolean = false

	private var end = false

	private var dos: DeflaterOutputStream? = null
	private var inflater: Inflater? = null

	private val buf1 = ByteArray(1)

	val totalIn: Double get() = if (compress) dos!!.total_in else inflater!!.total_in
	val totalOut: Double get() = if (compress) dos!!.total_out else inflater!!.total_out

	constructor(out: OutputStream) : super(out) {
		this.out = out
		inflater = Inflater()
		inflater!!.init()
		compress = false
	}

	constructor(out: OutputStream, level: Int, nowrap: Boolean = false) : super(out) {
		this.out = out
		val deflater = Deflater(level, nowrap)
		dos = DeflaterOutputStream(out, deflater)
		compress = true
	}

	override fun write(b: Int) {
		buf1[0] = b.toByte()
		write(buf1, 0, 1)
	}

	override fun write(b: ByteArray, off: Int, len: Int) {
		if (len == 0) return
		if (compress) {
			dos!!.write(b, off, len)
		} else {
			inflater!!.setInput(b, off, len, true)
			var err = JZlib.Z_OK
			while (inflater!!.avail_in > 0) {
				inflater!!.setOutput(buf, 0, buf.size)
				err = inflater!!.inflate(flushMode)
				if (inflater!!.next_out_index > 0)
					out!!.write(buf, 0, inflater!!.next_out_index)
				if (err != JZlib.Z_OK)
					break
			}
			if (err != JZlib.Z_OK)
				throw ZStreamException("inflating: " + inflater!!.msg)
			return
		}
	}

	fun finish() {
		val err: Int
		if (compress) {
			val tmp = flushMode
			var flush = JZlib.Z_FINISH
			try {
				write(byteArrayOf(), 0, 0)
			} finally {
				flush = tmp
			}
		} else {
			dos!!.finish()
		}
		flush()
	}

	fun end() {
		if (end) return
		if (compress) {
			try {
				dos!!.finish()
			} catch (e: Exception) {
			}

		} else {
			inflater!!.end()
		}
		end = true
	}

	override fun close() {
		try {
			try {
				finish()
			} catch (ignored: Exception) {
			}
		} finally {
			end()
			out!!.close()
		}
	}

	override fun flush() {
		out!!.flush()
	}

}
