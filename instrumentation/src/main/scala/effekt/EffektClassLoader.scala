package effekt

import scala.collection.mutable

/**
 * A simple `ClassLoader` that looks-up the available classes in a standard map.
 * Based on the OPAL class loader.
 *   https://bitbucket.org/delors/opal/src/HEAD/OPAL/common/src/main/scala/org/opalj/util/InMemoryClassLoader.scala?at=develop&fileviewer=file-view-default
 *
 *
 * ORIGINAL LICENSE (OPAL Project)
 * -------------------------------
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
class EffektClassLoader(
        val db: Map[String, Array[Byte]],
        parent:      ClassLoader
) extends ClassLoader(parent) {

    private val classCache = mutable.HashMap.empty[String, Class[_]]

    private def loadOrDefineInstrumentedClass(name: String): Class[_] = {
      classCache.getOrElseUpdate(name, db.get(name) match {
          case Some(data) ⇒ defineClass(name, data, 0, data.length)
          case None       ⇒ throw new ClassNotFoundException(name)
      })
    }

    @throws[ClassNotFoundException]
    override def findClass(name: String): Class[_] = {
      if (!db.contains(name)) return super.findClass(name)

      loadOrDefineInstrumentedClass(name)
    }

    @throws[ClassNotFoundException]
    override def loadClass(name: String, resolve: Boolean): Class[_] = {
      if (!db.contains(name)) return super.loadClass(name, resolve)

      loadOrDefineInstrumentedClass(name)
    }
}
