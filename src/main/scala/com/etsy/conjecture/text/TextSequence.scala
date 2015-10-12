package com.etsy.conjecture.text

import com.etsy.conjecture.data.{BinaryLabeledInstance,BinaryLabel,MulticlassLabel,MulticlassLabeledInstance}

case class TextSequence(tokens: Seq[String]) {

    def ++(that: TextSequence): TextSequence = TextSequence(tokens ++ that.tokens)

    def mkString(glue: String = " "): String = tokens.mkString(glue)

    override def toString = mkString(" ")

    def intersect(that: TextSequence): TextSequence = TextSequence(tokens.intersect(that.tokens))

    def filterBlank = TextSequence(tokens.filter { x => x.isEmpty })

    def filterStopwords = TextSequence(tokens.filter { x => !Stopwords(x.toLowerCase) })

    def stopwords = TextSequence(tokens.filter { x => Stopwords(x.toLowerCase) })

    def filterBadwords = TextSequence(tokens.filter { x => !BadWords(x.toLowerCase) })

    def badwords = TextSequence(tokens.filter { x => BadWords(x.toLowerCase) })

    def filterAllCaps = TextSequence(tokens.filter { x => !x.matches("^[A-Z]+$") })

    def allCaps = TextSequence(tokens.filter { x => x.matches("^[A-Z]+$") })

    def filterCapitalized = TextSequence(tokens.filter { x => !x.matches("^[A-Z][^A-Z]*") })

    def capitalized = TextSequence(tokens.filter { x => x.matches("^[A-Z][^A-Z]*") })

    def filterLowercase = TextSequence(tokens.filter { x => !x.matches("^[a-z]+$") })

    def allLowercase = TextSequence(tokens.filter { x => x.matches("^[a-z]+$") })

    def filterURLs = TextSequence(tokens.filter { x => !x.matches("^https?://.+") })

    def allURLs = TextSequence(tokens.filter { x => x.matches("^https?://.+") })

    def filterListings = TextSequence(tokens.filter { x => !x.matches("^https?://.+etsy.+/listing/[0-9]+.*") })

    def allListings = TextSequence(tokens.filter { x => x.matches("^https?://.+etsy.+/listing/[0-9]+.*") })

    def size: Int = tokens.size

    def stopWordCount: Int = stopwords.size

    def stopWordFraq(bins: Int = 10): Int = (math.round(bins * stopWordCount / size) / bins.toDouble).toInt

    def badWordCount: Int = badwords.size

    def badWordFraq(bins: Int = 10): Int = (math.round(bins * badWordCount / size) / bins.toDouble).toInt

    def capsCount: Int = allCaps.size

    def capFraq(bins: Int = 10): Int = (math.round(bins * capsCount / size) / bins.toDouble).toInt

    def urlCount: Int = allURLs.size

    def urlFraq(bins: Int = 10): Int = (math.round(bins * urlCount / size) / bins.toDouble).toInt

    def listingsCount: Int = allListings.size

    def listingsFraq(bins: Int = 10): Int = (math.round(bins * listingsCount / size) / bins.toDouble).toInt

    def sizeBin = math.floor(math.log(size)).toInt

    // filtering methods

    def replaceNumbers(replacement: String = "_num_") = TextSequence(tokens.map { input => input.replaceAll("[0-9]+", replacement).replaceAll(replacement + "\\s+" + replacement, replacement) })

    def replaceHTMLEscapes(replacement: String = " ") = TextSequence(tokens.map { input => input.replaceAll("&[^;]+;", replacement) })

    def removeHTMLTags() = TextSequence(tokens.map { input => input.replaceAll("<.*?>", " ") })

    def replaceHTMLTags(replacement: String = " ") = TextSequence(tokens.map { input => input.replaceAll("<[^>]+>", " ") })

    def replaceNonAlphaNumeric(replacement: String = " ") = TextSequence(tokens.map { input => input.replaceAll("[^a-zA-Z0-9\\.\\s\\-]+", replacement) })

    def replaceNonAlphaNumericUnderscore(replacement: String = " ") = TextSequence(tokens.map { input => input.replaceAll("[^a-zA-Z0-9\\.\\s\\-_]+", replacement) })

    def replaceNonAlpha(replacement: String = " ") = TextSequence(tokens.map { input => input.replaceAll("[^a-zA-Z]+", replacement) })

    def collapseHyphens() = TextSequence(tokens.map { input => input.replaceAll("--+", "--") })

    def collapseUnderscores() = TextSequence(tokens.map { input => input.replaceAll("__+", "__") })

    def collapsePeriods() = TextSequence(tokens.map { input => input.replaceAll("\\.\\.+", "..") })

    def stripPunctuation() = TextSequence(tokens.map { input => input.replaceAll("^[^A-Za-z0-0]+", "").replaceAll("[^A-Za-z0-9]+$", "") })

    // compact any white space
    def collapse() = TextSequence(tokens.map { input => input.replaceAll("\\s+", " ") })

    // remove any whitespace from the right of a string
    def rstrip() = TextSequence(tokens.map { input => input.replaceAll("\\s+$", "") })

    // remove any whitespace from the left of a string
    def lstrip() = TextSequence(tokens.map { input => input.replaceAll("^\\s+", "") })

    // remove any leading or trailing whitespace
    def strip() = TextSequence(tokens.map { input => (input.trim) })

    // clean up any whitespace
    def wsclean() = strip().collapse()

    // remove any unprintable non-ASCII characters
    def removeUnprintables(input: String) = TextSequence(tokens.map { input => input.replaceAll("[^\\x20-\\x7E]", "") })

    def collapseWhitespaceAndPunc = TextSequence(tokens.map { input =>
        input.replaceAll("\\s+", " ")
            .replaceAll("[\\-]+", "-")
            .replaceAll("[\\.]+", ".")
    })

    def ngrams(n: Int, glue: String = " ") = new TextSequence(tokens.sliding(n).map { x => x.mkString(glue) }.toList)

    def shingles(n: Int, whitespace: String = "_"): TextSequence = {
        val str = tokens.mkString(whitespace)
        TextSequence(str.sliding(n).toList)
    }

    def prependNameSpace(namespace: String) = new TextSequence(tokens.map { x => namespace + x })

    def toList = tokens.toList

    def toBinaryLabeledInstance(label: Double): BinaryLabeledInstance = {
      toBinaryLabeledInstance(new BinaryLabel(label))
    }

    def toBinaryLabeledInstance(label: BinaryLabel): BinaryLabeledInstance = {
        val instance = new BinaryLabeledInstance(label)

        tokens.foreach {
            x => instance.addTerm(x)
        }

        instance
    }


    def toMulticlassLabeledInstance(label: MulticlassLabel): MulticlassLabeledInstance = {
        val instance = new MulticlassLabeledInstance(label)

        tokens.foreach {
            x => instance.addTerm(x)
        }

        instance
    }


}

object Stopwords {
    def apply(input: String): Boolean = stopwords.contains(input)

    val stopwords = Set("a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "b", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "bill", "both", "bottom", "brief", "but", "by", "c", "cmon", "cs", "call", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "con", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "couldnt", "course", "cry", "currently", "d", "de", "definitely", "describe", "described", "despite", "detail", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "due", "during", "e", "each", "edu", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "f", "far", "few", "fifteen", "fifth", "fify", "fill", "find", "fire", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "forty", "found", "four", "from", "front", "full", "further", "furthermore", "g", "get", "gets", "getting", "give", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "h", "had", "hadnt", "happens", "hardly", "has", "hasnt", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "hundred", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "interest", "into", "inward", "is", "isnt", "it", "itd", "itll", "its", "its", "itself", "j", "just", "k", "keep", "keeps", "kept", "know", "known", "knows", "l", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "m", "made", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "n", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "o", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "p", "part", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "put", "q", "que", "quite", "qv", "r", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "s", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "system", "t", "ts", "take", "taken", "tell", "ten", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "thea", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "thickv", "thin", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "top", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twelve", "twenty", "twice", "two", "u", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "uucp", "v", "value", "various", "very", "via", "viz", "vs", "w", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "wouldnt", "x", "y", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves", "z", "zero")
}

object BadWords {

    def apply(input: String): Boolean = badwords.contains(input)

    val badwords = Set("ahole", "arse", "ass", "asshole", "asswipe", "bastard", "batty", "bender", "bitch", "bloody", "bollocks", "boner", "bumboy", "bugger", "coon", "cock", "cocksucker", "cracker", "crap", "cumsucker", "cunt", "damn", "dick", "dildo", "douchebag", "faggot", "fistfucker", "fuck", "fucker", "fuckwit", "fucktwat", "gaylord", "ho", "honky", "jackass", "jism", "joey", "knobcheese", "minge", "minger", "mong", "motherfucker", "munter", "pickle", "piss", "piss", "prick", "pussy", "rimmer", "schmuck", "shit", "slut", "spakka", "spaz", "skank", "taint", "tit", "tool", "tosser", "twat", "whore", "wanker")
}
