import json
import sys
import math

if __name__ == '__main__':
  if len(sys.argv) != 2:
    sys.exit("Usage: python " +  sys.argv[0] + " [model file]")
  vec = json.load(open(sys.argv[1]))['param']['vector'].items()
  vec.sort(key = lambda tup: -math.fabs(tup[1]))
  for v in vec:
    print v[0] + "\t" + str(v[1])
