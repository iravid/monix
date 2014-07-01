/*
 * Copyright (c) 2014 by its authors. Some rights reserved. 
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
 
package monifu.reactive

import monifu.reactive.observers.SynchronousObserver
import monifu.reactive.streams.{ObserverAsSubscriber, SubscriberAsObserver, SynchronousObserverAsSubscriber}
import org.reactivestreams.Subscriber

import scala.concurrent.{ExecutionContext, Future}

/**
 * The Observer from the Rx pattern is the trio of callbacks that
 * get subscribed to an Observable for receiving events.
 *
 * The events received must follow the Rx grammar, which is:
 *      onNext *   (onComplete | onError)?
 *
 * That means an Observer can receive zero or multiple events, the stream
 * ending either in one or zero `onComplete` or `onError` (just one, not both),
 * and after onComplete or onError, a well behaved Observable implementation
 * shouldn't send any more onNext events.
 */
trait Observer[-T] {
  def onNext(elem: T): Future[Ack]

  def onError(ex: Throwable): Unit

  def onComplete(): Unit
}

object Observer {
  /**
   * Given a [[Subscriber]] as defined by the [[http://www.reactive-streams.org/ Reactive Streams]]
   * specification, it builds an [[Observer]] instance compliant with the Monifu Rx implementation.
   */
  def from[T](subscriber: Subscriber[T])(implicit ec: ExecutionContext): Observer[T] = {
    SubscriberAsObserver(subscriber)
  }

  /**
   * Transforms the source [[Observer]] into a [[Subscriber]] instance as defined by the
   * [[http://www.reactive-streams.org/ Reactive Streams]] specification.
   */
  def asSubscriber[T](observer: Observer[T], requestSize: Int = 128)(implicit ec: ExecutionContext): Subscriber[T] = {
    observer match {
      case sync: SynchronousObserver[_] =>
        val inst = sync.asInstanceOf[SynchronousObserver[T]]
        SynchronousObserverAsSubscriber(inst, requestSize)
      case async =>
        ObserverAsSubscriber(async, requestSize)
    }
  }

  /**
   * Implicit conversion from [[Observer]] to `org.reactivestreams.Subscriber`.
   */
  def ObserverIsSubscriber[T](source: Observer[T])(implicit ec: ExecutionContext): Subscriber[T] =
    Observer.asSubscriber(source)
}