-- 设置项目名称
set_project("trade-engine")

-- 设置编译语言
set_languages("c++17")

-- 设置目标输出目录
set_targetdir("build")

-- 编译选项
add_cxxflags("-fPIC")

-- 自动添加源文件
-- 通过os.files递归收集每个目录的源文件

-- 收集各个子目录的源文件
local source_gateway = os.files("gateway/*.cpp")
local source_strategy = os.files("strategy/*.cpp")
local source_common = os.files("common/*.cpp")
local source_trade = os.files("trade/*.cpp")
local source_quote = os.files("quote/*.cpp")
local source_shell = os.files("shell/*.cpp")
local source_test = os.files("test/*.cpp")

-- 输出路径消息（可选）
message("EXECUTABLE_OUTPUT_PATH: " .. get_targetdir())

-- 添加库的链接目录
add_linkdirs("/usr/local/lib", get_targetdir() .. "/lib")

-- 查找库（例如slh_pack_recv、event_pthreads等）
local slh = find_library("slh_pack_recv", {path = get_targetdir() .. "/lib"})
local fmt = find_library("event_pthreads", {path = "/lib64"})

message("find libs: " .. slh)

-- 创建 shared 库目标 'common'
target("common")
    set_kind("shared")
    add_files(source_common, source_gateway)
    add_includedirs("common/include")  -- 添加头文件目录
    add_linkdirs("/usr/local/lib")
    add_links("fmtlog", "fmt", "jsoncpp", "sqlite3", "event_pthreads", "event", "dl", "rt", "pthread", slh)

-- 创建可执行文件 'qts-trade'
target("qts-trade")
    set_kind("binary")
    add_files(source_trade, source_strategy)
    add_deps("common")  -- qts-trade依赖common库
    add_linkdirs("/usr/local/lib")

-- 创建可执行文件 'qts-quote'
target("qts-quote")
    set_kind("binary")
    add_files(source_quote)
    add_deps("common")  -- qts-quote依赖common库
    add_linkdirs("/usr/local/lib")

-- 创建可执行文件 'qts-test'
target("qts-test")
    set_kind("binary")
    add_files(source_test)
    add_deps("common")  -- qts-test依赖common库
    add_linkdirs("/usr/local/lib")
