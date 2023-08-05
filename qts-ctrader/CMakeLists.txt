cmake_minimum_required(VERSION 3.16)
project(mts-core)

add_definitions(-DUSE_SF)

#设置编译选项（也可以通过add_definitions实现）
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS   "-Wno-write-strings -Wno-return-type")
set(CMAKE_BUILD_TYPE Debug)
#if (DEBUG)
#    set (CMAKE_CXX_FLAGS "-std=c++17 -Wall -Werror -fPIC -g -O0 ${CMAKE_CXX_FLAGS}")
#else ()
#    set (CMAKE_CXX_FLAGS "-std=c++17 -Wall -Werror -fPIC -O2 ${CMAKE_CXX_FLAGS}")
#endif ()


set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE)
set(CMAKE_INSTALL_RPATH $ORIGIN/lib $ORIGIN) #通过LD_DEBUG=libs查看实际搜索路径

# 目录说明
#CMAKE_SOURCE_DIR/PROJUECT_SOURCE_DIR  项目顶级目录
#CMAKE_BINARY_DIR/PROJUECT_BINARY_DIR  cmake编译目录(即执行cmake命令的目录比如:cmake-build-debug)
#CMAKE_CURRENT_SOURCE_DIR              当前CMakeLists.txt目录,会动态变化
#LIBRARY_OUTPUT_DIR     目标链接库存放目录
#EXECUTABLE_OUTPUT_PATH 目标二进制存放目录

#build path
set(EXECUTABLE_OUTPUT_PATH  ${CMAKE_SOURCE_DIR}/build)
#lib path
set(LIBRARY_OUTPUT_PATH ${EXECUTABLE_OUTPUT_PATH}/lib)



file(COPY conf DESTINATION ${EXECUTABLE_OUTPUT_PATH})
file(COPY lib DESTINATION  ${EXECUTABLE_OUTPUT_PATH})

#include_directories(${INC} ${SRC})
include_directories(include src)

add_subdirectory(depends)
add_subdirectory(src)


