<?php

// This is bascially an exact replica of com.etsy.conjecture.text.Text
class Conjecture_Text {

    private $input = null;

    static function build($text) {
        return new Conjecture_Text($text);
    }

    function __construct($text) {
        $this->input = $text;
    }

    function toString() {
        return $this->input;
    }

    function replaceNumbers($replacement = "_num_") {
        $text = preg_replace("/[0-9]+/", $replacement, $this->input);
        return new Conjecture_Text(preg_replace("/".$replacement."\\s+".$replacement."/", $replacement, $text));
    }

    function replaceHTMLEscapes($replacement = " ") {
        return new Conjecture_Text(preg_replace("/&[^;]+;/", $replacement, $this->input));
    }

    function removeHTMLTags() {
        return new Conjecture_Text(preg_replace("/<.*?>/", " ", $this->input));
    }

    function replaceHTMLTags($replacement = " ") {
        return new Conjecture_Text(preg_replace("/<[^>]+>/", " ", $this->input));
    }

    function replaceNonAlphaNumeric($replacement = " ") {
        return new Conjecture_Text(preg_replace("/[^a-zA-Z0-9\\.\\s\\-]+/", $replacement, $this->input));
    }

    function replaceNonAlphaNumericUnderscore($replacement = " ") {
        return new Conjecture_Text(preg_replace("/[^a-zA-Z0-9\\.\\s\\-_]+/", $replacement, $this->input));
    }

    function replaceNonAlpha($replacement = " ") {
        return new Conjecture_Text(preg_replace("/[^a-zA-Z]+/", $replacement, $this->input));
    }

    function collapseHyphens() {
        return new Conjecture_Text(preg_replace("/--+/", "--", $this->input));
    }

    function collapseUnderscores() {
        return new Conjecture_Text(preg_replace("/__+/", "__", $this->input));
    }

    function collapsePeriods() {
        return new Conjecture_Text(preg_replace("/\.\.+/", "..", $this->input));
    }

    function stripPunctuation() {
        $temp = preg_replace("^[^A-Za-z0-9]", "", $this->input);
        return new Conjecture_Text(preg_replace("[^A-Za-z0-9]$", "", $temp));
    }

    // compact any white space
    function collapse() {
        return new Conjecture_Text(preg_replace("/\\s+/", " ", $this->input));
    }

    // remove any whitespace from the right of a string
    function rstrip() {
        return new Conjecture_Text(preg_replace("/\\s+$/", "", $this->input));
    }

    // remove any whitespace from the left of a string
    function lstrip() {
        return new Conjecture_Text(preg_replace("/^\\s+/", "", $this->input));
    }

    // remove any leading or trailing whitespace
    function strip() {
        return $this->rstrip()->lstrip();
    }

    // clean up any whitespace
    function wsclean() {
        return $this->strip()->collapse();
    }

    // remove any unprintable non-ASCII characters
    function removeUnprintables() {
        return new Conjecture_Text(preg_replace("/[^\\x20-\\x7E]/", "", $this->input));
    }

    function collapseWhitespaceAndPunc() {
        $text = $this->collapse()->collapseHyphens();
        return new Conjecture_Text(preg_replace("/\\.\\.+/", ".", $text->toString()));
    }

    function toLowerCase() {
        return new Conjecture_Text(strtolower($this->input));
    }

    function standardTextFilter() {
        return $this->removeHTMLTags()
                    ->replaceHTMLEscapes()
                    ->replaceNumbers()
                    ->replaceNonAlphaNumericUnderscore()
                    ->collapseHyphens()
                    ->collapseUnderscores()
                    ->wsclean();
    }

    function toArrayFromShingles($n) {
        $shingles = array();

        $chars = str_split($this->input);
        for ($i = 0; $i < count($chars) - $n + 1; $i++) {
            $shingle = array_slice($chars, $i, $n);
            $shingles[] = implode("", $shingle);
        }

        return $shingles;
    }

    function toSequenceFromShingles($n) {
        return new Conjecture_TextSequence($this->toArrayFromShingles($n));
    }
}
