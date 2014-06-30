<?php


class Conjecture_MulticlassLogisticRegressionClassifier extends Conjecture_MulticlassClassifier {

    private $param = null;

    public function predict($instance_vec) {
        $category_results = [];
        $total = 0;

        foreach ($this->param as $category => $classifier) {
            $prediction = exp($classifier->dot($instance_vec));
            $category_results[$category] = $prediction;
            $total += $prediction;
        }

        return array_map( function($prob) use ($total) {
                return $prob / $total;
        }, $category_results);
    }

}