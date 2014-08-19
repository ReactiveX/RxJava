/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

import rx.{Producer => JProducer}


trait Producer {

  // Java calls XXX, Scala receives XXX
  private[scala] val asJavaProducer: JProducer = new JProducer {
    def request(n: Long): Unit = {
      Producer.this.request(n)
    }
  }

  def request(n: Long): Unit = {}
}

object Producer {
  /**
   * Scala calls XXX; Java receives XXX.
   */
  private[scala] def apply[T](producer: JProducer): Producer = {
    new Producer {
      override val asJavaProducer = producer

      override def request(n: Long): Unit = {
        producer.request(n)
      }
    }
  }

  def apply[T](producer: Long => Unit): Producer = {
    // Java calls XXX; Scala receives XXX.
    Producer(new JProducer {
      override def request(n: Long): Unit = {
        producer(n)
      }
    })
  }
}
