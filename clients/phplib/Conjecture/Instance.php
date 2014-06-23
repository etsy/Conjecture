<?php

  /**
   * container class representing instances that are considered
   * as input to predictive models in Conjecture. Has a rich set
   * of adders and setters that mirrors the API of the java code,
   * https://github.etsycorp.com/Engineering/Conjecture
   */
class Conjecture_Instance extends Conjecture_Vector{

    private static $NAMESPACE_SEP = "___";

    private $id = null;
    private $label = null;


    public function __construct(array $vector = array()) {
        parent::__construct($vector);
    }

    public function getId() {
        return $this->id;
    }

    public function setId($id) {
        $this->id = $id;
        return $this;
    }

    public function put($key, $value = 1.0) {
        $this->vector[$key] = $value;
    }

    public function update($key, $value = 1.0) {
        if (array_key_exists($key, $this->vector)) {
            $this->vector[$key] = $this->vector[$key] + $value;
        } else {
            $this->vector[$key] = $value;
        }
        return $this;
    }

    //some methods to mirror java maps that this class mirrors

    public function putAll(array $vector) {
        foreach ($vector as $key => $value) {
            $this->put($key, $value);
        }
    }

    public function containsKey($key) {
        return array_key_exists($key, $this->vector);
    }

    public function containsValue($key) {
        return in_array($key, $this->vector);
    }


    public function keySet() {
        return array_keys($this->vector);
    }

    public function values() {
        return array_values($this->vector);
    }

    public function size() {
        return count($this->vector);
    }

    public function isEmpty() {
        return empty($this->vector);
    }

    public function remove($key) {
        unset($this->vector[$key]);
    }

    public function toString() {
        return json_encode($this->vector);
    }

    public function addTerm($term, $featureWeight = 1.0, $namespace = "") {
        $key = $namespace == "" ? $term : $namespace . self::$NAMESPACE_SEP . $term;
        $this->update($key, $featureWeight);
        return $this;
    }

    public function addTerms(array $terms, $featureWeight = 1.0, $namespace = "") {
        foreach ($terms as $term) {
            $this->addTerm($term, $featureWeight, $namespace);
        }
        return $this;
    }

    public function addNumericArray(array $numberValues, $namespace = "") {
        for ($i = 0; $i < count($numberValues); $i++ ) {
            $this->addTerm((string)$i, $numberValues[$i], $namespace);
        }
        return $this;
    }

}
