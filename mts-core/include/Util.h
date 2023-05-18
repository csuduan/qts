#pragma once
//#include "windows.h"
#include <string>
#include "iostream"
#include <ctime>
#include <vector>
#include "json/json.h"
#include <fstream>
#include <sys/time.h>
#include "fmtlog/fmtlog.h"
#include <iconv.h>
using namespace  std;

#define utf8(A) Util::g2u(A)


class Util {
public:
    bool static starts_with(const std::string& str, const std::string prefix) {
        return (str.rfind(prefix, 0) == 0);
    }

    bool static ends_With(const std::string& str, const std::string suffix) {
        if (suffix.length() > str.length()) { return false; }

        return (str.rfind(suffix) == (str.length() - suffix.length()));
    }

    static inline int getTime(timespec * time){
        //auto now=std::chrono::high_resolution_clock::now();
        //std::time_t tt = std::chrono::high_resolution_clock::to_time_t(now);
        //std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::high_resolution_clock::now().time_since_epoch()).count();
        clock_gettime(CLOCK_MONOTONIC,time);
        return 0;
    }
    static  string getDate(){
        auto now=std::chrono::high_resolution_clock::now();
        std::time_t tt = std::chrono::high_resolution_clock::to_time_t(now);
        char date[9]={0};
        strftime(date, sizeof(date), "%Y%m%d", localtime(&tt));
        return string(date);
    }
    ///格式化纳秒时间(不适合高性能场景,平均耗时3us)
    static string  formatTime(long ns){
        time_t rawtime = ns / 1000000000;
        char str_time[100] ;
        strftime(str_time, sizeof(str_time), "%Y%m%d %H%M%S", localtime(&rawtime));
        string time=str_time;
        return time;
    }
    //从文件读入到string里
    static string readFile(char * filename)
    {
        ifstream ifile(filename);
        ostringstream buf;
        char ch;
        while(buf&&ifile.get(ch))
            buf.put(ch);
        return buf.str();
    }
    static Json::Value loadJson(string file){
        Json::Reader reader ;
        Json::Value root ;
        ifstream in(file.data(),ios::binary);
        if(!in.is_open()){
             cout<< "can not open file " << file << endl ;
            exit(1);
        }
        reader.parse(in,root);
        in.close();
        return root;
    }
    static void split(const string& s, vector<string>& tokens, const string& delimiters = " ")
    {
        string::size_type lastPos = s.find_first_not_of(delimiters, 0);
        string::size_type pos = s.find_first_of(delimiters, lastPos);
        while (string::npos != pos || string::npos != lastPos) {
            tokens.push_back(s.substr(lastPos, pos - lastPos));//use emplace_back after C++11
            lastPos = s.find_first_not_of(delimiters, pos);
            pos = s.find_first_of(delimiters, lastPos);
        }
    }
    static void trim(std::string &s){
        if(s.empty())
            return;

        //去除空格
        s.erase(0, s.find_first_not_of(" "));
        s.erase(s.find_last_not_of(" ")+1);

        //去除换行符
        s.erase(0, s.find_first_not_of("\n"));
        s.erase(s.find_last_not_of("\n")+1);
    }


    static double delaynsec(timespec * begin, timespec *end)
    {
        return (end->tv_sec * 1000000000 + end->tv_nsec ) -
               (begin->tv_sec * 1000000000 + begin->tv_nsec);
    }
    static void toCString(const std::vector<std::string>& source, char** destination)
    {
        // 注意释放内存
        for (int n = 0; n < static_cast<int>(source.size()); n++)
        {
            destination[n] = new char[32];
            destination[n][31] = '\0';
            strncpy(destination[n], source[n].c_str(), 31);
        }
        //destination[source.size()] = NULL;
    }



    /*static LPCWSTR stringToLPCWSTR(std::string orig)
    {
        size_t origsize = orig.length() + 1;
        const size_t newsize = 100;
        size_t convertedChars = 0;
        wchar_t* wcstring = (wchar_t*)malloc(sizeof(wchar_t) * (orig.length() - 1));
        mbstowcs_s(&convertedChars, wcstring, origsize, orig.c_str(), _TRUNCATE);

        return wcstring;
    }

    static std::string UnicodeToUtf8(const std::wstring& strUnicode)
    {
        int len = WideCharToMultiByte(CP_UTF8, 0, strUnicode.c_str(), -1, NULL, 0, NULL, NULL);
        if (len == 0)
        {
            return "";
        }

        char* pRes = new char[len];
        if (pRes == NULL)
        {
            return "";
        }

        WideCharToMultiByte(CP_UTF8, 0, strUnicode.c_str(), -1, pRes, len, NULL, NULL);
        pRes[len - 1] = '\0';
        std::string result = pRes;
        delete[] pRes;

        return result;
    }
    static std::wstring Utf8ToUnicode(const std::string& strUTF8)
    {
        int len = MultiByteToWideChar(CP_UTF8, 0, strUTF8.c_str(), -1, NULL, 0);
        if (len == 0)
        {
            return L"";
        }

        wchar_t* pRes = new wchar_t[len];
        if (pRes == NULL)
        {
            return L"";
        }

        MultiByteToWideChar(CP_UTF8, 0, strUTF8.c_str(), -1, pRes, len);
        pRes[len - 1] = L'\0';
        std::wstring result = pRes;
        delete[] pRes;

        return result;
    }


    static std::wstring StringToWString(const std::string& str)
    {
        int len = MultiByteToWideChar(CP_ACP, 0, str.c_str(), -1, NULL, 0);
        if (len == 0)
        {
            return L"";
        }

        wchar_t* pRes = new wchar_t[len];
        if (pRes == NULL)
        {
            return L"";
        }

        MultiByteToWideChar(CP_ACP, 0, str.c_str(), -1, pRes, len);
        pRes[len - 1] = L'\0';
        std::wstring result = pRes;
        delete[] pRes;

        return result;
    }

    static std::string WStringToString(const std::wstring& wstr)
    {
        int len = WideCharToMultiByte(CP_ACP, 0, wstr.c_str(), -1, NULL, 0, NULL, NULL);
        if (len == 0)
        {
            return "";
        }

        char* pRes = new char[len];
        if (pRes == NULL)
        {
            return "";
        }

        WideCharToMultiByte(CP_ACP, 0, wstr.c_str(), -1, pRes, len, NULL, NULL);
        pRes[len - 1] = '\0';
        std::string result = pRes;
        delete[] pRes;

        return result;

    }*/

    // 编码转换
    static int code_convert(char *from_charset,char *to_charset,char *inbuf,size_t inlen,char *outbuf,size_t outlen)
    {
        iconv_t cd;
        int rc;
        char **pin = &inbuf;
        char **pout = &outbuf;

        cd = iconv_open(to_charset,from_charset);
        if (cd==0) return -1;
        if (iconv(cd,pin,&inlen,pout,&outlen)==-1) return -1;
        iconv_close(cd);
        return 0;
    }

    // gbk转为utf8
    static string g2u(const char *inbuf)
    {
        int inlen=strlen(inbuf);
        string strRet;
        strRet.resize(inlen*2+2);
        if(code_convert("gbk","utf-8",const_cast<char *>(inbuf),inlen,&strRet[0],strRet.size()))
            return inbuf;
        return strRet;
    }



};
