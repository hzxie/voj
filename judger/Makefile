CC=g++
CFLAGS=-c -std=c++11 -Wall -fPIC -I ${JAVA_HOME}/include
LDFLAGS=-fPIC -shared

ifeq ($(OS), Windows_NT)
	SOURCES_DIR=src/main/cpp/windows
	OBJECTS_DIR=target/cpp
	EXECUTABLE=target/classes/JudgerCore.dll
else
	UNAME_S := $(shell uname -s)
	ifeq ($(UNAME_S), Linux)
		SOURCES_DIR=src/main/cpp/unix
		OBJECTS_DIR=target/cpp
		EXECUTABLE=target/classes/libJudgerCore.so
	endif
	ifeq ($(UNAME_S), Darwin)
		SOURCES_DIR=src/main/cpp/unix
		OBJECTS_DIR=target/cpp
		EXECUTABLE=target/classes/libJudgerCore.so
	endif
endif

SOURCES=$(shell find '$(SOURCES_DIR)' -type f -name '*.cpp')
OBJECTS=$(SOURCES:$(SOURCES_DIR)/%.cpp=$(OBJECTS_DIR)/%.o)

all: $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

$(OBJECTS): $(SOURCES)
	mkdir -p $(OBJECTS_DIR)
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -rf $(OBJECTS_DIR) $(EXECUTABLE)
