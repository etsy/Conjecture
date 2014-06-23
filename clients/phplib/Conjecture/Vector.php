<?php 

class Conjecture_Vector {

    protected $vector = null;

    function __construct($array = array()) {
        $this->vector = (array)$array;
    }

    public function dot($rhs) {
        $keys = array_intersect_key($this->vector, $rhs->vector);
        $res = 0.0;

        foreach ($keys as $key => $val) {
            $res += $this->vector[$key] * $rhs->vector[$key];
        }

        return $res;
    }

    public function getParams() {
        return $this->vector;
    }

    public function getParam($k) {
        if (array_key_exists($k, $this->vector)) {
            return $this->vector[$k];
        } else {
            return 0.0;
        }
    }
}
