
def formatTime(seconds):
        millis = int((seconds - int(seconds)) * 1000)
        seconds = int(seconds)
        
        days, remain = (seconds // 86400, seconds % 86400)
        hours, remain = (remain // 3600, remain % 3600)
        minutes, remain = (remain // 60, remain % 60)
        seconds = remain
        
        return '%sd %sh %sm %ss %sms' % (days, hours, minutes, seconds, millis)