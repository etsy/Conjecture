import json
import sys
import math

if __name__ == '__main__':
  if len(sys.argv) != 3:
    sys.exit("Usage: python " +  sys.argv[0] + " [model file] [model file]")
  a = json.load(open(sys.argv[1]))['param']['vector']
  b = json.load(open(sys.argv[2]))['param']['vector']
  features = set(a.keys()) | set(b.keys())
  diff = []
  for f in features:
    dv = a.get(f, 0.0) - b.get(f, 0.0)
    if math.fabs(dv) > 0.01:
      diff.append((f, dv, a.get(f), b.get(f)))
  diff.sort( key = lambda tup: -math.fabs(tup[1]))
  for t in diff:
    print t[0] + "\t" + str(t[2]) + "\t" + str(t[3]) + "\t(" + str(t[1]) + ")"

