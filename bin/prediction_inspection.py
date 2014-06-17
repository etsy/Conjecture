import json
import sys
from optparse import OptionParser
from math import floor

colors = ["FF0000", "FF1000", "FF2000", "FF3000", "FF4000", "FF5000", "FF6000",
          "FF7000", "FF8000", "FF9000", "FFA000", "FFB000", "FFC000", "FFD000",
          "FFE000", "FFF000", "FFFF00", "F0FF00", "E0FF00", "D0FF00", "C0FF00",
          "B0FF00", "A0FF00", "90FF00", "80FF00", "70FF00", "60FF00", "50FF00",
          "40FF00", "30FF00", "20FF00", "10FF00"]
bins = len(colors)


parser = OptionParser(usage="""builds a simple web page providing introspection on predictions made by conjecture models.
Depends on the supporting data provided in the instance itself, currently only supporting binary
classification problems
Usage: %prog [options]
""")

parser.add_option('-o', '--out', dest='out', default=False, action='store',
                  help="[optional] destination of the generated html. Defaults to standard out")
parser.add_option('-f', '--file', dest='file', default=False, action='store',
                  help="[optional] file storing input predictions and instances. Defaults to standard in")
parser.add_option('-l', '--label', dest='label', default=False, action='store',
                  help="[optional] only keep examples with this label")
parser.add_option('-L', '--limit', dest='limit', default=1000, action='store',
                  help="maximum number of prediction examples to display. Default: 1000")


(options, args) = parser.parse_args()

output = open(options.out, 'w') if (options.out) else sys.stdout
input = open(options.file, 'r') if(options.file) else sys.stdin

limit = int(options.limit)

output.write("<html>")
ct = 0

for line in input:
    parts = line.strip().split("\t")
    content = json.loads(parts[0])
    label = int(content['label']['value'])
    pred = float(parts[2])

    if (options.label and str(label) != options.label):
        continue

    error = min(1.0, abs(pred-label))
    bin = bins - int(floor(error*bins)) - 1

    color = "#" + colors[bin]
    out = ""

    support = json.loads(content['supporting_data'])

    for key in support.keys():
        out = out + "<b>" + key + "</b></br>" + support[key] + "<br/>"

    if (len(out) < 10000 and ct < limit):
        try:
            output.write("<div style='background-color: "  + color + "; width: 700px;'>");
            output.write("%d (%f)<br/>" %( label, pred))
            output.write(out)
            output.write("</div><p>")
            ct = ct + 1
        except:
            pass

    if (ct >= limit):
        break

output.write("</html>");
output.flush()
output.close()
