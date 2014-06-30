<?php

class Conjecture_Finder {

    private $config = null;

    public function __construct(Conjecture_Config $config) {
        $this->config = $config;
    }


    /**
     * Loads a model local to a user's vm.
     */
    public function getLocalModel($local_file_path) {
        $model = json_decode(self::parseFile($local_file_path));
        $cv = new Conjecture_Vector($model->param->vector);
        $binary_classifier = new Conjecture_BinaryClassifier($cv);
        return $binary_classifier;
    }

    /**
     * Decode model json at a given filepath.
     */
    static function parseFile($fp) {
        $res = file($fp);
        if ($res) {
            $res = implode("", $res);
            $res = stripslashes($res);
            return $res;
        } else {
            throw new Exception("model file not found: $fp");
        }
    }

    private function getLatestModelJsonForProblem($file_name) {
        if ($this->config->useDummyConjectureModel()) {
            return self::getDummyModel();
        }

        $fp = $this->config->getConjectureModelPath() . "/" . $file_name;
        return self::parseFile($fp);
    }

    public function getLatestModelForProblem($file_name) {
        $json = $this->getLatestModelJsonForProblem($file_name);
        return json_decode($json);
    }

    public function getLatestBinaryClassificationVectorForProblem($file_name) {
        $model = $this->getLatestModelForProblem($file_name);
        return new Conjecture_Vector($model->param->vector);
    }

    public function getLatestBinaryClassifierForProblem($file_name) {
        return new Conjecture_BinaryClassifier($this->getLatestBinaryClassificationVectorForProblem($file_name));
    }

    public function getOneVsAllClassifier($file_name) {
        $model_array = $this->getLatestModelForProblem($file_name);

        foreach ($model_array as $cat => $params) {
            $category_params[$cat] = new Conjecture_BinaryClassifier(new Conjecture_Vector($params));
        }

        return new Conjecture_MulticlassOneVsAllClassifier($category_params);
    }


    public function getMulticlassClassifier($file_name) {
        $model_array = $this->getLatestModelForProblem($file_name);

        foreach ($model_array["param"] as $cat => $category_model) {
            $categeory_params = $category_model["vector"];
            $category_params[$cat] = new Conjecture_BinaryClassifier(new Conjecture_Vector($category_params));
        }

        return new Conjecture_MulticlassOneVsAllClassifier($category_params);
    }


    static function build(Conjecture_Config $config) {
        return new Conjecture_Finder($config);
    }

    /**
     * Creates and returns a JSON dummy model with no vectors
     * used for development settings where "real" JSON models
     * may not be present
     */
    private static function getDummyModel() {

        $dummy_model = array("param" => array(
            "vector" => array(),
            "modelType" => "dummy",
            "regularizationWeights" => array(),
            "epoch" => 1,
            "period" => 1,
            "truncationUpdate" => 0,
            "truncationThreshold" => 0,
            "initialLearningRate" => .1,
            "useExponentialLearningRate" => false,
            "exponentialLearningRate" => 1.0,
            "examplesPerEpoch" => 1,
        ));
        return json_encode($dummy_model);
    }
}
