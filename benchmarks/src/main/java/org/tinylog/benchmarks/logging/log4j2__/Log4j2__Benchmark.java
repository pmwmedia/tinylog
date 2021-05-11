/*
 * Copyright 2018 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.tinylog.benchmarks.logging.log4j2__;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.tinylog.benchmarks.logging.AbstractBenchmark;

/**
 * Benchmark for Apache Log4j 2.
 */
public class Log4j2__Benchmark extends AbstractBenchmark<LifeCycle> {

	/** */
	public Log4j2__Benchmark() {
	}

	@Benchmark
	@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
	@Override
	public void discard(final LifeCycle lifeCycle) {
		lifeCycle.getLogger().debug("Hello {}!", MAGIC_NUMBER);
	}

	@Benchmark
	@BenchmarkMode(Mode.SingleShotTime)
	@Override
	public void output(final LifeCycle lifeCycle) throws IOException, InterruptedException {
		Logger logger = lifeCycle.getLogger();
		for (int i = 0; i < LOG_ENTRIES; ++i) {
			logger.info("Hello {}!", MAGIC_NUMBER);
		}

		lifeCycle.waitForWriting();
	}

	@Benchmark
	@BenchmarkMode(Mode.SampleTime)
	@Override
	public void outputSingle(final LifeCycle lifeCycle) throws IOException, InterruptedException {
		lifeCycle.getLogger().info("Hello {}!", MAGIC_NUMBER);
	}

}
