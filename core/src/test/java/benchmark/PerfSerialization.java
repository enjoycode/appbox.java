package benchmark;

import java.util.concurrent.TimeUnit;

import appbox.serialization.BytesOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import testutils.TestHelper;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class PerfSerialization {

    @Benchmark
    public void perfSerialization() {
        var model = TestHelper.makeEntityModel();

        //serialize
        var output1 = new BytesOutputStream(200);
        TestHelper.serializeTo(model, output1);
        //deserialize
        //var input    = output1.copyToInput();
        //var outModel = (EntityModel) deserializeFrom(input);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PerfSerialization.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

}
