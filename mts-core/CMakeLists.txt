cmake_minimum_required(VERSION 3.16)
project(mts-core)
add_definitions(-DUSE_SF)
add_definitions("-Wall -g")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS   "-Wno-write-strings -Wno-return-type")
set(CMAKE_BUILD_TYPE Debug)

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_INSTALL_RPATH $ORIGIN/lib $ORIGIN) #通过LD_DEBUG=libs查看实际搜索路径

#build path
set(EXECUTABLE_OUTPUT_PATH  ${CMAKE_BINARY_DIR}/build)
#lib path
set(LIBRARY_OUTPUT_PATH ${EXECUTABLE_OUTPUT_PATH}/lib)



file(COPY conf DESTINATION ${EXECUTABLE_OUTPUT_PATH})
file(COPY lib DESTINATION  ${EXECUTABLE_OUTPUT_PATH})

#include_directories(${INC} ${SRC})
include_directories(include src)

add_subdirectory(depends)
add_subdirectory(src)


