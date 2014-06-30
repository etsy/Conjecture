<?php


class Conjecture_MulticlassClassifier {

    private $param = null;

    /**
     * each param is a Conjecture_Vector
     */
    function __construct($param) {
        $this->param = $param;
    }

    public function predict($instance_vec) {
        $category_results = [];
        $total = 0;

        foreach ($this->param as $category => $classifier) {
            $prediction = $classifier->dot($instance_vec);
            $category_results[$category] = $prediction;
            $total += $prediction;
        }

        return array_map( function($prob) use ($total) {
                return $prob / $total;
        }, $category_results);
    }

    public function getParams() {
        return $this->param;
    }

    public function explain($instance_vec, $n = 10) {
        $explains = [];

        foreach ($this->param as $category => $category_model) {
            $explains[$category] = $this->categoryExplain($instance_vec, $category_model, $n);
        }

        return implode(", ", $explains);
    }


    private function categoryExplain($instance_vec, $category_model, $n = 10) {

        $keys = array_intersect_key($category_model->getParams(), $instance_vec->getParams());
        $keys = array_map('abs', $keys);
        arsort($keys);
        $res = array_slice($keys, 0, (count($keys) < $n ? count($keys) : $n));

        foreach ($res as $k => $v) {
            $res[$k] = "$k(" . round($category_model->getParams($k), 2) . ")";
        }

        return implode(", ", $res);
    }
}