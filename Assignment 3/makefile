build: store.java bank.java
	javac *.java
clean:
	rm *.class
runStore:
	make build
	java store 3000 127.0.0.1 8000
	make clean
