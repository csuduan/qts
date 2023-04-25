project(trade-engine)
# 生成链接库
#set(LIBRARY_OUTPUT_PATH ${EXECUTABLE_OUTPUT_PATH}/lib)

#add_compile_options(-fPIC)


AUX_SOURCE_DIRECTORY(gateway SOURCE_GATEWAY)
AUX_SOURCE_DIRECTORY(strategy SOURCE_STRATEGY)
AUX_SOURCE_DIRECTORY(common SOURCE_COMMON)


AUX_SOURCE_DIRECTORY(master SOURCE_MASTER)
AUX_SOURCE_DIRECTORY(trade SOURCE_TRADE)
AUX_SOURCE_DIRECTORY(shell SOURCE_SHELL)
AUX_SOURCE_DIRECTORY(test SOURCE_TEST)




#link_directories(${CMAKE_BINARY_DIR}/lib)
FIND_LIBRARY(SLH slh_pack_recv.a ${EXECUTABLE_OUTPUT_PATH}/lib)
FIND_LIBRARY(FMT fmt /usr/local/lib)
#MESSAGE(STATUS,"this var key = ${SLH}")


add_library (common SHARED ${SOURCE_COMMON} ${SOURCE_GATEWAY} )
add_executable(mts-trade  ${SOURCE_TRADE} ${SOURCE_STRATEGY})
add_executable(mts-master  ${SOURCE_MASTER})
add_executable(mts-shell ${SOURCE_SHELL})
add_executable(mts-test ${SOURCE_TEST})



target_link_libraries(common  fmtlog ${FMT} ${SLH} jsoncpp  sqlite3 dl rt pthread event)
target_link_libraries(mts-trade  common)
target_link_libraries(mts-master  common)
target_link_libraries(mts-shell common)
target_link_libraries(mts-test common )


