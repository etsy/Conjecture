<?php


class Conjecture_MulticlassOneVsAllClassifier {

    private $param = null;

    /**
     * $param is an array that maps category to a Conjecture_BinaryClassifier
     * that represents that class
     */
    function __construct($param) {
        $this->param = $param;
    }

    public function predict($instance_vec) {
        $category_results = [];
        $total = 0;

        foreach ($this->param as $category => $classifier) {
            $prediction = $classifier->predict($instance_vec);
            $category_results[$category] = $prediction;
            $total += $prediction;
        }

        return array_map( function($prob) use ($total) {
                return $prob / $total;
        }, $category_results);
    }

    public function getParams() {
        $out_params = [];

        foreach ($this->param as $category => $classifier) {
            $out_params[$category] = $classifier->getParams();
        }

        return $out_params;
    }

    public function explain($instance_vec, $n = 10) {
        $explains = [];

        foreach ($this->param as $category => $classifier) {
            $explains[$category] = $classifier->explain($instance_vec, $n);
        }

        return implode(", ", $explains);
    }

}