CC=g++
CFLAGS=-c -std=c++11 -Wall -fPIC -I ${JAVA_HOME}/include
LDFLAGS=-fPIC -shared

SOURCES_DIR=src/main/cpp
OBJECTS_DIR=target/cpp
EXECUTABLE=target/classes/libJudgerCore.so

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
