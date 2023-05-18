project(trade-engine)
# 生成链接库
#set(LIBRARY_OUTPUT_PATH ${EXECUTABLE_OUTPUT_PATH})

#add_compile_options(-fPIC)



AUX_SOURCE_DIRECTORY(gateway SOURCE_GATEWAY)
AUX_SOURCE_DIRECTORY(strategy SOURCE_STRATEGY)
AUX_SOURCE_DIRECTORY(common SOURCE_COMMON)


AUX_SOURCE_DIRECTORY(master SOURCE_MASTER)
AUX_SOURCE_DIRECTORY(trade SOURCE_TRADE)
AUX_SOURCE_DIRECTORY(shell SOURCE_SHELL)
AUX_SOURCE_DIRECTORY(test SOURCE_TEST)




link_directories(${EXECUTABLE_OUTPUT_PATH}/lib)
#FIND_LIBRARY(SLH slh_pack_recv.a ${EXECUTABLE_OUTPUT_PATH}/lib)
#FIND_LIBRARY(EV libevent.so ${EXECUTABLE_OUTPUT_PATH}/lib)
#FIND_LIBRARY(FMT libfmt.so.8 ${EXECUTABLE_OUTPUT_PATH}/lib NO_DEFAULT_PATH)
#MESSAGE(STATUS,"find libs:${EV}")


add_library (common SHARED ${SOURCE_COMMON} ${SOURCE_GATEWAY} )
add_executable(mts-trade  ${SOURCE_TRADE} ${SOURCE_STRATEGY})
#add_executable(mts-master  ${SOURCE_MASTER})
add_executable(mts-shell ${SOURCE_SHELL})
#add_executable(mts-test ${SOURCE_TEST})



target_link_libraries(common  fmtlog fmt  slh_pack_recv.a  jsoncpp  sqlite3 event dl rt pthread)
target_link_libraries(mts-trade  common)
#target_link_libraries(mts-master  common)
target_link_libraries(mts-shell common)
#target_link_libraries(mts-test common )


