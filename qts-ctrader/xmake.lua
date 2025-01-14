-- 设置项目名称
set_project("qts-trader")

-- 设置 C++ 语言标准为 C++17
set_languages("c++17")

-- 添加编译选项
add_cxxflags("-Wno-write-strings", "-Wno-return-type")

-- 定义宏
add_defines("USE_SF")

-- 设置编译模式
set_mode("debug")  -- 或者 "release" 视具体需求而定

-- 设置目标文件输出目录
set_targetdir(path.join(os.projectdir(), "build"))

-- 设置库文件输出目录
set_rpathdir(path.join(os.targetdir(), "lib"))

-- 添加依赖的第三方库路径
add_includedirs("3rdparty/include", "include", "src")


-- 添加子模块(或者使用includes)
add_subdirs("3rdparty/src", "src")
-- 添加第三方库和源代码的子目录
-- includes("3rdparty/src")
-- includes("src")




-- 复制文件
before_build(function (target)
    os.cp("conf", path.join(os.targetdir(), "conf"))
    os.cp("3rdparty/lib", path.join(os.targetdir(), "lib"))
end)

