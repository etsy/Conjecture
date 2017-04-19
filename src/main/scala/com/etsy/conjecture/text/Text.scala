package com.etsy.conjecture.text

case class Text(val input: String) {

    private implicit def text2str(txt: Text): String = txt.input
    private implicit def str2text(str: String): Text = new Text(str)

    override def toString = input.toString

    def replaceNumbers(replacement: String = "_num_") = Text(input.replaceAll("[0-9]+", replacement).replaceAll(replacement + "\\s+" + replacement, replacement))

    def replaceHTMLEscapes(replacement: String = " ") = Text(input.replaceAll("&[^;]+;", replacement))

    def removeHTMLTags() = Text(input.replaceAll("<.*?>", " ")) //Text(XML.loadString(input).text)

    def replaceHTMLTags(replacement: String = " ") = Text(input.replaceAll("<[^>]+>", " "))

    def replaceNonAlphaNumeric(replacement: String = " ") = Text(input.replaceAll("[^a-zA-Z0-9\\.\\s\\-]+", replacement))

    def replaceNonAlphaNumericUnderscore(replacement: String = " ") = Text(input.replaceAll("[^a-zA-Z0-9\\.\\s\\-_]+", replacement))

    def replaceNonAlpha(replacement: String = " ") = Text(input.replaceAll("[^a-zA-Z]+", replacement))

    def collapseHyphens() = Text(input.replaceAll("--+", "--"))

    def collapseUnderscores() = Text(input.replaceAll("__+", "__"))

    def collapsePeriods() = Text(input.replaceAll("\\.\\.+", ".."))

    def toLowerCase() = Text(input.toLowerCase)

    def toUpperCase() = Text(input.toUpperCase)

    def stripPunctuation() = Text(input.replaceAll("^[^A-Za-z0-0]+", "").replaceAll("[^A-Za-z0-9]+$", ""))

    // compact any white space
    def collapse() = Text(input.replaceAll("\\s+", " "))

    // remove any whitespace from the right of a string
    def rstrip() = Text(input.replaceAll("\\s+$", ""))

    // remove any whitespace from the left of a string
    def lstrip() = Text(input.replaceAll("^\\s+", ""))

    // remove any leading or trailing whitespace
    def strip() = Text(input.trim)

    // clean up any whitespace
    def wsclean() = strip().collapse()

    // remove any unprintable non-ASCII characters
    def removeUnprintables(input: String) = Text(input.replaceAll("[^\\x20-\\x7E]", ""))

    def collapseWhitespaceAndPunc = Text(input.replaceAll("\\s+", " ")
        .replaceAll("[\\-]+", "-")
        .replaceAll("[\\.]+", "."))

    def standardTextFilter = Text(removeHTMLTags()
        .replaceHTMLEscapes()
        .replaceNumbers()
        .replaceNonAlphaNumericUnderscore()
        .collapseHyphens()
        .collapseUnderscores()
        .wsclean())

    def toListFromShingles(n: Int, ns: Int*): List[String] = (List(n) ++ ns.toList).flatMap{ i: Int => input.sliding(i) }.toList

    def toSequenceFromShingles(n: Int, ns: Int*): TextSequence = new TextSequence(toListFromShingles(n, ns: _*))

    def toList(sep: String = " "): List[String] = input.split(sep).toList

    def toSequence(sep: String = " "): TextSequence = new TextSequence(toList(sep))

    def isEmpty(): Boolean = input.isEmpty()
}
