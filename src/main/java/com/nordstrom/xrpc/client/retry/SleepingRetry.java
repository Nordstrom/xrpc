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

import java.util.concurrent.TimeUnit;

abstract class SleepingRetry implements RetryPolicy {
  private final int n;

  protected SleepingRetry(int n) {
    this.n = n;
  }

  // made public for testing
  public int getN() {
    return n;
  }

  public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
    if (retryCount < n) {
      try {
        sleeper.sleepFor(getSleepTimeMs(retryCount, elapsedTimeMs), TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
      return true;
    }
    return false;
  }

  protected abstract int getSleepTimeMs(int retryCount, long elapsedTimeMs);
}
