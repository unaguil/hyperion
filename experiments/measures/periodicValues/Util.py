import math

def getPeriod(time, period, simulationTime):
    if (time >= simulationTime):
        return None;
    else:
        return int(math.floor(time / period))
    
def getPeriods(period, simulationTime):
    return int(math.ceil(simulationTime / period))