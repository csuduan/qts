#pragma once
#include <iostream>
using namespace std;


class Log
{

public:
	Log();
	//template<typename ... Args>
	//string string_format(const string& format, Args ... args) {
	//	size_t size = 1 + snprintf(nullptr, 0, format.c_str(), args ...);  // Extra space for \0
	//	// unique_ptr<char[]> buf(new char[size]);
	//	char bytes[size];
	//	snprintf(bytes, size, format.c_str(), args ...);
	//	return string(bytes);
	//}

	template<typename ... Args>
	void  debug(string msg, Args ... args)
	{
		
		char bytes[1024];
		snprintf(bytes, sizeof(bytes), msg.c_str(), args ...);
		cout << "[debug] " << bytes << endl;
	}

	template<typename ... Args>
	void info(string msg, Args ... args)
	{
		char bytes[1024];
		snprintf(bytes, sizeof(bytes), msg.c_str(), args ...);
		cout << "[info] " << bytes << endl;
	}
	template<typename ... Args>
	void warn(string msg, Args ... args)
	{
		char bytes[1024];
		snprintf(bytes, sizeof(bytes), msg.c_str(), args ...);
		cout << "[warn] " << bytes << endl;
	}

	template<typename ... Args>
	void error(string msg, Args ... args)
	{
		char bytes[1024];
		snprintf(bytes, sizeof(bytes), msg.c_str(), args ...);
		cout << "[error] " << bytes << endl;
	}


};



class Logger {
public:
	static Log getLogger()
	{
		return log;
	}
private:
	static Log log;
};



