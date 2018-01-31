/*
 * Copyright 2018 Nordstrom, Inc.
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
package com.nordstrom.xrpc.client.retry;

/*
 * Copyright 2017 Nordstrom, Inc.
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

import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryLoop {
  private static final RetrySleeper sleeper =
      new RetrySleeper() {
        @Override
        public void sleepFor(long time, TimeUnit unit) throws InterruptedException {
          unit.sleep(time);
        }
      };

  private final long startTimeMs = System.currentTimeMillis();
  private final RetryPolicy retryPolicy;
  private final AtomicReference<TracerDriver> tracer;
  private boolean isDone = false;
  private int retryCount = 0;

  public RetryLoop(RetryPolicy retryPolicy, AtomicReference<TracerDriver> tracer) {
    this.retryPolicy = retryPolicy;
    this.tracer = tracer;
  }

  public static RetrySleeper getDefaultRetrySleeper() {
    return sleeper;
  }

  public static boolean shouldRetry(int rc) {
    return true;
  }

  public static boolean isRetryException(Throwable exception) {
    if (exception instanceof ConnectException
        || exception instanceof ConnectTimeoutException
        || exception instanceof UnknownHostException) {
      return true;
    }
    return false;
  }

  public boolean shouldContinue() {
    return !isDone;
  }

  public void markComplete() {
    isDone = true;
  }

  public void takeException(Exception exception) throws Exception {
    boolean rethrow = true;
    if (isRetryException(exception)) {

      if (retryPolicy.allowRetry(retryCount++, System.currentTimeMillis() - startTimeMs, sleeper)) {
        tracer.get().addCount("retries-allowed", 1);
        rethrow = false;
      } else {
        tracer.get().addCount("retries-disallowed", 1);
      }
    }

    if (rethrow) {
      throw exception;
    }
  }
}
