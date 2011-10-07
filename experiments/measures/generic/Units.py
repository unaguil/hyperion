
MESSAGES = 'messages'
RATIO = '[0.0 - 0.1] ratio'
NEIGHBORS = 'neighbors'
PARAMETERS = 'parameters'
MILLIS = 'milliseconds'
SECONDS = 'seconds'
COMPOSITIONS = 'compositions'


def str_formatter(units, value):
    if units == MILLIS:
        return '%.0f' % value
    if units == SECONDS:
        return '%.3f' % value
    
    return '%.2f' % value