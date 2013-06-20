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

package org.eknet.publet.sharry.lib

import java.nio.file.{StandardOpenOption, Path}
import java.io.{OutputStream, InputStream}
import javax.crypto.{SecretKeyFactory, CipherInputStream, Cipher, CipherOutputStream}
import javax.crypto.spec.{PBEKeySpec, IvParameterSpec, SecretKeySpec}
import com.google.common.io.ByteStreams
import com.google.common.hash.Hashing
import java.security.SecureRandom

/**
 * Utility methods for symmetric encryption using AES.
 *
 * The encryption uses AES in CBC mode. The IV is created randomly and prepended
 * to the cipher text. The first 16 bytes of the encrypted text is the IV.
 *
 * The password can be any char array. It is first run trough a message digest
 * and the correct number of bytes are used for the AES key.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 20:49
 */
object SymmetricCrypt {

  def hashString(name: CharSequence) = Hashing.sha256().hashString(name).asBytes()

  def createKey(password: String, salt: Array[Byte]) = {
    val sfac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val ks = new PBEKeySpec(password.toCharArray, salt, math.pow(2, 16).toInt, 256)
    val key = sfac.generateSecret(ks)
    key.getEncoded
  }

  /**
   * Encrypts the file at the given path and writes it to the specifed `target`
   * path. The target path must not exist.
   *
   * @param source
   * @param target
   * @param password
   */
  def encrypt(source: Path, target: Path, password: String, salt: Array[Byte]) {
    val key = createKey(password, salt)
    val in = source.getInput()
    val out = target.getOutput(StandardOpenOption.CREATE_NEW)
    in.exec {
      out.exec {
        encrypt(in, out, key)
      }
    }
  }

  /**
   * Decrypts the file at the given `source` path and writes it to the
   * given `target` path. The `target` path must not exist.
   *
   * @param source
   * @param target
   * @param password
   */
  def decrypt(source: Path, target: Path, password: String, salt: Array[Byte]) {
    val key = createKey(password, salt)
    val in = source.getInput()
    val out = target.getOutput(StandardOpenOption.CREATE_NEW)
    in.exec {
      out.exec {
        decrypt(in, out, key)
      }
    }
  }

  def decrypt(source: Path, target: OutputStream, password: String, salt: Array[Byte]) {
    val key = createKey(password, salt)
    val in = source.getInput()
    in.exec {
      decrypt(in, target, key)
    }
  }

  /**
   * Encryptes the bytes from the given input stream and writes them into the given output stream.
   *
   * Note that the outputsteam is closed by this method, since it is required to close the concrete
   * [[javax.crypto.CipherOutputStream]]. Otherwise the final block is not encrypted properly. The
   * input stream, however, is not closed by this method.
   *
   * The IV is created using [[java.security.SecureRandom]] and written as the first 16 bytes into
   * the output stream.
   *
   * @param source
   * @param target
   * @param key
   */
  def encrypt(source: InputStream, target: OutputStream, key: Array[Byte]) = {
    val ivbytes = new Array[Byte](16)
    new SecureRandom().nextBytes(ivbytes)
    val iv = new IvParameterSpec(ivbytes)
    target.write(ivbytes)
    val cipher = createCipher(key, iv, Cipher.ENCRYPT_MODE)
    val cout = new CipherOutputStream(target, cipher)
    val result = ByteStreams.copy(source, cout)
    cout.close()
    result
  }

  /**
   * Decrypts the bytes from the input stream and writes them into the given outputstream.
   *
   * Note that the input stream is closed by this method, as it is necessary to call it
   * on the concrete [[javax.crypto.CipherInputStream]], otherwise the last block is not
   * decrypted. The given outputstream, however, is not closed by this method.
   *
   * It is assumed that the IV is located at the first 16 bytes in the input stream.
   *
   * @param source
   * @param target
   * @param key
   */
  def decrypt(source: InputStream, target: OutputStream, key: Array[Byte]) = {
    val ivbytes = new Array[Byte](16)
    source.read(ivbytes)
    val iv = new IvParameterSpec(ivbytes)
    val cipher = createCipher(key, iv, Cipher.DECRYPT_MODE)
    val cin = new CipherInputStream(source, cipher)
    val result = ByteStreams.copy(cin, target)
    cin.close()
    result
  }

  def createCipher(key: Array[Byte], iv: IvParameterSpec, cipherMode: Int) = {
    require(key.length > 0, "A key must be provided.")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val keyspec = new SecretKeySpec(key, "AES")
    cipher.init(cipherMode, keyspec, iv)
    cipher
  }
}
