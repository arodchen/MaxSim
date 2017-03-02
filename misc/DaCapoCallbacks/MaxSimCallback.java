/*
 * Copyright 2017 Andrey Rodchenko, School of Computer Science, The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

/**
 * MaxSim Callback.
 */
public class MaxSimCallback extends Callback {

  public MaxSimCallback(CommandLineArgs args) {
    super(args);
  }

  /* Immediately prior to start of the benchmark */
  public void start(String benchmark, boolean warmup) {
    if (!warmup) {
        System.setProperty("MaxSim.Command", "ROI_BEGIN()");
    }
    super.start(benchmark, warmup);
  };

  /* Immediately after the end of the benchmark */
  public void stop(boolean warmup) {
    super.stop(warmup);
    if (!warmup) {
        System.setProperty("MaxSim.Command", "ROI_END()");
    }
  };
}

