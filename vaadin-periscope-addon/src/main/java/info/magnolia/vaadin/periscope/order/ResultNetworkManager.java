package info.magnolia.vaadin.periscope.order;

import info.magnolia.vaadin.periscope.result.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.learning.config.Nesterovs;

public class ResultNetworkManager {

    private final MultiLayerNetwork network;

    private static final int ASCII_CHARS = 128;
    private static final int INPUT_DIGITS = 20;
    private static final int INPUT_CHANNELS = INPUT_DIGITS * ASCII_CHARS;

    final OutputLayer outputLayer;
    private List<String> resultIds = new ArrayList<>();

    public ResultNetworkManager() {
        outputLayer = new OutputLayer.Builder()
                // FIXME: Do this dynamically
                .nOut(1000)
                .activation(Activation.SOFTMAX)
                .build();

        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .iterations(1)
                .regularization(true).l2(1e-4)
                .weightInit(WeightInit.RELU)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .activation(Activation.RELU)
                .learningRate(0.05)
                .updater(new Nesterovs(0.6))
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(INPUT_CHANNELS)
                        .nOut(5000)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nOut(2000)
                        .build())
                .layer(2, outputLayer)
                .setInputType(InputType.feedForward(INPUT_CHANNELS))
                .backprop(true)
                .pretrain(false)
                .build();
        this.network = new MultiLayerNetwork(configuration);
        this.network.init();
    }

    public void addResults(Collection<Result> results) {
        results.stream().map(Result::getId).forEach(id -> {
            if (!resultIds.contains(id)) {
                resultIds.add(id);
            }
        });

        // FIXME: Uncomment and fix
//        outputLayer.setNOut(resultIds.size());
//        network.init();
    }

    public void train(String query, Result result) {
        this.network.fit(inputToArray(query), outputToArray(result.getId()));
    }

    public void sort(String query, List<Result> results) {
        INDArray resultArray = this.network.output(inputToArray(query));
        List<String> sortedIds = outputArrayToResults(resultArray);
        results.sort((a, b) -> sortedIds.indexOf(a.getId()) - sortedIds.indexOf(b.getId()));
    }

    /**
     * Encode a string into a float array. Each character is represented by a 128-length subarray where one entry at its
     * corresponding ascii code position is 1 and everything else 0.
     */
    private INDArray inputToArray(String query) {
        float[] chars = new float[INPUT_CHANNELS];
        IntStream.range(0, INPUT_DIGITS).forEach(i -> {
            if (query.length() <= i) return;

            final int asciiCode = query.charAt(i);
            chars[i * ASCII_CHARS + asciiCode] = 1;
        });
        return new NDArray(chars);
    }

    private INDArray outputToArray(String resultId) {
        float[] nodes = new float[outputLayer.getNOut()];

        final int resultIndex = resultIds.indexOf(resultId);
        nodes[resultIndex] = 1;

        return new NDArray(nodes);
    }

    private List<String> outputArrayToResults(INDArray resultArray) {
        final List<String> resultVals = new ArrayList<>(resultIds);
        resultVals.sort((a, b) -> Math.round(Math.signum(resultArray.getFloat(resultIds.indexOf(b)) - resultArray.getFloat(resultIds.indexOf(a)))));
        return resultVals;
    }
}
