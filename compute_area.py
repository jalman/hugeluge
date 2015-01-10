import sys
import math
import numpy as np
import itertools

def dsquared(x0, y0, x1, y1):
    return (x0-x1)**2 + (y0-y1)**2

def area_diagram(rsquared=6, xmin = 0, xmax = 10, ymin = 0, ymax = 10, xcenter = 4, ycenter = 4):
    themap = []
    count = 0
    for x in xrange(xmin, xmax+1):
        row = []
        for y in xrange(ymin, ymax+1):
            if dsquared(x, y, xcenter, ycenter) <= rsquared:
                row.append(1)
            else:
                row.append(0)
        themap.append(row)
    return themap

def area_diagram_offsets(rsquared=24):
    r = int(math.sqrt(rsquared)) + 1

    ret = []
    for x in xrange(-r, r+1):
        for y in xrange(-r, r+1):
            if dsquared(x, y, 0, 0) <= rsquared:
                ret.append( (x,y) )

    return ret


def offsets_in_range_after_move(rsquared=24, dx=0, dy=0):
    r = int(math.sqrt(rsquared)) + 1
    xmin = -r if dx > 0 else -r + dx
    xmax = r + dx if dx > 0 else r
    ymin = -r if dy > 0 else -r + dy
    ymax = r + dy if dy > 0 else r

    ret = []
    for x in xrange(xmin, xmax+1):
        for y in xrange(ymin, ymax+1):
            if dsquared(x, y, dx, dy) <= rsquared and dsquared(x, y, 0, 0) > rsquared:
                ret.append( (x,y) )

    return ret

adjacent_moves = [ dxdy for dxdy in itertools.product( (-1, 0, 1), (-1, 0, 1) ) if dxdy != (0,0) ]
dir_names = ["NORTH_WEST", "NORTH", "NORTH_EAST", "WEST", "EAST", "SOUTH_WEST", "SOUTH", "SOUTH_EAST"]

def offsets_in_range_after_adj_moves(rsquared=24):
    ret = {}
    for i in xrange(8):
        dx, dy = adjacent_moves[i]
        ret[dir_names[i]] =  offsets_in_range_after_move(rsquared, dx, dy)
    return ret

dirs_in_enum_order = ["NORTH", "NORTH_EAST", "EAST", "SOUTH_EAST", "SOUTH", "SOUTH_WEST", "WEST", "NORTH_WEST"]
dirs_to_idx_map = dict([(dirs_in_enum_order[i], i) for i in xrange(len(dirs_in_enum_order))])
def java_ize_offsets(offsetdict):
    return ','.join([ "{ //" + i[0] + "\n  " + 
        ','.join([ "{{{}, {}}}".format(j[0], j[1])
            for j in i[1]])
        + "\n}\n"
        for i in sorted(offsetdict.iteritems(), key=lambda x: dirs_to_idx_map[x[0]])])
        

if __name__ == '__main__':
    rsquaredmin = int(sys.argv[1])
    rsquaredmax = int(sys.argv[2])
    for rsquared in range(rsquaredmin, rsquaredmax+1):
        r = int(math.sqrt(rsquared))+1

        themap = area_diagram(rsquared, -r, r, -r, r, 0, 0)
        print '\n'.join([''.join([str(i) for i in row]) for row in themap])

