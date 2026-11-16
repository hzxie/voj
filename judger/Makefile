CC=g++
# jni.h lives in $(JAVA_HOME)/include and includes jni_md.h, which lives in the platform's
# subdirectory (include/linux, include/win32).
CFLAGS=-c -std=c++11 -Wall -I "$(JAVA_HOME)/include" -I "$(JAVA_HOME)/include/$(JNI_PLATFORM)"
LDFLAGS=-shared
EXTFLAGS=

ifeq ($(OS), Windows_NT)
	JNI_PLATFORM=win32
	SOURCES_DIR=src/main/cpp/windows
	OBJECTS_DIR=target/cpp
	EXECUTABLE=target/classes/JudgerCore.dll
	# Link the MinGW runtime (libstdc++, libgcc, winpthread) into the DLL, so that the JVM can load
	# it without MinGW's bin directory on PATH. System libraries are still imported from their DLLs.
	LDFLAGS+=-static
	EXTFLAGS=-luserenv -lpsapi
else
	UNAME_S := $(shell uname -s)
	ifeq ($(UNAME_S), Linux)
		JNI_PLATFORM=linux
		SOURCES_DIR=src/main/cpp/unix
		OBJECTS_DIR=target/cpp
		EXECUTABLE=target/classes/libJudgerCore.so
		CFLAGS+=-fPIC
		LDFLAGS+=-fPIC
		EXTFLAGS=-lpthread -lrt -lseccomp
	endif
endif

# The headers shared by the runners: editing one rebuilds every object.
HEADERS=$(wildcard src/main/cpp/*.h)
SOURCES=$(wildcard $(SOURCES_DIR)/*.cpp)
OBJECTS=$(SOURCES:$(SOURCES_DIR)/%.cpp=$(OBJECTS_DIR)/%.o)

all: $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) $(EXTFLAGS) -o $@

$(OBJECTS_DIR)/%.o: $(SOURCES_DIR)/%.cpp $(HEADERS)
	mkdir -p $(OBJECTS_DIR)
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -rf $(OBJECTS_DIR) $(EXECUTABLE)

.PHONY: all clean
