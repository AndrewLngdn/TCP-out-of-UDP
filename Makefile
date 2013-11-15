.SUFFIXES: .java .class

.java.class:
	javac $<

CLASSES = Receiver.class Sender.class 

all: $(CLASSES)

clean:
	rm *.class

