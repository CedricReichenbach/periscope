package info.magnolia.vaadin.periscope.order;

import info.magnolia.vaadin.periscope.result.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.learning.config.Nesterovs;

public class NeuralNetworkManager {

    private final MultiLayerNetwork network;

    private static final int ASCII_CHARS = 128;
    private static final int INPUT_DIGITS = 20;
    private static final int INPUT_CHANNELS = INPUT_DIGITS * ASCII_CHARS;

    private final OutputLayer outputLayer;
    private final List<String> resultIds = new ArrayList<>();

    public NeuralNetworkManager() {
        this.outputLayer = new OutputLayer.Builder()
                // FIXME: Do this dynamically
                .nOut(1000)
                .activation(Activation.SOFTMAX)
                .build();

        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .regularization(true).l2(1e-5)
                .weightInit(WeightInit.RELU)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .activation(Activation.TANH)
                .learningRate(0.02)
                .updater(new Nesterovs(0.4))
                .list()
                .layer(0, new ConvolutionLayer.Builder(ASCII_CHARS, 3)
                        .nIn(1)
                        .nOut(500)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nOut(200)
                        .build())
                .layer(2, new DenseLayer.Builder()
                        .nOut(100)
                        .build())
                .layer(3, outputLayer)
                .setInputType(InputType.convolutionalFlat(ASCII_CHARS, INPUT_DIGITS, 1))
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

    /**
     * Provide data to neural network.
     * @param query which query was used to generate the result.
     * @param result is what user had selected with the given query.
     */
    public void train(String query, Result result) {
        this.network.fit(inputToArray(query), outputToArray(result.getId()));
    }

    /**
     * Sorts the results based on the query of the user.
     * Takes into account what neural network is suggesting and does ordering according to.
     */
    public void sort(String query, List<Result> results) {
        INDArray resultArray = this.network.output(inputToArray(query));
        List<String> sortedIds = outputArrayToResults(resultArray);
        results.sort(Comparator.comparingInt(a -> sortedIds.indexOf(a.getId())));
    }

    /**
     * Encode a string into a float array. Each character is represented by a 128-length subarray where one entry at its
     * corresponding ascii code position is 1 and everything else 0.
     */
    private INDArray inputToArray(String query) {
        final String asciiQuery = StringUtils.stripAccents(query);

        float[] chars = new float[INPUT_CHANNELS];
        IntStream.range(0, INPUT_DIGITS).forEach(i -> {
            if (asciiQuery.length() <= i) return;

            final int asciiCode = asciiQuery.charAt(i) % ASCII_CHARS;
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
