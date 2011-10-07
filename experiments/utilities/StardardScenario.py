#!/usr/bin/python

import math
import sys

print '**********************************************************'
print '            Standard scenario generator'
print ' This script generates scenarios with:'
print ' Average Network Partitioning (ANP) < 5 %%'
print ' Average Shortest Path Hop Count (AspHops) >= 4'
print '**********************************************************'

ar = int(raw_input('Aspect ratio [1,2,3,4]: '))

if not ar in [1,2,3,4]:
    print 'Invalid aspect ratio'
    sys.exit()
    
minNodes = {1: 95, 2: 85, 3: 75, 4: 70}
prompt = 'Number of nodes (>= %d): ' % minNodes[ar]
nodes = int(raw_input(prompt))

transmissionRange = 100

print 'Generating scenario (1 x %d) for %d nodes' % (ar, nodes)
print '\t*Transmission range: %d m' % transmissionRange

if ar == 1:
    minArea = 10.15*math.log(nodes) - 1.74
    maxArea = 0.41*nodes + 6.01
if ar == 2:
    minArea = 9.29*math.log(nodes) - 3.12
    maxArea = 0.41*nodes + 3.03
if ar == 3:
    minArea = 6.02*math.log(nodes) + 5.75
    maxArea = 0.39*nodes + 4.09
if ar == 4:
    minArea = 4.33*math.log(nodes) + 8.67
    maxArea = 0.41*nodes + 1.06


print '%.2f < Area < %.2f' % (minArea, maxArea)

area = float(raw_input('Select area: '))

if area <= minArea or area >= maxArea:
    print 'Area is invalid.'
    sys.exit()
else:
    width = math.sqrt(area/ar) * transmissionRange
    height = ar * width
    
print 'Scenario width %.2f m' % width
print 'Scenario height %.2f m' % height