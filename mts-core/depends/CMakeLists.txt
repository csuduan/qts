project(depends)
set(LIBRARY_OUTPUT_PATH ../lib)


AUX_SOURCE_DIRECTORY(jsoncpp SOURCE_JSONCPP)

add_library (jsoncpp  ${SOURCE_JSONCPP})
