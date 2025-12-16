cmake_minimum_required(VERSION 3.22.1)
project(voice_engine)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(
        voice_engine
        SHARED
        native-lib.cpp
        VoiceEngine.cpp
        PcmBuffer.cpp
)

find_library(log-lib log)

target_link_libraries(
        voice_engine
        ${log-lib}
)
