-- 设置项目名称
set_project("3rdparty")

-- 设置编译语言
set_languages("c++17")

-- 设置目标输出目录
set_targetdir("build")
add_cxxflags("-Wno-write-strings", "-Wno-return-type")

-- 创建 fmtlog 库目标
target("fmtlog")
    set_kind("shared")
    add_files("src/fmtlog/*.cpp")
    add_includedirs("src/fmtlog")  -- 添加头文件目录
    -- set_targetdir("build/fmtlog")  -- 可选：设置目标输出目录

-- 创建 jsoncpp 库目标
target("jsoncpp")
    set_kind("shared")  -- 也可以选择 shared
    add_files("src/jsoncpp/*.cpp")
    add_includedirs("src/jsoncpp")  -- 添加头文件目录
    -- set_targetdir("build/jsoncpp")  -- 可选：设置目标输出目录
