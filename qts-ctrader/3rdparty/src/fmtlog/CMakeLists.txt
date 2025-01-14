cmake_minimum_required(VERSION 3.15)

project(fmtlog CXX)

#add_definitions("-DFMTLOG_HEADER_ONLY")


add_compile_options(-Wall -O3 -std=c++17 -fPIC)
#link_libraries(pthread)
link_directories(.)

#include_directories(../fmt/include)
#include_directories(/usr/local/include)


#add_library(fmtlog-shared SHARED fmtlog.cc)
#if(MSVC)
#  target_link_libraries(fmtlog-shared fmt)
#endif()
#install(TARGETS fmtlog-shared)
#link_libraries(fmt)
add_library(fmtlog  SHARED fmtlog.cc)
#target_link_libraries(fmtlog fmt)

#if(MSVC)
#  target_link_libraries(fmtlog-static fmt)
#endif()

#add_subdirectory(test)
#if(NOT MSVC)
#  add_subdirectory(bench)
#endif()
