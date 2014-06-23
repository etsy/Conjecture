<?php

class Conjecture_BinaryClassifier {
    private $param = null;

    function __construct($param_vec) {
        $this->param = $param_vec;
    }

    public function dot($instance_vec) {
        return $this->param->dot($instance_vec);
    }

    public function predict($instance_vec) {
        $dot = $this->dot($instance_vec);
        $exd = exp($dot);
        return $exd / (1.0 + $exd);
    }

    public function getParams() {
        return $this->param->getParams();
    }

    public function explain($instance_vec, $n = 10) {
        $keys = array_intersect_key($this->param->getParams(), $instance_vec->getParams());
        $keys = array_map('abs', $keys);
        arsort($keys);
        $res = array_slice($keys, 0, (count($keys) < $n ? count($keys) : $n));
        foreach ($res as $k => $v) {
            $res[$k] = "$k(" . round($this->param->getParam($k), 2) . ")";
        }
        return implode(", ", $res);
    }
}
