<?php

// This is bascially an exact replica of com.etsy.conjecture.text.TextSequence
class Conjecture_TextSequence {

    private $tokens = null;

    function __construct(array $tokens) {
        $this->tokens = $tokens;
    }

    /**
     * concatenates two TextSequences into an additional text sequence
     */
    function concat($other) {
        return new Conjecture_TextSequence(array_merge($this->tokens, $other->tokens));
    }

    function mkString($glue = " ") {
        return implode($glue, $this->tokens);
    }

    function toString() {
        return $this->mkString(" ");
    }

    function getTokens() {
        return $this->tokens;
    }

    function filterBlank() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return $x !== "";
                                                        }
                                                       )
                                          );
    }

    function filterStopwords() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !in_array($x, self::$stopwordList);
                                                        }
                                                       )
                                          );
    }

    function stopwords() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return in_array($x, self::$stopwordList);
                                                        }
                                                       )
                                          );
    }


    function filterBadwords() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !in_array($x, self::$badwordList);
                                                        }
                                                       )
                                          );
    }

    function badwords() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return in_array($x, self::$badwordList);
                                                        }
                                                       )
                                          );
    }

    function filterAllCaps() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !preg_match('/^[A-Z]+$/', $x);
                                                        }
                                                       )
                                          );
    }

    function AllCaps() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return preg_match('/^[A-Z]+$/', $x);
                                                        }
                                                       )
                                          );
    }

    function filterCapitalized() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !preg_match('/^[A-Z][^A-Z]+$/', $x);
                                                        }
                                                       )
                                          );
    }

    function capitalized() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return preg_match('/^[A-Z][^A-Z]+$/', $x);
                                                        }
                                                       )
                                          );
    }

    function filterLowercase() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !preg_match('/^[a-z]+$/', $x);
                                                        }
                                                       )
                                          );
    }

    function allLowercase() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return preg_match('/^[a-z]+$/', $x);
                                                        }
                                                       )
                                          );
    }

    function filterURLs() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !preg_match('/^https?://.+/', $x);
                                                        }
                                                       )
                                          );
    }

    function allURLs() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return preg_match('/^https?://.+/', $x);
                                                        }
                                                       )
                                          );
    }

    function filterListings() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return !preg_match('/^https?://.+etsy.+/listing/[0-9]+.*/', $x);
                                                        }
                                                       )
                                          );
    }

    function allListings() {
        return new Conjecture_TextSequence(array_filter($this->tokens,
                                                        function($x) {
                                                            return preg_match('/^https?://.+etsy.+/listing/[0-9]+.*/', $x);
                                                        }
                                                       )
                                          );
    }

    function size() {
        return count($this->tokens);
    }

    function stopWordCount() {
        return $this->stopwords()->size();
    }

    function stopWordFraq($bins = 10.0) {
        return floor(round($bins*$this->stopWordCount()/$this->size())/$bins);
    }

    function badWordCount() {
        return $this->badwords()->size();
    }

    function badWordFraq($bins = 10.0) {
        return floor(round($bins*$this->badWordCount()/$this->size())/$bins);
    }

    function capsCount() {
        return $this->allCaps()->size();
    }

    function capFraq($bins = 10.0) {
        return floor(round($bins*$this->capsCount()/$this->size())/$bins);
    }

    function urlCount() {
        return $this->allURLs()->size();
    }

    function urlFraq($bins = 10.0) {
        return floor(round($bins*$this->urlCount()/$this->size())/$bins);
    }

    function listingsCount() {
        return $this->badwords()->size();
    }

    function listingsFraq($bins = 10.0) {
        return floor(round($bins*$this->allListings()/$this->size())/$bins);
    }

    function sizeBin() {
        return floor(log($this->size()));
    }

    // filtering methods (TODO)

    function replaceNumbers($replacement = "_num_") {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($replacement) {
                                                   $text = preg_replace("/[0-9]+/", $replacement, $x);
                                                   return preg_replace("/".$replacement."\\s+".$replacement."/", $replacement, $text);
                                               }, $this->tokens));
    }


    function replaceHTMLEscapes($replacement = " ") {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($replacement) {
                                                   return preg_replace("/&[^;]+;/", $replacement, $x);
                                               }, $this->tokens));
    }

    function removeHTMLTags() {
        return $this->replaceHTMLTags(" ");
    }

    function replaceHTMLTags($replacement = " ") {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($replacement) {
                                                   return preg_replace("/<[^>]+>/", $replacement, $x);
                                               }, $this->tokens));
    }

    function replaceNonAlphaNumeric($replacement = " ") {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($replacement) {
                                                   return preg_replace("/[^a-zA-Z0-9\\.\\s\\-]+/", $replacement, $x);
                                               }, $this->tokens));
    }

    function replaceNonAlphaNumericUnderscore($replacement = " ") {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($replacement) {
                                                   return preg_replace("/[^a-zA-Z0-9\\.\\s\\-_]+/", $replacement, $x);
                                               }, $this->tokens));
    }

    function replaceNonAlpha($replacement = " ") {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($replacement) {
                                                   return preg_replace("/[^a-zA-Z\\.\\s\\-_]+/", $replacement, $x);
                                               }, $this->tokens));
    }

    function collapseHyphens() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/--+/", "--", $x);
                                               }, $this->tokens));
    }

    function collapseUnderscores() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/__+/", "__", $x);
                                               }, $this->tokens));
    }

    function collapsePeriods() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/\.\.+/", "..", $x);
                                               }, $this->tokens));
    }

    function stripPunctuation() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   $temp = preg_replace("^[^A-Za-z0-9]", "", $x);
                                                   return preg_replace("[^A-Za-z0-9]$", "", $temp);
                                               }, $this->tokens));
    }

    // compact any white space
    function collapse() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/\\s+/", " ", $x);
                                               }, $this->tokens));
    }

    function rstrip() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/^\\s+/", "", $x);
                                               }, $this->tokens));
    }

    function lstrip() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/\\s+$/", "", $x);
                                               }, $this->tokens));
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
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   return preg_replace("/[^\\x20-\\x7E]/", "", $x);
                                               }, $this->tokens));

    }

    function collapseWhitespaceAndPunc() {
        return new Conjecture_TextSequence(array_map(
                                               function($x) {
                                                   $ws = preg_replace("/\\s+/", " ", $x);
                                                   $dh = preg_replace("/[\\-]+/", "-", $ws);
                                                   return preg_replace("/[\\.]+/", ".", $dh);
                                               }, $this->tokens));
    }

    function prependNameSpace($namespace) {
        return new Conjecture_TextSequence(array_map(
                                               function($x) use ($namespace) {
                                                   return $namespace . $x;
                                               }, $this->tokens));
    }

    function toList() {
        return $this->tokens;
    }

    function shingles($n, $whitespace = "_") {
        $str = implode($whitespace, $this->tokens);
        $arr = explode('', $str);

        $shingles = array();
        for ($i = 0; $i < count($arr) - $n; $i++) {
            $shingles[] = implode('', array_slice($arr, $i, $i + $n));
        }

        return new Conjecture_TextSequence($shingles);
    }

    function ngrams($n, $glue = " ") {
        $grams = array();
        for ($i = 0; $i < count($this->tokens) - $n+1; $i++) {
            $grams[] = implode($glue, array_slice($this->tokens, $i, $n));
        }

        return new Conjecture_TextSequence($grams);
    }

    function unigramsAndBigrams($glue = " ") {
      return $this->ngrams(1)->concat($this->ngrams(2, $glue));
    }

    function toInstance() {
        $instance = new Conjecture_Instance();

        foreach ($this->tokens as $token) {
            $instance->addTerm($token);
        }

        return $instance;
    }

    static $stopwordList = array("a","as","able","about","above","according","accordingly","across","actually","after","afterwards","again","against","aint","all","allow","allows","almost","alone","along","already","also","although","always","am","among","amongst","amoungst","amount","an","and","another","any","anybody","anyhow","anyone","anything","anyway","anyways","anywhere","apart","appear","appreciate","appropriate","are","arent","around","as","aside","ask","asking","associated","at","available","away","awfully","b","back","be","became","because","become","becomes","becoming","been","before","beforehand","behind","being","believe","below","beside","besides","best","better","between","beyond","bill","both","bottom","brief","but","by","c","cmon","cs","call","came","can","cant","cannot","cant","cause","causes","certain","certainly","changes","clearly","co","com","come","comes","con","concerning","consequently","consider","considering","contain","containing","contains","corresponding","could","couldnt","couldnt","course","cry","currently","d","de","definitely","describe","described","despite","detail","did","didnt","different","do","does","doesnt","doing","dont","done","down","downwards","due","during","e","each","edu","eg","eight","either","eleven","else","elsewhere","empty","enough","entirely","especially","et","etc","even","ever","every","everybody","everyone","everything","everywhere","ex","exactly","example","except","f","far","few","fifteen","fifth","fify","fill","find","fire","first","five","followed","following","follows","for","former","formerly","forth","forty","found","four","from","front","full","further","furthermore","g","get","gets","getting","give","given","gives","go","goes","going","gone","got","gotten","greetings","h","had","hadnt","happens","hardly","has","hasnt","hasnt","have","havent","having","he","hes","hello","help","hence","her","here","heres","hereafter","hereby","herein","hereupon","hers","herself","hi","him","himself","his","hither","hopefully","how","howbeit","however","hundred","i","id","ill","im","ive","ie","if","ignored","immediate","in","inasmuch","inc","indeed","indicate","indicated","indicates","inner","insofar","instead","interest","into","inward","is","isnt","it","itd","itll","its","its","itself","j","just","k","keep","keeps","kept","know","known","knows","l","last","lately","later","latter","latterly","least","less","lest","let","lets","like","liked","likely","little","look","looking","looks","ltd","m","made","mainly","many","may","maybe","me","mean","meanwhile","merely","might","mill","mine","more","moreover","most","mostly","move","much","must","my","myself","n","name","namely","nd","near","nearly","necessary","need","needs","neither","never","nevertheless","new","next","nine","no","nobody","non","none","noone","nor","normally","not","nothing","novel","now","nowhere","o","obviously","of","off","often","oh","ok","okay","old","on","once","one","ones","only","onto","or","other","others","otherwise","ought","our","ours","ourselves","out","outside","over","overall","own","p","part","particular","particularly","per","perhaps","placed","please","plus","possible","presumably","probably","provides","put","q","que","quite","qv","r","rather","rd","re","really","reasonably","regarding","regardless","regards","relatively","respectively","right","s","said","same","saw","say","saying","says","second","secondly","see","seeing","seem","seemed","seeming","seems","seen","self","selves","sensible","sent","serious","seriously","seven","several","shall","she","should","shouldnt","show","side","since","sincere","six","sixty","so","some","somebody","somehow","someone","something","sometime","sometimes","somewhat","somewhere","soon","sorry","specified","specify","specifying","still","sub","such","sup","sure","system","t","ts","take","taken","tell","ten","tends","th","than","thank","thanks","thanx","that","thats","thats","the","thea","their","theirs","them","themselves","then","thence","there","theres","thereafter","thereby","therefore","therein","theres","thereupon","these","they","theyd","theyll","theyre","theyve","thickv","thin","think","third","this","thorough","thoroughly","those","though","three","through","throughout","thru","thus","to","together","too","took","top","toward","towards","tried","tries","truly","try","trying","twelve","twenty","twice","two","u","un","under","unfortunately","unless","unlikely","until","unto","up","re","werent","what","whats","whatever","when","whence","whenever","where","wheres","whereafter","whereas","whereby","wherein","whereupon","wherever","whether","which","while","whither","who","whos","whoever","whole","whom","whose","why","will","willing","wish","with","within","without","wont","wonder","would","wouldnt","x","y","yes","yet","you","youd","youll","youre","youve","your","yours","yourself","yourselves","z","zero");

    static $badwordList = array("ahole", "arse", "ass", "asshole", "asswipe", "bastard", "batty", "bender", "bitch", "bloody", "bollocks", "boner", "bumboy", "bugger", "coon", "cock", "cocksucker", "cracker", "crap", "cumsucker", "cunt", "damn", "dick", "dildo", "douchebag", "faggot", "fistfucker", "fuck", "fucker", "fuckwit", "fucktwat", "gaylord", "ho", "honky", "jackass", "jism", "joey", "knobcheese", "minge", "minger", "mong", "motherfucker", "munter", "pickle", "piss", "piss", "prick", "pussy", "rimmer", "schmuck", "shit", "slut", "spakka", "spaz", "skank", "taint", "tit", "tool", "tosser", "twat", "whore", "wanker");
}
