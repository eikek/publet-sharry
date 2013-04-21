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

package org.eknet.publet.sharry

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.04.13 17:06
 */
object RandomId {
  private val counter = new AtomicInteger(0)
  private val chars = List('-') ::: ('a' to 'z').toList ::: ('A' to 'Z').toList ::: ('0' to '9').toList
  private val random = new SecureRandom()
  private val reseedInterval = 3000

  private def nextInt(n: Int) = {
    if (counter.incrementAndGet() > reseedInterval) {
      counter.set(0)
      random.setSeed(random.generateSeed(16))
    }
    random.nextInt(n)
  }

  def generatePassword(len: Int) = {
    val pw = for (i <- 1 to len) yield chars(nextInt(chars.size))
    pw.toArray
  }

  def generate(len: Int): String = new String(generatePassword(len))

  def generate(minLen: Int, maxLen: Int): String = {
    val len = if (minLen == maxLen) minLen else nextInt(maxLen-minLen) + minLen
    generate(len)
  }

  def stream(minLen: Int, maxLen: Int): Stream[String] =
    generate(minLen, maxLen) #:: stream(minLen, maxLen)

  def stream(len: Int): Stream[String] = generate(len) #:: stream(len)

}
