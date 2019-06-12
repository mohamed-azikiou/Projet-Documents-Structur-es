import sys, re, codecs, os ,glob
files = [f for f in glob.glob(sys.argv[1] + "/**/*", recursive=True)]
print(str(len(files))+' '+str(sys.argv[1]))
List = []

def Correct(Str):
    match=re.search(r"(.*?)(?=<)<(/?)(.*?(?=[\s|>|/]))(.*?)(/?)>(.*)",Str,re.DOTALL)
    if match==None:
        return Str
    elif match.group(5)=="/":
        return match.group(1)+'<'+match.group(3)+match.group(4)+match.group(5)+'>'+Correct(match.group(6))
    elif match.group(2)=="/":
        popped=List.pop()
        if popped==match.group(3):
            return match.group(1)+'<'+match.group(2)+match.group(3)+match.group(5)+'>'+Correct(match.group(6))
        else:
            return match.group(1)+'</'+popped+">"+Correct('<'+'/'+match.group(3)+'>'+match.group(6))
    elif match.group(2)=="" and match.group(5)=="":
        List.append(match.group(3))
        return match.group(1)+'<'+match.group(3)+match.group(4)+'>'+Correct(match.group(6))

def OpenFile(path):
    encodings = ['utf-8', 'ansi','windows-1250', 'windows-1252', 'utf_16', 'utf_16_be', 'utf_16_le']
    for e in encodings:
        try:
            inputFile = codecs.open(path, 'r', encoding=e)
            inputFile.readlines()
            inputFile.seek(0)
        except UnicodeDecodeError:
            continue
        else:
            break
    return inputFile


for f in files:
    if "M457.xml" in f:
        inputFile=OpenFile(f)
        lines=inputFile.readlines()
        inputFile.close()
        outputFile=open(f,"w",encoding="ansi",newline='')
        for line in lines:
            ss=Correct(line)
            for i in range(ord('\u00a0'),ord('\u00ff')+1):
                ss=ss.replace(chr(i),"&#x"+hex(i)[-2:]+";")
            outputFile.write(ss.replace("&\n","\n"))
        outputFile.close()
    elif "renault.html" in f:
        inputFile=OpenFile(f)
        lines=inputFile.read()
        inputFile.close()
        outputFile=open(f,"w",encoding="utf8")
        outputFile.write(lines.replace("&nbsp;","&#160;").replace("\r\n","\n"))
        outputFile.close()
