tipo = int(input())

def gerar1():
    for i in range(0,100):
        print("teste de ficheiro")

def gerar2():
    for i in range(0,150):
        if i%2 == 0:
            print("aaaaaaaaabbbbbbbbbb")
        else:
            print("bbbbbbbbaaaaaaaaaaa")

def gerar3():
    for i in range(0,300):
        print("ola")


if tipo == 1:
    gerar1()
elif tipo == 2:
    gerar2()
elif tipo == 3:
    gerar3()
else:
    pass
