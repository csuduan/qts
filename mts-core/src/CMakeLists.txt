project(trade-engine)
# 生成链接库
set(LIBRARY_OUTPUT_PATH ../lib)
#add_compile_options(-fPIC)


AUX_SOURCE_DIRECTORY(engine SOURCE_ENGINE)
AUX_SOURCE_DIRECTORY(gateway SOURCE_GATEWAY)
AUX_SOURCE_DIRECTORY(strategy SOURCE_STRATEGY)
AUX_SOURCE_DIRECTORY(util SOURCE_UTIL)


link_directories(./ost)
add_library (${PROJECT_NAME} SHARED
        ${SOURCE_ENGINE}
        ${SOURCE_GATEWAY}
        ${SOURCE_STRATEGY}
        ${SOURCE_UTIL}
        )

#TARGET_LINK_LIBRARIES(${PROJECT_NAME}  utapi)