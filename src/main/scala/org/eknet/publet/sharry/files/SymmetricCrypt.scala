/*
 * Copyright 2013 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eknet.publet.sharry.files

import java.nio.file.{StandardOpenOption, Path}
import java.io.{OutputStream, InputStream}
import javax.crypto.{CipherInputStream, Cipher, CipherOutputStream}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import com.google.common.io.ByteStreams
import com.google.common.hash.Hashing

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 20:49
 */
object SymmetricCrypt {

  def hashString(name: CharSequence) = Hashing.sha512().hashString(name).asBytes()

  def encrypt(source: Path, target: Path, password: Array[Char]) {
    val key = hashString(password)
    val iv = key.take(16)
    val in = source.getInput()
    val out = target.getOutput(StandardOpenOption.CREATE_NEW)
    in.exec {
      out.exec {
        encrypt(in, out, key, iv)
      }
    }
  }

  def decrypt(source: Path, target: Path, password: Array[Char]) {
    val key = hashString(password)
    val iv = key.take(16)
    val in = source.getInput()
    val out = target.getOutput(StandardOpenOption.CREATE_NEW)
    in.exec {
      out.exec {
        decrypt(in, out, key, iv)
      }
    }
  }

  def decrypt(source: Path, target: OutputStream, password: Array[Char]) {
    val key = hashString(password)
    val iv = key.take(16)
    val in = source.getInput()
    in.exec {
      decrypt(in, target, key, iv)
    }
  }

  def encrypt(source: InputStream, target: OutputStream, key: Array[Byte], iv: Array[Byte]) = {
    val cipher = createCipher(key, iv)
    val cout = new CipherOutputStream(target, cipher)
    ByteStreams.copy(source, cout)
  }

  def decrypt(source: InputStream, target: OutputStream, key: Array[Byte], iv: Array[Byte]) = {
    val cipher = createCipher(key, iv)
    val cin = new CipherInputStream(source, cipher)
    ByteStreams.copy(cin, target)
  }

  def createCipher(key: Array[Byte], iv: Array[Byte]) = {
    require(key.length > 0, "A key must be provided.")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    val keyspec = new SecretKeySpec(key, "AES")
    if (iv.length > 0) {
      cipher.init(Cipher.ENCRYPT_MODE, keyspec, new IvParameterSpec(iv))
    } else {
      cipher.init(Cipher.ENCRYPT_MODE, keyspec)
    }
    cipher
  }
}
