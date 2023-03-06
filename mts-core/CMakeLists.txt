cmake_minimum_required(VERSION 3.16)
project(mts-core)

configure_file (
        "${PROJECT_SOURCE_DIR}/config.h.in"
        "${PROJECT_BINARY_DIR}/config.h"
)

set(CMAKE_CXX_STANDARD 20)
set(SRC ./src)
set(LIB ./lib)
set(INC ./include)
set(EXECUTABLE_OUTPUT_PATH  ${CMAKE_BINARY_DIR}/bin)


include_directories(${INC})
#include_directories(protobuf)
add_subdirectory(${SRC})
add_subdirectory(depends)

#include(FindProtobuf)
#find_package(Protobuf REQUIRED)
#include_directories(${PROTOBUF_INCLUDE_DIR})
#INCLUDE_DIRECTORIES(${CMAKE_CURRENT_BINARY_DIR})


#find_package( json 3 REQUIRED )
file(COPY config.json DESTINATION ${CMAKE_BINARY_DIR}/bin)
file(COPY lib DESTINATION ${CMAKE_BINARY_DIR})

#添加静态库（add_executable之前）
link_libraries(trade-engine jsoncpp)
#生成执行文件或者库
add_executable(${PROJECT_NAME}  ${SRC}/TradeCore.cpp)
add_executable(usd-client  ${SRC}/UdsClient.cpp)



#添加头文件（-I）
#target_include_directories( ${PROJECT_NAME} include ${subprojects_SOURCE_DIR}/include)

#需要链接的库目录（-L）
#link_directories(
#        ./
#        ${LIB}/ctp
#        /usr/local/lib/
#)
#添加链接库文件（-l）
target_link_libraries(${PROJECT_NAME}
        dl
        rt
        pthread
        event)

target_link_libraries(usd-client
        dl
        rt
        pthread
        event
#        ${PROTOBUF_LIBRARY}
        )



