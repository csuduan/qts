#project(trade-engine)
# 生成链接库
#set(LIBRARY_OUTPUT_PATH ${EXECUTABLE_OUTPUT_PATH})

#add_compile_options(-fPIC)



AUX_SOURCE_DIRECTORY(gateway SOURCE_GATEWAY)
AUX_SOURCE_DIRECTORY(strategy SOURCE_STRATEGY)
AUX_SOURCE_DIRECTORY(common SOURCE_COMMON)

AUX_SOURCE_DIRECTORY(trade SOURCE_TRADE)
AUX_SOURCE_DIRECTORY(quote SOURCE_QUOTE)
AUX_SOURCE_DIRECTORY(shell SOURCE_SHELL)
AUX_SOURCE_DIRECTORY(test SOURCE_TEST)


MESSAGE("EXECUTABLE_OUTPUT_PATH:${EXECUTABLE_OUTPUT_PATH}")

#默认链接搜索路径/usr/lib,/usr/lib64 (通过ldconfig -p|grep xxx查找)
link_directories(/usr/local/lib)
link_directories(${EXECUTABLE_OUTPUT_PATH}/lib)

FIND_LIBRARY(SLH slh_pack_recv ${EXECUTABLE_OUTPUT_PATH}/lib)
FIND_LIBRARY(FMT event_pthreads /lib64)
#FIND_LIBRARY(FMT libfmt.so.8 ${EXECUTABLE_OUTPUT_PATH}/lib NO_DEFAULT_PATH)
MESSAGE(STATUS,"find libs:${SLH}")


add_library (common SHARED ${SOURCE_COMMON} ${SOURCE_GATEWAY} )
add_executable(qts-trade  ${SOURCE_TRADE} ${SOURCE_STRATEGY})
add_executable(qts-quote ${SOURCE_QUOTE})
add_executable(qts-test ${SOURCE_TEST})




target_link_libraries(common  fmtlog fmt  ${SLH}  jsoncpp  sqlite3 event_pthreads event dl rt pthread)
target_link_libraries(qts-trade  common)
target_link_libraries(qts-quote common)
target_link_libraries(qts-test common)


