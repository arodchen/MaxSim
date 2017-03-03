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

